package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.WorldgenEpoch;

final class NEWorldSavedDataTest {

    @Test
    void roundTripWorldMetadataPreservesAllFields() {
        WorldMetadata original = WorldMetadata.create(
            "fixed-test-world-uuid-12345",
            SaveSchemaVersion.CURRENT,
            WorldgenEpoch.INITIAL,
            "N0.01.0-dev",
            4200L);

        NEWorldSavedData savedData = new NEWorldSavedData();
        savedData.setMetadata(original);

        NBTTagCompound nbt = new NBTTagCompound();
        savedData.writeToNBT(nbt);

        NEWorldSavedData loadedData = new NEWorldSavedData();
        loadedData.readFromNBT(nbt);

        WorldMetadata loadedMeta = loadedData.metadata();
        assertNotNull(loadedMeta);
        assertEquals(original.worldId(), loadedMeta.worldId());
        assertEquals(original.schemaVersion(), loadedMeta.schemaVersion());
        assertEquals(original.worldgenEpoch(), loadedMeta.worldgenEpoch());
        assertEquals(original.contentVersion(), loadedMeta.contentVersion());
        assertEquals(original.createdAtTick(), loadedMeta.createdAtTick());
        assertEquals(original, loadedMeta);
    }

    @Test
    void rejectsMissingSchemaVersion() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("WorldgenEpoch", 1);
        nbt.setString("WorldId", "test-world");
        nbt.setString("ContentVersion", "N0.01.0-dev");

        NEWorldSavedData data = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data.readFromNBT(nbt));
    }

    @Test
    void rejectsUnsupportedNewerSchemaVersion() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("SchemaVersion", 9999);
        nbt.setInteger("WorldgenEpoch", 1);
        nbt.setString("WorldId", "test-world");
        nbt.setString("ContentVersion", "N0.01.0-dev");

        NEWorldSavedData data = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data.readFromNBT(nbt));
    }

    @Test
    void rejectsMissingOrInvalidWorldgenEpoch() {
        NBTTagCompound nbtMissing = new NBTTagCompound();
        nbtMissing.setInteger("SchemaVersion", 1);
        nbtMissing.setString("WorldId", "test-world");
        nbtMissing.setString("ContentVersion", "N0.01.0-dev");

        NEWorldSavedData data1 = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data1.readFromNBT(nbtMissing));

        NBTTagCompound nbtInvalid = new NBTTagCompound();
        nbtInvalid.setInteger("SchemaVersion", 1);
        nbtInvalid.setInteger("WorldgenEpoch", -5);
        nbtInvalid.setString("WorldId", "test-world");
        nbtInvalid.setString("ContentVersion", "N0.01.0-dev");

        NEWorldSavedData data2 = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data2.readFromNBT(nbtInvalid));
    }

    @Test
    void rejectsMissingWorldIdOrContentVersion() {
        NBTTagCompound nbtNoId = new NBTTagCompound();
        nbtNoId.setInteger("SchemaVersion", 1);
        nbtNoId.setInteger("WorldgenEpoch", 1);
        nbtNoId.setString("ContentVersion", "N0.01.0-dev");

        NEWorldSavedData data1 = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data1.readFromNBT(nbtNoId));

        NBTTagCompound nbtNoVer = new NBTTagCompound();
        nbtNoVer.setInteger("SchemaVersion", 1);
        nbtNoVer.setInteger("WorldgenEpoch", 1);
        nbtNoVer.setString("WorldId", "world-123");

        NEWorldSavedData data2 = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> data2.readFromNBT(nbtNoVer));
    }
}
