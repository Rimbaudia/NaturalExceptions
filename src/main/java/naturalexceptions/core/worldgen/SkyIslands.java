package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating islands high above the land. One candidate per {@link #CELL}-block cell, a little under half of them
 * real, so an island is usually in sight within a couple of hundred blocks. Each island is a pure shape: a gently domed
 * top over a noisy cone
 * that tapers to a ragged underside.
 */
public final class SkyIslands {

    public static final int CELL = 256;
    static final double PRESENCE = 0.45;
    public static final int MIN_CENTRE_Y = 135;
    public static final int MAX_CENTRE_Y = 175;
    public static final double MIN_RADIUS = 12.0;
    public static final double MAX_RADIUS = 30.0;
    /** Islands this large carry a sky cache. */
    public static final double CACHE_RADIUS = 18.0;
    private static final int MARGIN = 36;
    /** Deepest an island's underside reaches below its top, so none dips into the hills. */
    static final double MAX_THICKNESS = 30.0;

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
        private final Kind kind;

        Island(long cellX, long cellZ, int centreX, int centreY, int centreZ, double radius, double thickness,
            Kind kind) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.centreX = centreX;
            this.centreY = centreY;
            this.centreZ = centreZ;
            this.radius = radius;
            this.thickness = thickness;
            this.kind = kind;
            this.pond = kind == Kind.LAKE;
            this.tree = kind != Kind.CRAG;
        }

        public Kind kind() {
            return kind;
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
        double thickness = Math.min(MAX_THICKNESS, radius * (0.7 + 0.5 * noise.unit(cellX, cellZ, 6)));
        double roll = noise.unit(cellX, cellZ, 7);
        Kind kind = roll < 0.3 ? Kind.GROVE : roll < 0.6 ? Kind.MEADOW : roll < 0.8 ? Kind.CRAG : Kind.LAKE;
        if (kind == Kind.LAKE && radius < 14.0) kind = Kind.MEADOW; // too small to hold a lake
        return new Island(cellX, cellZ, x, y, z, radius, thickness, kind);
    }

    /** Islands that may reach any column in the box [minX, maxX] x [minZ, maxZ]. */
    public List<Island> near(int minX, int minZ, int maxX, int maxZ) {
        List<Island> result = new ArrayList<>();
        // The ragged edge reaches 1.18 radii and the cloud rim a little beyond that.
        int reach = (int) Math.ceil(MAX_RADIUS * 1.18 * CLOUD_RIM_OUTER) + 2;
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
        // The pond gives way to a shrine on its shore.
        return island.pond && reach(island, x, z) < POND && !shrineCovers(island, x, z);
    }

    /** The island's tree position {x, z}, on solid ground away from any pond. */
    public int[] treeSpot(Island island) {
        double angle = noise.unit(island.cellX, island.cellZ, 9) * 2.0 * Math.PI;
        double r = island.radius * 0.45; // clear of a lake's pond
        return new int[] { island.centreX + (int) Math.round(Math.cos(angle) * r),
            island.centreZ + (int) Math.round(Math.sin(angle) * r) };
    }

    /** The sky cache's position {x, z}: opposite the tree, near the middle. */
    public int[] cacheSpot(Island island) {
        double angle = noise.unit(island.cellX, island.cellZ, 9) * 2.0 * Math.PI + Math.PI;
        double r = island.radius * 0.34;
        return spotAt(island, angle, r);
    }

    private static int[] spotAt(Island island, double angle, double r) {
        return new int[] { island.centreX + (int) Math.round(Math.cos(angle) * r),
            island.centreZ + (int) Math.round(Math.sin(angle) * r) };
    }

    // --- What the island is like on top ---------------------------------------------------------------------

    /** Share of an island's reach that a Lake island's pond covers. */
    static final double POND = 0.34;
    /** The cloud rim hangs from here to here, as a share of the island's reach. */
    static final double CLOUD_RIM_INNER = 0.9;
    static final double CLOUD_RIM_OUTER = 1.15;

    public enum Kind {
        /** Trees, ferns, grass and a few flowers. */
        GROVE,
        /** Long grass and flowers, bushes and mossy boulders. */
        MEADOW,
        /** Bare rock, gravel and ore breaking the surface. */
        CRAG,
        /** A wide pond spilling over the edge as a waterfall. */
        LAKE
    }

    /** The top block of a column. */
    public enum Surface {
        GRASS,
        STONE,
        GRAVEL,
        COBBLE,
        COAL_ORE,
        IRON_ORE,
        COPPER_ORE
    }

    /** What stands on a column's top block. */
    public enum Decor {
        NONE,
        TALL_GRASS,
        FERN,
        RED_FLOWER,
        YELLOW_FLOWER,
        WINDBLOOM,
        BUSH,
        BOULDER,
        TALL_BOULDER,
        DEAD_BUSH
    }

    public Surface surface(Island island, int x, int z) {
        if (island.kind != Kind.CRAG) return Surface.GRASS;
        // Crags keep a few grassy hollows; the rest is rock with ore breaking through.
        if (noise.smooth(x, z, 4.0, island.cellX * 3 + island.cellZ) > 0.72) return Surface.GRASS;
        double roll = noise.unit(x, z, 31);
        if (roll < 0.03) return Surface.COAL_ORE;
        if (roll < 0.05) return Surface.IRON_ORE;
        if (roll < 0.07) return Surface.COPPER_ORE;
        if (roll < 0.30) return Surface.GRAVEL;
        if (roll < 0.52) return Surface.COBBLE;
        return Surface.STONE;
    }

    public Decor decor(Island island, int x, int z) {
        if (column(island, x, z) == null || pondAt(island, x, z)
            || waterfallAt(island, x, z)
            || shrineCovers(island, x, z)) return Decor.NONE;
        Decor boulder = boulderAt(island, x, z);
        if (boulder != Decor.NONE) return boulder;
        Surface surface = surface(island, x, z);
        double roll = noise.unit(x, z, 32);
        if (surface != Surface.GRASS) return roll < 0.03 ? Decor.DEAD_BUSH : Decor.NONE;
        switch (island.kind) {
            case GROVE:
                return roll < 0.30 ? Decor.TALL_GRASS
                    : roll < 0.40 ? Decor.FERN
                        : roll < 0.44 ? Decor.RED_FLOWER
                            : roll < 0.47 ? Decor.YELLOW_FLOWER
                                : roll < 0.49 ? Decor.WINDBLOOM : roll < 0.53 ? Decor.BUSH : Decor.NONE;
            case MEADOW:
                return roll < 0.45 ? Decor.TALL_GRASS
                    : roll < 0.49 ? Decor.RED_FLOWER
                        : roll < 0.53 ? Decor.YELLOW_FLOWER
                            : roll < 0.57 ? Decor.WINDBLOOM : roll < 0.60 ? Decor.BUSH : Decor.NONE;
            case LAKE:
                return roll < 0.30 ? Decor.TALL_GRASS
                    : roll < 0.33 ? Decor.RED_FLOWER
                        : roll < 0.36 ? Decor.YELLOW_FLOWER : roll < 0.39 ? Decor.WINDBLOOM : Decor.NONE;
            case CRAG:
            default:
                return roll < 0.25 ? Decor.TALL_GRASS : roll < 0.28 ? Decor.WINDBLOOM : Decor.NONE;
        }
    }

    /** Boulders: a few low rock heaps, taller at their middle. */
    private Decor boulderAt(Island island, int x, int z) {
        int count = island.kind == Kind.CRAG ? 4 : island.kind == Kind.MEADOW ? 3 : island.kind == Kind.GROVE ? 1 : 0;
        for (int i = 0; i < count; i++) {
            double angle = noise.unit(island.cellX + i * 7, island.cellZ, 33) * Math.PI * 2.0;
            double r = island.radius * (0.2 + 0.55 * noise.unit(island.cellX + i * 7, island.cellZ, 34));
            double bx = island.centreX + Math.cos(angle) * r, bz = island.centreZ + Math.sin(angle) * r;
            double d = Math.hypot(x + 0.5 - bx, z + 0.5 - bz);
            if (d < 0.8) return Decor.TALL_BOULDER;
            if (d < 1.6) return Decor.BOULDER;
        }
        return Decor.NONE;
    }

    /** Where trees stand {x, z}: several in a grove, perhaps one in a meadow, one by a lake, none on a crag. */
    public List<int[]> treeSpots(Island island) {
        List<int[]> spots = new ArrayList<>();
        int count = island.kind == Kind.GROVE ? 5 + (int) (noise.unit(island.cellX, island.cellZ, 35) * 4)
            : island.kind == Kind.MEADOW ? (noise.unit(island.cellX, island.cellZ, 36) < 0.5 ? 1 : 0)
                : island.kind == Kind.LAKE ? 1 : 0;
        for (int i = 0; i < count; i++) {
            int[] spot = i == 0 ? treeSpot(island) : null;
            if (spot == null) {
                double angle = noise.unit(island.cellX + i * 13, island.cellZ, 37) * Math.PI * 2.0;
                double r = island.radius * (0.25 + 0.4 * noise.unit(island.cellX + i * 13, island.cellZ, 38));
                spot = new int[] { island.centreX + (int) Math.round(Math.cos(angle) * r),
                    island.centreZ + (int) Math.round(Math.sin(angle) * r) };
            }
            if (column(island, spot[0], spot[1]) != null && !pondAt(island, spot[0], spot[1])
                && !waterfallAt(island, spot[0], spot[1])
                && !nearShrine(island, spot[0], spot[1], 3)) spots.add(spot);
        }
        return spots;
    }

    /** Whether a grove's tree here is a birch. */
    public boolean birch(Island island, int[] spot) {
        return island.kind == Kind.GROVE && noise.unit(spot[0], spot[1], 39) < 0.4;
    }

    // --- Clouds and the waterfall -----------------------------------------------------------------------------

    /**
     * {lowY, highY} of Cloud in this column, or null: a ragged rim hanging around the edge just below the top, and
     * puffs under the island's underside.
     */
    public int[] cloud(Island island, int x, int z) {
        double d = reach(island, x, z);
        double fluff = noise.smooth(x, z, 3.5, island.cellX * 5 + island.cellZ + 77);
        if (d >= CLOUD_RIM_INNER && d < CLOUD_RIM_OUTER && fluff > 0.42) {
            int top = island.centreY - (d < 1.0 ? 1 : 0);
            return new int[] { top - (fluff > 0.7 ? 2 : 1), top };
        }
        if (d < 0.85 && d > 0.25 && fluff > 0.68) {
            int[] column = column(island, x, z);
            if (column != null) return new int[] { column[0] - (fluff > 0.8 ? 2 : 1), column[0] - 1 };
        }
        return null;
    }

    private double waterfallHeading(Island island) {
        return noise.unit(island.cellX, island.cellZ, 40) * Math.PI * 2.0;
    }

    /** Whether water runs in this column's top: the channel from a lake's pond out to the edge. */
    public boolean waterfallAt(Island island, int x, int z) {
        if (island.kind != Kind.LAKE || column(island, x, z) == null) return false;
        double heading = waterfallHeading(island);
        double dx = x + 0.5 - island.centreX, dz = z + 0.5 - island.centreZ;
        double along = dx * Math.cos(heading) + dz * Math.sin(heading);
        double across = -dx * Math.sin(heading) + dz * Math.cos(heading);
        return along > 0.0 && Math.abs(across) < 0.8 && reach(island, x, z) >= POND - 0.05;
    }

    /** The lip {x, z} where the waterfall pours off the edge: the channel's last column, or null. */
    public int[] waterfallLip(Island island) {
        if (island.kind != Kind.LAKE) return null;
        double heading = waterfallHeading(island);
        int[] last = null;
        for (double r = 0.0; r < island.radius * 1.3; r += 0.5) {
            int x = (int) Math.floor(island.centreX + 0.5 + Math.cos(heading) * r);
            int z = (int) Math.floor(island.centreZ + 0.5 + Math.sin(heading) * r);
            if (column(island, x, z) == null) break;
            if (waterfallAt(island, x, z)) last = new int[] { x, z };
        }
        return last;
    }

    // --- Shrines ----------------------------------------------------------------------------------------------

    public enum ShrineKind {
        /** A floor of Skystone, four (partly broken) corner pillars and lintels; the chest on a raised step. */
        OPEN,
        /** A hollow tower with windows and a ladder; the chest on its roof. */
        WATCHTOWER,
        /** A raised platform ringed with pillars; the chest under an arch. */
        PLATFORM
    }

    /** One block of a shrine, relative to its origin on the ground (dy = 0 replaces the island's top block). */
    public static final class Piece {

        public enum Block {
            SKYSTONE,
            GLASS_PANE,
            LADDER_NORTH,
            AIR,
            CHEST
        }

        public final int dx;
        public final int dy;
        public final int dz;
        public final Block block;

        Piece(int dx, int dy, int dz, Block block) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.block = block;
        }
    }

    public ShrineKind shrineKind(Island island) {
        if (!island.hasCache()) return null;
        double roll = noise.unit(island.cellX, island.cellZ, 41);
        return roll < 0.4 ? ShrineKind.OPEN : roll < 0.7 ? ShrineKind.WATCHTOWER : ShrineKind.PLATFORM;
    }

    /** Half the width of a shrine's footprint. */
    static int shrineHalf(ShrineKind kind) {
        return kind == ShrineKind.PLATFORM ? 4 : kind == ShrineKind.OPEN ? 3 : 2;
    }

    /** The shrine's origin {x, y, z}: the cache spot, on the island's top there. Null if the island has none. */
    public int[] shrineOrigin(Island island) {
        if (shrineKind(island) == null) return null;
        int[] spot = cacheSpot(island);
        int[] column = column(island, spot[0], spot[1]);
        return column == null ? null : new int[] { spot[0], column[1], spot[1] };
    }

    public boolean shrineCovers(Island island, int x, int z) {
        return nearShrine(island, x, z, 0);
    }

    private boolean nearShrine(Island island, int x, int z, int margin) {
        ShrineKind kind = shrineKind(island);
        int[] origin = kind == null ? null : shrineOrigin(island);
        if (origin == null) return false;
        int half = shrineHalf(kind) + margin + (kind == ShrineKind.PLATFORM ? 1 : 0);
        return Math.abs(x - origin[0]) <= half && Math.abs(z - origin[2]) <= half;
    }

    /** Every block of the island's shrine, relative to {@link #shrineOrigin}; empty if it has none. */
    public List<Piece> shrinePieces(Island island) {
        List<Piece> pieces = new ArrayList<>();
        ShrineKind kind = shrineKind(island);
        if (kind == null || shrineOrigin(island) == null) return pieces;
        int half = shrineHalf(kind);
        switch (kind) {
            case OPEN: {
                clear(pieces, half, 1, 6);
                floor(pieces, half, 0);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) add(pieces, dx, 1, dz, Piece.Block.SKYSTONE);
                int[][] corners = { { -half, -half }, { half, -half }, { -half, half }, { half, half } };
                boolean[] whole = new boolean[4];
                for (int c = 0; c < 4; c++) {
                    int broken = (int) (noise.unit(island.cellX + c, island.cellZ, 42) * 3); // 0, 1 or 2 missing
                    whole[c] = broken == 0;
                    for (int dy = 1; dy <= 4 - broken; dy++)
                        add(pieces, corners[c][0], dy, corners[c][1], Piece.Block.SKYSTONE);
                }
                // Lintels span only between two whole pillars.
                if (whole[0] && whole[1])
                    for (int dx = -half; dx <= half; dx++) add(pieces, dx, 5, -half, Piece.Block.SKYSTONE);
                if (whole[2] && whole[3])
                    for (int dx = -half; dx <= half; dx++) add(pieces, dx, 5, half, Piece.Block.SKYSTONE);
                add(pieces, 0, 2, 0, Piece.Block.CHEST);
                break;
            }
            case WATCHTOWER: {
                int height = 8;
                clear(pieces, half, 1, height + 2);
                floor(pieces, half, 0);
                for (int dy = 1; dy <= height; dy++) {
                    for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
                        boolean wall = Math.abs(dx) == half || Math.abs(dz) == half;
                        if (!wall) continue;
                        boolean door = dz == -half && dx == 0 && dy <= 2;
                        boolean window = dy == 5 && (dx == 0 || dz == 0);
                        if (door) continue;
                        add(pieces, dx, dy, dz, window ? Piece.Block.GLASS_PANE : Piece.Block.SKYSTONE);
                    }
                }
                // Roof with a hatch over the ladder, crenellations around it, the chest on top.
                for (int dx = -half + 1; dx <= half - 1; dx++) for (int dz = -half + 1; dz <= half - 1; dz++) {
                    if (dx == 0 && dz == half - 1) continue;
                    add(pieces, dx, height, dz, Piece.Block.SKYSTONE);
                }
                for (int dy = 1; dy <= height; dy++) add(pieces, 0, dy, half - 1, Piece.Block.LADDER_NORTH);
                for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
                    boolean ring = Math.abs(dx) == half || Math.abs(dz) == half;
                    if (ring && (dx + dz) % 2 == 0) add(pieces, dx, height + 1, dz, Piece.Block.SKYSTONE);
                }
                add(pieces, -1, height + 1, -1, Piece.Block.CHEST);
                break;
            }
            case PLATFORM:
            default: {
                clear(pieces, half, 1, 6);
                for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
                    add(pieces, dx, 0, dz, Piece.Block.SKYSTONE);
                    add(pieces, dx, 2, dz, Piece.Block.SKYSTONE);
                    boolean post = (Math.abs(dx) == half || dx == 0) && (Math.abs(dz) == half || dz == 0);
                    if (post) add(pieces, dx, 1, dz, Piece.Block.SKYSTONE);
                    boolean ring = (Math.abs(dx) == half || Math.abs(dz) == half) && (dx + dz) % 2 == 0;
                    if (ring) for (int dy = 3; dy <= 4; dy++) add(pieces, dx, dy, dz, Piece.Block.SKYSTONE);
                }
                // An arch over the chest, and a step up onto the platform.
                for (int dy = 3; dy <= 4; dy++) {
                    add(pieces, -1, dy, 0, Piece.Block.SKYSTONE);
                    add(pieces, 1, dy, 0, Piece.Block.SKYSTONE);
                }
                for (int dx = -1; dx <= 1; dx++) add(pieces, dx, 5, 0, Piece.Block.SKYSTONE);
                add(pieces, 0, 1, -half - 1, Piece.Block.SKYSTONE);
                add(pieces, 0, 3, 0, Piece.Block.CHEST);
                break;
            }
        }
        return pieces;
    }

    /** Absolute position {x, y, z} of the shrine's cache chest, or null. */
    public int[] shrineChest(Island island) {
        int[] origin = shrineOrigin(island);
        if (origin == null) return null;
        for (Piece piece : shrinePieces(island)) {
            if (piece.block == Piece.Block.CHEST)
                return new int[] { origin[0] + piece.dx, origin[1] + piece.dy, origin[2] + piece.dz };
        }
        return null;
    }

    private static void clear(List<Piece> pieces, int half, int fromDy, int toDy) {
        for (int dy = fromDy; dy <= toDy; dy++) for (int dx = -half; dx <= half; dx++)
            for (int dz = -half; dz <= half; dz++) pieces.add(new Piece(dx, dy, dz, Piece.Block.AIR));
    }

    private static void floor(List<Piece> pieces, int half, int dy) {
        for (int dx = -half; dx <= half; dx++)
            for (int dz = -half; dz <= half; dz++) add(pieces, dx, dy, dz, Piece.Block.SKYSTONE);
    }

    /** Later pieces replace earlier ones at the same place (clearing comes first, then the build). */
    private static void add(List<Piece> pieces, int dx, int dy, int dz, Piece.Block block) {
        pieces.add(new Piece(dx, dy, dz, block));
    }

    /** A deterministic value in [0, 1) for one block of an island (ore veins, loot). */
    public double blockRoll(int x, int y, int z, long lane) {
        return noise.unit3(x, y, z, lane);
    }
}
