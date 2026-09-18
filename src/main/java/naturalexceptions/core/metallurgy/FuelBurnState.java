package naturalexceptions.core.metallurgy;

/**
 * Physical combustion state of active fuel inside a metallurgical vessel.
 *
 * <p>
 * Translates discrete fuel items into continuous thermal power (Watts = Joules/second)
 * transferred directly into vessel contents over burn ticks.
 */
public final class FuelBurnState {

    public static final double JOULES_PER_BURN_TICK = 10000.0; // 10 kJ per vanilla burn tick
    public static final double TICKS_PER_SECOND = 20.0;

    private int remainingTicks;
    private int totalBurnTicks;
    private double powerWatts;

    public FuelBurnState() {
        this(0, 0, JOULES_PER_BURN_TICK * TICKS_PER_SECOND);
    }

    public FuelBurnState(int remainingTicks, int totalBurnTicks, double powerWatts) {
        this.remainingTicks = Math.max(0, remainingTicks);
        this.totalBurnTicks = Math.max(0, totalBurnTicks);
        this.powerWatts = powerWatts > 0.0 ? powerWatts : (JOULES_PER_BURN_TICK * TICKS_PER_SECOND);
    }

    public boolean isBurning() {
        return remainingTicks > 0;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public int totalBurnTicks() {
        return totalBurnTicks;
    }

    public double powerWatts() {
        return powerWatts;
    }

    public void addFuel(int burnTicks) {
        if (burnTicks <= 0) return;
        this.remainingTicks += burnTicks;
        this.totalBurnTicks += burnTicks;
    }

    /**
     * Advances combustion by deltaTicks, returning total Joules generated.
     */
    public double step(int deltaTicks) {
        if (remainingTicks <= 0 || deltaTicks <= 0) {
            return 0.0;
        }
        int consumed = Math.min(remainingTicks, deltaTicks);
        remainingTicks -= consumed;
        if (remainingTicks == 0) {
            totalBurnTicks = 0;
        }
        return consumed * (powerWatts / TICKS_PER_SECOND);
    }
}
