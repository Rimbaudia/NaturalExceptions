package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A hoist's store, kept at its windlass and reached from its basket as well: what is put in at the foot of the
 * quarry is taken out at the top. It remembers the basket it was last worked from.
 */
public final class TileEntityHoist extends TileEntityStorage {

    private int[] basket;

    public TileEntityHoist() {
        super(9, "container.naturalexceptions.hoist");
    }

    void link(int x, int y, int z) {
        basket = new int[] { x, y, z };
        markDirty();
    }

    /** Usable from beside the windlass, or from beside its basket. */
    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) return true;
        if (player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0) return true;
        return basket != null && worldObj.getBlock(basket[0], basket[1], basket[2]) instanceof BlockHoist
            && player.getDistanceSq(basket[0] + 0.5, basket[1] + 0.5, basket[2] + 0.5) <= 64.0;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (basket != null) tag.setIntArray("Basket", basket);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        basket = tag.hasKey("Basket") ? tag.getIntArray("Basket") : null;
    }
}
