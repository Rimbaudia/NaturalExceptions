package naturalexceptions.core.thermal;

/**
 * Declared fixed-temperature debug/test thermal source or sink (e.g. 1600 K heat reservoir, cryogenic sink).
 */
public final class FixedTemperatureReservoir implements ThermalReservoir {

    private final double temperatureKelvin;

    public FixedTemperatureReservoir(double temperatureKelvin) {
        if (temperatureKelvin <= 0.0) {
            throw new IllegalArgumentException("Temperature must be strictly positive: " + temperatureKelvin);
        }
        this.temperatureKelvin = temperatureKelvin;
    }

    @Override
    public double temperatureKelvin() {
        return temperatureKelvin;
    }

    @Override
    public boolean isInfinite() {
        return true;
    }

    @Override
    public double currentEnergyJoules() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void addEnergyJoules(double joules) {
        // Absorbed by reservoir without temperature change
    }

    @Override
    public void removeEnergyJoules(double joules) {
        // Provided by reservoir without temperature change
    }
}
