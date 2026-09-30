package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/**
 * A barrel holds a great deal of one thing: up to sixteen stacks of a single item, or up to four buckets of water
 * (a bucket is three bottles). It is one or the other; an empty barrel takes whichever comes first.
 */
public final class TileEntityBarrel extends TileEntity {

    public static final int STACKS = 16;
    public static final int WATER_BUCKETS = 4;
    /** Water is kept in bottles: three to a bucket. */
    public static final int BOTTLES_PER_BUCKET = 3;

    /** What it holds (count 1), or null. */
    private ItemStack kind;
    private int count;
    private int water;

    public ItemStack kind() {
        return kind;
    }

    public int count() {
        return count;
    }

    /** Water held, in bottles. */
    public int water() {
        return water;
    }

    public int capacity() {
        return kind == null ? 0 : STACKS * kind.getMaxStackSize();
    }

    public boolean isEmpty() {
        return count == 0 && water == 0;
    }

    /** Whether {@code stack} may go in: the barrel is empty, or holds the same item with the same data. */
    public boolean accepts(ItemStack stack) {
        if (stack == null || water > 0) return false;
        if (kind == null) return !stack.isItemStackDamageable() || stack.getItemDamage() == 0;
        return kind.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(kind, stack);
    }

    /** Puts in as much of {@code stack} as fits; returns how many went in (the stack itself is not changed). */
    public int insert(ItemStack stack) {
        if (!accepts(stack)) return 0;
        if (kind == null) {
            kind = stack.copy();
            kind.stackSize = 1;
        }
        int moved = Math.min(stack.stackSize, capacity() - count);
        count += moved;
        if (moved > 0) changed();
        return moved;
    }

    /** Takes out up to {@code most} items as one stack, or null if it holds none. */
    public ItemStack extract(int most) {
        if (kind == null || count == 0) return null;
        int taken = Math.min(Math.min(most, kind.getMaxStackSize()), count);
        ItemStack out = kind.copy();
        out.stackSize = taken;
        count -= taken;
        if (count == 0) kind = null;
        changed();
        return out;
    }

    /** Pours in a bucket of water; false if the barrel holds items or has no room for a bucket. */
    public boolean pourWater() {
        if (kind != null || water + BOTTLES_PER_BUCKET > WATER_BUCKETS * BOTTLES_PER_BUCKET) return false;
        water += BOTTLES_PER_BUCKET;
        changed();
        return true;
    }

    /** Draws a bucket of water out; false if there is not a bucketful. */
    public boolean drawWater() {
        if (water < BOTTLES_PER_BUCKET) return false;
        water -= BOTTLES_PER_BUCKET;
        changed();
        return true;
    }

    /** Draws a bottle of water out; false if it is dry. */
    public boolean drawBottle() {
        if (water == 0) return false;
        water--;
        changed();
        return true;
    }

    private void changed() {
        if (worldObj == null) return;
        markDirty();
        if (!worldObj.isRemote) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    /** Everything left in it, as stacks, for when it is broken. */
    public java.util.List<ItemStack> spill() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        while (count > 0) out.add(extract(Integer.MAX_VALUE));
        water = 0;
        return out;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (kind != null) tag.setTag("Kind", kind.writeToNBT(new NBTTagCompound()));
        tag.setInteger("Count", count);
        tag.setInteger("Water", water);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        kind = tag.hasKey("Kind") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Kind")) : null;
        count = tag.getInteger("Count");
        water = tag.getInteger("Water");
        if (kind == null) count = 0;
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
