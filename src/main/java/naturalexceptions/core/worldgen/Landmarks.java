package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Giant terrain features that break the vanilla silhouette: chasms, stone spires, sinkholes and natural arches.
 * One candidate per {@link #CELL}-block cell, about half of them real. Every shape is a pure function of world
 * coordinates plus, where a feature meets the ground, the column's own surface height, so any chunk can build its
 * share without looking at its neighbours.
 */
public final class Landmarks {

    public static final int CELL = 512;
    static final double PRESENCE = 0.5;
    private static final int MARGIN = 120;

    public enum Kind {
        CHASM,
        SPIRES,
        SINKHOLE,
        ARCH
    }

    /** Chasm floor: open air down to here, lava just below. */
    public static final int CHASM_FLOOR_Y = 11;
    /** Sinkhole shaft bottom and the flooded cavern beneath it. */
    public static final int SINKHOLE_BOTTOM_Y = 24;
    public static final int CAVERN_FLOOR_Y = 6;
    public static final int CAVERN_WATER_TOP_Y = 12;

    public static final class Pillar {

        public final int x;
        public final int z;
        public final double radius;
        public final int topY;

        Pillar(int x, int z, double radius, int topY) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.topY = topY;
        }
    }

    public static final class Landmark {

        private final Kind kind;
        private final long cellX;
        private final long cellZ;
        private final int x;
        private final int z;
        /** Chasm/arch: centreline points {x, z}; spires: unused. */
        private final double[][] path;
        private final double width;
        private final double height;
        private final List<Pillar> pillars;
        private final int reach;

        Landmark(Kind kind, long cellX, long cellZ, int x, int z, double[][] path, double width, double height,
            List<Pillar> pillars, int reach) {
            this.kind = kind;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.x = x;
            this.z = z;
            this.path = path;
            this.width = width;
            this.height = height;
            this.pillars = pillars;
            this.reach = reach;
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

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        /** Chasm: widest half-width; sinkhole: shaft radius; arch: tube radius. */
        public double width() {
            return width;
        }

        /** Arch: rise of the span above its feet. */
        public double height() {
            return height;
        }

        public List<Pillar> pillars() {
            return pillars;
        }

        /** How far from {@link #x()}, {@link #z()} anything of this landmark can reach. */
        public int reach() {
            return reach;
        }

        double[][] path() {
            return path;
        }
    }

    private final LandmarkNoise noise;

    public Landmarks(long seed) {
        this.noise = new LandmarkNoise(seed ^ 0x4749414e54464541L);
    }

    public Landmark atCell(long cellX, long cellZ) {
        if (noise.unit(cellX, cellZ, 1) >= PRESENCE) return null;
        int x = (int) (cellX * CELL + MARGIN + noise.unit(cellX, cellZ, 2) * (CELL - 2 * MARGIN));
        int z = (int) (cellZ * CELL + MARGIN + noise.unit(cellX, cellZ, 3) * (CELL - 2 * MARGIN));
        Kind kind = Kind.values()[(int) (noise.unit(cellX, cellZ, 4) * Kind.values().length)];
        switch (kind) {
            case CHASM:
                return chasm(cellX, cellZ, x, z);
            case SPIRES:
                return spires(cellX, cellZ, x, z);
            case SINKHOLE: {
                double radius = 10.0 + noise.unit(cellX, cellZ, 5) * 14.0;
                return new Landmark(kind, cellX, cellZ, x, z, null, radius, 0.0, null, (int) (radius * 1.7) + 2);
            }
            case ARCH:
            default:
                return arch(cellX, cellZ, x, z);
        }
    }

    private Landmark chasm(long cellX, long cellZ, int x, int z) {
        double length = 120.0 + noise.unit(cellX, cellZ, 5) * 100.0;
        double width = 4.0 + noise.unit(cellX, cellZ, 6) * 5.0;
        double heading = noise.unit(cellX, cellZ, 7) * Math.PI * 2.0;
        int points = 24;
        double[][] path = new double[points + 1][];
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double along = (t - 0.5) * length;
            // A lazy S-bend: the ravine wanders sideways by up to a fifth of its length.
            double side = Math.sin(t * Math.PI * 2.0 + noise.unit(cellX, cellZ, 8) * 6.28) * length * 0.1
                + (noise.smooth(t * 40.0, cellX * 7 + cellZ, 10.0, 9) - 0.5) * length * 0.12;
            path[i] = new double[] { x + Math.cos(heading) * along - Math.sin(heading) * side,
                z + Math.sin(heading) * along + Math.cos(heading) * side };
        }
        return new Landmark(Kind.CHASM, cellX, cellZ, x, z, path, width, 0.0, null, (int) (length * 0.75) + 12);
    }

    private Landmark spires(long cellX, long cellZ, int x, int z) {
        int count = 3 + (int) (noise.unit(cellX, cellZ, 5) * 5.0);
        List<Pillar> pillars = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = noise.unit(cellX + i, cellZ, 6) * Math.PI * 2.0;
            double distance = i == 0 ? 0.0 : 6.0 + noise.unit(cellX + i, cellZ, 7) * 20.0;
            double radius = 2.0 + noise.unit(cellX + i, cellZ, 8) * 5.0;
            int top = 63 + 40 + (int) (noise.unit(cellX + i, cellZ, 9) * 50.0);
            pillars.add(
                new Pillar(
                    x + (int) Math.round(Math.cos(angle) * distance),
                    z + (int) Math.round(Math.sin(angle) * distance),
                    radius,
                    top));
        }
        return new Landmark(Kind.SPIRES, cellX, cellZ, x, z, null, 0.0, 0.0, pillars, 36);
    }

    private Landmark arch(long cellX, long cellZ, int x, int z) {
        double span = 30.0 + noise.unit(cellX, cellZ, 5) * 30.0;
        double heading = noise.unit(cellX, cellZ, 6) * Math.PI;
        double[][] ends = { { x - Math.cos(heading) * span / 2.0, z - Math.sin(heading) * span / 2.0 },
            { x + Math.cos(heading) * span / 2.0, z + Math.sin(heading) * span / 2.0 } };
        double radius = 3.0 + noise.unit(cellX, cellZ, 7) * 2.5;
        double rise = 20.0 + noise.unit(cellX, cellZ, 8) * 15.0;
        return new Landmark(Kind.ARCH, cellX, cellZ, x, z, ends, radius, rise, null, (int) (span / 2.0 + radius) + 3);
    }

    /** Landmarks that may reach any column in the box [minX, maxX] x [minZ, maxZ]. */
    public List<Landmark> near(int minX, int minZ, int maxX, int maxZ) {
        List<Landmark> result = new ArrayList<>();
        int maxReach = 200;
        long cx0 = Math.floorDiv(minX - maxReach, CELL), cx1 = Math.floorDiv(maxX + maxReach, CELL);
        long cz0 = Math.floorDiv(minZ - maxReach, CELL), cz1 = Math.floorDiv(maxZ + maxReach, CELL);
        for (long cx = cx0; cx <= cx1; cx++) {
            for (long cz = cz0; cz <= cz1; cz++) {
                Landmark landmark = atCell(cx, cz);
                if (landmark == null) continue;
                if (maxX < landmark.x - landmark.reach || minX > landmark.x + landmark.reach
                    || maxZ < landmark.z - landmark.reach
                    || minZ > landmark.z + landmark.reach) continue;
                result.add(landmark);
            }
        }
        return result;
    }

    // --- Chasm ---------------------------------------------------------------------------------------------

    /**
     * The chasm's half-width at this column's position along it, and the column's distance from its centreline:
     * {halfWidth, distance}. Null when the column is nowhere near. The ravine narrows to nothing at both ends.
     */
    public double[] chasm(Landmark chasm, int x, int z) {
        double[][] path = chasm.path;
        double best = Double.MAX_VALUE;
        double bestT = 0.0;
        for (int i = 0; i < path.length - 1; i++) {
            double ax = path[i][0], az = path[i][1], bx = path[i + 1][0], bz = path[i + 1][1];
            double dx = bx - ax, dz = bz - az;
            double lenSq = dx * dx + dz * dz;
            double u = lenSq == 0.0 ? 0.0
                : Math.max(0.0, Math.min(1.0, ((x + 0.5 - ax) * dx + (z + 0.5 - az) * dz) / lenSq));
            double px = ax + dx * u - (x + 0.5), pz = az + dz * u - (z + 0.5);
            double d = Math.sqrt(px * px + pz * pz);
            if (d < best) {
                best = d;
                bestT = (i + u) / (path.length - 1);
            }
        }
        double halfWidth = chasm.width * Math.sqrt(Math.max(0.0, Math.sin(bestT * Math.PI)))
            * (0.85 + 0.3 * noise.smooth(x, z, 9.0, chasm.cellX ^ chasm.cellZ));
        if (best > halfWidth + 2.0) return null;
        return new double[] { halfWidth, best };
    }

    /** Whether the chasm is open at height y in a column whose surface is at surfaceY: walls slope inwards. */
    public static boolean chasmOpen(double[] chasm, int y, int surfaceY) {
        if (chasm == null || y <= CHASM_FLOOR_Y - 1 || y > surfaceY) return false;
        double depth = (y - CHASM_FLOOR_Y) / (double) Math.max(1, surfaceY - CHASM_FLOOR_Y);
        return chasm[1] < chasm[0] * (0.45 + 0.55 * depth);
    }

    // --- Spires --------------------------------------------------------------------------------------------

    /** Whether a spire stands at (x, y, z) for a column whose ground is at groundY. */
    public boolean spireAt(Landmark spires, int x, int y, int z, int groundY) {
        for (Pillar pillar : spires.pillars) {
            if (y <= groundY || y > pillar.topY) continue;
            double dx = x + 0.5 - pillar.x, dz = z + 0.5 - pillar.z;
            double climb = (y - groundY) / (double) Math.max(1, pillar.topY - groundY);
            double radius = pillar.radius * (1.0 - 0.55 * Math.pow(climb, 1.4))
                * (0.85 + 0.3 * noise.smooth(x + y * 0.3, z, 4.0, pillar.x ^ pillar.z));
            if (dx * dx + dz * dz < radius * radius) return true;
        }
        return false;
    }

    // --- Sinkhole ------------------------------------------------------------------------------------------

    /** Whether the sinkhole shaft or its flooded cavern is open at (x, y, z), below a surface at surfaceY. */
    public boolean sinkholeOpen(Landmark sinkhole, int x, int y, int z, int surfaceY) {
        double dx = x + 0.5 - sinkhole.x, dz = z + 0.5 - sinkhole.z;
        double angle = Math.atan2(dz, dx);
        double radius = sinkhole.width * (0.85 + 0.3 * noise.around(angle, 7, sinkhole.cellX ^ sinkhole.cellZ));
        double r = Math.sqrt(dx * dx + dz * dz);
        if (y >= SINKHOLE_BOTTOM_Y && y <= surfaceY) return r < radius;
        // The cavern: a domed chamber twice as wide as the shaft, its floor flooded.
        double cavern = radius * 2.0;
        double top = SINKHOLE_BOTTOM_Y + 2;
        if (y < CAVERN_FLOOR_Y || y > top) return false;
        double ny = (y - CAVERN_FLOOR_Y) / (top - CAVERN_FLOOR_Y);
        return r < cavern * Math.sqrt(Math.max(0.0, 1.0 - ny * ny)) || y >= SINKHOLE_BOTTOM_Y && r < radius;
    }

    // --- Arch ----------------------------------------------------------------------------------------------

    /** The arch's feet stand at this height; the span rises {@link Landmark#height()} above it. */
    public static final int ARCH_BASE_Y = 66;

    /**
     * Whether the arch has rock at (x, y, z) for a column whose ground is at groundY: a rock tube along a curve
     * between the two feet, with the feet running down into the ground.
     */
    public static boolean archAt(Landmark arch, int x, int y, int z, int groundY) {
        double[] a = arch.path[0], b = arch.path[1];
        double dx = b[0] - a[0], dz = b[1] - a[1];
        double lenSq = dx * dx + dz * dz;
        double t = Math.max(0.0, Math.min(1.0, ((x + 0.5 - a[0]) * dx + (z + 0.5 - a[1]) * dz) / lenSq));
        double px = a[0] + dx * t - (x + 0.5), pz = a[1] + dz * t - (z + 0.5);
        double lateral = Math.sqrt(px * px + pz * pz);
        double r = arch.width * (1.0 + 0.6 * Math.abs(t - 0.5) * 2.0);
        if (lateral >= r) return false;
        double centreY = ARCH_BASE_Y + arch.height * Math.sin(t * Math.PI);
        double vertical = y - centreY;
        if (vertical * vertical + lateral * lateral < r * r) return true;
        // The legs: near either end, rock continues from the tube down to the ground.
        return (t < 0.12 || t > 0.88) && y > groundY && y <= centreY;
    }
}
