package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.material.Microstructure;

final class MaterialCarrierNbtCodecTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");

    @Test
    void authoredMaterialRoundTripsThroughNbt() {
        MaterialRef ref = MaterialRef.authored(CU);
        NBTTagCompound nbt = new NBTTagCompound();

        MaterialCarrierNbtCodec.writeToNbt(ref, (CanonicalMaterialKey) null, nbt);

        assertTrue(nbt.hasKey(MaterialCarrierNbtCodec.KEY_FORMAT, 99));
        assertEquals(MaterialCarrierNbtCodec.CURRENT_FORMAT, nbt.getInteger(MaterialCarrierNbtCodec.KEY_FORMAT));
        assertEquals("mat:authored:naturalexceptions:copper", nbt.getString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF));

        MaterialRef decodedRef = MaterialCarrierNbtCodec.readRefFromNbt(nbt);
        assertEquals(ref, decodedRef);
        assertNull(MaterialCarrierNbtCodec.readStateKeyFromNbt(nbt));
        assertNull(MaterialCarrierNbtCodec.readCompositionFromNbt(nbt));
    }

    @Test
    void generatedMaterialWithFullStateKeyRoundTripsThroughNbt() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();
        CanonicalMaterialKey stateKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.COLD_WORKED);
        MaterialRef ref = MaterialRef.generatedFrom(stateKey);
        NBTTagCompound nbt = new NBTTagCompound();

        MaterialCarrierNbtCodec.writeToNbt(ref, stateKey, nbt);

        MaterialRef decodedRef = MaterialCarrierNbtCodec.readRefFromNbt(nbt);
        CanonicalMaterialKey decodedKey = MaterialCarrierNbtCodec.readStateKeyFromNbt(nbt);

        assertEquals(ref, decodedRef);
        assertNotNull(decodedKey);
        assertEquals(stateKey, decodedKey);
        assertEquals(MaterialPhase.SOLID, decodedKey.phase());
        assertEquals(Microstructure.ANNEALED, decodedKey.microstructure());
        assertEquals(ConditioningHistory.COLD_WORKED, decodedKey.conditioning());
        assertEquals(
            7_000,
            decodedKey.composition()
                .parts(CU));
        assertEquals(
            3_000,
            decodedKey.composition()
                .parts(SN));
    }

    @Test
    void rejectsMissingOrUnsupportedFormat() {
        NBTTagCompound noFormat = new NBTTagCompound();
        noFormat.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "mat:authored:naturalexceptions:copper");
        assertThrows(PersistenceValidationException.class, () -> MaterialCarrierNbtCodec.readRefFromNbt(noFormat));

        NBTTagCompound futureFormat = new NBTTagCompound();
        futureFormat.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 999);
        futureFormat.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "mat:authored:naturalexceptions:copper");
        assertThrows(PersistenceValidationException.class, () -> MaterialCarrierNbtCodec.readRefFromNbt(futureFormat));
    }

    @Test
    void rejectsMissingOrMalformedMaterialRef() {
        NBTTagCompound badRef = new NBTTagCompound();
        badRef.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 1);
        badRef.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "invalid_ref_format");
        assertThrows(PersistenceValidationException.class, () -> MaterialCarrierNbtCodec.readRefFromNbt(badRef));

        NBTTagCompound missingRef = new NBTTagCompound();
        missingRef.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 1);
        assertThrows(PersistenceValidationException.class, () -> MaterialCarrierNbtCodec.readRefFromNbt(missingRef));
    }

    @Test
    void rejectsGeneratedRefWithMissingReconstructionPayload() {
        NBTTagCompound missingPayload = new NBTTagCompound();
        missingPayload.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 2);
        // Valid generated ref syntax, but missing StateKey and Composition tags
        MaterialRef genRef = MaterialRef.generated(DeterministicId.derive("test", "missing"));
        missingPayload.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, genRef.asString());

        assertThrows(
            PersistenceValidationException.class,
            () -> MaterialCarrierNbtCodec.readRefFromNbt(missingPayload));
    }

    @Test
    void rejectsReconstructionPayloadInconsistentWithDeclaredRef() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(comp);
        MaterialRef actualRef = MaterialRef.generatedFrom(key);

        NBTTagCompound inconsistentNbt = new NBTTagCompound();
        inconsistentNbt.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 2);
        // Deliberately set a DIFFERENT ref than what the stateKey produces
        MaterialRef tamperedRef = MaterialRef.generated(DeterministicId.derive("tampered", "fake"));
        inconsistentNbt.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, tamperedRef.asString());
        inconsistentNbt.setString(MaterialCarrierNbtCodec.KEY_STATE_KEY, key.canonicalString());

        assertThrows(
            PersistenceValidationException.class,
            () -> MaterialCarrierNbtCodec.readRefFromNbt(inconsistentNbt));
    }

    @Test
    void rejectsMalformedStateKeyString() {
        NBTTagCompound badState = new NBTTagCompound();
        badState.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 2);
        badState.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "mat:authored:naturalexceptions:copper");
        badState.setString(MaterialCarrierNbtCodec.KEY_STATE_KEY, "corrupt@not@valid");

        assertThrows(PersistenceValidationException.class, () -> MaterialCarrierNbtCodec.readStateKeyFromNbt(badState));
    }

    @Test
    void activeThermalStateRoundTripsThroughNbt() {
        MaterialRef ref = MaterialRef.authored(CU);
        NBTTagCompound nbt = new NBTTagCompound();
        MaterialCarrierNbtCodec.writeToNbt(ref, (CanonicalMaterialKey) null, nbt);

        naturalexceptions.core.property.MaterialProperties props = naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
        naturalexceptions.core.thermal.ActiveThermalState hotState = naturalexceptions.core.thermal.ActiveThermalState
            .atTemperature(800.0, 0.0, 2.5, props);

        MaterialCarrierNbtCodec.writeThermalStateToNbt(hotState, nbt);
        assertTrue(MaterialCarrierNbtCodec.hasActiveThermalState(nbt));

        naturalexceptions.core.thermal.ActiveThermalState restored = MaterialCarrierNbtCodec
            .readThermalStateFromNbt(nbt, props);
        assertNotNull(restored);
        assertEquals(hotState.energyJoules(), restored.energyJoules(), 0.001);
        assertEquals(hotState.massKg(), restored.massKg(), 0.001);
        assertEquals(hotState.temperatureKelvin(), restored.temperatureKelvin(), 0.01);
    }

    @Test
    void ambientSampleOmitsThermalTagForSparsePersistence() {
        MaterialRef ref = MaterialRef.authored(CU);
        NBTTagCompound nbt = new NBTTagCompound();
        MaterialCarrierNbtCodec.writeToNbt(ref, (CanonicalMaterialKey) null, nbt);

        naturalexceptions.core.property.MaterialProperties props = naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
        naturalexceptions.core.thermal.ActiveThermalState ambientState = naturalexceptions.core.thermal.ActiveThermalState
            .ambient(1.0, props);

        MaterialCarrierNbtCodec.writeThermalStateToNbt(ambientState, nbt);
        assertFalse(MaterialCarrierNbtCodec.hasActiveThermalState(nbt));
        assertNull(MaterialCarrierNbtCodec.readThermalStateFromNbt(nbt, props));
    }

    @Test
    void rejectsCorruptedThermalEnergyOrMass() {
        NBTTagCompound badEnergy = new NBTTagCompound();
        NBTTagCompound thermal = new NBTTagCompound();
        thermal.setDouble(MaterialCarrierNbtCodec.KEY_THERMAL_ENERGY, -500.0);
        thermal.setDouble(MaterialCarrierNbtCodec.KEY_THERMAL_MASS, 1.0);
        badEnergy.setTag(MaterialCarrierNbtCodec.KEY_THERMAL, thermal);

        naturalexceptions.core.property.MaterialProperties props = naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
        assertThrows(
            PersistenceValidationException.class,
            () -> MaterialCarrierNbtCodec.readThermalStateFromNbt(badEnergy, props));

        NBTTagCompound badMass = new NBTTagCompound();
        NBTTagCompound thermalZeroMass = new NBTTagCompound();
        thermalZeroMass.setDouble(MaterialCarrierNbtCodec.KEY_THERMAL_ENERGY, 1000.0);
        thermalZeroMass.setDouble(MaterialCarrierNbtCodec.KEY_THERMAL_MASS, 0.0);
        badMass.setTag(MaterialCarrierNbtCodec.KEY_THERMAL, thermalZeroMass);

        assertThrows(
            PersistenceValidationException.class,
            () -> MaterialCarrierNbtCodec.readThermalStateFromNbt(badMass, props));
    }
}
