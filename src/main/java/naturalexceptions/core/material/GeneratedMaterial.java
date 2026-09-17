package naturalexceptions.core.material;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;

/**
 * Representation of an interned canonical generated material.
 */
public final class GeneratedMaterial {

    private final MaterialRef ref;
    private final CanonicalComposition composition;

    public GeneratedMaterial(MaterialRef ref, CanonicalComposition composition) {
        this.ref = Objects.requireNonNull(ref, "ref");
        this.composition = Objects.requireNonNull(composition, "composition");
        if (!ref.isGenerated()) {
            throw new IllegalArgumentException("GeneratedMaterial must have a GENERATED MaterialRef: " + ref);
        }
    }

    public MaterialRef ref() {
        return ref;
    }

    public CanonicalComposition composition() {
        return composition;
    }

    public DeterministicId id() {
        return ref.generatedId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeneratedMaterial)) return false;
        GeneratedMaterial that = (GeneratedMaterial) o;
        return ref.equals(that.ref) && composition.equals(that.composition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, composition);
    }

    @Override
    public String toString() {
        return "GeneratedMaterial{" + "ref=" + ref + ", composition=" + composition + '}';
    }
}
