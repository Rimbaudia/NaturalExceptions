package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Zoned pockets deep underground: Lumenite grottos (y 14-34), oval caverns walled with Lumenite ore and floored
 * with glowing clusters; and magma pockets (y 5-14), lava-filled chambers wrapped in baked Magma Rock. Pure
 * functions of position, like the other landmarks, so chunks build them independently.
 */
public final class DeepLayers {

    public static final int GROTTO_CELL = 112;
    public static final int MAGMA_CELL = 96;
    static final double GROTTO_PRESENCE = 0.35;
    static final double MAGMA_PRESENCE = 0.4;
    /** Share of a grotto's wall that is Lumenite ore, and of its floor that grows clusters. */
    public static final double ORE_SHARE = 0.14;
    public static final double CLUSTER_SHARE = 0.09;

    public enum Part {
        /** Nothing of this pocket here. */
        NONE,
        /** Open cavern. */
        OPEN,
        /** Open floor cell of a grotto where a cluster grows. */
        CLUSTER,
        /** Grotto wall turned to Lumenite ore. */
        ORE,
        /** Lava inside a magma pocket. */
        LAVA,
        /** Magma Rock around a magma pocket. */
        MAGMA_ROCK
    }

    public static final class Pocket {

        private final boolean magma;
        private final long cellX;
        private final long cellZ;
        private final int x;
        private final int y;
        private final int z;
        private final double rx;
        private final double ry;
        private final double rz;

        Pocket(boolean magma, long cellX, long cellZ, int x, int y, int z, double rx, double ry, double rz) {
            this.magma = magma;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
        }

        public boolean isMagma() {
            return magma;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        public double rx() {
            return rx;
        }

        public double ry() {
            return ry;
        }

        public double rz() {
            return rz;
        }

        public int minY() {
            return (int) Math.floor(y - ry - 2);
        }

        public int maxY() {
            return (int) Math.ceil(y + ry + 2);
        }
    }

    private final LandmarkNoise noise;

    public DeepLayers(long seed) {
        this.noise = new LandmarkNoise(seed ^ 0x444545504c415952L);
    }

    public Pocket grottoAtCell(long cellX, long cellZ) {
        if (noise.unit(cellX, cellZ, 1) >= GROTTO_PRESENCE) return null;
        int margin = 26;
        int x = (int) (cellX * GROTTO_CELL + margin + noise.unit(cellX, cellZ, 2) * (GROTTO_CELL - 2 * margin));
        int z = (int) (cellZ * GROTTO_CELL + margin + noise.unit(cellX, cellZ, 3) * (GROTTO_CELL - 2 * margin));
        int y = 20 + (int) (noise.unit(cellX, cellZ, 4) * 8);
        double rx = 10.0 + noise.unit(cellX, cellZ, 5) * 10.0;
        double rz = 10.0 + noise.unit(cellX, cellZ, 6) * 10.0;
        double ry = 5.0 + noise.unit(cellX, cellZ, 7) * 4.0;
        return new Pocket(false, cellX, cellZ, x, y, z, rx, ry, rz);
    }

    public Pocket magmaAtCell(long cellX, long cellZ) {
        if (noise.unit(cellX, cellZ, 11) >= MAGMA_PRESENCE) return null;
        int margin = 18;
        int x = (int) (cellX * MAGMA_CELL + margin + noise.unit(cellX, cellZ, 12) * (MAGMA_CELL - 2 * margin));
        int z = (int) (cellZ * MAGMA_CELL + margin + noise.unit(cellX, cellZ, 13) * (MAGMA_CELL - 2 * margin));
        int y = 9 + (int) (noise.unit(cellX, cellZ, 14) * 3);
        double rx = 6.0 + noise.unit(cellX, cellZ, 15) * 6.0;
        double rz = 6.0 + noise.unit(cellX, cellZ, 16) * 6.0;
        double ry = 3.0 + noise.unit(cellX, cellZ, 17) * 2.0;
        return new Pocket(true, cellX, cellZ, x, y, z, rx, ry, rz);
    }

    /** Pockets that may reach any column in the box [minX, maxX] x [minZ, maxZ]. */
    public List<Pocket> near(int minX, int minZ, int maxX, int maxZ) {
        List<Pocket> result = new ArrayList<>();
        collect(result, minX, minZ, maxX, maxZ, GROTTO_CELL, false);
        collect(result, minX, minZ, maxX, maxZ, MAGMA_CELL, true);
        return result;
    }

    private void collect(List<Pocket> into, int minX, int minZ, int maxX, int maxZ, int cell, boolean magma) {
        int reach = 26;
        for (long cx = Math.floorDiv(minX - reach, cell); cx <= Math.floorDiv(maxX + reach, cell); cx++) {
            for (long cz = Math.floorDiv(minZ - reach, cell); cz <= Math.floorDiv(maxZ + reach, cell); cz++) {
                Pocket pocket = magma ? magmaAtCell(cx, cz) : grottoAtCell(cx, cz);
                if (pocket != null) into.add(pocket);
            }
        }
    }

    /** Normalised ellipsoid distance with a lumpy surface: below 1 is inside. */
    private double shape(Pocket pocket, int x, int y, int z) {
        double dx = (x + 0.5 - pocket.x) / pocket.rx;
        double dy = (y + 0.5 - pocket.y) / pocket.ry;
        double dz = (z + 0.5 - pocket.z) / pocket.rz;
        double lumps = 0.85 + 0.3 * noise.smooth(x + y * 0.7, z - y * 0.4, 5.0, pocket.cellX * 13 + pocket.cellZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz) / lumps;
    }

    /** What this pocket makes of the block at (x, y, z). */
    public Part partAt(Pocket pocket, int x, int y, int z) {
        if (y < 5 || y < pocket.minY() || y > pocket.maxY()) return Part.NONE;
        double d = shape(pocket, x, y, z);
        if (pocket.magma) {
            if (d < 1.0) return y <= pocket.y ? Part.LAVA : Part.OPEN;
            return d < 1.35 ? Part.MAGMA_ROCK : Part.NONE;
        }
        if (d < 1.0) {
            boolean floor = shape(pocket, x, y - 1, z) >= 1.0;
            if (floor && noise.unit3(x, y, z, 21) < CLUSTER_SHARE) return Part.CLUSTER;
            return Part.OPEN;
        }
        if (d < 1.25 && noise.unit3(x, y, z, 22) < ORE_SHARE) return Part.ORE;
        return Part.NONE;
    }
}
