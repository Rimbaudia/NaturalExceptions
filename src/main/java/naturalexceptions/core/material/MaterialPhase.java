package naturalexceptions.core.material;

import java.util.Objects;

/**
 * Materially relevant physical phase identifier.
 *
 * <p>
 * Distinguishes physical phases where state of matter or crystalline phase
 * alters the canonical physical identity and properties of a material.
 */
public enum MaterialPhase {

    SOLID("solid"),
    LIQUID("liquid"),
    GAS("gas"),
    AMORPHOUS("amorphous"),
    ALPHA("alpha"),
    BETA("beta");

    private final String id;

    MaterialPhase(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static MaterialPhase parse(String str) {
        Objects.requireNonNull(str, "phase string");
        for (MaterialPhase phase : values()) {
            if (phase.id.equalsIgnoreCase(str.trim())) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown material phase: " + str);
    }
}
