package naturalexceptions.core.material;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;

/**
 * Representation of an interned canonical generated material.
 *
 * <p>
 * Holds the canonical {@link MaterialRef} and the complete {@link CanonicalMaterialKey}
 * (composition, phase, microstructure, conditioning).
 */
public final class GeneratedMaterial {

    private final MaterialRef ref;
    private final CanonicalMaterialKey key;

    public GeneratedMaterial(MaterialRef ref, CanonicalMaterialKey key) {
        this.ref = Objects.requireNonNull(ref, "ref");
        this.key = Objects.requireNonNull(key, "key");
        if (!ref.isGenerated()) {
            throw new IllegalArgumentException("GeneratedMaterial must have a GENERATED MaterialRef: " + ref);
        }
    }

    public GeneratedMaterial(MaterialRef ref, CanonicalComposition composition) {
        this(ref, CanonicalMaterialKey.of(composition));
    }

    public MaterialRef ref() {
        return ref;
    }

    public CanonicalMaterialKey key() {
        return key;
    }

    public CanonicalComposition composition() {
        return key.composition();
    }

    public MaterialPhase phase() {
        return key.phase();
    }

    public Microstructure microstructure() {
        return key.microstructure();
    }

    public ConditioningHistory conditioning() {
        return key.conditioning();
    }

    public DeterministicId id() {
        return ref.generatedId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeneratedMaterial)) return false;
        GeneratedMaterial that = (GeneratedMaterial) o;
        return ref.equals(that.ref) && key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, key);
    }

    @Override
    public String toString() {
        return "GeneratedMaterial{" + "ref=" + ref + ", key=" + key + '}';
    }
}
