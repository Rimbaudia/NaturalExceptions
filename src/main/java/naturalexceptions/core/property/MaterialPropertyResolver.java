package naturalexceptions.core.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.GeneratedMaterial;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;

/**
 * Production layered material property resolver.
 *
 * <p>
 * Resolution pipeline order:
 * <ol>
 * <li><b>Baseline continuous mixing law:</b> Weighted constituent elemental averages for density,
 * specific heats, thermal conductivity, melting point, latent heat, hardness, and electrical resistivity.</li>
 * <li><b>Constituent traits / interaction layer:</b> Pairwise and solute interactions (e.g. solid-solution
 * hardening).</li>
 * <li><b>Known phase / alloy rules:</b> Metallurgical phase windows and intermetallic formation (e.g.
 * alpha-bronze).</li>
 * <li><b>Supernatural exceptions:</b> Anomaly overrides defying mundane material physics (e.g. stellar matter).</li>
 * <li><b>Caching:</b> Preserves derived {@link ResolvedProperties} keyed by {@link MaterialRef} and
 * {@link MaterialPhase}.</li>
 * </ol>
 *
 * <p>
 * Universal baseline guarantee: Every representable composition produces physically valid properties.
 * Cache eviction never mutates or destroys canonical material truth.
 */
public final class MaterialPropertyResolver {

    private static final class CacheKey {

        private final MaterialRef ref;
        private final MaterialPhase phase;

        CacheKey(MaterialRef ref, MaterialPhase phase) {
            this.ref = Objects.requireNonNull(ref, "ref");
            this.phase = phase != null ? phase : MaterialPhase.SOLID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey cacheKey = (CacheKey) o;
            return ref.equals(cacheKey.ref) && phase == cacheKey.phase;
        }

        @Override
        public int hashCode() {
            return 31 * ref.hashCode() + phase.hashCode();
        }
    }

    private final List<PropertyResolverStage> stages;
    private final Map<CacheKey, ResolvedProperties> cache = new ConcurrentHashMap<>();

    public MaterialPropertyResolver() {
        this(defaultStages());
    }

    public MaterialPropertyResolver(List<PropertyResolverStage> stages) {
        this.stages = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(stages, "stages")));
    }

    public static List<PropertyResolverStage> defaultStages() {
        return Arrays.asList(
            new ContinuousMixingStage(),
            new ConstituentInteractionStage(),
            new PhaseAlloyRuleStage(),
            new SupernaturalExceptionStage());
    }

    public List<PropertyResolverStage> stages() {
        return stages;
    }

    public ResolvedProperties resolve(GeneratedMaterial generated) {
        if (generated == null) {
            throw new IllegalArgumentException("GeneratedMaterial cannot be null");
        }
        return resolve(generated.ref(), generated.composition(), generated.phase());
    }

    public ResolvedProperties resolve(CanonicalMaterialKey key) {
        if (key == null) {
            throw new IllegalArgumentException("CanonicalMaterialKey cannot be null");
        }
        return resolve(MaterialRef.generatedFrom(key), key.composition(), key.phase());
    }

    public ResolvedProperties resolve(MaterialDefinition definition) {
        return resolve(definition, MaterialPhase.SOLID);
    }

    public ResolvedProperties resolve(MaterialDefinition definition, MaterialPhase phase) {
        if (definition == null) {
            throw new IllegalArgumentException("MaterialDefinition cannot be null");
        }
        MaterialPhase targetPhase = phase != null ? phase : MaterialPhase.SOLID;
        MaterialRef ref = MaterialRef.authored(
            definition.id()
                .resourceId());
        CacheKey cacheKey = new CacheKey(ref, targetPhase);
        ResolvedProperties cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (definition.properties() != null) {
            MaterialProperties props = targetPhase == MaterialPhase.LIQUID ? definition.properties()
                .forPhase(targetPhase) : definition.properties();
            ResolvedProperties res = new ResolvedProperties(
                props,
                new ResolverTrace(
                    Collections.singletonList(
                        "authored:definition" + (targetPhase == MaterialPhase.LIQUID ? ":liquid" : ""))));
            cache.put(cacheKey, res);
            return res;
        }
        CanonicalComposition comp = definition.standardComposition() != null ? definition.standardComposition()
            : CanonicalComposition.builder()
                .add(
                    definition.id()
                        .resourceId(),
                    100)
                .build();
        return resolve(ref, comp, targetPhase);
    }

    public ResolvedProperties resolve(MaterialRef ref, CanonicalComposition composition) {
        return resolve(ref, composition, MaterialPhase.SOLID);
    }

    public ResolvedProperties resolve(MaterialRef ref, CanonicalComposition composition, MaterialPhase phase) {
        if (ref == null) {
            throw new IllegalArgumentException("MaterialRef cannot be null");
        }
        if (composition == null) {
            throw new IllegalArgumentException("CanonicalComposition cannot be null");
        }
        MaterialPhase targetPhase = phase != null ? phase : MaterialPhase.SOLID;
        CacheKey cacheKey = new CacheKey(ref, targetPhase);
        ResolvedProperties cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        ResolutionContext context = new ResolutionContext(ref, composition, targetPhase);
        for (PropertyResolverStage stage : stages) {
            stage.apply(context);
        }

        ResolvedProperties result = new ResolvedProperties(context.buildProperties(), context.buildTrace());
        cache.put(cacheKey, result);
        return result;
    }

    public boolean isCached(MaterialRef ref) {
        return isCached(ref, MaterialPhase.SOLID);
    }

    public boolean isCached(MaterialRef ref, MaterialPhase phase) {
        return cache.containsKey(new CacheKey(ref, phase));
    }

    public int cacheSize() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
    }
}
