package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinEvents;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.scheduler.ScheduledEvent;

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
    void roundTripScheduledEventsPreservesIdentityAndOrdering() {
        NEWorldSavedData savedData = new NEWorldSavedData();
        savedData.setMetadata(WorldMetadata.createNew("world-persist-test", "N0.01.0-dev"));

        DeterministicId id1 = DeterministicId.derive("delayed-test-1");
        DeterministicId id2 = DeterministicId.derive("delayed-test-2");

        ScheduledEvent ev1 = new ScheduledEvent(id1, BuiltinEvents.DELAYED_TEST_EVENT, 500L, "payload-1");
        ScheduledEvent ev2 = new ScheduledEvent(id2, BuiltinEvents.DELAYED_TEST_EVENT, 250L, "payload-2");

        savedData.scheduler()
            .schedule(ev1);
        savedData.scheduler()
            .schedule(ev2);

        NBTTagCompound nbt = new NBTTagCompound();
        savedData.writeToNBT(nbt);

        NEWorldSavedData loadedData = new NEWorldSavedData();
        loadedData.readFromNBT(nbt);

        assertEquals(
            2,
            loadedData.scheduler()
                .pendingCount());
        assertEquals(
            250L,
            loadedData.scheduler()
                .peekNextDueTick());

        List<ScheduledEvent> dueAt300 = loadedData.scheduler()
            .pollDueEvents(300L);
        assertEquals(1, dueAt300.size());
        assertEquals(
            id2,
            dueAt300.get(0)
                .id());
        assertEquals(
            "payload-2",
            dueAt300.get(0)
                .payload());

        List<ScheduledEvent> dueAt600 = loadedData.scheduler()
            .pollDueEvents(600L);
        assertEquals(1, dueAt600.size());
        assertEquals(
            id1,
            dueAt600.get(0)
                .id());

        assertEquals(
            0,
            loadedData.scheduler()
                .pendingCount());
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

    @Test
    void rejectsMalformedScheduledEvents() {
        NBTTagCompound validBase = new NBTTagCompound();
        validBase.setInteger("SchemaVersion", 1);
        validBase.setInteger("WorldgenEpoch", 1);
        validBase.setString("WorldId", "world-123");
        validBase.setString("ContentVersion", "N0.01.0-dev");

        // Malformed ID
        NBTTagCompound badIdNbt = (NBTTagCompound) validBase.copy();
        NBTTagList listBadId = new NBTTagList();
        NBTTagCompound evBadId = new NBTTagCompound();
        evBadId.setString("Id", "not-a-valid-hex");
        evBadId.setString("EventType", "naturalexceptions:test/delayed_event");
        evBadId.setLong("DueTick", 100L);
        listBadId.appendTag(evBadId);
        badIdNbt.setTag("ScheduledEvents", listBadId);

        NEWorldSavedData dataBadId = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> dataBadId.readFromNBT(badIdNbt));

        // Malformed EventType
        NBTTagCompound badTypeNbt = (NBTTagCompound) validBase.copy();
        NBTTagList listBadType = new NBTTagList();
        NBTTagCompound evBadType = new NBTTagCompound();
        evBadType.setString(
            "Id",
            DeterministicId.derive("test")
                .hex());
        evBadType.setString("EventType", "invalid_no_namespace");
        evBadType.setLong("DueTick", 100L);
        listBadType.appendTag(evBadType);
        badTypeNbt.setTag("ScheduledEvents", listBadType);

        NEWorldSavedData dataBadType = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> dataBadType.readFromNBT(badTypeNbt));

        // Negative DueTick
        NBTTagCompound badTickNbt = (NBTTagCompound) validBase.copy();
        NBTTagList listBadTick = new NBTTagList();
        NBTTagCompound evBadTick = new NBTTagCompound();
        evBadTick.setString(
            "Id",
            DeterministicId.derive("test")
                .hex());
        evBadTick.setString("EventType", "naturalexceptions:test/delayed_event");
        evBadTick.setLong("DueTick", -10L);
        listBadTick.appendTag(evBadTick);
        badTickNbt.setTag("ScheduledEvents", listBadTick);

        NEWorldSavedData dataBadTick = new NEWorldSavedData();
        assertThrows(PersistenceValidationException.class, () -> dataBadTick.readFromNBT(badTickNbt));
    }
}
