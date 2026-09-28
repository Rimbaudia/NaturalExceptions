package naturalexceptions.core.thermal;

import java.util.Objects;

import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Mutable, energy-authoritative active thermal state of an individual physical material sample or body.
 *
 * <p>
 * Truth invariants:
 * <ul>
 * <li>Thermal energy is stored as Joules (strictly non-negative).</li>
 * <li>Mass is stored in kilograms.</li>
 * <li>Temperature (Kelvin), melt fraction, and phase are DERIVED on demand.</li>
 * <li>Under no circumstances is temperature incremented or decremented directly.</li>
 * </ul>
 */
public final class ActiveThermalState {

    private double energyJoules;
    private double massKg;
    private MaterialProperties properties;

    public ActiveThermalState(double energyJoules, double massKg, MaterialProperties properties) {
        if (!Double.isFinite(energyJoules) || energyJoules < 0.0) {
            throw new IllegalArgumentException("Thermal energy must be finite and non-negative: " + energyJoules);
        }
        if (!Double.isFinite(massKg) || massKg <= 0.0) {
            throw new IllegalArgumentException("Mass must be strictly positive: " + massKg);
        }
        this.energyJoules = energyJoules;
        this.massKg = massKg;
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public static ActiveThermalState ambient(double massKg, MaterialProperties properties) {
        double ambientEnergy = ThermodynamicModel
            .energyForTemperature(ThermodynamicModel.DEFAULT_AMBIENT_TEMPERATURE_K, 0.0, massKg, properties);
        return new ActiveThermalState(ambientEnergy, massKg, properties);
    }

    public static ActiveThermalState atTemperature(double temperatureKelvin, double meltFraction, double massKg,
        MaterialProperties properties) {
        double targetEnergy = ThermodynamicModel
            .energyForTemperature(temperatureKelvin, meltFraction, massKg, properties);
        return new ActiveThermalState(targetEnergy, massKg, properties);
    }

    public double energyJoules() {
        return energyJoules;
    }

    public double massKg() {
        return massKg;
    }

    public MaterialProperties properties() {
        return properties;
    }

    public void setProperties(MaterialProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public double temperatureKelvin() {
        return ThermodynamicModel.deriveTemperature(energyJoules, massKg, properties);
    }

    public double meltFraction() {
        return ThermodynamicModel.deriveMeltFraction(energyJoules, massKg, properties);
    }

    public MaterialPhase phase() {
        return ThermodynamicModel.derivePhase(energyJoules, massKg, properties);
    }

    public void addEnergyJoules(double joules) {
        if (!Double.isFinite(joules) || joules < 0.0 || !Double.isFinite(energyJoules + joules)) {
            throw new IllegalArgumentException("Energy addition must be finite, non-negative, and representable: " + joules);
        }
        energyJoules += joules;
    }

    public void removeEnergyJoules(double joules) {
        if (!Double.isFinite(joules) || joules < 0.0) {
            throw new IllegalArgumentException("Energy removal must be finite and non-negative: " + joules);
        }
        energyJoules = Math.max(0.0, energyJoules - joules);
    }

    public void setEnergyJoules(double joules) {
        if (!Double.isFinite(joules) || joules < 0.0) {
            throw new IllegalArgumentException("Thermal energy must be finite and non-negative: " + joules);
        }
        this.energyJoules = joules;
    }

    public void setMassKg(double massKg) {
        if (!Double.isFinite(massKg) || massKg <= 0.0) {
            throw new IllegalArgumentException("Mass must be strictly positive: " + massKg);
        }
        this.massKg = massKg;
    }

    public boolean isEquilibratedWith(double ambientTemperatureK, double toleranceK) {
        if (meltFraction() > 0.0) {
            return false;
        }
        return Math.abs(temperatureKelvin() - ambientTemperatureK) <= toleranceK;
    }
}
