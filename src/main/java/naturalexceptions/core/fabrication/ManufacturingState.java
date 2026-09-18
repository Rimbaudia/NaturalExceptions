package naturalexceptions.core.fabrication;

import java.util.Objects;

/**
 * Coarse manufacturing history state of an individual physical component.
 */
public enum ManufacturingState {

    AS_CAST("as_cast"),
    WORKED("worked"),
    FINISHED("finished"),
    COLD_WORKED("cold_worked"),
    ANNEALED("annealed");

    private final String id;

    ManufacturingState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ManufacturingState parse(String str) {
        Objects.requireNonNull(str, "manufacturing state string");
        for (ManufacturingState s : values()) {
            if (s.id.equalsIgnoreCase(str.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ManufacturingState: " + str);
    }
}
