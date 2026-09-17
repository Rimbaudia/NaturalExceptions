package naturalexceptions.core.material;

import java.util.Objects;

public final class MaterialDefinition {

    private final MaterialId id;
    private final String translationKey;

    public MaterialDefinition(MaterialId id, String translationKey) {
        this.id = Objects.requireNonNull(id, "id");
        this.translationKey = Objects.requireNonNull(translationKey, "translationKey");
    }

    public MaterialId id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }
}
