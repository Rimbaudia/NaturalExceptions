package naturalexceptions.core.material;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;

public final class MaterialCatalogue {

    private final Map<MaterialId, MaterialDefinition> definitions = new LinkedHashMap<>();
    private final Map<MaterialRef, MaterialDefinition> authoredByRef = new HashMap<>();
    private final Map<MaterialRef, GeneratedMaterial> generatedByRef = new HashMap<>();
    private final Map<CanonicalMaterialKey, GeneratedMaterial> generatedByKey = new HashMap<>();

    public synchronized void register(MaterialDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        if (definitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate material id: " + definition.id());
        }
        MaterialRef ref = MaterialRef.authored(
            definition.id()
                .resourceId());
        authoredByRef.put(ref, definition);
    }

    public synchronized MaterialDefinition require(MaterialId id) {
        MaterialDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material id: " + id);
        }
        return definition;
    }

    public synchronized MaterialDefinition getAuthored(MaterialRef ref) {
        if (ref == null || !ref.isAuthored()) {
            return null;
        }
        return authoredByRef.get(ref);
    }

    public synchronized MaterialDefinition getByRef(MaterialRef ref) {
        return getAuthored(ref);
    }

    public synchronized GeneratedMaterial getOrRegisterGenerated(CanonicalMaterialKey key) {
        Objects.requireNonNull(key, "key");
        GeneratedMaterial existing = generatedByKey.get(key);
        if (existing != null) {
            return existing;
        }
        MaterialRef ref = MaterialRef.generatedFrom(key);
        GeneratedMaterial created = new GeneratedMaterial(ref, key);
        generatedByRef.put(ref, created);
        generatedByKey.put(key, created);
        return created;
    }

    public synchronized GeneratedMaterial getOrRegisterGenerated(CanonicalComposition composition) {
        Objects.requireNonNull(composition, "composition");
        return getOrRegisterGenerated(CanonicalMaterialKey.of(composition));
    }

    public synchronized GeneratedMaterial getGenerated(MaterialRef ref) {
        if (ref == null || !ref.isGenerated()) {
            return null;
        }
        return generatedByRef.get(ref);
    }

    public synchronized boolean contains(MaterialRef ref) {
        if (ref == null) return false;
        return ref.isAuthored() ? authoredByRef.containsKey(ref) : generatedByRef.containsKey(ref);
    }

    public synchronized int authoredCount() {
        return authoredByRef.size();
    }

    public synchronized int generatedCount() {
        return generatedByRef.size();
    }

    public synchronized void clearGenerated() {
        generatedByRef.clear();
        generatedByKey.clear();
    }

    public synchronized Map<MaterialId, MaterialDefinition> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
    }
}
