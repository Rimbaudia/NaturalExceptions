package naturalexceptions.core.thermal;

import java.util.Objects;

import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Pure domain thermodynamic model.
 *
 * <p>
 * Implements authoritative thermal energy derivation:
 * <ul>
 * <li>Stored primary truth: thermal energy E (Joules) and mass m (kg).</li>
 * <li>Derived state: temperature T (Kelvin), melt fraction (0.0 - 1.0), and phase.</li>
 * <li>Monotonicity guarantee: Adding energy never lowers temperature; removing energy never raises temperature.</li>
 * <li>Phase transition plateau: Latent heat of fusion is consumed during melting and released during freezing
 * at constant T_melt without temperature discontinuity.</li>
 * </ul>
 */
public final class ThermodynamicModel {

    public static final double DEFAULT_AMBIENT_TEMPERATURE_K = 293.15; // 20.0 °C
    public static final double ABSOLUTE_ZERO_K = 0.0;

    private ThermodynamicModel() {}

    /**
     * Computes the solid enthalpy limit (energy required to reach melting point from 0 K as solid).
     */
    public static double solidEnthalpyLimitJoules(double massKg, MaterialProperties props) {
        validateInputs(massKg, props);
        return massKg * props.specificHeatSolid() * props.meltingPointKelvin();
    }

    /**
     * Computes the liquidus enthalpy threshold (solid limit plus complete latent heat of fusion).
     */
    public static double liquidEnthalpyThresholdJoules(double massKg, MaterialProperties props) {
        double solidLimit = solidEnthalpyLimitJoules(massKg, props);
        return solidLimit + massKg * props.latentHeatOfFusion();
    }

    /**
     * Derives temperature (Kelvin) from authoritative thermal energy and mass.
     */
    public static double deriveTemperature(double energyJoules, double massKg, MaterialProperties props) {
        validateInputs(massKg, props);
        if (energyJoules <= 0.0) {
            return ABSOLUTE_ZERO_K;
        }

        double eSolid = solidEnthalpyLimitJoules(massKg, props);
        if (energyJoules < eSolid) {
            // Below melting point: pure solid heating
            return energyJoules / (massKg * props.specificHeatSolid());
        }

        double eLiquid = liquidEnthalpyThresholdJoules(massKg, props);
        if (energyJoules <= eLiquid) {
            // Within latent heat plateau: constant melting temperature
            return props.meltingPointKelvin();
        }

        // Above melting point: superheated liquid
        double excessEnergy = energyJoules - eLiquid;
        return props.meltingPointKelvin() + excessEnergy / (massKg * props.specificHeatLiquid());
    }

    /**
     * Derives the molten fraction [0.0, 1.0] from authoritative thermal energy.
     */
    public static double deriveMeltFraction(double energyJoules, double massKg, MaterialProperties props) {
        validateInputs(massKg, props);
        double eSolid = solidEnthalpyLimitJoules(massKg, props);
        if (energyJoules <= eSolid) {
            return 0.0;
        }

        double latentHeatTotal = massKg * props.latentHeatOfFusion();
        if (latentHeatTotal <= 0.0) {
            return 1.0;
        }

        double eLiquid = eSolid + latentHeatTotal;
        if (energyJoules >= eLiquid) {
            return 1.0;
        }

        return (energyJoules - eSolid) / latentHeatTotal;
    }

    /**
     * Derives the physical {@link MaterialPhase} from authoritative thermal energy.
     */
    public static MaterialPhase derivePhase(double energyJoules, double massKg, MaterialProperties props) {
        double meltFraction = deriveMeltFraction(energyJoules, massKg, props);
        if (meltFraction >= 1.0) {
            return MaterialPhase.LIQUID;
        }
        return MaterialPhase.SOLID;
    }

    /**
     * Computes the exact thermal energy in Joules required for a specific temperature and melt fraction.
     */
    public static double energyForTemperature(double targetTemperatureK, double meltFraction, double massKg,
        MaterialProperties props) {
        validateInputs(massKg, props);
        if (targetTemperatureK <= 0.0) {
            return 0.0;
        }

        double tm = props.meltingPointKelvin();
        double eSolid = solidEnthalpyLimitJoules(massKg, props);
        double latentTotal = massKg * props.latentHeatOfFusion();

        if (targetTemperatureK < tm) {
            return massKg * props.specificHeatSolid() * targetTemperatureK;
        } else if (Math.abs(targetTemperatureK - tm) < 1.0e-9) {
            double clampedMelt = Math.max(0.0, Math.min(1.0, meltFraction));
            return eSolid + clampedMelt * latentTotal;
        } else {
            double eLiquid = eSolid + latentTotal;
            double superheat = targetTemperatureK - tm;
            return eLiquid + massKg * props.specificHeatLiquid() * superheat;
        }
    }

    private static void validateInputs(double massKg, MaterialProperties props) {
        if (massKg <= 0.0) {
            throw new IllegalArgumentException("Mass must be strictly positive: " + massKg);
        }
        Objects.requireNonNull(props, "MaterialProperties cannot be null");
    }
}
