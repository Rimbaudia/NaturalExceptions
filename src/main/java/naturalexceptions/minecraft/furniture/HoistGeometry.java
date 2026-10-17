package naturalexceptions.minecraft.furniture;

import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Shapes and sizes a hoist's drawing shares between the windlass and the basket. */
public final class HoistGeometry {

    /** The sheave's wheel and the windlass's drum, radii in blocks. */
    public static final double WHEEL = 0.375, DRUM = 0.2;

    private HoistGeometry() {}

    public static double[][] basket() {
        return BlockHoist.BASKET;
    }

    /**
     * The way the rope leaves the sheave's wheel to go down the shaft: across the wheel, away from the windlass (a unit
     * step along x or z, the way the wheel turns).
     */
    public static double[] away(EntityHoistLoad load) {
        return away(load.worldObj, load.windlassX, load.windlassZ, load.sheaveX, load.sheaveY, load.sheaveZ);
    }

    public static double[] away(net.minecraft.world.World world, int wx, int wz, int sx, int sy, int sz) {
        boolean axleAlongX = BlockSheave.axleAlongX(world.getBlockMetadata(sx, sy, sz));
        return axleAlongX ? new double[] { 0, 0, sz >= wz ? 1 : -1 } : new double[] { sx >= wx ? 1 : -1, 0, 0 };
    }

    @SideOnly(Side.CLIENT)
    public static IIcon ropeIcon() {
        return Blocks.planks.getIcon(1, 0);
    }
}
