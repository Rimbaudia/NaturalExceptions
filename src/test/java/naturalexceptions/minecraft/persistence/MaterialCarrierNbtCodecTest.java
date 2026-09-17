package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;

final class MaterialCarrierNbtCodecTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");

    @Test
    void authoredMaterialRoundTripsThroughNbt() {
        MaterialRef ref = MaterialRef.authored(CU);
        NBTTagCompound nbt = new NBTTagCompound();

        MaterialCarrierNbtCodec.writeToNbt(ref, null, nbt);

        assertTrue(nbt.hasKey(MaterialCarrierNbtCodec.KEY_FORMAT, 99));
        assertEquals(1, nbt.getInteger(MaterialCarrierNbtCodec.KEY_FORMAT));
        assertEquals("mat:authored:naturalexceptions:copper", nbt.getString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF));

        MaterialRef decodedRef = MaterialCarrierNbtCodec.readRefFromNbt(nbt);
        assertEquals(ref, decodedRef);
        assertNull(MaterialCarrierNbtCodec.readCompositionFromNbt(nbt));
    }

    @Test
    void generatedMaterialWithCompositionRoundTripsThroughNbt() {
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 30)
            .build();
        MaterialRef ref = MaterialRef.generatedFrom(comp);
        NBTTagCompound nbt = new NBTTagCompound();

        MaterialCarrierNbtCodec.writeToNbt(ref, comp, nbt);

        MaterialRef decodedRef = MaterialCarrierNbtCodec.readRefFromNbt(nbt);
        CanonicalComposition decodedComp = MaterialCarrierNbtCodec.readCompositionFromNbt(nbt);

        assertEquals(ref, decodedRef);
        assertNotNull(decodedComp);
        assertEquals(comp, decodedComp);
        assertEquals(7_000, decodedComp.parts(CU));
        assertEquals(3_000, decodedComp.parts(SN));
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
    void rejectsMalformedCompositionString() {
        NBTTagCompound badComp = new NBTTagCompound();
        badComp.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 1);
        badComp.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "mat:authored:naturalexceptions:copper");
        badComp.setString(MaterialCarrierNbtCodec.KEY_COMPOSITION, "corrupt_comp@notanumber");

        assertThrows(
            PersistenceValidationException.class,
            () -> MaterialCarrierNbtCodec.readCompositionFromNbt(badComp));
    }
}
