package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** What a dead household left: never a Sky Key, rarely a treasure, never nothing. */
final class HouseholdLootTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static InventoryBasic chest(long seed) {
        InventoryBasic chest = new InventoryBasic("household", false, 27);
        SkyCacheLoot.fill(chest, seed, SkyCacheLoot.Tier.HOUSEHOLD);
        return chest;
    }

    @Test
    void neverASkyKeyRarelyATreasureNeverEmpty() {
        Set<Item> treasures = new HashSet<>(SkyCacheLoot.treasures());
        int withTreasure = 0, n = 2000;
        for (long seed = 1; seed <= n; seed++) {
            InventoryBasic chest = chest(seed * 31L);
            boolean any = false, treasure = false;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                ItemStack stack = chest.getStackInSlot(i);
                if (stack == null) continue;
                any = true;
                assertTrue(stack.getItem() != NEItems.skyKey, "a household never holds a Sky Key (seed " + seed + ")");
                if (treasures.contains(stack.getItem())) treasure = true;
            }
            assertTrue(any, "a household chest is never empty (seed " + seed + ")");
            if (treasure) withTreasure++;
        }
        double share = withTreasure / (double) n;
        assertTrue(share > 0.002 && share < 0.03, "treasure in about 1 household chest in 80, was " + share);
    }

    @Test
    void theSameChestFillsTheSameWay() {
        for (long seed = 1; seed <= 50; seed++) {
            InventoryBasic a = chest(seed), b = chest(seed);
            for (int i = 0; i < a.getSizeInventory(); i++)
                assertEquals(String.valueOf(a.getStackInSlot(i)), String.valueOf(b.getStackInSlot(i)));
        }
    }
}
