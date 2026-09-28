package naturalexceptions.core.fluid;

import java.util.Objects;

import naturalexceptions.core.material.MaterialRef;

/** Declared environmental source/sink boundary, separate from finite stored liquid. */
public final class FluidSourceBoundary {
    private final String stableId;
    private final MaterialRef materialRef;
    private final boolean infinite;

    public FluidSourceBoundary(String stableId, MaterialRef materialRef, boolean infinite) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Boundary ID required");
        this.stableId = stableId;
        this.materialRef = Objects.requireNonNull(materialRef, "materialRef");
        this.infinite = infinite;
    }

    public static FluidSourceBoundary infinite(String stableId, MaterialRef materialRef) {
        return new FluidSourceBoundary(stableId, materialRef, true);
    }

    public String stableId() { return stableId; }
    public MaterialRef materialRef() { return materialRef; }
    public boolean infinite() { return infinite; }
}
