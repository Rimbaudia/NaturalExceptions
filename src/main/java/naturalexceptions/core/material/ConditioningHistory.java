package naturalexceptions.core.material;

import java.util.Objects;

/**
 * Materially relevant persistent history or processing conditioning.
 *
 * <p>
 * Distinguishes materials whose persistent physical behavior has been modified
 * by historical mechanical, thermal, or supernatural treatment (e.g. cold-working,
 * work-hardening, radiation exposure).
 */
public enum ConditioningHistory {

    NONE("none"),
    HOT_ROLLED("hot_rolled"),
    COLD_WORKED("cold_worked"),
    AGE_HARDENED("age_hardened"),
    STELLAR_IRRADIATED("stellar_irradiated");

    private final String id;

    ConditioningHistory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ConditioningHistory parse(String str) {
        Objects.requireNonNull(str, "conditioning history string");
        for (ConditioningHistory cond : values()) {
            if (cond.id.equalsIgnoreCase(str.trim())) {
                return cond;
            }
        }
        throw new IllegalArgumentException("Unknown conditioning history: " + str);
    }
}
