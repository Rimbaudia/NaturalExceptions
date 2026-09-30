package naturalexceptions.minecraft.item;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A Glider Cloak's colour: the colour of the wool it was sewn from, kept on the item as that wool's metadata
 * (0 white .. 15 black). A cloak with none is white.
 */
public final class GliderCloaks {

    public static final String[] NAMES = { "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
        "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black" };
    private static final String KEY = "Colour";

    private GliderCloaks() {}

    public static int colour(ItemStack cloak) {
        if (cloak == null || !cloak.hasTagCompound()) return 0;
        return cloak.getTagCompound()
            .getInteger(KEY) & 15;
    }

    public static ItemStack withColour(ItemStack cloak, int wool) {
        if (!cloak.hasTagCompound()) cloak.setTagCompound(new NBTTagCompound());
        cloak.getTagCompound()
            .setInteger(KEY, wool & 15);
        return cloak;
    }

    /** The colour as RGB, from the dye of that colour. */
    public static int rgb(int wool) {
        return ItemDye.field_150922_c[15 - (wool & 15)];
    }

    /** The colour most of the grid's wool is; on a tie, the first such wool in the grid. */
    public static int woolColour(InventoryCrafting grid) {
        int[] counts = new int[16];
        int first = -1;
        Item wool = Item.getItemFromBlock(Blocks.wool);
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack stack = grid.getStackInSlot(i);
            if (stack == null || stack.getItem() != wool) continue;
            int meta = stack.getItemDamage() & 15;
            counts[meta]++;
            if (first < 0) first = meta;
        }
        if (first < 0) return 0;
        int best = first;
        for (int meta = 0; meta < 16; meta++) if (counts[meta] > counts[best]) best = meta;
        return best;
    }
}
