package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * Server-authoritative inventory container for the Starter Smelter.
 */
public final class ContainerStarterSmelter extends Container {

    private final TileEntityStarterSmelter tileEntity;

    public ContainerStarterSmelter(InventoryPlayer playerInv, TileEntityStarterSmelter tileEntity) {
        this.tileEntity = tileEntity;

        // Slot 0: Fuel (x=36, y=54)
        addSlotToContainer(new Slot(tileEntity, TileEntityStarterSmelter.SLOT_FUEL, 36, 54));

        // Slot 1: Feed (x=36, y=20)
        addSlotToContainer(new Slot(tileEntity, TileEntityStarterSmelter.SLOT_FEED, 36, 20));

        // Slot 2: Mold / Output (x=134, y=36)
        addSlotToContainer(new Slot(tileEntity, TileEntityStarterSmelter.SLOT_MOLD, 134, 36));

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

    public TileEntityStarterSmelter tileEntity() {
        return tileEntity;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileEntity.isUseableByPlayer(player);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int actionId) {
        if (actionId == 0) {
            // Action 0: Pour Metal
            ItemStack moldSlot = tileEntity.getStackInSlot(TileEntityStarterSmelter.SLOT_MOLD);
            if (moldSlot != null && moldSlot.getItem() instanceof ItemCastingMold
                && !ItemCastingMold.isFilled(moldSlot)) {
                if (tileEntity.pourIntoMold(moldSlot)) {
                    player.addChatMessage(new ChatComponentText("\u00a7aPoured molten metal into mold."));
                    detectAndSendChanges();
                    return true;
                } else {
                    player
                        .addChatMessage(new ChatComponentText("\u00a7cNo pourable liquid metal available in smelter."));
                    return false;
                }
            }

            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof ItemCastingMold && !ItemCastingMold.isFilled(held)) {
                if (tileEntity.pourIntoMold(held)) {
                    player.addChatMessage(new ChatComponentText("\u00a7aPoured molten metal into held mold."));
                    detectAndSendChanges();
                    return true;
                } else {
                    player
                        .addChatMessage(new ChatComponentText("\u00a7cNo pourable liquid metal available in smelter."));
                    return false;
                }
            }

            player.addChatMessage(
                new ChatComponentText("\u00a7eInsert an empty Casting Mold into the mold slot to pour metal."));
            return false;
        }

        if (actionId == 1) {
            // Action 1: Skim Slag
            ItemStack slag = tileEntity.skimSlag();
            if (slag != null) {
                ItemStack outputSlot = tileEntity.getStackInSlot(TileEntityStarterSmelter.SLOT_MOLD);
                if (outputSlot == null) {
                    tileEntity.setInventorySlotContents(TileEntityStarterSmelter.SLOT_MOLD, slag);
                } else
                    if (outputSlot.getItem() == slag.getItem() && outputSlot.stackSize < outputSlot.getMaxStackSize()) {
                        outputSlot.stackSize++;
                    } else {
                        if (!player.inventory.addItemStackToInventory(slag)) {
                            player.dropPlayerItemWithRandomChoice(slag, false);
                        }
                    }
                player.addChatMessage(new ChatComponentText("\u00a7eSkimmed molten slag from vessel surface."));
                detectAndSendChanges();
                return true;
            } else {
                player.addChatMessage(new ChatComponentText("\u00a77No slag layer present to skim."));
                return false;
            }
        }

        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // Machine slots (0, 1, 2) to player inventory
            if (slotIndex < TileEntityStarterSmelter.INVENTORY_SIZE) {
                if (!this.mergeItemStack(
                    itemstack1,
                    TileEntityStarterSmelter.INVENTORY_SIZE,
                    this.inventorySlots.size(),
                    true)) {
                    return null;
                }
            } else {
                // Player inventory to machine slots
                if (tileEntity.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FUEL, itemstack1)) {
                    if (!this.mergeItemStack(
                        itemstack1,
                        TileEntityStarterSmelter.SLOT_FUEL,
                        TileEntityStarterSmelter.SLOT_FUEL + 1,
                        false)) {
                        return null;
                    }
                } else if (tileEntity.isItemValidForSlot(TileEntityStarterSmelter.SLOT_MOLD, itemstack1)) {
                    if (!this.mergeItemStack(
                        itemstack1,
                        TileEntityStarterSmelter.SLOT_MOLD,
                        TileEntityStarterSmelter.SLOT_MOLD + 1,
                        false)) {
                        return null;
                    }
                } else if (tileEntity.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, itemstack1)) {
                    if (!this.mergeItemStack(
                        itemstack1,
                        TileEntityStarterSmelter.SLOT_FEED,
                        TileEntityStarterSmelter.SLOT_FEED + 1,
                        false)) {
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
        }

        return itemstack;
    }
}
