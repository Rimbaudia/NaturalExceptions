package naturalexceptions.core.material;

import java.util.Objects;

/**
 * Materially relevant microstructure classification.
 *
 * <p>
 * Distinguishes grain, crystalline, or defect structures where internal physical
 * arrangement alters persistent physical properties.
 */
public enum Microstructure {

    AS_CAST("as_cast"),
    ANNEALED("annealed"),
    WORK_HARDENED("work_hardened"),
    QUENCHED("quenched"),
    TEMPERED("tempered"),
    METASTABLE_CRYSTALLINE("metastable_crystalline");

    private final String id;

    Microstructure(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Microstructure parse(String str) {
        Objects.requireNonNull(str, "microstructure string");
        for (Microstructure ms : values()) {
            if (ms.id.equalsIgnoreCase(str.trim())) {
                return ms;
            }
        }
        throw new IllegalArgumentException("Unknown microstructure: " + str);
    }
}
