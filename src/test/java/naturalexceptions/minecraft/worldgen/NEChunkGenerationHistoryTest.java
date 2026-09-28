package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.worldgen.ChunkGenerationRecord;
import naturalexceptions.core.worldgen.WorldgenStage;

final class NEChunkGenerationHistoryTest {

    @Test
    void generatedChunkProvenanceSurvivesTheChunkSaveAndLoadEvents() {
        NEChunkGenerationHistory history = new NEChunkGenerationHistory();
        Chunk generated = new Chunk(null, -12, 24);
        ChunkGenerationRecord expected = completeRecord(-775L, -12, 24);
        NEChunkGenerationHistory.mark(generated, expected);
        NBTTagCompound chunkNbt = new NBTTagCompound();

        history.onChunkSave(new ChunkDataEvent.Save(generated, chunkNbt));
        Chunk reloaded = new Chunk(null, -12, 24);
        history.onChunkLoad(new ChunkDataEvent.Load(reloaded, chunkNbt));

        assertEquals(expected, NEChunkGenerationHistory.record(reloaded));
    }

    private ChunkGenerationRecord completeRecord(long seed, int chunkX, int chunkZ) {
        ChunkGenerationRecord record = new ChunkGenerationRecord(seed, WorldgenEpoch.CURRENT, chunkX, chunkZ);
        for (WorldgenStage stage : WorldgenStage.PIPELINE) record = record.withStage(stage);
        return record;
    }
}
