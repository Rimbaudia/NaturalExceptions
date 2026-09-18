package naturalexceptions.core.metallurgy;

/**
 * Physical lining and refractory integrity profile of a smelting vessel.
 *
 * <p>
 * Simulates over-temperature degradation: exceeding safe temperature limits gradually wears
 * and damages the lining rather than denying physical actions artificially.
 */
public final class VesselLining {

    public static final double DEFAULT_MAX_SAFE_TEMP_K = 1500.0; // Clay / refractory brick limit

    private final double maxSafeTemperatureK;
    private double damage; // 0.0 = pristine, 1.0 = fully breached

    public VesselLining() {
        this(DEFAULT_MAX_SAFE_TEMP_K, 0.0);
    }

    public VesselLining(double maxSafeTemperatureK, double damage) {
        if (maxSafeTemperatureK <= 0.0) {
            throw new IllegalArgumentException("Safe temperature limit must be positive: " + maxSafeTemperatureK);
        }
        this.maxSafeTemperatureK = maxSafeTemperatureK;
        this.damage = Math.max(0.0, Math.min(1.0, damage));
    }

    public double maxSafeTemperatureK() {
        return maxSafeTemperatureK;
    }

    public double damage() {
        return damage;
    }

    public boolean isDamaged() {
        return damage > 0.0;
    }

    public boolean isBreached() {
        return damage >= 1.0;
    }

    /**
     * Applies thermal stress over a time duration. Exceeding maxSafeTemperatureK accumulates damage proportionally.
     */
    public void applyThermalStress(double temperatureK, double deltaSeconds) {
        if (temperatureK > maxSafeTemperatureK && deltaSeconds > 0.0) {
            double overTemp = temperatureK - maxSafeTemperatureK;
            double damageDelta = (overTemp / 100.0) * (deltaSeconds / 60.0) * 0.05; // 5% damage per minute per 100K
                                                                                    // over limit
            this.damage = Math.min(1.0, this.damage + damageDelta);
        }
    }

    public void repair(double repairFraction) {
        if (repairFraction <= 0.0) return;
        this.damage = Math.max(0.0, this.damage - repairFraction);
    }
}
