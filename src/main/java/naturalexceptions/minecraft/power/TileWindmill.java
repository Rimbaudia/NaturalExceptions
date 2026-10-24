package naturalexceptions.minecraft.power;

import net.minecraft.world.World;

import naturalexceptions.core.power.Grid;
import naturalexceptions.core.power.Wind;

/**
 * A windmill: four cloth sails on a hub, turning a generator in its nacelle. Its governor holds {@link #VOLTS}; the
 * wind decides how much it can give. It needs open air before its sails (a 5 x 5 disc), and wind (height, weather).
 */
public final class TileWindmill extends TileSource {

    public static final double VOLTS = 24.0;
    private int source = -1;
    private double wind, volts, amps;

    /** The wind its sails catch here and now: the wind, less what blocks them. */
    public static double caught(World world, int x, int y, int z, int facing) {
        double wind = Wind.at(x, y, z, world.getTotalWorldTime(), world.isRaining(), world.isThundering());
        if (wind <= 0.0) return 0.0;
        int blocked = 0;
        int fx = facing == 4 ? -1 : facing == 5 ? 1 : 0, fz = facing == 2 ? -1 : facing == 3 ? 1 : 0;
        for (int a = -2; a <= 2; a++) for (int b = -2; b <= 2; b++) {
            if (a == 0 && b == 0) continue;
            int sx = x + fx + (fx == 0 ? a : 0), sy = y + b, sz = z + fz + (fz == 0 ? a : 0);
            if (!world.isAirBlock(sx, sy, sz)) blocked++;
        }
        return wind * (1.0 - blocked / 24.0);
    }

    // --- On the client: how far its sails have turned -------------------------------------------------------

    private double angle, clientWind;
    private long angleTick = -1;

    /** The sails' angle now, for the renderer: they turn as fast as the wind here drives them. */
    public double clientAngle(float partial, double degreesPerTick) {
        long now = worldObj.getTotalWorldTime();
        if (angleTick < 0 || now - angleTick > 200) {
            angleTick = now;
            clientWind = caught(worldObj, xCoord, yCoord, zCoord, facing());
        }
        while (angleTick < now) {
            if (angleTick % 20 == 0) clientWind = caught(worldObj, xCoord, yCoord, zCoord, facing());
            if (clientWind >= Wind.CUT_IN) angle = (angle + clientWind * degreesPerTick) % 360.0;
            angleTick++;
        }
        return angle + (clientWind >= Wind.CUT_IN ? clientWind * degreesPerTick * partial : 0.0);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return net.minecraft.util.AxisAlignedBB
            .getBoundingBox(xCoord - 2.5, yCoord - 2.0, zCoord - 2.5, xCoord + 3.5, yCoord + 3.0, zCoord + 3.5);
    }

    public int facing() {
        int meta = worldObj == null ? 2 : worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
        return meta >= 2 && meta <= 5 ? meta : 2;
    }

    @Override
    public void stamp(Grid grid, int node) {
        wind = caught(worldObj, xCoord, yCoord, zCoord, facing());
        source = wind >= Wind.CUT_IN ? grid.source(node, VOLTS, Wind.internalOhms(wind)) : -1;
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        volts = grid.voltage(node);
        amps = source >= 0 ? grid.sourceCurrent(source) : 0.0;
    }

    @Override
    public String readout() {
        return wind < Wind.CUT_IN ? String.format("Windmill: becalmed (wind %.0f%%)", wind * 100.0)
            : String.format(
                "Windmill: wind %.0f%%, %.1f V, giving %.1f A (%.0f W)",
                wind * 100.0,
                volts,
                amps,
                volts * amps);
    }
}
