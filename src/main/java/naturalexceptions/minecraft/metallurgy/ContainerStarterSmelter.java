package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.minecraft.item.ItemCastingMold;

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
        addSlotToContainer(new SlotMold(tileEntity, TileEntityStarterSmelter.SLOT_MOLD, 134, 36));

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
        if (actionId == 0) {
            // Action 0: Pour Metal
            ItemStack moldSlot = tileEntity.getStackInSlot(TileEntityStarterSmelter.SLOT_MOLD);
            if (moldSlot != null && moldSlot.getItem() instanceof ItemCastingMold
                && !ItemCastingMold.isFilled(moldSlot)) {
                ItemStack singleMold = moldSlot.stackSize == 1 ? moldSlot : moldSlot.splitStack(1);
                if (tileEntity.pourIntoMold(singleMold)) {
                    if (singleMold != moldSlot) {
                        if (moldSlot.stackSize <= 0) {
                            tileEntity.setInventorySlotContents(TileEntityStarterSmelter.SLOT_MOLD, singleMold);
                        } else {
                            if (!player.inventory.addItemStackToInventory(singleMold)) {
                                player.dropPlayerItemWithRandomChoice(singleMold, false);
                            }
                        }
                    }
                    player.addChatMessage(new ChatComponentText("\u00a7aPoured molten metal into mold."));
                    detectAndSendChanges();
                    return true;
                } else {
                    if (singleMold != moldSlot) {
                        moldSlot.stackSize++;
                    }
                    sendDiagnosticPourFailure(player);
                    return false;
                }
            }

            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof ItemCastingMold && !ItemCastingMold.isFilled(held)) {
                ItemStack singleMold = held.stackSize == 1 ? held : held.splitStack(1);
                if (tileEntity.pourIntoMold(singleMold)) {
                    if (singleMold != held) {
                        if (!player.inventory.addItemStackToInventory(singleMold)) {
                            player.dropPlayerItemWithRandomChoice(singleMold, false);
                        }
                    }
                    player.addChatMessage(new ChatComponentText("\u00a7aPoured molten metal into held mold."));
                    detectAndSendChanges();
                    return true;
                } else {
                    if (singleMold != held) {
                        held.stackSize++;
                    }
                    sendDiagnosticPourFailure(player);
                    return false;
                }
            }

            // Check adjacent casting table for empty mold
            if (tileEntity.getWorldObj() != null) {
                int[][] offsets = { { 0, 0, 1 }, { 0, 0, -1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 1 }, { 0, -1, -1 },
                    { 1, -1, 0 }, { -1, -1, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 1, 1 }, { 0, 1, -1 }, { 1, 1, 0 },
                    { -1, 1, 0 } };
                for (int[] off : offsets) {
                    TileEntity te = tileEntity.getWorldObj()
                        .getTileEntity(
                            tileEntity.xCoord + off[0],
                            tileEntity.yCoord + off[1],
                            tileEntity.zCoord + off[2]);
                    if (te instanceof naturalexceptions.minecraft.fabrication.TileEntityCastingTable) {
                        naturalexceptions.minecraft.fabrication.TileEntityCastingTable table = (naturalexceptions.minecraft.fabrication.TileEntityCastingTable) te;
                        ItemStack tableMold = table.getMold();
                        if (tableMold != null && !ItemCastingMold.isFilled(tableMold)) {
                            if (tileEntity.pourIntoMold(tableMold)) {
                                table.markDirty();
                                tileEntity.getWorldObj()
                                    .markBlockForUpdate(table.xCoord, table.yCoord, table.zCoord);
                                player.addChatMessage(
                                    new ChatComponentText(
                                        "\u00a7aPoured molten metal into adjacent Casting Table mold."));
                                detectAndSendChanges();
                                return true;
                            } else {
                                sendDiagnosticPourFailure(player);
                                return false;
                            }
                        }
                    }
                }
            }

            player.addChatMessage(
                new ChatComponentText(
                    "\u00a7eInsert an empty Casting Mold into the mold slot or onto adjacent Casting Table."));
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

    private void sendDiagnosticPourFailure(EntityPlayer player) {
        double maxTemp = 293.15;
        for (AggregateLayer l : tileEntity.vesselState()
            .layers()) {
            if (l.temperatureKelvin() > maxTemp) {
                maxTemp = l.temperatureKelvin();
            }
        }
        player.addChatMessage(
            new ChatComponentText(
                String
                    .format("\u00a7cNo pourable liquid metal available (Crucible: %.0f K, need >= 1358 K).", maxTemp)));
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
                } else if (itemstack1.getItem() instanceof ItemCastingMold) {
                    if (!this.mergeItemStack(
                        itemstack1,
                        TileEntityStarterSmelter.SLOT_MOLD,
                        TileEntityStarterSmelter.SLOT_MOLD + 1,
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

    private static final class SlotMold extends Slot {

        public SlotMold(IInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() instanceof ItemCastingMold;
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }
    }
}
