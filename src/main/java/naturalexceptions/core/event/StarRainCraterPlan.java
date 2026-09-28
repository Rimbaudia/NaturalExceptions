package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic shape of a Fallen Star impact, relative to the struck surface point (dy = 0 is the old surface
 * block). A severity-scaled bowl is cleared, its floor scorched, ejecta thrown onto a rim, and a blocky ball of fused
 * crust around a small stellar core sits half-buried at the bottom. Pure geometry: no world types.
 */
public final class StarRainCraterPlan {

    public enum Role {
        /** Cleared by the impact. */
        CLEAR,
        /** Scorched floor directly under the bowl. */
        LINING,
        /** Fused-rock shell of the ball. */
        CRUST,
        /** Stellar matter at the heart of the ball. */
        CORE
    }

    public static final class Cell {

        private final int dx;
        private final int dy;
        private final int dz;
        private final Role role;
        private final int variant;

        Cell(int dx, int dy, int dz, Role role, int variant) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.role = role;
            this.variant = variant;
        }

        public int dx() {
            return dx;
        }

        public int dy() {
            return dy;
        }

        public int dz() {
            return dz;
        }

        public Role role() {
            return role;
        }

        /** Deterministic material choice for LINING (0..3) and rim ejecta (0..2); 0 otherwise. */
        public int variant() {
            return variant;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Cell)) return false;
            Cell that = (Cell) other;
            return dx == that.dx && dy == that.dy && dz == that.dz && role == that.role && variant == that.variant;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dx, dy, dz, role, variant);
        }
    }

    /** A column just outside the bowl that receives one ejecta block on top of its own local surface. */
    public static final class RimColumn {

        private final int dx;
        private final int dz;
        private final int variant;

        RimColumn(int dx, int dz, int variant) {
            this.dx = dx;
            this.dz = dz;
            this.variant = variant;
        }

        public int dx() {
            return dx;
        }

        public int dz() {
            return dz;
        }

        public int variant() {
            return variant;
        }
    }

    /** Height cleared above the old surface inside the bowl (trees, tall terrain). */
    public static final int CLEAR_ABOVE = 8;

    private final int severity;
    private final int radius;
    private final int depth;
    private final int ballCenterDy;
    private final List<Cell> cells;
    private final List<RimColumn> rim;

    private StarRainCraterPlan(int severity, int radius, int depth, int ballCenterDy, List<Cell> cells,
        List<RimColumn> rim) {
        this.severity = severity;
        this.radius = radius;
        this.depth = depth;
        this.ballCenterDy = ballCenterDy;
        this.cells = Collections.unmodifiableList(cells);
        this.rim = Collections.unmodifiableList(rim);
    }

    public static int radiusFor(int severity) {
        return 2 + 2 * severity;
    }

    public static int depthFor(int severity) {
        return 2 + severity;
    }

    /** Ball radius in blocks: 1.5, 2.3, 3.0 (3, 5, 7 blocks across on the block grid). */
    public static double ballRadiusFor(int severity) {
        return severity == 1 ? 1.5 : severity == 2 ? 2.3 : 3.0;
    }

    /** Squared distance from the ball centre within which cells are stellar core rather than crust. */
    static int coreRadiusSquared(int severity) {
        return severity >= 3 ? 1 : 0;
    }

    public static StarRainCraterPlan of(int severity, long seed) {
        if (severity < 1 || severity > 3) throw new IllegalArgumentException("Severity must be 1..3");
        int radius = radiusFor(severity);
        int depth = depthFor(severity);
        double ballRadius = ballRadiusFor(severity);
        int ballCenterDy = -depth + 1;
        int reach = radius + 2;

        List<Cell> cells = new ArrayList<>();
        List<RimColumn> rim = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                double edge = radius + rimNoise(seed, dx, dz);
                if (r <= edge) {
                    double t = r / edge;
                    int floor = -(int) Math.floor(depth * Math.sqrt(Math.max(0.0, 1.0 - t * t)));
                    for (int dy = floor; dy <= CLEAR_ABOVE; dy++) {
                        if (!inBall(dx, dy - ballCenterDy, dz, ballRadius)) cells.add(new Cell(dx, dy, dz, Role.CLEAR, 0));
                    }
                    int liningDy = floor - 1;
                    if (!inBall(dx, liningDy - ballCenterDy, dz, ballRadius)) {
                        cells.add(new Cell(dx, liningDy, dz, Role.LINING, liningVariant(seed, dx, dz, severity)));
                    }
                } else if (r <= edge + 1.5) {
                    rim.add(new RimColumn(dx, dz, (int) Long.remainderUnsigned(mix(seed ^ key(dx, dz) ^ 0x52494dL), 3L)));
                }
            }
        }
        int ceil = (int) Math.ceil(ballRadius);
        int core = coreRadiusSquared(severity);
        for (int bx = -ceil; bx <= ceil; bx++) {
            for (int by = -ceil; by <= ceil; by++) {
                for (int bz = -ceil; bz <= ceil; bz++) {
                    if (!inBall(bx, by, bz, ballRadius)) continue;
                    int d2 = bx * bx + by * by + bz * bz;
                    cells.add(new Cell(bx, ballCenterDy + by, bz, d2 <= core ? Role.CORE : Role.CRUST, 0));
                }
            }
        }
        return new StarRainCraterPlan(severity, radius, depth, ballCenterDy, cells, rim);
    }

    private static boolean inBall(int bx, int by, int bz, double ballRadius) {
        return bx * bx + by * by + bz * bz <= ballRadius * ballRadius + 1.0e-9;
    }

    /** Rim irregularity of -1, 0 or +1 block, smooth enough to look like a ragged circle. */
    private static int rimNoise(long seed, int dx, int dz) {
        double angle = Math.atan2(dz, dx);
        int sector = (int) Math.floor((angle + Math.PI) / (Math.PI / 4.0)) & 7;
        return (int) Long.remainderUnsigned(mix(seed ^ (sector * 0x9e3779b97f4a7c15L)), 3L) - 1;
    }

    private static int liningVariant(long seed, int dx, int dz, int severity) {
        long roll = Long.remainderUnsigned(mix(seed ^ key(dx, dz) ^ 0x4c494eL), 100L);
        if (severity >= 3 && roll < 6) return 3; // glassy obsidian where it was hottest
        if (roll < 40) return 0; // scorched coarse earth
        if (roll < 70) return 1; // shattered gravel
        return 2; // cracked cobblestone
    }

    private static long key(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xffffffffL);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public int severity() {
        return severity;
    }

    public int radius() {
        return radius;
    }

    public int depth() {
        return depth;
    }

    public int ballCenterDy() {
        return ballCenterDy;
    }

    public List<Cell> cells() {
        return cells;
    }

    public List<RimColumn> rim() {
        return rim;
    }

    public List<Cell> cellsWithRole(Role role) {
        List<Cell> result = new ArrayList<>();
        for (Cell cell : cells) if (cell.role() == role) result.add(cell);
        return result;
    }

    /** Splits a mass into {@code parts} milligram amounts that sum exactly to {@code totalMg}. */
    public static long[] splitMilligrams(long totalMg, int parts) {
        if (parts <= 0 || totalMg < 0) throw new IllegalArgumentException("Invalid split");
        long[] result = new long[parts];
        long each = totalMg / parts;
        for (int i = 0; i < parts; i++) result[i] = each;
        result[parts - 1] += totalMg - each * parts;
        return result;
    }

    /** Deterministic plan seed from an impact identity. */
    public static long seedOf(naturalexceptions.core.id.DeterministicId id) {
        return Long.parseUnsignedLong(
            id.hex()
                .substring(0, 16),
            16);
    }

    /** Horizontal reach of everything this impact touches, for chunk-loaded checks. */
    public int horizontalReach() {
        return radius + 3;
    }
}
