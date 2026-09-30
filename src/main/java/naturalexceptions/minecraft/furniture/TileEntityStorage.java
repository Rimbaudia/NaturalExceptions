package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A piece of furniture that holds items: a crate, a pantry shelf, a market counter, a table. Its slots are saved
 * with it and sent to clients, so what it holds can be drawn on it.
 */
public abstract class TileEntityStorage extends TileEntity implements IInventory {

    private final ItemStack[] slots;
    private final String name;

    protected TileEntityStorage(int size, String name) {
        this.slots = new ItemStack[size];
        this.name = name;
    }

    @Override
    public int getSizeInventory() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < slots.length ? slots[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack stack = getStackInSlot(slot);
        if (stack == null) return null;
        ItemStack taken = stack.splitStack(Math.min(count, stack.stackSize));
        if (stack.stackSize <= 0) slots[slot] = null;
        markDirty();
        return taken;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) slots[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots.length) return;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) stack.stackSize = getInventoryStackLimit();
        slots[slot] = stack;
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return name;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    /** Puts a whole stack into the first free slot that takes it; false if there is none. */
    public boolean addToFreeSlot(ItemStack stack) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null && isItemValidForSlot(i, stack)) {
                setInventorySlotContents(i, stack);
                return true;
            }
        }
        return false;
    }

    /** The last filled slot, or -1. */
    public int lastFilled() {
        for (int i = slots.length - 1; i >= 0; i--) if (slots[i] != null) return i;
        return -1;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Scatters what it holds, as a chest does when broken. */
    public void dropContents(World world, int x, int y, int z) {
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = slots[i];
            if (stack == null) continue;
            slots[i] = null;
            EntityItem item = new EntityItem(
                world,
                x + 0.2 + world.rand.nextFloat() * 0.6,
                y + 0.5,
                z + 0.2 + world.rand.nextFloat() * 0.6,
                stack);
            item.motionX = world.rand.nextGaussian() * 0.05;
            item.motionY = 0.2;
            item.motionZ = world.rand.nextGaussian() * 0.05;
            world.spawnEntityInWorld(item);
        }
    }

    public NBTTagList writeSlots() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Slot", (byte) i);
            slots[i].writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }

    public void readSlots(NBTTagList list) {
        java.util.Arrays.fill(slots, null);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot < slots.length) slots[slot] = ItemStack.loadItemStackFromNBT(tag);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("Items", writeSlots());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSlots(tag.getTagList("Items", 10));
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        readFromNBT(packet.func_148857_g());
    }
}
