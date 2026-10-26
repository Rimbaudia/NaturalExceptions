package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating islands high above the land, in three tiers: many small ones, fewer medium ones with lakes and groves,
 * and rare grand ones that each hold a sky dungeon. Every island is a pure shape: a gently domed top over a noisy
 * cone that tapers to a ragged underside.
 */
public final class SkyIslands {

    public enum Tier {

        /** Scattered and common: a few blocks of meadow, grove or crag. */
        SMALL(128, 0.40, 5.0, 12.0, 14.0, 130, 180, 16, 0.05),
        /** Every few hundred blocks: lakes, groves, meadows and crags; sometimes a hidden cache. */
        MEDIUM(384, 0.40, 14.0, 24.0, 26.0, 135, 175, 36, 0.30),
        /**
         * A few per few thousand blocks: each holds a sky dungeon. A sanctum's island is 100 to 110 blocks across the
         * radius, a citadel's a vast 200 to 220 (see {@link #GRAND_SANCTUM_RADIUS}). The margin keeps even the
         * largest (its ragged edge reaching 1.18 radii) clear of its neighbours.
         */
        GRAND(1536, 0.50, 100.0, 220.0, 72.0, 176, 190, 290, 0.0);

        final int cell;
        final double presence;
        final double minRadius;
        final double maxRadius;
        final double maxThickness;
        final int minY;
        final int maxY;
        final int margin;
        final double cacheChance;

        Tier(int cell, double presence, double minRadius, double maxRadius, double maxThickness, int minY, int maxY,
            int margin, double cacheChance) {
            this.cell = cell;
            this.presence = presence;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.maxThickness = maxThickness;
            this.minY = minY;
            this.maxY = maxY;
            this.margin = margin;
            this.cacheChance = cacheChance;
        }

        public int cell() {
            return cell;
        }

        public double maxRadius() {
            return maxRadius;
        }
    }

    /** The medium tier's grid (kept for callers that think in one grid). */
    public static final int CELL = 384;
    public static final int MIN_CENTRE_Y = 130;
    public static final int MAX_CENTRE_Y = 190;
    public static final double MIN_RADIUS = 5.0;
    public static final double MAX_RADIUS = 220.0;
    /** Radius ranges of the two kinds of dungeon island. */
    public static final double[] GRAND_SANCTUM_RADIUS = { 100.0, 110.0 };
    public static final double[] GRAND_CITADEL_RADIUS = { 200.0, 220.0 };

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
        private final Tier tier;
        private final boolean cache;

        Island(Tier tier, long cellX, long cellZ, int centreX, int centreY, int centreZ, double radius,
            double thickness, Kind kind, boolean cache) {
            this.tier = tier;
            this.cache = cache;
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

        /** Whether a plain cache chest lies hidden in a boulder heap on this island (never on a dungeon island). */
        public boolean hasCache() {
            return cache;
        }

        public Tier tier() {
            return tier;
        }

        /** Grand islands hold a sky dungeon. */
        public boolean hasDungeon() {
            return tier == Tier.GRAND;
        }
    }

    private final LandmarkNoise noise;

    public SkyIslands(long seed) {
        this.noise = new LandmarkNoise(seed ^ 0x534b5949534c414eL);
    }

    /** Identifies this world's islands (for caching the dungeons built on them). */
    public long seed() {
        return noise.seed();
    }

    /** A medium-tier island in its cell, or null. */
    public Island atCell(long cellX, long cellZ) {
        return atCell(Tier.MEDIUM, cellX, cellZ);
    }

    public Island atCell(Tier tier, long cellX, long cellZ) {
        long t = tier.ordinal() * 1000L;
        if (noise.unit(cellX, cellZ, t + 1) >= tier.presence) return null;
        int span = tier.cell - 2 * tier.margin;
        int x = (int) (cellX * tier.cell + tier.margin + noise.unit(cellX, cellZ, t + 2) * span);
        int z = (int) (cellZ * tier.cell + tier.margin + noise.unit(cellX, cellZ, t + 3) * span);
        // A lesser island never stands within a dungeon island's reach: it would grow into its rock and its town.
        if (tier != Tier.GRAND && nearGrand(x, z, tier.maxRadius * 1.18 * CLOUD_RIM_OUTER)) return null;
        int y = tier.minY + (int) (noise.unit(cellX, cellZ, t + 4) * (tier.maxY - tier.minY + 1));
        // Grand islands on their own grid: offset their cell ids to keep hashes apart from the others.
        long idX = tier == Tier.MEDIUM ? cellX : cellX + (tier.ordinal() + 1) * 0x10000000L;
        double[] range = { tier.minRadius, tier.maxRadius };
        if (tier == Tier.GRAND)
            range = kindOf(idX, cellZ) == SkyDungeon.Kind.CITADEL ? GRAND_CITADEL_RADIUS : GRAND_SANCTUM_RADIUS;
        double radius = range[0] + noise.unit(cellX, cellZ, t + 5) * (range[1] - range[0]);
        double thickness = tier == Tier.GRAND ? tier.maxThickness * (0.9 + 0.1 * noise.unit(cellX, cellZ, t + 6))
            : Math.min(tier.maxThickness, radius * (0.7 + 0.5 * noise.unit(cellX, cellZ, t + 6)));
        double roll = noise.unit(cellX, cellZ, t + 7);
        Kind kind = roll < 0.3 ? Kind.GROVE : roll < 0.6 ? Kind.MEADOW : roll < 0.8 ? Kind.CRAG : Kind.LAKE;
        if (kind == Kind.LAKE && tier != Tier.MEDIUM) kind = Kind.MEADOW; // lakes need a medium island
        if (tier == Tier.GRAND && kind == Kind.CRAG) kind = Kind.GROVE;
        boolean cache = noise.unit(cellX, cellZ, t + 8) < tier.cacheChance;
        return new Island(tier, idX, cellZ, x, y, z, radius, thickness, kind, cache);
    }

    /** Whether a grand island's reach (its ragged edge and cloud rim, plus {@code pad}) covers (x, z). */
    private boolean nearGrand(int x, int z, double pad) {
        Tier g = Tier.GRAND;
        long gx = Math.floorDiv(x, g.cell), gz = Math.floorDiv(z, g.cell);
        for (long cx = gx - 1; cx <= gx + 1; cx++) for (long cz = gz - 1; cz <= gz + 1; cz++) {
            Island grand = atCell(g, cx, cz);
            if (grand == null) continue;
            double reach = grand.radius * 1.18 * CLOUD_RIM_OUTER + pad;
            double dx = x - grand.centreX, dz = z - grand.centreZ;
            if (dx * dx + dz * dz < reach * reach) return true;
        }
        return false;
    }

    /** Islands of every tier that may reach any column in the box [minX, maxX] x [minZ, maxZ]. */
    public List<Island> near(int minX, int minZ, int maxX, int maxZ) {
        List<Island> result = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            // The ragged edge reaches 1.18 radii and the cloud rim a little beyond that.
            int reach = (int) Math.ceil(tier.maxRadius * 1.18 * CLOUD_RIM_OUTER) + 2;
            long cx0 = Math.floorDiv(minX - reach, tier.cell), cx1 = Math.floorDiv(maxX + reach, tier.cell);
            long cz0 = Math.floorDiv(minZ - reach, tier.cell), cz1 = Math.floorDiv(maxZ + reach, tier.cell);
            for (long cx = cx0; cx <= cx1; cx++) {
                for (long cz = cz0; cz <= cz1; cz++) {
                    Island island = atCell(tier, cx, cz);
                    if (island != null) result.add(island);
                }
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
        // A dungeon island is a massive block of rock, its underside flat until near the rim; others taper to a point.
        boolean grand = island.tier == Tier.GRAND;
        double taper = Math.pow(1.0 - d, grand ? 0.3 : 0.7);
        double noiseAt = noise.smooth(x, z, 5.0, island.cellX ^ (island.cellZ << 20));
        double ragged = grand ? 0.9 + 0.2 * noiseAt : 0.75 + 0.5 * noiseAt;
        int bottom = island.centreY - (int) Math.round(island.thickness * taper * ragged);
        if (bottom > top - 1) bottom = top - 1;
        return new int[] { bottom, top };
    }

    /** Whether this column of the island holds the pond's water at its top. */
    public boolean pondAt(Island island, int x, int z) {
        return island.pond && reach(island, x, z) < POND;
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
            || dungeonCovers(island, x, z)) return Decor.NONE;
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

    /** Boulders: a few low rock heaps, taller at their middle; a cache chest hides in a ring of them. */
    private Decor boulderAt(Island island, int x, int z) {
        if (island.cache) {
            int[] spot = cacheSpot(island);
            double d = Math.hypot(x - spot[0], z - spot[1]);
            if (d < 0.5) return Decor.NONE; // the chest itself
            if (d < 1.9) return noise.unit(x, z, 43) < 0.35 ? Decor.TALL_BOULDER : Decor.BOULDER;
        }
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
                && !dungeonCovers(island, spot[0], spot[1])
                && !(island.cache && Math.hypot(spot[0] - cacheSpot(island)[0], spot[1] - cacheSpot(island)[1]) < 4))
                spots.add(spot);
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
        if (d >= CLOUD_RIM_INNER && d < CLOUD_RIM_OUTER && fluff > 0.42 && !inUpdraftGap(island, x, z)) {
            int top = island.centreY - (d < 1.0 ? 1 : 0);
            return new int[] { top - (fluff > 0.7 ? 2 : 1), top };
        }
        if (d < 0.85 && d > 0.25 && fluff > 0.68) {
            int[] column = column(island, x, z);
            if (column != null) return new int[] { column[0] - (fluff > 0.8 ? 2 : 1), column[0] - 1 };
        }
        return null;
    }

    /**
     * The bearing (radians, from east toward south) of the island's updraft, or NaN if it has none: every great
     * island has one, and a third of the rest (see {@link Updrafts}).
     */
    public double updraftBearing(Island island) {
        long h = seed() ^ island.cellX * 0x2545F4914F6CDD1DL ^ island.cellZ * 0x9E3779B97F4A7C15L ^ 0x55504452L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        if (island.tier() != Tier.GRAND && Math.floorMod(h, 3) != 0) return Double.NaN;
        return Math.floorMod(h >>> 8, 3600) / 1800.0 * Math.PI;
    }

    /** How wide a gap the cloud rim leaves where an updraft rises past it, either side of the bearing (blocks). */
    static final double UPDRAFT_GAP = 5.0;

    /** Whether (x, z) lies in the gap the island's cloud rim leaves for its updraft. */
    boolean inUpdraftGap(Island island, int x, int z) {
        double bearing = updraftBearing(island);
        if (Double.isNaN(bearing)) return false;
        double dx = x + 0.5 - island.centreX, dz = z + 0.5 - island.centreZ;
        double along = dx * Math.cos(bearing) + dz * Math.sin(bearing);
        double across = -dx * Math.sin(bearing) + dz * Math.cos(bearing);
        return along > 0 && Math.abs(across) < UPDRAFT_GAP;
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

    // --- Dungeons -----------------------------------------------------------------------------------------

    /** Which dungeon a grand island holds, or null. */
    public SkyDungeon.Kind dungeonKind(Island island) {
        if (!island.hasDungeon()) return null;
        return kindOf(island.cellX, island.cellZ);
    }

    private SkyDungeon.Kind kindOf(long idX, long cellZ) {
        return noise.unit(idX, cellZ, 50) < 0.5 ? SkyDungeon.Kind.SANCTUM : SkyDungeon.Kind.CITADEL;
    }

    /** Whether a dungeon's surface works (citadel, sanctum gateway) cover this column, so nothing grows there. */
    public boolean dungeonCovers(Island island, int x, int z) {
        if (!island.hasDungeon()) return false;
        SkyDungeon dungeon = SkyDungeon.of(this, island);
        return dungeon != null && dungeon.coversSurface(x, z);
    }

    /** Share of a medium or grand island's underside columns a Stormglass point hangs from. */
    public static final double STORMGLASS_HANG = 0.04;
    /** Share of the rock within {@link #STORMGLASS_DEPTH} blocks of an island's underside that is Stormglass vein. */
    public static final double STORMGLASS_VEIN = 0.05;
    public static final int STORMGLASS_DEPTH = 3;

    /**
     * How long a point of Stormglass hangs under this column of an island: 0 for none, else 1 to 3 blocks. Storms
     * break on the big islands, so small ones grow none.
     */
    public int stormglassHang(Island island, int x, int z) {
        if (island.tier == Tier.SMALL || noise.unit3(x, 0, z, 61) >= STORMGLASS_HANG) return 0;
        return 1 + (int) (noise.unit3(x, 1, z, 62) * 3);
    }

    /** Whether the rock at (x, y, z), in a column whose underside is {@code bottom}, is a Stormglass vein. */
    public boolean stormglassVein(int x, int y, int z, int bottom) {
        return y < bottom + STORMGLASS_DEPTH && noise.unit3(x, y, z, 63) < STORMGLASS_VEIN;
    }

    /** A deterministic value in [0, 1) for one block of an island (ore veins, loot). */
    public double blockRoll(int x, int y, int z, long lane) {
        return noise.unit3(x, y, z, lane);
    }
}
