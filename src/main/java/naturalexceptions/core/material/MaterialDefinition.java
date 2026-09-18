package naturalexceptions.core.material;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.property.MaterialProperties;

public final class MaterialDefinition {

    private final MaterialId id;
    private final String translationKey;
    private final CanonicalComposition standardComposition;
    private final MaterialProperties properties;

    public MaterialDefinition(MaterialId id, String translationKey) {
        this(id, translationKey, null, null);
    }

    public MaterialDefinition(MaterialId id, String translationKey, CanonicalComposition standardComposition,
        MaterialProperties properties) {
        this.id = Objects.requireNonNull(id, "id");
        this.translationKey = Objects.requireNonNull(translationKey, "translationKey");
        this.standardComposition = standardComposition;
        this.properties = properties;
    }

    public MaterialId id() {
        return id;
    }

    public MaterialRef ref() {
        return MaterialRef.authored(id.resourceId());
    }

    public String translationKey() {
        return translationKey;
    }

    public CanonicalComposition standardComposition() {
        return standardComposition;
    }

    public MaterialProperties properties() {
        return properties;
    }
}
