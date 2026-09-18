package naturalexceptions.core.thermal;

/**
 * Declared environmental ambient reservoir.
 *
 * <p>
 * Acts as an effectively infinite thermal boundary at a specified ambient temperature (default 293.15 K).
 * Absorbs or provides energy without changing temperature.
 */
public final class AmbientReservoir implements ThermalReservoir {

    private final double ambientTemperatureKelvin;

    public AmbientReservoir() {
        this(ThermodynamicModel.DEFAULT_AMBIENT_TEMPERATURE_K);
    }

    public AmbientReservoir(double ambientTemperatureKelvin) {
        if (ambientTemperatureKelvin <= 0.0) {
            throw new IllegalArgumentException(
                "Ambient temperature must be strictly positive: " + ambientTemperatureKelvin);
        }
        this.ambientTemperatureKelvin = ambientTemperatureKelvin;
    }

    @Override
    public double temperatureKelvin() {
        return ambientTemperatureKelvin;
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
        // Absorbed by environment without altering ambient temperature
    }

    @Override
    public void removeEnergyJoules(double joules) {
        // Supplied by environment without altering ambient temperature
    }
}
