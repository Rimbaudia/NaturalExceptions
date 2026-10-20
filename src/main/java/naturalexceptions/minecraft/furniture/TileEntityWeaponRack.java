package naturalexceptions.minecraft.furniture;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

/** A weapon rack: three places, one weapon or tool in each. */
public final class TileEntityWeaponRack extends TileEntityStorage {

    public TileEntityWeaponRack() {
        super(3, "container.naturalexceptions.weaponRack");
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return isWeapon(stack);
    }

    /** Swords, tools, hoes, bows, and anything held like one (the mod's own weapons). */
    public static boolean isWeapon(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item instanceof ItemSword || item instanceof ItemTool
            || item instanceof ItemHoe
            || item instanceof ItemBow
            // Its field, not isFull3D(): that is client-only, and the rack is checked on the server too.
            || naturalexceptions.minecraft.SidedVanilla.isFull3D(item);
    }
}
