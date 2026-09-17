package naturalexceptions.core.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaterialCatalogue {

    private final Map<MaterialId, MaterialDefinition> definitions = new LinkedHashMap<>();

    public void register(MaterialDefinition definition) {
        if (definitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate material id: " + definition.id());
        }
    }

    public MaterialDefinition require(MaterialId id) {
        MaterialDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material id: " + id);
        }
        return definition;
    }

    public Map<MaterialId, MaterialDefinition> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }
}
