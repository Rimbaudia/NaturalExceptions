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

    public static final double DEFAULT_FLAME_TEMPERATURE_K = 1880.0;

    private int remainingTicks;
    private int totalBurnTicks;
    private double powerWatts;
    private double flameTemperatureK = DEFAULT_FLAME_TEMPERATURE_K;
    private String fuelLabel = "Ordinary Heat";
    private double burnFractionAccumulator = 0.0;

    public FuelBurnState() {
        this(0, 0, JOULES_PER_BURN_TICK * TICKS_PER_SECOND);
    }

    public FuelBurnState(int remainingTicks, int totalBurnTicks, double powerWatts) {
        this(remainingTicks, totalBurnTicks, powerWatts, DEFAULT_FLAME_TEMPERATURE_K, "Ordinary Heat");
    }

    public FuelBurnState(int remainingTicks, int totalBurnTicks, double powerWatts, double flameTemperatureK,
        String fuelLabel) {
        this.remainingTicks = Math.max(0, remainingTicks);
        this.totalBurnTicks = Math.max(0, totalBurnTicks);
        this.powerWatts = powerWatts > 0.0 ? powerWatts : (JOULES_PER_BURN_TICK * TICKS_PER_SECOND);
        this.flameTemperatureK = flameTemperatureK > 0.0 ? flameTemperatureK : DEFAULT_FLAME_TEMPERATURE_K;
        this.fuelLabel = fuelLabel != null ? fuelLabel : "Ordinary Heat";
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

    public double flameTemperatureK() {
        return flameTemperatureK;
    }

    public String fuelLabel() {
        return fuelLabel;
    }

    public void addFuel(int burnTicks) {
        addFuel(burnTicks, DEFAULT_FLAME_TEMPERATURE_K, "Coal \u2014 Strong Heat");
    }

    public void addFuel(int burnTicks, double flameTempK, String label) {
        if (burnTicks <= 0) return;
        this.remainingTicks += burnTicks;
        this.totalBurnTicks += burnTicks;
        if (flameTempK > this.flameTemperatureK || this.remainingTicks == burnTicks) {
            this.flameTemperatureK = flameTempK;
            this.fuelLabel = label;
        }
    }

    /**
     * Advances combustion by deltaTicks under standard natural draft.
     */
    public double step(int deltaTicks) {
        return step(deltaTicks, 1.0);
    }

    /**
     * Advances combustion by deltaTicks scaled by combustion draft airMultiplier.
     * Higher draft accelerates the fuel consumption rate and increases power output.
     */
    public double step(int deltaTicks, double burnMultiplier) {
        if (remainingTicks <= 0 || deltaTicks <= 0) {
            return 0.0;
        }
        double effectiveMultiplier = Math.max(0.2, burnMultiplier);
        double ticksToBurn = deltaTicks * effectiveMultiplier;
        burnFractionAccumulator += ticksToBurn;
        int ticksConsumed = (int) burnFractionAccumulator;
        burnFractionAccumulator -= ticksConsumed;

        int consumed = Math.min(remainingTicks, ticksConsumed);
        remainingTicks -= consumed;
        if (remainingTicks == 0) {
            totalBurnTicks = 0;
            burnFractionAccumulator = 0.0;
        }
        return consumed * (powerWatts / TICKS_PER_SECOND);
    }
}
