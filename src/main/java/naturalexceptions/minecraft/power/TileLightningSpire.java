package naturalexceptions.minecraft.power;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.core.power.Grid;

/**
 * A Lightning Spire: a Stormglass crystal on an iron rod, set high under open sky. In a thunderstorm the lightning
 * finds it; each strike pours {@link #STRIKE_JOULES} into the Storm Cells on its network (shared between them).
 */
public final class TileLightningSpire extends TileSource {

    public static final double STRIKE_JOULES = 600_000.0;
    /** About one strike a minute in a storm, while the sky is open over it. */
    public static final int STRIKE_CHANCE_TICKS = 1200;
    private long strikes;

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote || !worldObj.isThundering()) return;
        if (!worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord)) return;
        if (worldObj.rand.nextInt(STRIKE_CHANCE_TICKS) != 0) return;
        strike();
    }

    /** The lightning comes down on it: the cells on its network drink it. */
    public void strike() {
        worldObj.addWeatherEffect(new EntityLightningBolt(worldObj, xCoord + 0.5, yCoord + 1.0, zCoord + 0.5));
        strikes++;
        java.util.List<TileStormCell> cells = new java.util.ArrayList<>();
        for (TileEntity tile : PowerNetworks.of(worldObj)
            .devices(xCoord, yCoord, zCoord)) if (tile instanceof TileStormCell) cells.add((TileStormCell) tile);
        for (TileStormCell cell : cells) cell.addCharge(STRIKE_JOULES / cells.size());
    }

    @Override
    public void stamp(Grid grid, int node) {}

    @Override
    public void powered(Grid grid, int node, double seconds) {}

    @Override
    public String readout() {
        int cells = 0;
        for (TileEntity tile : PowerNetworks.of(worldObj)
            .devices(xCoord, yCoord, zCoord)) if (tile instanceof TileStormCell) cells++;
        boolean open = worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord);
        return String.format(
            "Lightning Spire: %s; %d Storm Cell%s on its network%s",
            open ? "open to the sky" : "the sky is closed over it",
            cells,
            cells == 1 ? "" : "s",
            worldObj.isThundering() ? " (a storm is on!)" : "");
    }
}
