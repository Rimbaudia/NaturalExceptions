package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;

/** A pantry shelf: nine places for food, and only food. A meal taken from it is a proper one ({@link Meals}). */
public final class TileEntityPantryShelf extends TileEntityStorage {

    public TileEntityPantryShelf() {
        super(9, "container.naturalexceptions.pantryShelf");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFood;
    }

    /** The first food on the shelf, or -1. */
    public int firstFood() {
        for (int i = 0; i < getSizeInventory(); i++) if (getStackInSlot(i) != null) return i;
        return -1;
    }
}
