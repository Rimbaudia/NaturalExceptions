package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.registry.NEItems;

/** What a sky cache holds: a few useful supplies, rolled from the island's own seed so it is the same every time. */
public final class SkyCacheLoot {

    private SkyCacheLoot() {}

    public static void fill(IInventory chest, long seed) {
        Random random = new Random(seed);
        put(chest, random, NEItems.bandage, 2 + random.nextInt(3));
        put(chest, random, NEItems.copperIngot, 3 + random.nextInt(4));
        if (random.nextInt(10) < 3) put(chest, random, NEItems.exposureMask, 1);
    }

    private static void put(IInventory chest, Random random, Item item, int count) {
        if (item == null || count <= 0) return;
        for (int tries = 0; tries < 8; tries++) {
            int slot = random.nextInt(chest.getSizeInventory());
            if (chest.getStackInSlot(slot) == null) {
                chest.setInventorySlotContents(slot, new ItemStack(item, count));
                return;
            }
        }
    }
}
