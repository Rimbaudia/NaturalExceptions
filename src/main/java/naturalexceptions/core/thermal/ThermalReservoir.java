package naturalexceptions.core.thermal;

/**
 * Common abstraction for thermal systems, environments, and finite bodies capable of heat exchange.
 */
public interface ThermalReservoir {

    /**
     * Current temperature in Kelvin.
     */
    double temperatureKelvin();

    /**
     * Whether this reservoir has an effectively infinite heat capacity (e.g. ambient environment, fixed heat source).
     */
    boolean isInfinite();

    /**
     * Stored thermal energy in Joules. Infinite reservoirs may return {@link Double#POSITIVE_INFINITY}.
     */
    double currentEnergyJoules();

    /**
     * Adds thermal energy in Joules to this reservoir.
     */
    void addEnergyJoules(double joules);

    /**
     * Removes thermal energy in Joules from this reservoir.
     */
    void removeEnergyJoules(double joules);
}
