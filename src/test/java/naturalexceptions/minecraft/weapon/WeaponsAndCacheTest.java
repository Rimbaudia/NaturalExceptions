package naturalexceptions.minecraft.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.worldgen.SkyCacheLoot;

final class WeaponsAndCacheTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void oneShardLoadsEightBolts() {
        assertNull(ShardcasterCharge.fire(0, false), "empty and no shard: nothing fires");
        int[] loaded = ShardcasterCharge.fire(0, true);
        assertArrayEquals(new int[] { ShardcasterCharge.SHOTS_PER_SHARD - 1, 1 }, loaded, "a shard is used");
        int charge = loaded[0], shards = loaded[1], shots = 1;
        while (charge > 0) {
            int[] next = ShardcasterCharge.fire(charge, true);
            charge = next[0];
            shards += next[1];
            shots++;
        }
        assertEquals(ShardcasterCharge.SHOTS_PER_SHARD, shots);
        assertEquals(1, shards, "eight shots from one shard");
    }

    @Test
    void skyfallStarAcceleratesOntoItsTarget() {
        assertEquals(0.0, EntitySkyfallStar.progress(0), 1e-9);
        assertEquals(1.0, EntitySkyfallStar.progress(EntitySkyfallStar.FLIGHT_TICKS), 1e-9);
        double early = EntitySkyfallStar.progress(2) - EntitySkyfallStar.progress(1);
        double late = EntitySkyfallStar.progress(EntitySkyfallStar.FLIGHT_TICKS)
            - EntitySkyfallStar.progress(EntitySkyfallStar.FLIGHT_TICKS - 1);
        assertTrue(late > early * 3, "falls faster as it comes down");
    }

    @Test
    void everySkyCacheHoldsExactlyOneTreasureTheSameEachTime() {
        Set<Item> pool = new HashSet<>(SkyCacheLoot.treasures());
        assertEquals(7, pool.size(), "five wearables and two weapons");
        Set<Item> seen = new HashSet<>();
        for (long seed = 0; seed < 60; seed++) {
            InventoryBasic chest = new InventoryBasic("cache", false, 27);
            SkyCacheLoot.fill(chest, seed);
            int treasures = 0;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                ItemStack stack = chest.getStackInSlot(i);
                if (stack != null && pool.contains(stack.getItem())) {
                    treasures++;
                    seen.add(stack.getItem());
                }
            }
            assertEquals(1, treasures, "seed " + seed);
            InventoryBasic again = new InventoryBasic("cache", false, 27);
            SkyCacheLoot.fill(again, seed);
            for (int i = 0; i < chest.getSizeInventory(); i++)
                assertTrue(ItemStack.areItemStacksEqual(chest.getStackInSlot(i), again.getStackInSlot(i)), "same loot");
        }
        assertEquals(pool, seen, "every treasure turns up somewhere");
    }
}
