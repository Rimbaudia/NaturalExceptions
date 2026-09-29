package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating islands high above the land. One candidate per {@link #CELL}-block cell, about a third of them real,
 * so an island shows every few hundred blocks. Each island is a pure shape: a gently domed top over a noisy cone
 * that tapers to a ragged underside.
 */
public final class SkyIslands {

    public static final int CELL = 384;
    static final double PRESENCE = 0.34;
    public static final int MIN_CENTRE_Y = 150;
    public static final int MAX_CENTRE_Y = 200;
    public static final double MIN_RADIUS = 10.0;
    public static final double MAX_RADIUS = 30.0;
    /** Islands this large carry a sky cache. */
    public static final double CACHE_RADIUS = 18.0;
    private static final int MARGIN = 40;

    public static final class Island {

        private final long cellX;
        private final long cellZ;
        private final int centreX;
        private final int centreY;
        private final int centreZ;
        private final double radius;
        private final double thickness;
        private final boolean pond;
        private final boolean tree;

        Island(long cellX, long cellZ, int centreX, int centreY, int centreZ, double radius, double thickness,
            boolean pond, boolean tree) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.centreX = centreX;
            this.centreY = centreY;
            this.centreZ = centreZ;
            this.radius = radius;
            this.thickness = thickness;
            this.pond = pond;
            this.tree = tree;
        }

        public long cellX() {
            return cellX;
        }

        public long cellZ() {
            return cellZ;
        }

        public int centreX() {
            return centreX;
        }

        public int centreY() {
            return centreY;
        }

        public int centreZ() {
            return centreZ;
        }

        public double radius() {
            return radius;
        }

        public double thickness() {
            return thickness;
        }

        public boolean hasPond() {
            return pond;
        }

        public boolean hasTree() {
            return tree;
        }

        public boolean hasCache() {
            return radius >= CACHE_RADIUS;
        }
    }

    private final LandmarkNoise noise;

    public SkyIslands(long seed) {
        this.noise = new LandmarkNoise(seed ^ 0x534b5949534c414eL);
    }

    public Island atCell(long cellX, long cellZ) {
        if (noise.unit(cellX, cellZ, 1) >= PRESENCE) return null;
        int x = (int) (cellX * CELL + MARGIN + noise.unit(cellX, cellZ, 2) * (CELL - 2 * MARGIN));
        int z = (int) (cellZ * CELL + MARGIN + noise.unit(cellX, cellZ, 3) * (CELL - 2 * MARGIN));
        int y = MIN_CENTRE_Y + (int) (noise.unit(cellX, cellZ, 4) * (MAX_CENTRE_Y - MIN_CENTRE_Y + 1));
        double radius = MIN_RADIUS + noise.unit(cellX, cellZ, 5) * (MAX_RADIUS - MIN_RADIUS);
        double thickness = radius * (0.7 + 0.5 * noise.unit(cellX, cellZ, 6));
        boolean pond = radius >= 14.0 && noise.unit(cellX, cellZ, 7) < 0.5;
        boolean tree = noise.unit(cellX, cellZ, 8) < 0.7;
        return new Island(cellX, cellZ, x, y, z, radius, thickness, pond, tree);
    }

    /** Islands that may reach any column in the box [minX, maxX] x [minZ, maxZ]. */
    public List<Island> near(int minX, int minZ, int maxX, int maxZ) {
        List<Island> result = new ArrayList<>();
        int reach = (int) Math.ceil(MAX_RADIUS * 1.25) + 1;
        long cx0 = Math.floorDiv(minX - reach, CELL), cx1 = Math.floorDiv(maxX + reach, CELL);
        long cz0 = Math.floorDiv(minZ - reach, CELL), cz1 = Math.floorDiv(maxZ + reach, CELL);
        for (long cx = cx0; cx <= cx1; cx++) {
            for (long cz = cz0; cz <= cz1; cz++) {
                Island island = atCell(cx, cz);
                if (island != null) result.add(island);
            }
        }
        return result;
    }

    /** Horizontal distance from the island's centre as a fraction of its (ragged) edge; below 1 is inside. */
    public double reach(Island island, int x, int z) {
        double dx = x + 0.5 - island.centreX;
        double dz = z + 0.5 - island.centreZ;
        double angle = Math.atan2(dz, dx);
        double edge = island.radius * (0.82 + 0.36 * noise.around(angle, 9, island.cellX * 31 + island.cellZ));
        return Math.sqrt(dx * dx + dz * dz) / edge;
    }

    /** {bottomY, topY} of the island in this column, or null if the column misses it. */
    public int[] column(Island island, int x, int z) {
        double d = reach(island, x, z);
        if (d >= 1.0) return null;
        int top = island.centreY + (d < 0.5 ? 1 : 0);
        double taper = Math.pow(1.0 - d, 0.7);
        double ragged = 0.75 + 0.5 * noise.smooth(x, z, 5.0, island.cellX ^ (island.cellZ << 20));
        int bottom = island.centreY - (int) Math.round(island.thickness * taper * ragged);
        if (bottom > top - 1) bottom = top - 1;
        return new int[] { bottom, top };
    }

    /** Whether this column of the island holds the pond's water at its top. */
    public boolean pondAt(Island island, int x, int z) {
        return island.pond && reach(island, x, z) < 0.28;
    }

    /** The island's tree position {x, z}, on solid ground away from any pond. */
    public int[] treeSpot(Island island) {
        double angle = noise.unit(island.cellX, island.cellZ, 9) * 2.0 * Math.PI;
        double r = island.radius * 0.38;
        return new int[] { island.centreX + (int) Math.round(Math.cos(angle) * r),
            island.centreZ + (int) Math.round(Math.sin(angle) * r) };
    }

    /** The sky cache's position {x, z}: opposite the tree, near the middle. */
    public int[] cacheSpot(Island island) {
        double angle = noise.unit(island.cellX, island.cellZ, 9) * 2.0 * Math.PI + Math.PI;
        double r = island.radius * 0.34;
        return new int[] { island.centreX + (int) Math.round(Math.cos(angle) * r),
            island.centreZ + (int) Math.round(Math.sin(angle) * r) };
    }

    /** A deterministic value in [0, 1) for one block of an island (ore veins, loot). */
    public double blockRoll(int x, int y, int z, long lane) {
        return noise.unit3(x, y, z, lane);
    }
}
