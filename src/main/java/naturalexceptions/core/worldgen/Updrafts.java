package naturalexceptions.core.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rising air beside the sky islands: under some of them (every great one, a third of the rest) a column of warm air
 * climbs from the ground up past the island's rim, just off its rock (its cloud rim leaves a gap there), and a little
 * over its top. Whoever stands in it is
 * carried slowly up (fast, with a Glider Cloak open), to step off onto the island at the top. Worked out from the
 * island alone, so it is the same every time.
 */
public final class Updrafts {

    /** How wide the column is (blocks from its middle). */
    public static final double RADIUS = 1.6;
    /** It rises from here (lower, it is the ground's business) to this far over the island's top. */
    public static final int BASE_Y = 40, OVER_TOP = 4;
    /** How far out from the island's rock (and its cloud) the column stands. */
    static final double CLEAR = 2.5;

    /** A column: its middle, and how high it reaches. */
    public static final class Column {

        public final double x, z;
        public final int top;

        Column(double x, double z, int top) {
            this.x = x;
            this.z = z;
            this.top = top;
        }

        /** Whether (x, y, z) is in it. */
        public boolean holds(double px, double py, double pz) {
            double dx = px - x, dz = pz - z;
            return dx * dx + dz * dz < RADIUS * RADIUS && py >= BASE_Y && py < top;
        }
    }

    private final SkyIslands field;
    private final Map<String, Column> known = new LinkedHashMap<String, Column>(64, 0.75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Column> eldest) {
            return size() > 256;
        }
    };
    private static final Column NONE = new Column(0, 0, Integer.MIN_VALUE);

    public Updrafts(SkyIslands field) {
        this.field = field;
    }

    public SkyIslands field() {
        return field;
    }

    /** The island's updraft, or null if it has none. */
    public synchronized Column of(SkyIslands.Island island) {
        String key = island.tier() + ":" + island.cellX() + ":" + island.cellZ();
        Column column = known.get(key);
        if (column == null) {
            column = work(island);
            known.put(key, column == null ? NONE : column);
        }
        return column == NONE ? null : column;
    }

    private Column work(SkyIslands.Island island) {
        double bearing = field.updraftBearing(island);
        if (Double.isNaN(bearing)) return null;
        double cos = Math.cos(bearing), sin = Math.sin(bearing);
        // Out from the middle along the bearing until past the rock (the cloud rim leaves a gap here for it), then
        // out until the column's whole width is clear, the edge being ragged from one bearing to the next.
        double far = island.radius() * 1.6 + 12.0, edge = far;
        for (double d = island.radius() * 0.5; d < far; d += 1.0) {
            int x = (int) Math.floor(island.centreX() + cos * d), z = (int) Math.floor(island.centreZ() + sin * d);
            if (field.column(island, x, z) == null) {
                edge = d;
                break;
            }
        }
        double d = edge + CLEAR;
        while (!clear(island, island.centreX() + 0.5 + cos * d, island.centreZ() + 0.5 + sin * d) && d < far + 8.0)
            d += 1.0;
        return new Column(
            island.centreX() + 0.5 + cos * d,
            island.centreZ() + 0.5 + sin * d,
            island.centreY() + 1 + OVER_TOP);
    }

    /** Whether a column at (x, z) is clear of the island's rock and cloud across its whole width. */
    private boolean clear(SkyIslands.Island island, double x, double z) {
        for (double a = 0; a < Math.PI * 2; a += Math.PI / 8) for (double r : new double[] { 0.0, RADIUS + 0.5 }) {
            int cx = (int) Math.floor(x + Math.cos(a) * r), cz = (int) Math.floor(z + Math.sin(a) * r);
            if (field.column(island, cx, cz) != null || field.cloud(island, cx, cz) != null) return false;
        }
        return true;
    }

    /** The column (x, y, z) is in, or null. */
    public Column at(double x, double y, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        for (SkyIslands.Island island : field.near(ix - 8, iz - 8, ix + 8, iz + 8)) {
            Column column = of(island);
            if (column != null && column.holds(x, y, z)) return column;
        }
        return null;
    }
}
