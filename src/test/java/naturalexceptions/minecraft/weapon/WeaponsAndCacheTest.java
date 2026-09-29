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

    private static int[] count(InventoryBasic chest, Set<Item> pool) {
        int treasures = 0, vanilla = 0, other = 0;
        for (int i = 0; i < chest.getSizeInventory(); i++) {
            ItemStack stack = chest.getStackInSlot(i);
            if (stack == null) continue;
            if (pool.contains(stack.getItem())) treasures++;
            else if (Item.itemRegistry.getNameForObject(stack.getItem())
                .startsWith("minecraft:")) vanilla++;
            else other++;
        }
        return new int[] { treasures, vanilla, other };
    }

    private static InventoryBasic filled(long seed, SkyCacheLoot.Tier tier) {
        InventoryBasic chest = new InventoryBasic("cache", false, 27);
        SkyCacheLoot.fill(chest, seed, tier);
        return chest;
    }

    @Test
    void islandCachesAreOrdinaryLootAndNeverATreasure() {
        Set<Item> pool = new HashSet<>(SkyCacheLoot.treasures());
        assertEquals(7, pool.size(), "five wearables and two weapons");
        int vanilla = 0, other = 0;
        for (long seed = 0; seed < 200; seed++) {
            int[] c = count(filled(seed, SkyCacheLoot.Tier.ISLAND), pool);
            assertEquals(0, c[0], "no treasure in an island cache, seed " + seed);
            vanilla += c[1];
            other += c[2];
            InventoryBasic a = filled(seed, SkyCacheLoot.Tier.ISLAND), b = filled(seed, SkyCacheLoot.Tier.ISLAND);
            for (int i = 0; i < a.getSizeInventory(); i++)
                assertTrue(ItemStack.areItemStacksEqual(a.getStackInSlot(i), b.getStackInSlot(i)), "same loot");
        }
        assertTrue(vanilla > 3 * other, "mostly Minecraft items: " + vanilla + " vs " + other);
    }

    @Test
    void storeroomsRarelyHoldATreasureAndTheVaultAlwaysDoes() {
        Set<Item> pool = new HashSet<>(SkyCacheLoot.treasures());
        int stores = 0;
        for (long seed = 0; seed < 2000; seed++) stores += count(filled(seed, SkyCacheLoot.Tier.STORE), pool)[0];
        assertTrue(stores >= 60 && stores <= 150, "about one storeroom in twenty: " + stores + " / 2000");
        Set<Item> seen = new HashSet<>();
        for (long seed = 0; seed < 80; seed++) {
            InventoryBasic vault = filled(seed, SkyCacheLoot.Tier.VAULT_TREASURE);
            assertEquals(1, count(vault, pool)[0], "the vault holds one treasure, seed " + seed);
            for (int i = 0; i < vault.getSizeInventory(); i++) if (vault.getStackInSlot(i) != null && pool.contains(
                vault.getStackInSlot(i)
                    .getItem()))
                seen.add(
                    vault.getStackInSlot(i)
                        .getItem());
            assertEquals(0, count(filled(seed, SkyCacheLoot.Tier.VAULT_RICH), pool)[0], "riches, not treasure");
        }
        assertEquals(pool, seen, "every treasure turns up somewhere");
    }

    @Test
    void theKeyRoomHoldsTheSkyKey() {
        for (long seed = 0; seed < 20; seed++) {
            InventoryBasic chest = filled(seed, SkyCacheLoot.Tier.KEY);
            int keys = 0;
            for (int i = 0; i < chest.getSizeInventory(); i++)
                if (chest.getStackInSlot(i) != null && chest.getStackInSlot(i)
                    .getItem() == NEItems.skyKey) keys++;
            assertEquals(1, keys);
        }
    }
}
