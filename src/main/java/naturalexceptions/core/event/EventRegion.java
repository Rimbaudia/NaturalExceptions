package naturalexceptions.core.event;

import java.util.Objects;

/** Stable coarse region target for world-scoped events. */
public final class EventRegion implements Comparable<EventRegion> {

    private static final EventRegion GLOBAL = new EventRegion(true, 0, 0);

    private final boolean global;
    private final int x;
    private final int z;

    private EventRegion(boolean global, int x, int z) {
        this.global = global;
        this.x = x;
        this.z = z;
    }

    public static EventRegion of(int x, int z) {
        return new EventRegion(false, x, z);
    }

    public static EventRegion global() {
        return GLOBAL;
    }

    public boolean isGlobal() {
        return global;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    @Override
    public int compareTo(EventRegion other) {
        if (other == null) throw new NullPointerException("Cannot compare EventRegion to null");
        int result = Boolean.compare(other.global, global);
        if (result != 0) return result;
        result = Integer.compare(x, other.x);
        return result != 0 ? result : Integer.compare(z, other.z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EventRegion)) return false;
        EventRegion that = (EventRegion) other;
        return global == that.global && x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(global, x, z);
    }

    @Override
    public String toString() {
        return global ? "global" : "region[" + x + ',' + z + ']';
    }
}
