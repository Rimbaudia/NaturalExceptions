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
            // The authored stellar constituent carries ordinary, processable mechanical/thermal values. The
            // exception is its anomalous heat-transfer affinity, which increases continuously with its fraction.
            double fraction = comp.fraction(ConstituentData.ID_STELLAR);
            context.add(MaterialProperties.KEY_THERMAL_CONDUCTIVITY, 120.0 * fraction);
            context.addTrace("supernatural:stellar_override");
        }
    }
}
