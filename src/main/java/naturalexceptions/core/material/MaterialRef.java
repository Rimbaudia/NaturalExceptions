package naturalexceptions.core.material;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/**
 * Universal canonical material reference boundary.
 *
 * <p>
 * Identifies either:
 * <ul>
 * <li>an {@link MaterialRefKind#AUTHORED} material backed by a stable {@link ResourceId}; or</li>
 * <li>a {@link MaterialRefKind#GENERATED} material backed by a {@link DeterministicId}
 * derived from canonical composition and intrinsic identity state.</li>
 * </ul>
 *
 * <p>
 * Player-facing aliases, discoveries, and dynamic physical states (e.g. current temperature)
 * are explicitly excluded from this identity.
 */
public final class MaterialRef implements Comparable<MaterialRef> {

    public static final String PREFIX_AUTHORED = "mat:authored:";
    public static final String PREFIX_GENERATED = "mat:generated:";

    private final MaterialRefKind kind;
    private final ResourceId authoredId;
    private final DeterministicId generatedId;
    private final String encoded;

    private MaterialRef(MaterialRefKind kind, ResourceId authoredId, DeterministicId generatedId) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.authoredId = authoredId;
        this.generatedId = generatedId;
        if (kind == MaterialRefKind.AUTHORED) {
            this.encoded = PREFIX_AUTHORED + authoredId.toString();
        } else {
            this.encoded = PREFIX_GENERATED + generatedId.hex();
        }
    }

    public static MaterialRef authored(ResourceId id) {
        if (id == null) {
            throw new IllegalArgumentException("Authored ResourceId cannot be null");
        }
        return new MaterialRef(MaterialRefKind.AUTHORED, id, null);
    }

    public static MaterialRef authored(MaterialId id) {
        if (id == null) {
            throw new IllegalArgumentException("Authored MaterialId cannot be null");
        }
        return authored(id.resourceId());
    }

    public static MaterialRef authored(String namespace, String path) {
        return authored(ResourceId.of(namespace, path));
    }

    public static MaterialRef generated(DeterministicId id) {
        if (id == null) {
            throw new IllegalArgumentException("Generated DeterministicId cannot be null");
        }
        return new MaterialRef(MaterialRefKind.GENERATED, null, id);
    }

    public static MaterialRef generatedFrom(CanonicalMaterialKey key) {
        if (key == null) {
            throw new IllegalArgumentException("CanonicalMaterialKey cannot be null");
        }
        DeterministicId detId = DeterministicId.derive(
            "material",
            key.composition()
                .canonicalString(),
            key.phase()
                .id(),
            key.microstructure()
                .id(),
            key.conditioning()
                .id());
        return generated(detId);
    }

    public static MaterialRef generatedFrom(CanonicalComposition composition) {
        if (composition == null) {
            throw new IllegalArgumentException("CanonicalComposition cannot be null");
        }
        return generatedFrom(CanonicalMaterialKey.of(composition));
    }

    public static MaterialRef parse(String str) {
        if (str == null || str.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("MaterialRef string cannot be null or empty");
        }
        if (str.startsWith(PREFIX_AUTHORED)) {
            String pathPart = str.substring(PREFIX_AUTHORED.length());
            return authored(ResourceId.parse(pathPart));
        }
        if (str.startsWith(PREFIX_GENERATED)) {
            String hexPart = str.substring(PREFIX_GENERATED.length());
            return generated(DeterministicId.parse(hexPart));
        }
        throw new IllegalArgumentException(
            "Expected MaterialRef prefixed with " + PREFIX_AUTHORED + " or " + PREFIX_GENERATED + ": " + str);
    }

    public MaterialRefKind kind() {
        return kind;
    }

    public boolean isAuthored() {
        return kind == MaterialRefKind.AUTHORED;
    }

    public boolean isGenerated() {
        return kind == MaterialRefKind.GENERATED;
    }

    public ResourceId authoredId() {
        return authoredId;
    }

    public DeterministicId generatedId() {
        return generatedId;
    }

    public String asString() {
        return encoded;
    }

    @Override
    public String toString() {
        return encoded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialRef)) return false;
        MaterialRef that = (MaterialRef) o;
        if (kind != that.kind) return false;
        if (kind == MaterialRefKind.AUTHORED) {
            return authoredId.equals(that.authoredId);
        } else {
            return generatedId.equals(that.generatedId);
        }
    }

    @Override
    public int hashCode() {
        return kind == MaterialRefKind.AUTHORED ? authoredId.hashCode() : generatedId.hashCode();
    }

    @Override
    public int compareTo(MaterialRef other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare MaterialRef to null");
        }
        int cmpKind = this.kind.compareTo(other.kind);
        if (cmpKind != 0) {
            return cmpKind;
        }
        if (this.kind == MaterialRefKind.AUTHORED) {
            return this.authoredId.compareTo(other.authoredId);
        } else {
            return this.generatedId.compareTo(other.generatedId);
        }
    }
}
