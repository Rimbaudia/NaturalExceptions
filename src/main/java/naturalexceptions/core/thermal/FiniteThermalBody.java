package naturalexceptions.core.thermal;

import java.util.Objects;

import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Finite thermal body with explicit mass, material properties, and energy conservation.
 */
public final class FiniteThermalBody implements ThermalReservoir {

    private final ActiveThermalState state;

    public FiniteThermalBody(ActiveThermalState state) {
        this.state = Objects.requireNonNull(state, "ActiveThermalState cannot be null");
    }

    public static FiniteThermalBody of(double energyJoules, double massKg, MaterialProperties properties) {
        return new FiniteThermalBody(new ActiveThermalState(energyJoules, massKg, properties));
    }

    public static FiniteThermalBody ambient(double massKg, MaterialProperties properties) {
        return new FiniteThermalBody(ActiveThermalState.ambient(massKg, properties));
    }

    public static FiniteThermalBody atTemperature(double temperatureKelvin, double meltFraction, double massKg,
        MaterialProperties properties) {
        return new FiniteThermalBody(
            ActiveThermalState.atTemperature(temperatureKelvin, meltFraction, massKg, properties));
    }

    public ActiveThermalState state() {
        return state;
    }

    @Override
    public double temperatureKelvin() {
        return state.temperatureKelvin();
    }

    @Override
    public boolean isInfinite() {
        return false;
    }

    @Override
    public double currentEnergyJoules() {
        return state.energyJoules();
    }

    @Override
    public void addEnergyJoules(double joules) {
        state.addEnergyJoules(joules);
    }

    @Override
    public void removeEnergyJoules(double joules) {
        state.removeEnergyJoules(joules);
    }

    public double massKg() {
        return state.massKg();
    }

    public MaterialPhase phase() {
        return state.phase();
    }

    public double meltFraction() {
        return state.meltFraction();
    }

    public MaterialProperties properties() {
        return state.properties();
    }
}
