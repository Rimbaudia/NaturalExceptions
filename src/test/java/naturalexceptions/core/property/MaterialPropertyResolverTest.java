package naturalexceptions.core.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;

final class MaterialPropertyResolverTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId ZN = ResourceId.of("naturalexceptions", "zinc");
    private static final ResourceId STELLAR = ResourceId.of("naturalexceptions", "stellar_matter");
    private static final ResourceId UNKNOWN = ResourceId.of("custommod", "unknown_metal");

    @Test
    void pureCopperReceivesElementalProperties() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition pureCu = CanonicalComposition.of(CU);
        MaterialRef ref = MaterialRef.generatedFrom(pureCu);

        ResolvedProperties result = resolver.resolve(ref, pureCu);

        assertNotNull(result);
        assertEquals(
            8.96,
            result.properties()
                .density(),
            0.01);
        assertEquals(
            3.0,
            result.properties()
                .hardness(),
            0.01);
        assertEquals(
            100.0,
            result.properties()
                .conductivity(),
            0.01);
        assertTrue(
            result.trace()
                .layerApplied("baseline"));
        assertFalse(
            result.trace()
                .layerApplied("interaction:cu_sn"));
        assertFalse(
            result.trace()
                .layerApplied("phase:alpha_bronze"));
        assertFalse(
            result.trace()
                .layerApplied("supernatural:stellar_override"));
    }

    @Test
    void arbitraryUnknownCompositionReceivesValidBaseline() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(UNKNOWN, 60)
            .add(ZN, 40)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(comp);

        ResolvedProperties result = resolver.resolve(ref, comp);

        assertNotNull(result);
        assertTrue(
            result.properties()
                .density() > 0.0);
        assertTrue(
            result.properties()
                .hardness() > 0.0);
        assertTrue(
            result.properties()
                .conductivity() > 0.0);
        assertTrue(
            result.trace()
                .layerApplied("baseline"));
    }

    @Test
    void interactionLayerEnrichesCuSnMixture() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        // 95% Cu, 5% Sn (below alpha-bronze phase window, but triggers Cu-Sn interaction)
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 95)
            .add(SN, 5)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(comp);

        ResolvedProperties result = resolver.resolve(ref, comp);

        assertTrue(
            result.trace()
                .layerApplied("baseline"));
        assertTrue(
            result.trace()
                .layerApplied("interaction:cu_sn"));
        assertFalse(
            result.trace()
                .layerApplied("phase:alpha_bronze"));

        // Baseline hardness would be 0.95*3.0 + 0.05*1.5 = 2.925
        // Interaction adds hardening
        assertTrue(
            result.properties()
                .hardness() > 2.925);
    }

    @Test
    void knownPhaseAlloyRuleAppliesWhenInPhaseWindow() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        // 88% Cu, 12% Sn: classic alpha-bronze
        CanonicalComposition bronze = CanonicalComposition.builder()
            .add(CU, 88)
            .add(SN, 12)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(bronze);

        ResolvedProperties result = resolver.resolve(ref, bronze);

        assertTrue(
            result.trace()
                .layerApplied("baseline"));
        assertTrue(
            result.trace()
                .layerApplied("interaction:cu_sn"));
        assertTrue(
            result.trace()
                .layerApplied("phase:alpha_bronze"));
        // Bronze has high hardness due to phase formation
        assertTrue(
            result.properties()
                .hardness() >= 4.0);
    }

    @Test
    void supernaturalExceptionOverridesLowerLayers() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        // Bronze with 10% stellar matter
        CanonicalComposition stellarAlloy = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 10)
            .add(STELLAR, 10)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(stellarAlloy);

        ResolvedProperties result = resolver.resolve(ref, stellarAlloy);

        assertTrue(
            result.trace()
                .layerApplied("baseline"));
        assertTrue(
            result.trace()
                .layerApplied("supernatural:stellar_override"));
        // Stellar override sets supernatural hardness
        assertEquals(
            99.0,
            result.properties()
                .hardness(),
            0.01);
    }

    @Test
    void cachingReturnsSameInstanceWithoutRecomputation() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(comp);

        ResolvedProperties first = resolver.resolve(ref, comp);
        ResolvedProperties second = resolver.resolve(ref, comp);

        assertEquals(first.properties(), second.properties());
        assertEquals(
            first.trace()
                .appliedLayers(),
            second.trace()
                .appliedLayers());
    }
}
