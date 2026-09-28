package naturalexceptions.core.fabrication;

import java.util.Objects;

/**
 * Stable identifier for canonical component shapes and geometries.
 */
public enum GeometryId {

    BLANK("blank"),
    INGOT("ingot"),
    PLATE("plate"),
    ROD("rod"),
    HANDLE("handle"),
    PICK_HEAD("pick_head"),
    AXE_HEAD("axe_head"),
    SHOVEL_HEAD("shovel_head"),
    HOE_HEAD("hoe_head"),
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

    public boolean isToolHead() {
        return this == PICK_HEAD || this == AXE_HEAD || this == SHOVEL_HEAD || this == HOE_HEAD || this == BLADE;
    }
}
