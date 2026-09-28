package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class ContainerMoldAssemblyTable extends Container {

    private final TileEntityMoldAssemblyTable table;

    public ContainerMoldAssemblyTable(InventoryPlayer player, TileEntityMoldAssemblyTable table) {
        this.table = table;
        addSlotToContainer(new Slot(table, 0, MoldAssemblyLayout.INPUT_X, MoldAssemblyLayout.MACHINE_Y) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return table.isItemValidForSlot(0, stack);
            }
        });
        addSlotToContainer(new Slot(table, 1, MoldAssemblyLayout.OUTPUT_X, MoldAssemblyLayout.MACHINE_Y) {

            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlotToContainer(
            new Slot(player, col + row * 9 + 9, 8 + col * 18, MoldAssemblyLayout.PLAYER_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player, col, 8 + col * 18, MoldAssemblyLayout.HOTBAR_Y));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return table.isUseableByPlayer(player);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int button) {
        return canInteractWith(player) && table.shape(button);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return null;
        Slot slot = (Slot) inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!mergeItemStack(stack, 2, inventorySlots.size(), true)) return null;
        } else if (!moveBlankMoldToInput(stack)) {
            return null;
        }
        if (stack.stackSize == original.stackSize) return null;
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    private boolean moveBlankMoldToInput(ItemStack source) {
        if (!table.isItemValidForSlot(0, source)) return false;
        Slot inputSlot = (Slot) inventorySlots.get(0);
        ItemStack input = inputSlot.getStack();
        int limit = Math.min(source.getMaxStackSize(), inputSlot.getSlotStackLimit());
        if (input == null) {
            int moved = Math.min(source.stackSize, limit);
            if (moved <= 0) return false;
            ItemStack inserted = source.copy();
            inserted.stackSize = moved;
            inputSlot.putStack(inserted);
            if (inputSlot.getStack() == null) return false;
            source.stackSize -= moved;
            return true;
        }
        if (!table.isItemValidForSlot(0, input) || input.getItem() != source.getItem()
            || input.getItemDamage() != source.getItemDamage()
            || !ItemStack.areItemStackTagsEqual(input, source)) return false;
        int moved = Math.min(source.stackSize, limit - input.stackSize);
        if (moved <= 0) return false;
        input.stackSize += moved;
        source.stackSize -= moved;
        inputSlot.onSlotChanged();
        return true;
    }
}
