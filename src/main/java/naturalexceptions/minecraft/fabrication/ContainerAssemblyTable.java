package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.recipe.RecipeToolAssembly;

/**
 * Server-authoritative inventory container for the Assembly Table.
 */
public final class ContainerAssemblyTable extends Container {

    public static final int HEAD_SLOT_X = 48;
    public static final int HANDLE_SLOT_X = 76;
    public static final int RESULT_SLOT_X = 134;
    public static final int RECIPE_SLOT_Y = 35;

    private final TileEntityAssemblyTable tileEntity;

    public ContainerAssemblyTable(InventoryPlayer playerInv, TileEntityAssemblyTable tileEntity) {
        this.tileEntity = tileEntity;

        // Slot 0: Head Component
        addSlotToContainer(new SlotHead(tileEntity, TileEntityAssemblyTable.SLOT_HEAD, HEAD_SLOT_X, RECIPE_SLOT_Y));

        // Slot 1: Handle Component
        addSlotToContainer(
            new SlotHandle(tileEntity, TileEntityAssemblyTable.SLOT_HANDLE, HANDLE_SLOT_X, RECIPE_SLOT_Y));

        // Slot 2: Result Tool
        addSlotToContainer(
            new SlotResult(tileEntity, TileEntityAssemblyTable.SLOT_RESULT, RESULT_SLOT_X, RECIPE_SLOT_Y));

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public TileEntityAssemblyTable tileEntity() {
        return tileEntity;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileEntity.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // Slot 2: Result slot shift-click
            if (slotIndex == TileEntityAssemblyTable.SLOT_RESULT) {
                ItemStack assembled = tileEntity.computeResult();
                if (assembled != null) {
                    if (!this.mergeItemStack(assembled, 3, this.inventorySlots.size(), true)) {
                        return null;
                    }
                    tileEntity.decrStackSize(TileEntityAssemblyTable.SLOT_RESULT, 1);
                    slot.onSlotChanged();
                    detectAndSendChanges();
                    return assembled;
                }
                return null;
            }

            // Machine slots (0, 1) to player inventory
            if (slotIndex < 3) {
                if (!this.mergeItemStack(itemstack1, 3, this.inventorySlots.size(), true)) {
                    return null;
                }
            } else {
                // Player inventory to machine slots
                if (RecipeToolAssembly.extractHead(itemstack1) != null) {
                    if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                        return null;
                    }
                } else if (RecipeToolAssembly.extractHandle(itemstack1) != null) {
                    if (!this.mergeItemStack(itemstack1, 1, 2, false)) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            if (itemstack1.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.stackSize == itemstack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, itemstack1);
        }

        return itemstack;
    }

    public static class SlotHead extends Slot {

        public SlotHead(IInventory inventory, int slotIndex, int x, int y) {
            super(inventory, slotIndex, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return RecipeToolAssembly.extractHead(stack) != null;
        }
    }

    public static class SlotHandle extends Slot {

        public SlotHandle(IInventory inventory, int slotIndex, int x, int y) {
            super(inventory, slotIndex, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return RecipeToolAssembly.extractHandle(stack) != null;
        }
    }

    public static class SlotResult extends Slot {

        public SlotResult(IInventory inventory, int slotIndex, int x, int y) {
            super(inventory, slotIndex, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }
}
