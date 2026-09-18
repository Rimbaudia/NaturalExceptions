package naturalexceptions.core.fabrication;

import java.util.Objects;

/**
 * Stable identifier for canonical component shapes and geometries.
 */
public enum GeometryId {

    BLANK("blank"),
    PLATE("plate"),
    ROD("rod"),
    HANDLE("handle"),
    PICK_HEAD("pick_head"),
    BLADE("blade");

    private final String id;

    GeometryId(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static GeometryId parse(String str) {
        Objects.requireNonNull(str, "geometry id string");
        for (GeometryId g : values()) {
            if (g.id.equalsIgnoreCase(str.trim())) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown GeometryId: " + str);
    }
}
