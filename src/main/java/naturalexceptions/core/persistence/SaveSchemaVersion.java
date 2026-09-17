package naturalexceptions.core.persistence;

import java.util.Objects;

public final class SaveSchemaVersion implements Comparable<SaveSchemaVersion> {

    public static final SaveSchemaVersion CURRENT = new SaveSchemaVersion(1);

    private final int value;

    public SaveSchemaVersion(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Save schema version must be positive: " + value);
        }
        this.value = value;
    }

    public static SaveSchemaVersion of(int value) {
        if (value == 1) return CURRENT;
        return new SaveSchemaVersion(value);
    }

    public int value() {
        return value;
    }

    public boolean isCompatibleWith(SaveSchemaVersion other) {
        return other != null && this.value == other.value;
    }

    @Override
    public int compareTo(SaveSchemaVersion other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare SaveSchemaVersion to null");
        }
        return Integer.compare(this.value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaveSchemaVersion)) return false;
        SaveSchemaVersion that = (SaveSchemaVersion) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "SaveSchemaVersion(" + value + ")";
    }
}
