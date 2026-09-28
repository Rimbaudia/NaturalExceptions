package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntityFurnace;

/**
 * Server-authoritative inventory container for the Starter Smelter.
 */
public final class ContainerStarterSmelter extends Container {

    private final TileEntityStarterSmelter tileEntity;

    public ContainerStarterSmelter(InventoryPlayer playerInv, TileEntityStarterSmelter tileEntity) {
        this.tileEntity = tileEntity;

        // Slot 0: Fuel (x=36, y=54)
        addSlotToContainer(new SlotFuel(tileEntity, TileEntityStarterSmelter.SLOT_FUEL, 36, 54));

        // Slot 1: Feed (x=36, y=20)
        addSlotToContainer(new SlotFeed(tileEntity, TileEntityStarterSmelter.SLOT_FEED, 36, 20));

        // Slot 2: Mold / Output (x=134, y=36)
        addSlotToContainer(new SlotOutput(tileEntity, TileEntityStarterSmelter.SLOT_MOLD, 134, 36));

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
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (Object crafter : this.crafters) {
            if (crafter instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) crafter;
                Packet pkt = tileEntity.getDescriptionPacket();
                if (pkt != null) {
                    player.playerNetServerHandler.sendPacket(pkt);
                }
            }
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int actionId) {
        // Liquid transfer is physical: use the side-wall outlet/Casting Table or the Foundry Ladle.
        if (actionId == 0) return false;

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
                if (TileEntityFurnace.getItemBurnTime(itemstack1) > 0) {
                    if (!this.mergeItemStack(
                        itemstack1,
                        TileEntityStarterSmelter.SLOT_FUEL,
                        TileEntityStarterSmelter.SLOT_FUEL + 1,
                        false)) {
                        return null;
                    }
                } else if (TileEntityStarterSmelter.isSmeltableFeed(itemstack1)) {
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

    private static final class SlotFuel extends Slot {

        public SlotFuel(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && TileEntityFurnace.getItemBurnTime(stack) > 0;
        }
    }

    private static final class SlotFeed extends Slot {

        public SlotFeed(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return TileEntityStarterSmelter.isSmeltableFeed(stack);
        }
    }

    private static final class SlotOutput extends Slot {

        public SlotOutput(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotStackLimit() {
            return 64;
        }
    }
}
