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

    private static final Object[][] SHELF = { { Items.book, 1, 2 }, { Items.bowl, 1, 3 }, { Items.glass_bottle, 1, 3 },
        { Items.flower_pot, 1, 1 }, { Items.paper, 1, 4 }, { Items.bread, 1, 2 }, { Items.string, 1, 3 } };
    private static final Object[][] RACK = { { Items.wooden_sword, 1, 1 }, { Items.stone_sword, 1, 1 },
        { Items.bow, 1, 1 }, { Items.stone_axe, 1, 1 }, { Items.wooden_axe, 1, 1 } };

    private static final Object[][] CRATE = { { Items.arrow, 8, 24 }, { Items.string, 3, 8 }, { Items.flint, 2, 6 },
        { Items.coal, 3, 10 }, { Blocks.torch, 4, 12 }, { Items.leather, 1, 4 }, { Blocks.wool, 2, 5 },
        { Items.bread, 2, 5 }, { Items.potato, 3, 8 }, { Items.wheat, 4, 10 }, { Items.feather, 2, 8 },
        { Items.paper, 2, 6 }, { Items.bone, 2, 6 }, { Items.iron_ingot, 1, 3 } };
    /** Now and then a crate holds something of the mod's own: {item supplier index, least, most}. */
    private static final int[][] CRATE_EXTRAS = { { 0, 1, 2 }, { 1, 1, 3 }, { 2, 1, 3 } };
    private static final Object[][] BARREL = { { Items.apple, 12, 32 }, { Items.carrot, 16, 40 },
        { Items.potato, 16, 40 }, { Items.sugar, 8, 24 }, { Items.wheat_seeds, 16, 48 }, { Items.wheat, 12, 32 } };

    /** A crate's supplies: three to six kinds, and one time in four a bandage, copper or Stormglass besides. */
    public static void stockCrate(IInventory crate, long seed) {
        Random random = new Random(seed ^ 0x9E3779B9L);
        fill(crate, CRATE, 3 + random.nextInt(4), seed);
        if (random.nextInt(4) != 0) return;
        int[] extra = CRATE_EXTRAS[random.nextInt(CRATE_EXTRAS.length)];
        Item item = extra[0] == 0 ? naturalexceptions.minecraft.registry.NEItems.bandage
            : extra[0] == 1 ? naturalexceptions.minecraft.registry.NEItems.copperIngot
                : naturalexceptions.minecraft.registry.NEItems.stormglassShard;
        if (item == null) return;
        for (int slot = 0; slot < crate.getSizeInventory(); slot++) if (crate.getStackInSlot(slot) == null) {
            crate.setInventorySlotContents(
                slot,
                new ItemStack(item, extra[1] + random.nextInt(extra[2] - extra[1] + 1)));
            return;
        }
    }

    /** A dry barrel's store: one kind of produce, by the dozen. */
    public static ItemStack barrelStore(long seed) {
        Random random = new Random(seed ^ 0x7F4A7C15L);
        Object[] ware = BARREL[random.nextInt(BARREL.length)];
        int least = (Integer) ware[1], most = (Integer) ware[2];
        return new ItemStack((Item) ware[0], least + random.nextInt(most - least + 1));
    }

    /** A shelf's odds and ends: none to two. */
    public static void stockShelf(IInventory shelf, long seed) {
        fill(shelf, SHELF, new Random(seed).nextInt(3), seed);
    }

    /** A rack's worn weapons and tools, one or two, a little used. */
    public static void stockRack(IInventory rack, long seed) {
        Random random = new Random(seed ^ 0x2545F491L);
        int kinds = 1 + random.nextInt(2);
        for (int slot = 0; slot < kinds; slot++) {
            Item item = (Item) RACK[random.nextInt(RACK.length)][0];
            ItemStack stack = new ItemStack(item);
            stack.setItemDamage(item.getMaxDamage() / 3 + random.nextInt(Math.max(1, item.getMaxDamage() / 3)));
            rack.setInventorySlotContents(slot, stack);
        }
    }

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
