package naturalexceptions.minecraft.furniture;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A Lighthouse Lamp's beams: nothing to keep, only drawn from far away. */
public final class TileEntityLighthouseLamp extends TileEntity {

    /** How far the beams reach, in blocks. */
    public static final double BEAM = 96.0;
    /** Degrees the beams turn a tick. */
    public static final double TURN = 1.5;

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 320.0 * 320.0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
