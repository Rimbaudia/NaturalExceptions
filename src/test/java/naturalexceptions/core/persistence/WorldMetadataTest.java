package naturalexceptions.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class WorldMetadataTest {

    @Test
    void createsAndReadsWorldMetadata() {
        WorldMetadata meta = WorldMetadata
            .create("world-12345", SaveSchemaVersion.CURRENT, WorldgenEpoch.INITIAL, "N0.01.0-dev", 0L);

        assertEquals("world-12345", meta.worldId());
        assertEquals(SaveSchemaVersion.CURRENT, meta.schemaVersion());
        assertEquals(WorldgenEpoch.INITIAL, meta.worldgenEpoch());
        assertEquals("N0.01.0-dev", meta.contentVersion());
        assertEquals(0L, meta.createdAtTick());
    }

    @Test
    void freshMetadataUsesCurrentP6EpochWithoutChangingSaveSchema() {
        WorldMetadata metadata = WorldMetadata.createNew("fresh-p6-world", "N0.01.0-dev");

        assertEquals(WorldgenEpoch.CURRENT, metadata.worldgenEpoch());
        assertEquals(SaveSchemaVersion.CURRENT, metadata.schemaVersion());
        assertEquals(
            2,
            metadata.worldgenEpoch()
                .value());
    }

    @Test
    void distinguishesSchemaVersionAndWorldgenEpoch() {
        SaveSchemaVersion schema = SaveSchemaVersion.of(2);
        WorldgenEpoch epoch = WorldgenEpoch.of(2);

        // They must not be the same object or comparable to each other
        assertNotEquals(schema.getClass(), epoch.getClass());
        assertEquals(2, schema.value());
        assertEquals(2, epoch.value());

        // Epoch changes do not affect schema compatibility
        assertTrue(SaveSchemaVersion.CURRENT.isCompatibleWith(SaveSchemaVersion.of(1)));
        assertFalse(SaveSchemaVersion.CURRENT.isCompatibleWith(SaveSchemaVersion.of(2)));
    }

    @Test
    void rejectsInvalidMetadataInputs() {
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create(null, SaveSchemaVersion.CURRENT, WorldgenEpoch.INITIAL, "N0.01.0-dev", 0L));
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create("  ", SaveSchemaVersion.CURRENT, WorldgenEpoch.INITIAL, "N0.01.0-dev", 0L));
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create("world-1", null, WorldgenEpoch.INITIAL, "N0.01.0-dev", 0L));
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create("world-1", SaveSchemaVersion.CURRENT, null, "N0.01.0-dev", 0L));
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create("world-1", SaveSchemaVersion.CURRENT, WorldgenEpoch.INITIAL, null, 0L));
        assertThrows(
            IllegalArgumentException.class,
            () -> WorldMetadata.create("world-1", SaveSchemaVersion.CURRENT, WorldgenEpoch.INITIAL, "ver", -1L));
    }

    @Test
    void rejectsInvalidVersionNumbers() {
        assertThrows(IllegalArgumentException.class, () -> SaveSchemaVersion.of(0));
        assertThrows(IllegalArgumentException.class, () -> SaveSchemaVersion.of(-1));
        assertThrows(IllegalArgumentException.class, () -> WorldgenEpoch.of(0));
        assertThrows(IllegalArgumentException.class, () -> WorldgenEpoch.of(-1));
    }
}
