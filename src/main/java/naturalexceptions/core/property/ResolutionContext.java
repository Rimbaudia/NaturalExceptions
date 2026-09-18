package naturalexceptions.core.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;

/**
 * Mutable context passed through the layered resolution stages to build final {@link ResolvedProperties}.
 */
public final class ResolutionContext {

    private final MaterialRef ref;
    private final CanonicalComposition composition;
    private final MaterialPhase phase;
    private final Map<String, Double> properties = new HashMap<>();
    private final Set<String> traits = new LinkedHashSet<>();
    private final List<String> trace = new ArrayList<>();

    public ResolutionContext(MaterialRef ref, CanonicalComposition composition) {
        this(ref, composition, MaterialPhase.SOLID);
    }

    public ResolutionContext(MaterialRef ref, CanonicalComposition composition, MaterialPhase phase) {
        this.ref = Objects.requireNonNull(ref, "ref");
        this.composition = Objects.requireNonNull(composition, "composition");
        this.phase = phase != null ? phase : MaterialPhase.SOLID;
    }

    public MaterialRef ref() {
        return ref;
    }

    public CanonicalComposition composition() {
        return composition;
    }

    public MaterialPhase phase() {
        return phase;
    }

    public double get(String key, double defaultValue) {
        Double val = properties.get(key);
        return val != null ? val : defaultValue;
    }

    public void set(String key, double value) {
        properties.put(key, value);
    }

    public void add(String key, double delta) {
        set(key, get(key, 0.0) + delta);
    }

    public void multiply(String key, double factor) {
        set(key, get(key, 0.0) * factor);
    }

    public void addTrait(String trait) {
        if (trait != null && !trait.isEmpty()) {
            traits.add(trait);
        }
    }

    public void addTrace(String stageEntry) {
        if (stageEntry != null && !stageEntry.isEmpty()) {
            trace.add(stageEntry);
        }
    }

    public Set<String> traits() {
        return Collections.unmodifiableSet(traits);
    }

    public List<String> trace() {
        return Collections.unmodifiableList(trace);
    }

    public MaterialProperties buildProperties() {
        return new MaterialProperties(properties, traits);
    }

    public ResolverTrace buildTrace() {
        return new ResolverTrace(trace);
    }
}
