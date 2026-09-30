package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** What lies on a brazier's coals and how long it has been there; done, it jumps off cooked. */
public final class TileEntityBrazier extends TileEntity {

    /** Ticks a piece takes on the coals. */
    public static final int COOK_TICKS = 100;

    private ItemStack cooking;
    private int ticks;

    /** Lays one of {@code raw} on the coals; false if something is already cooking. */
    public boolean lay(ItemStack raw) {
        if (cooking != null || BlockBrazier.cooked(raw) == null) return false;
        cooking = raw.copy();
        cooking.stackSize = 1;
        ticks = 0;
        markDirty();
        return true;
    }

    public ItemStack cooking() {
        return cooking;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || cooking == null) return;
        if (++ticks % 20 == 0)
            worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.8, zCoord + 0.5, "random.fizz", 0.3F, 1.8F);
        if (ticks < COOK_TICKS) return;
        ItemStack done = BlockBrazier.cooked(cooking);
        cooking = null;
        ticks = 0;
        markDirty();
        if (done == null) return;
        EntityItem item = new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.9, zCoord + 0.5, done);
        item.motionX = (worldObj.rand.nextDouble() - 0.5) * 0.1;
        item.motionY = 0.25;
        item.motionZ = (worldObj.rand.nextDouble() - 0.5) * 0.1;
        worldObj.spawnEntityInWorld(item);
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.8, zCoord + 0.5, "random.pop", 0.4F, 1.2F);
    }

    /** Whatever was cooking falls off (the brazier broken). */
    void spill() {
        if (cooking == null) return;
        worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.9, zCoord + 0.5, cooking));
        cooking = null;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (cooking != null) tag.setTag("Cooking", cooking.writeToNBT(new NBTTagCompound()));
        tag.setInteger("Ticks", ticks);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        cooking = tag.hasKey("Cooking") ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Cooking")) : null;
        ticks = tag.getInteger("Ticks");
    }
}
