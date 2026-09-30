package naturalexceptions.minecraft.furniture;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What hangs on a tanning rack: up to three hides (rotten flesh, scraped and stretched), each curing into leather
 * after {@link #CURE} ticks in the air. What has cured stays hanging until it is taken down.
 */
public final class TileEntityTanningRack extends TileEntityStorage {

    /** Two minutes. */
    public static final int CURE = 2400;

    private final int[] age = new int[3];

    public TileEntityTanningRack() {
        super(3, "tile.tanningRack.name");
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack != null && stack.getItem() == Items.rotten_flesh;
    }

    /** Hangs one hide in a free place; false if the rack is full. */
    boolean hang(ItemStack hide) {
        for (int i = 0; i < 3; i++) if (getStackInSlot(i) == null) {
            ItemStack one = hide.copy();
            one.stackSize = 1;
            setInventorySlotContents(i, one);
            age[i] = 0;
            return true;
        }
        return false;
    }

    /** Takes down the first cured hide (or, if none has cured, the last hung), or null. */
    ItemStack takeDown() {
        for (int i = 0; i < 3; i++)
            if (getStackInSlot(i) != null && getStackInSlot(i).getItem() == Items.leather) return getStackInSlotOnClosing(i);
        for (int i = 2; i >= 0; i--) if (getStackInSlot(i) != null) return getStackInSlotOnClosing(i);
        return null;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack == null || stack.getItem() != Items.rotten_flesh) continue;
            if (++age[i] >= CURE) cure(i);
        }
    }

    /** The hide in place {@code i} is leather now. */
    void cure(int i) {
        setInventorySlotContents(i, new ItemStack(Items.leather));
        age[i] = 0;
        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (worldObj != null && !worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setIntArray("Age", age);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int[] saved = tag.getIntArray("Age");
        for (int i = 0; i < Math.min(saved.length, age.length); i++) age[i] = saved[i];
    }
}
