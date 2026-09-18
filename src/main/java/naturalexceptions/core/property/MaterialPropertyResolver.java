package naturalexceptions.core.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.GeneratedMaterial;
import naturalexceptions.core.material.MaterialDefinition;
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
 * <li><b>Caching:</b> Preserves derived {@link ResolvedProperties} keyed by {@link MaterialRef}.</li>
 * </ol>
 *
 * <p>
 * Universal baseline guarantee: Every representable composition produces physically valid properties.
 * Cache eviction never mutates or destroys canonical material truth.
 */
public final class MaterialPropertyResolver {

    private final List<PropertyResolverStage> stages;
    private final Map<MaterialRef, ResolvedProperties> cache = new ConcurrentHashMap<>();

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
        return resolve(generated.ref(), generated.composition());
    }

    public ResolvedProperties resolve(MaterialDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("MaterialDefinition cannot be null");
        }
        MaterialRef ref = MaterialRef.authored(
            definition.id()
                .resourceId());
        if (definition.properties() != null) {
            ResolvedProperties cached = cache.get(ref);
            if (cached != null) {
                return cached;
            }
            ResolvedProperties res = new ResolvedProperties(
                definition.properties(),
                new ResolverTrace(Collections.singletonList("authored:definition")));
            cache.put(ref, res);
            return res;
        }
        CanonicalComposition comp = definition.standardComposition() != null ? definition.standardComposition()
            : CanonicalComposition.builder()
                .add(
                    definition.id()
                        .resourceId(),
                    100)
                .build();
        return resolve(ref, comp);
    }

    public ResolvedProperties resolve(MaterialRef ref, CanonicalComposition composition) {
        if (ref == null) {
            throw new IllegalArgumentException("MaterialRef cannot be null");
        }
        if (composition == null) {
            throw new IllegalArgumentException("CanonicalComposition cannot be null");
        }
        ResolvedProperties cached = cache.get(ref);
        if (cached != null) {
            return cached;
        }

        ResolutionContext context = new ResolutionContext(ref, composition);
        for (PropertyResolverStage stage : stages) {
            stage.apply(context);
        }

        ResolvedProperties result = new ResolvedProperties(context.buildProperties(), context.buildTrace());
        cache.put(ref, result);
        return result;
    }

    public boolean isCached(MaterialRef ref) {
        return cache.containsKey(ref);
    }

    public int cacheSize() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
    }
}
