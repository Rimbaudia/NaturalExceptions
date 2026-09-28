package naturalexceptions.minecraft.worldgen;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.worldgen.ChunkGenerationRecord;

/** Binds generated provenance to the chunk's own save lifecycle; it does no loaded-chunk scanning or ticking. */
public final class NEChunkGenerationHistory {

    private static final Logger LOGGER = LogManager.getLogger("NaturalExceptions");
    private static final Map<Chunk, ChunkGenerationRecord> RECORDS = new WeakHashMap<>();

    public static void mark(Chunk chunk, ChunkGenerationRecord record) {
        if (chunk == null || record == null || !record.isComplete()) {
            throw new IllegalArgumentException("A fully materialized chunk record is required");
        }
        if (chunk.xPosition != record.chunkX() || chunk.zPosition != record.chunkZ()) {
            throw new IllegalArgumentException("Chunk position does not match its generation provenance");
        }
        synchronized (RECORDS) {
            RECORDS.put(chunk, record);
        }
    }

    public static ChunkGenerationRecord record(Chunk chunk) {
        synchronized (RECORDS) {
            return RECORDS.get(chunk);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        if (event == null || event.getChunk() == null
            || event.getData() == null
            || !event.getData()
                .hasKey(ChunkGenerationRecordNbtCodec.KEY_RECORD, 10))
            return;
        try {
            ChunkGenerationRecord record = ChunkGenerationRecordNbtCodec.read(event.getData());
            if (record.chunkX() != event.getChunk().xPosition || record.chunkZ() != event.getChunk().zPosition) {
                LOGGER.warn(
                    "Ignoring mismatched Natural Exceptions chunk history at {},{}",
                    event.getChunk().xPosition,
                    event.getChunk().zPosition);
                return;
            }
            synchronized (RECORDS) {
                RECORDS.put(event.getChunk(), record);
            }
        } catch (RuntimeException malformed) {
            LOGGER.warn("Ignoring malformed Natural Exceptions chunk history", malformed);
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        if (event == null || event.getChunk() == null || event.getData() == null) return;
        ChunkGenerationRecord record = record(event.getChunk());
        if (record != null) ChunkGenerationRecordNbtCodec.write(record, event.getData());
    }
}
