package naturalexceptions.core.property;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.MaterialPhase;

/**
 * Layer 3: Known phase / alloy rules (metallurgical structure enrichment and intermetallic/solid solution windows).
 */
public final class PhaseAlloyRuleStage implements PropertyResolverStage {

    @Override
    public String stageName() {
        return "phase_rules";
    }

    @Override
    public void apply(ResolutionContext context) {
        if (context.phase() == MaterialPhase.LIQUID) {
            return;
        }
        CanonicalComposition comp = context.composition();

        // Alpha-bronze phase window: ~75-92% Cu, 8-25% Sn
        if (comp.contains(ConstituentData.ID_CU) && comp.contains(ConstituentData.ID_SN)) {
            double cuFrac = comp.fraction(ConstituentData.ID_CU);
            double snFrac = comp.fraction(ConstituentData.ID_SN);
            if (cuFrac >= 0.75 && cuFrac <= 0.92 && snFrac >= 0.08 && snFrac <= 0.25) {
                context.add(MaterialProperties.KEY_HARDNESS, 1.2);
                context.add(MaterialProperties.KEY_DENSITY, 0.05);
                context.addTrace("phase:alpha_bronze");
            }
        }

        // Iron and carbon: a little carbon makes steel, hard and tough; past some two parts in a hundred it is cast
        // iron, which melts far lower (the eutectic) but is brittle.
        if (comp.constituentCount() == 2 && comp.contains(ConstituentData.ID_FE)
            && comp.contains(ConstituentData.ID_C)) {
            double carbon = comp.fraction(ConstituentData.ID_C);
            if (carbon >= 0.002 && carbon <= 0.021) {
                context.add(MaterialProperties.KEY_HARDNESS, 2.0);
                context.add(MaterialProperties.KEY_MELTING_POINT, -80.0);
                context.addTrait("tough");
                context.addTrace("phase:steel");
            } else if (carbon > 0.021) {
                context.add(MaterialProperties.KEY_HARDNESS, 1.0);
                context.add(MaterialProperties.KEY_MELTING_POINT, -400.0);
                context.addTrait("brittle");
                context.addTrace("phase:cast_iron");
            }
        }
    }
}
