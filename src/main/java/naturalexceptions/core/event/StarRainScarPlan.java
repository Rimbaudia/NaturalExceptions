package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Bounded deterministic horizontal surface footprint for a persisted Star Rain severity. */
public final class StarRainScarPlan {
    private StarRainScarPlan() { }

    public static List<Offset> offsets(int severity) {
        if (severity < 1 || severity > 3) throw new IllegalArgumentException("Severity must be 1..3");
        List<Offset> result = new ArrayList<>(severity == 1 ? 1 : severity == 2 ? 2 : 5);
        result.add(new Offset(0, 0));
        if (severity >= 2) result.add(new Offset(-1, 0));
        if (severity >= 3) {
            result.add(new Offset(1, 0));
            result.add(new Offset(0, -1));
            result.add(new Offset(0, 1));
        }
        return Collections.unmodifiableList(result);
    }

    public static final class Offset {
        private final int x;
        private final int z;
        public Offset(int x, int z) { this.x = x; this.z = z; }
        public int x() { return x; }
        public int z() { return z; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Offset)) return false;
            Offset that = (Offset) other;
            return x == that.x && z == that.z;
        }
        @Override public int hashCode() { return Objects.hash(x, z); }
    }
}
