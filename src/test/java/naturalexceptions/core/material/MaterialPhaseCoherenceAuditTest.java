package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemMaterialSample;

final class MaterialPhaseCoherenceAuditTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");

    @Test
    void authoredSamplePhaseStateCannotDisagreeWithResolver() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();

        // 1. Solid ambient state
        ActiveThermalState ambientThermal = ActiveThermalState.ambient(1.0, BuiltinMaterials.COPPER_PROPERTIES);
        assertEquals(MaterialPhase.SOLID, ambientThermal.phase());

        ResolvedProperties solidProps = resolver.resolve(BuiltinMaterials.COPPER, ambientThermal.phase());
        assertEquals(
            3.0,
            solidProps.properties()
                .hardness(),
            0.01);
        assertEquals(MaterialPhase.SOLID, ambientThermal.phase());

        // 2. Liquid heated state
        ActiveThermalState liquidThermal = ActiveThermalState
            .atTemperature(1500.0, 1.0, 1.0, BuiltinMaterials.COPPER_PROPERTIES);
        assertEquals(MaterialPhase.LIQUID, liquidThermal.phase());

        ResolvedProperties liquidProps = resolver.resolve(BuiltinMaterials.COPPER, liquidThermal.phase());
        assertEquals(
            0.0,
            liquidProps.properties()
                .hardness(),
            0.01);
        assertEquals(MaterialPhase.LIQUID, liquidThermal.phase());
    }

    @Test
    void generatedSamplePhaseStateCannotDisagreeWithResolver() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 88)
            .add(SN, 12)
            .build();

        // Solid state
        CanonicalMaterialKey solidKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        ResolvedProperties solidProps = resolver.resolve(solidKey);
        assertTrue(
            solidProps.properties()
                .hardness() > 3.0,
            "Solid alpha-bronze must have solid hardness");

        // Liquid thermal state
        ActiveThermalState liquidThermal = ActiveThermalState.atTemperature(1400.0, 1.0, 1.0, solidProps.properties());
        assertEquals(MaterialPhase.LIQUID, liquidThermal.phase());

        // Synchronized key
        CanonicalMaterialKey liquidKey = solidKey.withPhase(liquidThermal.phase());
        ResolvedProperties liquidProps = resolver.resolve(liquidKey);

        assertEquals(
            0.0,
            liquidProps.properties()
                .hardness(),
            0.01);
        assertEquals(MaterialPhase.LIQUID, liquidKey.phase());
        assertEquals(liquidThermal.phase(), liquidKey.phase());
    }

    @Test
    void liquidStateDoesNotReceiveCachedSolidProperties() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        MaterialRef cuRef = MaterialRef.authored(BuiltinMaterials.COPPER_RESOURCE_ID);

        // Pre-warm cache with solid copper
        ResolvedProperties solidProps = resolver.resolve(BuiltinMaterials.COPPER, MaterialPhase.SOLID);
        assertEquals(
            3.0,
            solidProps.properties()
                .hardness(),
            0.01);
        assertTrue(resolver.isCached(cuRef, MaterialPhase.SOLID));
        assertFalse(resolver.isCached(cuRef, MaterialPhase.LIQUID));

        // Query liquid copper: MUST NOT return cached solid properties
        ResolvedProperties liquidProps = resolver.resolve(BuiltinMaterials.COPPER, MaterialPhase.LIQUID);
        assertEquals(
            0.0,
            liquidProps.properties()
                .hardness(),
            0.01);
        assertTrue(resolver.isCached(cuRef, MaterialPhase.LIQUID));

        // Query solid again: verifies cache was not overwritten or corrupted
        ResolvedProperties solidAgain = resolver.resolve(BuiltinMaterials.COPPER, MaterialPhase.SOLID);
        assertEquals(
            3.0,
            solidAgain.properties()
                .hardness(),
            0.01);
    }

    @Test
    void phaseTransitionMaintainsDeterministicMaterialLineage() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 88)
            .add(SN, 12)
            .build();

        CanonicalMaterialKey solidKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.COLD_WORKED);
        MaterialRef solidRef = MaterialRef.generatedFrom(solidKey);

        // Phase change: melting
        CanonicalMaterialKey moltenKey = solidKey.asMolten();
        assertEquals(MaterialPhase.LIQUID, moltenKey.phase());
        assertEquals(ConditioningHistory.NONE, moltenKey.conditioning(), "Melting erases cold work");
        assertEquals(comp, moltenKey.composition(), "Composition invariant through melting");

        MaterialRef moltenRef = MaterialRef.generatedFrom(moltenKey);
        assertFalse(solidRef.equals(moltenRef), "Phase change creates distinct phase-state MaterialRef");

        // Phase change: solidification
        CanonicalMaterialKey resolidifiedKey = moltenKey.asSolidified();
        assertEquals(MaterialPhase.SOLID, resolidifiedKey.phase());
        assertEquals(Microstructure.AS_CAST, resolidifiedKey.microstructure());
        assertEquals(ConditioningHistory.NONE, resolidifiedKey.conditioning());
        assertEquals(comp, resolidifiedKey.composition());

        // Reheating: deterministic round trip
        CanonicalMaterialKey remoltenKey = resolidifiedKey.asMolten();
        assertEquals(moltenKey, remoltenKey);
        assertEquals(moltenRef, MaterialRef.generatedFrom(remoltenKey));
    }

    @Test
    void saveReloadAtPartialMeltPreservesEquivalentState() {
        // Copper at melting point (1357.77 K), 50% melted mushy state
        ActiveThermalState mushyState = ActiveThermalState
            .atTemperature(1357.77, 0.50, 2.5, BuiltinMaterials.COPPER_PROPERTIES);
        assertEquals(0.50, mushyState.meltFraction(), 1.0e-4);

        ItemStack stack = ItemMaterialSample
            .createStack(MaterialRef.authored(BuiltinMaterials.COPPER_RESOURCE_ID), null, mushyState);

        ActiveThermalState restored = ItemMaterialSample.getThermalState(stack, BuiltinMaterials.COPPER_PROPERTIES);
        assertNotNull(restored);
        assertEquals(mushyState.energyJoules(), restored.energyJoules(), 1.0);
        assertEquals(2.5, restored.massKg(), 1.0e-6);
        assertEquals(1357.77, restored.temperatureKelvin(), 0.05);
        assertEquals(0.50, restored.meltFraction(), 1.0e-3);
    }

    @Test
    void saveReloadAtLiquidStatePreservesEquivalentState() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 88)
            .add(SN, 12)
            .build();
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalMaterialKey initialSolidKey = CanonicalMaterialKey.of(comp);
        ResolvedProperties solidProps = resolver.resolve(initialSolidKey);

        ActiveThermalState liquidState = ActiveThermalState.atTemperature(1600.0, 1.0, 3.0, solidProps.properties());
        assertEquals(MaterialPhase.LIQUID, liquidState.phase());

        ItemStack stack = ItemMaterialSample
            .createStack(MaterialRef.generatedFrom(initialSolidKey), initialSolidKey, liquidState);

        MaterialRef restoredRef = ItemMaterialSample.getMaterialRef(stack);
        CanonicalMaterialKey restoredKey = ItemMaterialSample.getStateKey(stack);
        ActiveThermalState restoredThermal = ItemMaterialSample.getThermalState(stack, solidProps.properties());

        assertNotNull(restoredRef);
        assertNotNull(restoredKey);
        assertNotNull(restoredThermal);

        // All three must agree on liquid phase
        assertEquals(MaterialPhase.LIQUID, restoredKey.phase());
        assertEquals(MaterialPhase.LIQUID, restoredThermal.phase());
        assertEquals(MaterialRef.generatedFrom(restoredKey), restoredRef);
        assertEquals(1600.0, restoredThermal.temperatureKelvin(), 0.5);
    }

    @Test
    void propertyMixingRulesArePropertySpecificWhereRequired() {
        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        CanonicalComposition comp5050 = CanonicalComposition.builder()
            .add(CU, 50)
            .add(SN, 50)
            .build();

        ResolvedProperties solidRes = resolver
            .resolve(MaterialRef.generatedFrom(comp5050), comp5050, MaterialPhase.SOLID);
        MaterialProperties solidProps = solidRes.properties();

        // Density: harmonic volume-additive mean: 1 / (0.5/8.96 + 0.5/7.31) ≈ 8.051 g/cm³
        // Linear mean would be (8.96 + 7.31) / 2 = 8.135 g/cm³
        double expectedHarmonicDensity = 1.0 / (0.5 / 8.96 + 0.5 / 7.31);
        assertEquals(expectedHarmonicDensity, solidProps.density(), 0.02);

        // Heat capacity: Neumann-Kopp linear mass-fraction average: (385.0 + 228.0) / 2 = 306.5 J/(kg·K)
        double expectedSpecificHeatSolid = (385.0 + 228.0) / 2.0;
        assertEquals(expectedSpecificHeatSolid, solidProps.specificHeatSolid(), 0.1);

        // Hardness: solid must be > 0.0; liquid must be exactly 0.0
        assertTrue(solidProps.hardness() > 0.0);

        ResolvedProperties liquidRes = resolver
            .resolve(MaterialRef.generatedFrom(comp5050), comp5050, MaterialPhase.LIQUID);
        assertEquals(
            0.0,
            liquidRes.properties()
                .hardness(),
            0.001);
    }
}
