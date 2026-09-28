package naturalexceptions.core.persistence;

import java.util.Objects;

public final class WorldgenEpoch implements Comparable<WorldgenEpoch> {

    public static final WorldgenEpoch INITIAL = new WorldgenEpoch(1);
    /** Generation contract introduced by the staged P6 Overworld pipeline. */
    public static final WorldgenEpoch CURRENT = new WorldgenEpoch(2);

    private final int value;

    public WorldgenEpoch(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Worldgen epoch must be positive: " + value);
        }
        this.value = value;
    }

    public static WorldgenEpoch of(int value) {
        if (value == 1) return INITIAL;
        if (value == 2) return CURRENT;
        return new WorldgenEpoch(value);
    }

    public int value() {
        return value;
    }

    @Override
    public int compareTo(WorldgenEpoch other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare WorldgenEpoch to null");
        }
        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldgenEpoch)) return false;
        WorldgenEpoch that = (WorldgenEpoch) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "WorldgenEpoch(" + value + ")";
    }
}
