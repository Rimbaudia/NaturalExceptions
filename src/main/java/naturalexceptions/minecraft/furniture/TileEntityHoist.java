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

    /** The client's idea of this windlass's rope: {basket x, y, z, rope top}, found once (null while not found). */
    private int[] rope;
    private int ropeRetry;

    /**
     * The rope to draw: from the basket at the foot up to the boom over it; null when there is no basket in reach yet
     * (not loaded, say), or it is away on a ride.
     */
    public int[] rope() {
        if (worldObj == null) return null;
        if (rope == null) {
            if (ropeRetry-- > 0) return null;
            ropeRetry = 100;
            net.minecraft.block.Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (!(block instanceof BlockHoist)) return null;
            int[] windlass = { xCoord, yCoord, zCoord };
            int[] foot = ((BlockHoist) block).partner(worldObj, xCoord, yCoord, zCoord);
            if (foot == null) return null;
            rope = new int[] { foot[0], foot[1], foot[2], BlockHoist.ropeTop(worldObj, foot, windlass) };
        }
        net.minecraft.block.Block there = worldObj.getBlock(rope[0], rope[1], rope[2]);
        if (!(there instanceof BlockHoist) || ((BlockHoist) there).top) {
            rope = null;
            return null;
        }
        return worldObj.getBlockMetadata(rope[0], rope[1], rope[2]) == 0 ? rope : null;
    }

    /** The rope hangs far from the windlass: drawn whenever the windlass is anywhere near view. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
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
