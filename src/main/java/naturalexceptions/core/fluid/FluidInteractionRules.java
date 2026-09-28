package naturalexceptions.core.fluid;

/** Domain hook for deciding whether two liquid parcels merge, layer, or transform. */
public interface FluidInteractionRules {

    enum Result { MISCIBLE, IMMISCIBLE, TRANSFORM }

    Result classify(FluidParcel lower, FluidParcel incoming);

    /** Hook for a declared reaction. Return {@code null} when no transformation is registered. */
    default FluidTransformation transform(FluidParcel existing, FluidParcel incoming) {
        return null;
    }

    static FluidInteractionRules sameMaterialOnly() {
        return (a, b) -> a.materialRef().equals(b.materialRef())
            && a.composition().equals(b.composition()) ? Result.MISCIBLE : Result.IMMISCIBLE;
    }

    static FluidInteractionRules immiscibleByDefault() {
        return (a, b) -> a.materialRef().equals(b.materialRef())
            && a.composition().equals(b.composition()) ? Result.MISCIBLE : Result.IMMISCIBLE;
    }
}
