package naturalexceptions.minecraft.power;

/**
 * A Storm Cell: Stormglass in brine behind glass, storing lightning. Stiff (it can give kilowatts) and deep; a 24 V
 * network charges it part way, a strike on a Lightning Spire on its network fills it. It glows as it fills.
 */
public final class TileStormCell extends TileBattery {

    public static final double CAPACITY = 2_000_000.0;

    @Override
    protected double capacity() {
        return CAPACITY;
    }

    @Override
    protected double emptyVolts() {
        return 21.0;
    }

    @Override
    protected double fullVolts() {
        return 26.0;
    }

    @Override
    protected double internalOhms() {
        return 0.05;
    }

    @Override
    protected void changed() {
        if (worldObj == null) return;
        int glow = (int) Math.min(3, Math.floor(fraction() * 4.0));
        if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != glow) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, glow, 2);
            worldObj.func_147451_t(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public String readout() {
        return String.format(
            "Storm Cell: %.0f%% charged (%.0f kJ), %.1f V, %+.1f A",
            fraction() * 100.0,
            charge / 1000.0,
            volts,
            amps);
    }
}
