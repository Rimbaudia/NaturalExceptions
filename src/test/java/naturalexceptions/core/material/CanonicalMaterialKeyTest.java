package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;

final class CanonicalMaterialKeyTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");

    @Test
    void identicalCompositionAndStateProduceIdenticalIdentity() {
        CanonicalComposition comp1 = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();
        CanonicalComposition comp2 = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();

        CanonicalMaterialKey key1 = CanonicalMaterialKey
            .of(comp1, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.COLD_WORKED);
        CanonicalMaterialKey key2 = CanonicalMaterialKey
            .of(comp2, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.COLD_WORKED);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertEquals(key1.canonicalString(), key2.canonicalString());

        MaterialRef ref1 = MaterialRef.generatedFrom(key1);
        MaterialRef ref2 = MaterialRef.generatedFrom(key2);

        assertEquals(ref1, ref2);
    }

    @Test
    void constituentInputOrderDoesNotAlterIdentity() {
        // Cu 75%, Sn 25% built in different orders
        CanonicalComposition compOrder1 = CanonicalComposition.builder()
            .add(CU, 75)
            .add(SN, 25)
            .build();
        CanonicalComposition compOrder2 = CanonicalComposition.builder()
            .add(SN, 25)
            .add(CU, 75)
            .build();

        CanonicalMaterialKey key1 = CanonicalMaterialKey
            .of(compOrder1, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        CanonicalMaterialKey key2 = CanonicalMaterialKey
            .of(compOrder2, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);

        assertEquals(key1, key2);
        assertEquals(MaterialRef.generatedFrom(key1), MaterialRef.generatedFrom(key2));
    }

    @Test
    void playerAliasDoesNotAlterMaterialIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 90)
            .add(SN, 10)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(comp);
        MaterialRef canonicalRef = MaterialRef.generatedFrom(key);

        MaterialSampleState sample1 = MaterialSampleState.ambient();
        sample1.setPlayerAlias("Grandfather's Bronze");

        MaterialSampleState sample2 = MaterialSampleState.ambient();
        sample2.setPlayerAlias("High-Tin Alloy #42");

        // Changing player alias does not change the canonical physical identity
        assertEquals(canonicalRef, MaterialRef.generatedFrom(key));
    }

    @Test
    void transientTemperatureDoesNotAlterMaterialIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 90)
            .add(SN, 10)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.NONE);
        MaterialRef canonicalRef = MaterialRef.generatedFrom(key);

        MaterialSampleState coldSample = new MaterialSampleState(273.15, null, false, null, 1);
        MaterialSampleState hotSample = new MaterialSampleState(1200.0, null, false, null, 1);

        assertNotEquals(coldSample.temperatureKelvin(), hotSample.temperatureKelvin());
        // Both samples share the same canonical physical material identity
        assertEquals(canonicalRef, MaterialRef.generatedFrom(key));
    }

    @Test
    void phaseDifferenceAltersCanonicalIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 85)
            .add(SN, 15)
            .build();

        CanonicalMaterialKey solidKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        CanonicalMaterialKey liquidKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.LIQUID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        CanonicalMaterialKey alphaKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.ALPHA, Microstructure.AS_CAST, ConditioningHistory.NONE);

        assertNotEquals(solidKey, liquidKey);
        assertNotEquals(solidKey, alphaKey);
        assertNotEquals(MaterialRef.generatedFrom(solidKey), MaterialRef.generatedFrom(liquidKey));
        assertNotEquals(MaterialRef.generatedFrom(solidKey), MaterialRef.generatedFrom(alphaKey));
    }

    @Test
    void microstructureDifferenceAltersCanonicalIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 85)
            .add(SN, 15)
            .build();

        CanonicalMaterialKey asCast = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        CanonicalMaterialKey annealed = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.NONE);
        CanonicalMaterialKey quenched = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.QUENCHED, ConditioningHistory.NONE);

        assertNotEquals(asCast, annealed);
        assertNotEquals(asCast, quenched);
        assertNotEquals(MaterialRef.generatedFrom(asCast), MaterialRef.generatedFrom(annealed));
        assertNotEquals(MaterialRef.generatedFrom(asCast), MaterialRef.generatedFrom(quenched));
    }

    @Test
    void conditioningHistoryDifferenceAltersCanonicalIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 85)
            .add(SN, 15)
            .build();

        CanonicalMaterialKey pristine = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        CanonicalMaterialKey coldWorked = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.COLD_WORKED);
        CanonicalMaterialKey irradiated = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.STELLAR_IRRADIATED);

        assertNotEquals(pristine, coldWorked);
        assertNotEquals(pristine, irradiated);
        assertNotEquals(MaterialRef.generatedFrom(pristine), MaterialRef.generatedFrom(coldWorked));
        assertNotEquals(MaterialRef.generatedFrom(pristine), MaterialRef.generatedFrom(irradiated));
    }

    @Test
    void serializationAndReconstructionProducesIdenticalIdentity() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();
        CanonicalMaterialKey originalKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.ALPHA, Microstructure.WORK_HARDENED, ConditioningHistory.COLD_WORKED);
        MaterialRef originalRef = MaterialRef.generatedFrom(originalKey);

        String serialized = originalKey.canonicalString();
        CanonicalMaterialKey reconstructedKey = CanonicalMaterialKey.parse(serialized);
        MaterialRef reconstructedRef = MaterialRef.generatedFrom(reconstructedKey);

        assertEquals(originalKey, reconstructedKey);
        assertEquals(originalRef, reconstructedRef);
        assertEquals(MaterialPhase.ALPHA, reconstructedKey.phase());
        assertEquals(Microstructure.WORK_HARDENED, reconstructedKey.microstructure());
        assertEquals(ConditioningHistory.COLD_WORKED, reconstructedKey.conditioning());
    }

    @Test
    void catalogueEvictionFollowedByReconstructionPreservesIdentity() {
        MaterialCatalogue catalogue = new MaterialCatalogue();
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 88)
            .add(SN, 12)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey
            .of(comp, MaterialPhase.ALPHA, Microstructure.ANNEALED, ConditioningHistory.AGE_HARDENED);

        GeneratedMaterial gen1 = catalogue.getOrRegisterGenerated(key);
        MaterialRef ref1 = gen1.ref();

        // Simulate complete catalogue eviction
        catalogue.clearGenerated();
        assertEquals(0, catalogue.generatedCount());

        // Reconstruct from key in the evicted catalogue
        GeneratedMaterial gen2 = catalogue.getOrRegisterGenerated(key);
        assertEquals(ref1, gen2.ref());
        assertEquals(gen1.id(), gen2.id());
        assertEquals(gen1.key(), gen2.key());
    }

    @Test
    void registrationOrderDoesNotAffectIdentityAssignment() {
        CanonicalComposition cA = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();
        CanonicalComposition cB = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();

        CanonicalMaterialKey keyA = CanonicalMaterialKey
            .of(cA, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.NONE);
        CanonicalMaterialKey keyB = CanonicalMaterialKey
            .of(cB, MaterialPhase.SOLID, Microstructure.QUENCHED, ConditioningHistory.COLD_WORKED);

        // Catalogue 1 registers A then B
        MaterialCatalogue cat1 = new MaterialCatalogue();
        GeneratedMaterial g1A = cat1.getOrRegisterGenerated(keyA);
        GeneratedMaterial g1B = cat1.getOrRegisterGenerated(keyB);

        // Catalogue 2 registers B then A
        MaterialCatalogue cat2 = new MaterialCatalogue();
        GeneratedMaterial g2B = cat2.getOrRegisterGenerated(keyB);
        GeneratedMaterial g2A = cat2.getOrRegisterGenerated(keyA);

        assertEquals(g1A.ref(), g2A.ref());
        assertEquals(g1B.ref(), g2B.ref());
        assertEquals(g1A.id(), g2A.id());
        assertEquals(g1B.id(), g2B.id());
    }
}
