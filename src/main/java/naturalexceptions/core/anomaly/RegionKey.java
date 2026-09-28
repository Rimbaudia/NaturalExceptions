package naturalexceptions.core.anomaly;

import java.util.Objects;

/** Stable 512-block square region coordinate for coarse historical state. */
public final class RegionKey implements Comparable<RegionKey> {
    public static final int SIZE_BLOCKS = 512;
    private static final long MAX_WORLD_REGION = 4_194_304L;
    private final long x;
    private final long z;

    public RegionKey(long x, long z) {
        if (x < -MAX_WORLD_REGION || x > MAX_WORLD_REGION || z < -MAX_WORLD_REGION || z > MAX_WORLD_REGION) {
            throw new IllegalArgumentException("region lies outside Minecraft's block-coordinate domain");
        }
        this.x = x;
        this.z = z;
    }

    public static RegionKey fromBlock(int blockX, int blockZ) {
        return new RegionKey(Math.floorDiv(blockX, SIZE_BLOCKS), Math.floorDiv(blockZ, SIZE_BLOCKS));
    }

    public long x() { return x; }
    public long z() { return z; }
    public RegionKey west() { return new RegionKey(x - 1, z); }
    public RegionKey east() { return new RegionKey(x + 1, z); }
    public RegionKey north() { return new RegionKey(x, z - 1); }
    public RegionKey south() { return new RegionKey(x, z + 1); }

    @Override public int compareTo(RegionKey other) {
        int c = Long.compare(x, other.x);
        return c != 0 ? c : Long.compare(z, other.z);
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionKey)) return false;
        RegionKey that = (RegionKey) o;
        return x == that.x && z == that.z;
    }
    @Override public int hashCode() { return Objects.hash(x, z); }
    @Override public String toString() { return "[" + x + "," + z + "]"; }
}
