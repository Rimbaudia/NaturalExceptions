package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;

final class ChunkGenerationRecordTest {

    @Test
    void recordsStagesOnlyInTheAuthoritativeOrder() {
        ChunkGenerationRecord record = new ChunkGenerationRecord(4455L, WorldgenEpoch.of(2), -7, 19);
        for (WorldgenStage stage : WorldgenStage.PIPELINE) record = record.withStage(stage);

        assertTrue(record.isComplete());
        assertEquals(WorldgenStage.PIPELINE, record.stages());
        assertEquals(-7, record.chunkX());
        assertEquals(19, record.chunkZ());
        assertEquals(4455L, record.worldSeed());
        assertEquals(WorldgenEpoch.of(2), record.epoch());
    }

    @Test
    void rejectsSkippingOrReorderingStages() {
        ChunkGenerationRecord record = new ChunkGenerationRecord(1L, WorldgenEpoch.INITIAL, 0, 0);

        ChunkGenerationRecord empty = record;
        assertThrows(IllegalArgumentException.class, () -> empty.withStage(WorldgenStage.RESOURCES));
        record = record.withStage(WorldgenStage.BASE_TERRAIN);
        ChunkGenerationRecord partial = record;
        assertThrows(IllegalArgumentException.class, () -> partial.withStage(WorldgenStage.HYDROLOGY));
    }

    @Test
    void incompleteChunkIsNotClaimedAsFullyMaterialized() {
        ChunkGenerationRecord record = new ChunkGenerationRecord(1L, WorldgenEpoch.INITIAL, 0, 0)
            .withStage(WorldgenStage.BASE_TERRAIN);

        assertFalse(record.isComplete());
    }
}
