package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * What a citadel's pantries and market stalls hold when found: a few common, everyday things, never treasure. A stall
 * sells what its counter shows (produce, fish, pottery, cloth); a pantry keeps a little food.
 */
public final class Wares {

    private Wares() {}

    private static final Object[][] PRODUCE = { { Items.apple, 2, 6 }, { Items.carrot, 3, 8 }, { Items.potato, 3, 8 },
        { Items.bread, 1, 4 }, { Items.wheat, 4, 12 }, { Items.melon, 3, 8 }, { Items.pumpkin_pie, 1, 2 } };
    private static final Object[][] FISH = { { Items.fish, 2, 6 }, { Items.cooked_fished, 1, 4 },
        { Items.string, 2, 5 }, { Items.fishing_rod, 1, 1 } };
    private static final Object[][] POTTERY = { { Items.flower_pot, 1, 3 }, { Items.bowl, 2, 6 },
        { Items.brick, 4, 10 }, { Items.clay_ball, 4, 10 }, { Items.glass_bottle, 2, 5 } };
    private static final Object[][] CLOTH = { { Blocks.wool, 2, 6 }, { Items.string, 3, 8 }, { Items.leather, 1, 4 },
        { Blocks.carpet, 2, 4 }, { Items.dye, 2, 5 } };
    private static final Object[][] PANTRY = { { Items.bread, 1, 3 }, { Items.apple, 1, 3 },
        { Items.baked_potato, 1, 3 }, { Items.cooked_fished, 1, 2 }, { Items.cookie, 2, 6 }, { Items.carrot, 1, 4 } };

    /** Sets out two to four kinds of the stall's goods, the first slots first (the counter shows those). */
    public static void stockCounter(IInventory counter, int goods, long seed) {
        Object[][] table = goods == 1 ? FISH : goods == 2 ? POTTERY : goods == 3 ? CLOTH : PRODUCE;
        fill(counter, table, 2 + new Random(seed).nextInt(3), seed);
    }

    /** A little food: one to three kinds. */
    public static void stockPantry(IInventory shelf, long seed) {
        fill(shelf, PANTRY, 1 + new Random(seed).nextInt(3), seed);
    }

    private static void fill(IInventory inventory, Object[][] table, int kinds, long seed) {
        Random random = new Random(seed ^ 0x5DEECE66DL);
        for (int slot = 0; slot < Math.min(kinds, inventory.getSizeInventory()); slot++) {
            Object[] ware = table[random.nextInt(table.length)];
            int least = (Integer) ware[1], most = (Integer) ware[2];
            int count = least + random.nextInt(most - least + 1);
            Item item = ware[0] instanceof Item ? (Item) ware[0]
                : Item.getItemFromBlock((net.minecraft.block.Block) ware[0]);
            int meta = ware[0] == Blocks.wool || ware[0] == Blocks.carpet || ware[0] == Items.dye ? random.nextInt(16)
                : 0;
            inventory.setInventorySlotContents(slot, new ItemStack(item, count, meta));
        }
    }
}
