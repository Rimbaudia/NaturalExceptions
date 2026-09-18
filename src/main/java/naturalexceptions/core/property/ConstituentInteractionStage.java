package naturalexceptions.core.property;

import naturalexceptions.core.composition.CanonicalComposition;

/**
 * Layer 2: Constituent interaction layer (pairwise and group solute effects).
 */
public final class ConstituentInteractionStage implements PropertyResolverStage {

    @Override
    public String stageName() {
        return "interaction";
    }

    @Override
    public void apply(ResolutionContext context) {
        CanonicalComposition comp = context.composition();

        // Cu-Sn solid solution interaction
        if (comp.contains(ConstituentData.ID_CU) && comp.contains(ConstituentData.ID_SN)) {
            double cuFrac = comp.fraction(ConstituentData.ID_CU);
            double snFrac = comp.fraction(ConstituentData.ID_SN);
            // Solid solution hardening effect
            double interactionBoost = 2.5 * Math.min(cuFrac, snFrac);
            context.add(MaterialProperties.KEY_HARDNESS, interactionBoost);
            context.addTrace("interaction:cu_sn");
        }

        // Cu-Zn solid solution interaction (brass)
        if (comp.contains(ConstituentData.ID_CU) && comp.contains(ConstituentData.ID_ZN)) {
            double cuFrac = comp.fraction(ConstituentData.ID_CU);
            double znFrac = comp.fraction(ConstituentData.ID_ZN);
            double interactionBoost = 1.8 * Math.min(cuFrac, znFrac);
            context.add(MaterialProperties.KEY_HARDNESS, interactionBoost);
            context.addTrace("interaction:cu_zn");
        }
    }
}
