package naturalexceptions.minecraft.worldgen;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.worldgen.ChunkGenerationRecord;
import naturalexceptions.core.worldgen.WorldgenStage;

/** Compact Forge NBT adapter for the durable stage provenance attached to a materialized chunk. */
public final class ChunkGenerationRecordNbtCodec {

    public static final String KEY_RECORD = "NaturalExceptionsGeneration";

    private static final String KEY_SEED = "WorldSeed";
    private static final String KEY_EPOCH = "Epoch";
    private static final String KEY_CHUNK_X = "ChunkX";
    private static final String KEY_CHUNK_Z = "ChunkZ";
    private static final String KEY_STAGES = "Stages";

    private ChunkGenerationRecordNbtCodec() {}

    public static void write(ChunkGenerationRecord record, NBTTagCompound chunkNbt) {
        if (record == null || chunkNbt == null) throw new IllegalArgumentException("Record and chunk NBT are required");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong(KEY_SEED, record.worldSeed());
        tag.setInteger(
            KEY_EPOCH,
            record.epoch()
                .value());
        tag.setInteger(KEY_CHUNK_X, record.chunkX());
        tag.setInteger(KEY_CHUNK_Z, record.chunkZ());
        NBTTagList stages = new NBTTagList();
        for (WorldgenStage stage : record.stages()) stages.appendTag(new net.minecraft.nbt.NBTTagString(stage.key()));
        tag.setTag(KEY_STAGES, stages);
        chunkNbt.setTag(KEY_RECORD, tag);
    }

    public static ChunkGenerationRecord read(NBTTagCompound chunkNbt) {
        if (chunkNbt == null || !chunkNbt.hasKey(KEY_RECORD, 10)) {
            throw new IllegalArgumentException("Chunk NBT has no Natural Exceptions generation record");
        }
        NBTTagCompound tag = chunkNbt.getCompoundTag(KEY_RECORD);
        int epochValue = tag.getInteger(KEY_EPOCH);
        if (!tag.hasKey(KEY_SEED, 4) || !tag.hasKey(KEY_EPOCH, 3)
            || !tag.hasKey(KEY_CHUNK_X, 3)
            || !tag.hasKey(KEY_CHUNK_Z, 3)
            || !tag.hasKey(KEY_STAGES, 9)
            || epochValue <= 0) {
            throw new IllegalArgumentException("Invalid Natural Exceptions chunk generation record");
        }
        ChunkGenerationRecord record = new ChunkGenerationRecord(
            tag.getLong(KEY_SEED),
            WorldgenEpoch.of(epochValue),
            tag.getInteger(KEY_CHUNK_X),
            tag.getInteger(KEY_CHUNK_Z));
        NBTTagList stages = tag.getTagList(KEY_STAGES, 8);
        for (int i = 0; i < stages.tagCount(); i++) {
            record = record.withStage(WorldgenStage.fromKey(stages.getStringTagAt(i)));
        }
        if (!record.isComplete()) {
            throw new IllegalArgumentException("Incomplete Natural Exceptions chunk generation record");
        }
        return record;
    }
}
