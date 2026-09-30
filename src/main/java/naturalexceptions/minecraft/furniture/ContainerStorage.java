package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** A chest-like container for any furniture inventory whose size is a multiple of nine; slots respect what it takes. */
public final class ContainerStorage extends Container {

    private final IInventory storage;
    private final int rows;

    public ContainerStorage(IInventory player, IInventory storage) {
        this.storage = storage;
        this.rows = storage.getSizeInventory() / 9;
        storage.openInventory();
        int shift = (rows - 4) * 18;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(storage, c + r * 9, 8 + c * 18, 18 + r * 18) {

                @Override
                public boolean isItemValid(ItemStack stack) {
                    return inventory.isItemValidForSlot(getSlotIndex(), stack);
                }
            });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlotToContainer(new Slot(player, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + shift));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(player, c, 8 + c * 18, 161 + shift));
    }

    public int rows() {
        return rows;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return storage.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        int own = rows * 9;
        if (index < own) {
            if (!mergeItemStack(stack, own, inventorySlots.size(), true)) return null;
        } else {
            if (!storage.isItemValidForSlot(0, stack) || !mergeItemStack(stack, 0, own, false)) return null;
        }
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        return copy;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        storage.closeInventory();
    }
}
