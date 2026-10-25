package naturalexceptions.minecraft.power;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.power.Grid;

/**
 * Stored charge on a network: a Heart Socket's Stormglass heart, a Storm Cell. Its EMF rises with its charge, from
 * {@link #emptyVolts()} to {@link #fullVolts()}, so a network at 24 V can charge it part way and only something
 * stronger (a lightning strike) fills it. Charge in joules.
 */
public abstract class TileBattery extends TileSource {

    protected double charge;
    private int source = -1;
    protected double volts, amps;

    protected abstract double capacity();

    protected abstract double emptyVolts();

    protected abstract double fullVolts();

    protected abstract double internalOhms();

    /** Whether it can take part (a socket with no heart cannot). */
    protected boolean present() {
        return true;
    }

    /** The charge it holds (joules). */
    public double charge() {
        return charge;
    }

    public double fraction() {
        return capacity() <= 0 ? 0.0 : charge / capacity();
    }

    public void addCharge(double joules) {
        charge = Math.max(0.0, Math.min(capacity(), charge + joules));
        markDirty();
    }

    public double emf() {
        return emptyVolts() + (fullVolts() - emptyVolts()) * fraction();
    }

    /** Flat and being asked to give: out of this solve, until the next. */
    private boolean withdrawn;

    @Override
    public void stamp(Grid grid, int node) {
        source = present() && !withdrawn ? grid.source(node, emf(), internalOhms()) : -1;
    }

    /** A flat battery takes charge but gives none: if the solve had it giving, it withdraws. */
    @Override
    public boolean withdraws(Grid grid) {
        if (source < 0 || charge > 0.0 || grid.sourceCurrent(source) <= 0.0) return false;
        withdrawn = true;
        return true;
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        volts = grid.voltage(node);
        amps = source >= 0 ? grid.sourceCurrent(source) : 0.0;
        withdrawn = false;
        if (source < 0) return;
        // It gives (or takes) its EMF times its current; flat, it gives nothing.
        double joules = emf() * amps * seconds;
        if (charge <= 0.0 && joules > 0.0) joules = 0.0;
        addCharge(-joules);
        changed();
    }

    /** After each solve (for a look that follows the charge). */
    protected void changed() {}

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("Charge", charge);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge = tag.getDouble("Charge");
    }
}
