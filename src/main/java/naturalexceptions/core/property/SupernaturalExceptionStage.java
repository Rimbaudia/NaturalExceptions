package naturalexceptions.core.property;

import naturalexceptions.core.composition.CanonicalComposition;

/**
 * Layer 4: Supernatural / anomaly exceptions (reality-defying material overrides).
 */
public final class SupernaturalExceptionStage implements PropertyResolverStage {

    @Override
    public String stageName() {
        return "supernatural";
    }

    @Override
    public void apply(ResolutionContext context) {
        CanonicalComposition comp = context.composition();

        if (comp.contains(ConstituentData.ID_STELLAR)) {
            context.set(MaterialProperties.KEY_HARDNESS, 99.0);
            context.set(MaterialProperties.KEY_CONDUCTIVITY, 500.0);
            context.set(MaterialProperties.KEY_THERMAL_CONDUCTIVITY, 5000.0);
            context.set(MaterialProperties.KEY_MELTING_POINT, 3500.0);
            context.addTrace("supernatural:stellar_override");
        }
    }
}
