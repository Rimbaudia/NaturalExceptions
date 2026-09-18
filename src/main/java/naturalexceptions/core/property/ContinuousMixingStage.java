package naturalexceptions.core.property;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;

/**
 * Layer 1: Baseline continuous mixing law (mass/fraction-weighted constituent averages).
 *
 * <p>
 * Guarantees that every representable composition receives physically valid, non-zero baseline properties.
 */
public final class ContinuousMixingStage implements PropertyResolverStage {

    @Override
    public String stageName() {
        return "baseline";
    }

    @Override
    public void apply(ResolutionContext context) {
        CanonicalComposition comp = context.composition();

        double density = 0.0;
        double hardness = 0.0;
        double conductivity = 0.0;
        double thermalConductivity = 0.0;
        double specificHeatSolid = 0.0;
        double specificHeatLiquid = 0.0;
        double meltingPoint = 0.0;
        double latentHeat = 0.0;
        double resistivity = 0.0;

        for (ResourceId constituent : comp.constituents()) {
            double frac = comp.fraction(constituent);
            ConstituentData data = ConstituentData.lookup(constituent);

            density += frac * data.density();
            hardness += frac * data.hardness();
            conductivity += frac * data.conductivity();
            thermalConductivity += frac * data.thermalConductivity();
            specificHeatSolid += frac * data.specificHeatSolid();
            specificHeatLiquid += frac * data.specificHeatLiquid();
            meltingPoint += frac * data.meltingPointKelvin();
            latentHeat += frac * data.latentHeatOfFusion();
            resistivity += frac * data.electricalResistivity();

            if (frac >= 0.05) {
                for (String trait : data.biologicalTraits()) {
                    context.addTrait(trait);
                }
            }
        }

        context.set(MaterialProperties.KEY_DENSITY, density);
        context.set(MaterialProperties.KEY_HARDNESS, hardness);
        context.set(MaterialProperties.KEY_CONDUCTIVITY, conductivity);
        context.set(MaterialProperties.KEY_THERMAL_CONDUCTIVITY, thermalConductivity);
        context.set(MaterialProperties.KEY_SPECIFIC_HEAT_SOLID, specificHeatSolid);
        context.set(MaterialProperties.KEY_SPECIFIC_HEAT_LIQUID, specificHeatLiquid);
        context.set(MaterialProperties.KEY_MELTING_POINT, meltingPoint);
        context.set(MaterialProperties.KEY_LATENT_HEAT_FUSION, latentHeat);
        context.set(MaterialProperties.KEY_ELECTRICAL_RESISTIVITY, resistivity);

        context.addTrace(stageName());
    }
}
