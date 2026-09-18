package naturalexceptions.core.property;

import naturalexceptions.core.composition.CanonicalComposition;

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
    }
}
