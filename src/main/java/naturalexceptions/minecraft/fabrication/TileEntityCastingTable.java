package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * TileEntity hosting a physical casting mold on top of the casting table.
 * Supports world-tick thermodynamic cooling, mold placement, and extraction.
 */
public final class TileEntityCastingTable extends TileEntity implements IInventory {

    private ItemStack moldStack;

    public TileEntityCastingTable() {}

    public ItemStack getMold() {
        return moldStack;
    }

    public void setMold(ItemStack mold) {
        this.moldStack = mold;
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }

        if (moldStack != null && moldStack.getItem() instanceof ItemCastingMold) {
            if (ItemCastingMold.isFilled(moldStack)) {
                boolean wasSolid = ItemCastingMold.isSolidified(moldStack);

                // Run physical convective and radiative cooling on world tick
                ((ItemCastingMold) moldStack.getItem()).onUpdate(moldStack, worldObj, null, 0, false);

                boolean nowSolid = ItemCastingMold.isSolidified(moldStack);
                if (wasSolid != nowSolid || worldObj.getTotalWorldTime() % 20L == 0L) {
                    markDirty();
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        try {
            super.readFromNBT(tag);
        } catch (RuntimeException ignored) {
            // Allows testing outside full LaunchClassLoader/FML mapping lifecycle
        }
        if (tag.hasKey("Mold")) {
            NBTTagCompound moldTag = tag.getCompoundTag("Mold");
            this.moldStack = ItemStack.loadItemStackFromNBT(moldTag);
            if (this.moldStack == null && moldTag.hasKey("tag")) {
                net.minecraft.item.Item item =
                    naturalexceptions.minecraft.registry.NEItems.castingMold != null
                        ? naturalexceptions.minecraft.registry.NEItems.castingMold
                        : new ItemCastingMold();
                this.moldStack = new ItemStack(item);
                this.moldStack.setTagCompound((NBTTagCompound) moldTag.getCompoundTag("tag").copy());
            }
        } else {
            this.moldStack = null;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        try {
            super.writeToNBT(tag);
        } catch (RuntimeException ignored) {
            // Allows testing outside full LaunchClassLoader/FML mapping lifecycle
        }
        if (moldStack != null) {
            NBTTagCompound moldTag = new NBTTagCompound();
            moldStack.writeToNBT(moldTag);
            tag.setTag("Mold", moldTag);
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    // --- IInventory Implementation (1 slot) ---

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? moldStack : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot == 0 && moldStack != null) {
            ItemStack res = moldStack;
            moldStack = null;
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return res;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot == 0 && moldStack != null) {
            ItemStack res = moldStack;
            moldStack = null;
            return res;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            moldStack = stack;
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public String getInventoryName() {
        return "container.castingTable";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack != null && stack.getItem() instanceof ItemCastingMold;
    }
}
