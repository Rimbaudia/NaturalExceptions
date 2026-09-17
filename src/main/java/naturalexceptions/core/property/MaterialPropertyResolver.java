package naturalexceptions.core.property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.GeneratedMaterial;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialRef;

/**
 * Layered material property resolver architecture spike.
 *
 * <p>
 * <b>PROVISIONAL ARCHITECTURE SPIKE NOTICE (P2.1E):</b>
 * This resolver exists strictly as an architectural feasibility spike for P2 to demonstrate
 * that a 4-layer resolution pipeline (baseline, interaction, phase window, anomaly override)
 * can deterministically evaluate properties without gaps or crashes.
 *
 * <p>
 * This is <b>NOT</b> a frozen P3 API, is <b>NOT</b> proof that P3 is complete, and will be
 * revised or replaced during the actual P3 thermal and metallurgy implementation.
 * No Minecraft gameplay, tile entity, or content mechanics depend on its internal constants.
 *
 * <p>
 * Pipeline order:
 * <ol>
 * <li>Baseline continuous mixing law (weighted constituent averages).</li>
 * <li>Constituent traits / interaction layer (pairwise or group interactions).</li>
 * <li>Known phase / alloy rules (metallurgical structure enrichment).</li>
 * <li>Supernatural / anomaly exceptions (reality-defying overrides).</li>
 * </ol>
 *
 * <p>
 * Every representable composition produces a valid result. Caching preserves results by {@link MaterialRef}.
 */
public final class MaterialPropertyResolver {

    private static final ResourceId ID_CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId ID_SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId ID_ZN = ResourceId.of("naturalexceptions", "zinc");
    private static final ResourceId ID_PB = ResourceId.of("naturalexceptions", "lead");
    private static final ResourceId ID_STELLAR = ResourceId.of("naturalexceptions", "stellar_matter");

    private final Map<MaterialRef, ResolvedProperties> cache = new HashMap<>();

    public synchronized ResolvedProperties resolve(GeneratedMaterial generated) {
        if (generated == null) {
            throw new IllegalArgumentException("GeneratedMaterial cannot be null");
        }
        return resolve(generated.ref(), generated.composition());
    }

    public synchronized ResolvedProperties resolve(MaterialDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("MaterialDefinition cannot be null");
        }
        MaterialRef ref = MaterialRef.authored(
            definition.id()
                .resourceId());
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(
                definition.id()
                    .resourceId(),
                100)
            .build();
        return resolve(ref, comp);
    }

    public synchronized ResolvedProperties resolve(MaterialRef ref, CanonicalComposition composition) {
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

        List<String> trace = new ArrayList<>();

        // Layer 1: Baseline continuous mixing law
        double baselineDensity = 0.0;
        double baselineHardness = 0.0;
        double baselineConductivity = 0.0;

        for (ResourceId constituent : composition.constituents()) {
            double frac = composition.fraction(constituent);
            baselineDensity += frac * constituentDensity(constituent);
            baselineHardness += frac * constituentHardness(constituent);
            baselineConductivity += frac * constituentConductivity(constituent);
        }
        trace.add("baseline");

        double density = baselineDensity;
        double hardness = baselineHardness;
        double conductivity = baselineConductivity;

        // Layer 2: Constituent interaction layer
        if (composition.contains(ID_CU) && composition.contains(ID_SN)) {
            double cuFrac = composition.fraction(ID_CU);
            double snFrac = composition.fraction(ID_SN);
            // Solid solution hardening effect
            double interactionBoost = 2.5 * Math.min(cuFrac, snFrac);
            hardness += interactionBoost;
            trace.add("interaction:cu_sn");
        }

        // Layer 3: Known phase / alloy rules
        if (composition.contains(ID_CU) && composition.contains(ID_SN)) {
            double cuFrac = composition.fraction(ID_CU);
            double snFrac = composition.fraction(ID_SN);
            // Alpha-bronze phase window: ~75-92% Cu, 8-25% Sn
            if (cuFrac >= 0.75 && cuFrac <= 0.92 && snFrac >= 0.08 && snFrac <= 0.25) {
                hardness += 1.2;
                density += 0.05;
                trace.add("phase:alpha_bronze");
            }
        }

        // Layer 4: Supernatural / anomaly exceptions
        if (composition.contains(ID_STELLAR)) {
            hardness = 99.0;
            conductivity = 500.0;
            trace.add("supernatural:stellar_override");
        }

        MaterialProperties finalProps = MaterialProperties.of(density, hardness, conductivity);
        ResolvedProperties result = new ResolvedProperties(finalProps, new ResolverTrace(trace));
        cache.put(ref, result);
        return result;
    }

    private static double constituentDensity(ResourceId id) {
        if (ID_CU.equals(id)) return 8.96;
        if (ID_SN.equals(id)) return 7.31;
        if (ID_ZN.equals(id)) return 7.14;
        if (ID_PB.equals(id)) return 11.34;
        if (ID_STELLAR.equals(id)) return 15.0;
        return 5.0; // default unknown metal density
    }

    private static double constituentHardness(ResourceId id) {
        if (ID_CU.equals(id)) return 3.0;
        if (ID_SN.equals(id)) return 1.5;
        if (ID_ZN.equals(id)) return 2.5;
        if (ID_PB.equals(id)) return 1.5;
        if (ID_STELLAR.equals(id)) return 10.0;
        return 2.0; // default unknown metal hardness
    }

    private static double constituentConductivity(ResourceId id) {
        if (ID_CU.equals(id)) return 100.0;
        if (ID_SN.equals(id)) return 15.0;
        if (ID_ZN.equals(id)) return 28.0;
        if (ID_PB.equals(id)) return 8.0;
        if (ID_STELLAR.equals(id)) return 200.0;
        return 10.0; // default unknown metal conductivity
    }

    public synchronized void clearCache() {
        cache.clear();
    }
}
