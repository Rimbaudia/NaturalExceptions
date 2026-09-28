package naturalexceptions.core.anomaly;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Durable bounded circular influence centered at integer block coordinates. */
public final class SpatialAnomaly implements Comparable<SpatialAnomaly> {
    public static final int MAX_RADIUS_BLOCKS = 4096;

    private final DeterministicId id;
    private final ResourceId type;
    private final int centerX;
    private final int centerZ;
    private final int radiusBlocks;
    private final EnvironmentalInfluence influence;

    public SpatialAnomaly(DeterministicId id, ResourceId type, int centerX, int centerZ, int radiusBlocks,
                          EnvironmentalInfluence influence) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        if (radiusBlocks < 1 || radiusBlocks > MAX_RADIUS_BLOCKS) {
            throw new IllegalArgumentException("radiusBlocks must be between 1 and " + MAX_RADIUS_BLOCKS);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radiusBlocks = radiusBlocks;
        this.influence = Objects.requireNonNull(influence, "influence");
    }

    public DeterministicId id() { return id; }
    public ResourceId type() { return type; }
    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public int radiusBlocks() { return radiusBlocks; }
    public EnvironmentalInfluence influence() { return influence; }

    public boolean influences(int x, int z) {
        double dx = (double) x - centerX;
        double dz = (double) z - centerZ;
        return dx * dx + dz * dz <= (double) radiusBlocks * radiusBlocks;
    }

    @Override public int compareTo(SpatialAnomaly other) { return id.compareTo(other.id); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpatialAnomaly)) return false;
        SpatialAnomaly that = (SpatialAnomaly) o;
        return centerX == that.centerX && centerZ == that.centerZ && radiusBlocks == that.radiusBlocks
            && id.equals(that.id) && type.equals(that.type) && influence.equals(that.influence);
    }

    @Override public int hashCode() {
        return Objects.hash(id, type, centerX, centerZ, radiusBlocks, influence);
    }
}
