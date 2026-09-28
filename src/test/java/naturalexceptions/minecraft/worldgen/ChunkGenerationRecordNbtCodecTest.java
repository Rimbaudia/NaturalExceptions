package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.worldgen.ChunkGenerationRecord;
import naturalexceptions.core.worldgen.WorldgenStage;

final class ChunkGenerationRecordNbtCodecTest {

    @Test
    void chunkProvenanceRoundTripsWithoutLosingEpochSeedCoordinatesOrStageOrder() {
        ChunkGenerationRecord record = new ChunkGenerationRecord(-99531L, WorldgenEpoch.of(3), -17, 222);
        for (WorldgenStage stage : WorldgenStage.PIPELINE) record = record.withStage(stage);
        NBTTagCompound chunkData = new NBTTagCompound();

        ChunkGenerationRecordNbtCodec.write(record, chunkData);
        ChunkGenerationRecord restored = ChunkGenerationRecordNbtCodec.read(chunkData);

        assertEquals(record, restored);
    }

    @Test
    void rejectsChunkHistoryMissingItsWorldSeedLinkage() {
        NBTTagCompound record = new NBTTagCompound();
        record.setInteger("Epoch", 2);
        record.setInteger("ChunkX", 0);
        record.setInteger("ChunkZ", 0);
        record.setTag("Stages", new net.minecraft.nbt.NBTTagList());
        NBTTagCompound chunkNbt = new NBTTagCompound();
        chunkNbt.setTag(ChunkGenerationRecordNbtCodec.KEY_RECORD, record);

        assertThrows(IllegalArgumentException.class, () -> ChunkGenerationRecordNbtCodec.read(chunkNbt));
    }

    @Test
    void rejectsPartialChunkHistoryInsteadOfTreatingItAsMaterialized() {
        NBTTagCompound record = new NBTTagCompound();
        record.setLong("WorldSeed", 42L);
        record.setInteger("Epoch", 2);
        record.setInteger("ChunkX", -3);
        record.setInteger("ChunkZ", 9);
        record.setTag("Stages", new net.minecraft.nbt.NBTTagList());
        NBTTagCompound chunkNbt = new NBTTagCompound();
        chunkNbt.setTag(ChunkGenerationRecordNbtCodec.KEY_RECORD, record);

        assertThrows(IllegalArgumentException.class, () -> ChunkGenerationRecordNbtCodec.read(chunkNbt));
    }
}
