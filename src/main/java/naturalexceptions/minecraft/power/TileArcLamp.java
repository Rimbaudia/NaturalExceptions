package naturalexceptions.minecraft.power;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.power.Grid;

/**
 * An arc lamp: an arc struck between carbon rods behind glass, as bright as the power it is given. Rated for
 * {@link #RATED_VOLTS} volts ({@link #RATED_WATTS} watts); driven far past that, its carbons burn through and the lamp
 * bursts.
 */
public final class TileArcLamp extends TileEntity implements PowerDevice, PowerReadout {

    public static final double RATED_VOLTS = 24.0, RATED_WATTS = 120.0;
    public static final double OHMS = RATED_VOLTS * RATED_VOLTS / RATED_WATTS;
    /** Past this many times its rating for a second, it bursts. */
    public static final double BURST_FACTOR = 2.5;
    /** Light at each step of brightness, and the power each step needs. */
    public static final int[] LIGHT = { 0, 7, 12, 15 };
    public static final double[] STEP_WATTS = { 0.0, 10.0, 45.0, 90.0 };

    private int load = -1;
    private double watts, volts, over;
    private boolean known;

    /** How bright it burns on {@code watts}: 0 dark to 3 full. */
    public static int brightness(double watts) {
        for (int step = STEP_WATTS.length - 1; step > 0; step--) if (watts >= STEP_WATTS[step]) return step;
        return 0;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (known || worldObj == null || worldObj.isRemote) return;
        known = true;
        PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null) PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    @Override
    public void stamp(Grid grid, int node) {
        load = grid.load(node, OHMS);
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        volts = grid.voltage(node);
        watts = load >= 0 ? grid.power(load) : 0.0;
        over = watts > RATED_WATTS * BURST_FACTOR ? over + seconds : 0.0;
        if (over >= 1.0) {
            burst();
            return;
        }
        int step = brightness(watts);
        // A burning arc hums and hisses.
        if (step > 0 && worldObj.getTotalWorldTime() % 40 < PowerNetworks.TICKS) worldObj.playSoundEffect(
            xCoord + 0.5,
            yCoord + 0.5,
            zCoord + 0.5,
            "naturalexceptions:power.hum",
            0.1F * step,
            1.2F);
        if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != step) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, step, 2);
            worldObj.func_147451_t(xCoord, yCoord, zCoord);
        }
    }

    private void burst() {
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.glass", 1.0F, 1.2F);
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "naturalexceptions:power.zap", 1.0F, 0.8F);
        if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
            .func_147487_a("fireworksSpark", xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, 30, 0.2, 0.2, 0.2, 0.15);
        worldObj.func_147480_a(xCoord, yCoord, zCoord, true); // breaks, and drops itself
    }

    @Override
    public String readout() {
        return String.format(
            "Arc Lamp: %.1f V, %.0f W of %.0f rated%s",
            volts,
            watts,
            RATED_WATTS,
            watts > RATED_WATTS * 1.2 ? " (overdriven!)" : "");
    }
}
