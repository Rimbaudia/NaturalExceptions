package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import naturalexceptions.core.persistence.WorldgenEpoch;

/** Persistable provenance for the deterministic generation that materialized one chunk. */
public final class ChunkGenerationRecord {

    private final long worldSeed;
    private final WorldgenEpoch epoch;
    private final int chunkX;
    private final int chunkZ;
    private final List<WorldgenStage> stages;

    public ChunkGenerationRecord(long worldSeed, WorldgenEpoch epoch, int chunkX, int chunkZ) {
        this(worldSeed, epoch, chunkX, chunkZ, Collections.emptyList());
    }

    private ChunkGenerationRecord(long worldSeed, WorldgenEpoch epoch, int chunkX, int chunkZ,
        List<WorldgenStage> stages) {
        this.worldSeed = worldSeed;
        this.epoch = Objects.requireNonNull(epoch, "epoch");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
    }

    public long worldSeed() {
        return worldSeed;
    }

    public WorldgenEpoch epoch() {
        return epoch;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public List<WorldgenStage> stages() {
        return stages;
    }

    public boolean isComplete() {
        return stages.size() == WorldgenStage.PIPELINE.size();
    }

    public ChunkGenerationRecord withStage(WorldgenStage stage) {
        Objects.requireNonNull(stage, "stage");
        int expected = stages.size();
        if (expected >= WorldgenStage.PIPELINE.size() || WorldgenStage.PIPELINE.get(expected) != stage) {
            throw new IllegalArgumentException(
                "Expected stage " + (expected < WorldgenStage.PIPELINE.size() ? WorldgenStage.PIPELINE.get(expected)
                    .key() : "<complete>") + " but received " + stage.key());
        }
        List<WorldgenStage> updated = new ArrayList<>(stages);
        updated.add(stage);
        return new ChunkGenerationRecord(worldSeed, epoch, chunkX, chunkZ, updated);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ChunkGenerationRecord)) return false;
        ChunkGenerationRecord that = (ChunkGenerationRecord) other;
        return worldSeed == that.worldSeed && chunkX == that.chunkX
            && chunkZ == that.chunkZ
            && epoch.equals(that.epoch)
            && stages.equals(that.stages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldSeed, epoch, chunkX, chunkZ, stages);
    }
}
