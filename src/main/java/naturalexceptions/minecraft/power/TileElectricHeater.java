package naturalexceptions.minecraft.power;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.power.Grid;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

/**
 * An Electric Heater: a coil set into a Starter Smelter's refractory shell (in place of a brick). Everything it draws
 * goes into the smelter's charge as heat, no fuel and no bellows, up to its element's limit; rated {@link #RATED_WATTS}
 * at the network's 24 V, which takes heavy cable and a stiff source (Storm Cells).
 */
public final class TileElectricHeater extends TileSource {

    public static final double RATED_VOLTS = 24.0, RATED_WATTS = 1000.0;
    public static final double OHMS = RATED_VOLTS * RATED_VOLTS / RATED_WATTS;
    static final int SEEK = 7;

    private int load = -1;
    private double watts, volts, taken;
    private int[] smelter;
    private long sought = -1000;

    /** The smelter whose shell it is set in (looked for now and then). */
    private TileEntityStarterSmelter smelter() {
        long now = worldObj.getTotalWorldTime();
        if (smelter != null) {
            TileEntity tile = worldObj.getTileEntity(smelter[0], smelter[1], smelter[2]);
            if (tile instanceof TileEntityStarterSmelter && now - sought < 200) return (TileEntityStarterSmelter) tile;
        }
        if (now - sought < 100 && smelter == null) return null;
        sought = now;
        smelter = null;
        for (int x = -SEEK; x <= SEEK; x++) for (int y = -SEEK; y <= SEEK; y++) for (int z = -SEEK; z <= SEEK; z++) {
            TileEntity tile = worldObj.getTileEntity(xCoord + x, yCoord + y, zCoord + z);
            if (!(tile instanceof TileEntityStarterSmelter)) continue;
            naturalexceptions.minecraft.multiblock.SmelterVesselGeometry g = ((TileEntityStarterSmelter) tile)
                .vesselGeometry();
            if (g != null && g.inShell(xCoord, yCoord, zCoord)) {
                smelter = new int[] { tile.xCoord, tile.yCoord, tile.zCoord };
                return (TileEntityStarterSmelter) tile;
            }
        }
        return null;
    }

    @Override
    public void stamp(Grid grid, int node) {
        load = grid.load(node, OHMS);
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        volts = grid.voltage(node);
        watts = load >= 0 ? grid.power(load) : 0.0;
        TileEntityStarterSmelter target = watts > 1.0 ? smelter() : null;
        taken = target == null ? 0.0 : target.heatElectrically(watts * seconds) / seconds;
        int glow = watts >= RATED_WATTS * 0.5 ? 2 : watts > 50.0 ? 1 : 0;
        if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != glow) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, glow, 2);
            worldObj.func_147451_t(xCoord, yCoord, zCoord);
        }
        if (glow > 0 && worldObj instanceof WorldServer && worldObj.getTotalWorldTime() % 20 == 0)
            ((WorldServer) worldObj)
                .func_147487_a("flame", xCoord + 0.5, yCoord + 1.0, zCoord + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
    }

    @Override
    public String readout() {
        return String.format(
            "Electric Heater: %.1f V, %.0f W of %.0f rated; %s",
            volts,
            watts,
            RATED_WATTS,
            smelter == null ? "not set in a smelter's shell" : String.format("%.0f W into the charge", taken));
    }
}
