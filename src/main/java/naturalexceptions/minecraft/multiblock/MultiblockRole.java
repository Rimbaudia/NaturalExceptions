package naturalexceptions.minecraft.multiblock;

/**
 * Distinguishes required structural elements from optional attachments and related apparatus.
 */
public enum MultiblockRole {

    /** The core/controller block hosting the tile entity. */
    REQUIRED_CONTROLLER("Controller", true),
    /** Structural block essential to forming the multiblock. */
    REQUIRED_BLOCK("Required Structure", true),
    /** Required internal air cavity or chimney flue. */
    REQUIRED_AIR("Required Cavity", false),
    /** Optional attachment block (e.g. Tuyere). */
    OPTIONAL_ATTACHMENT("Optional Attachment", false),
    /** External related apparatus (e.g. Smelter Outlet, Casting Table). */
    RELATED_EXTERNAL("External Apparatus", false);

    private final String displayLabel;
    private final boolean countsAsMaterialItem;

    MultiblockRole(String displayLabel, boolean countsAsMaterialItem) {
        this.displayLabel = displayLabel;
        this.countsAsMaterialItem = countsAsMaterialItem;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public boolean countsAsMaterialItem() {
        return countsAsMaterialItem;
    }

    public boolean isRequired() {
        return this == REQUIRED_CONTROLLER || this == REQUIRED_BLOCK || this == REQUIRED_AIR;
    }
}
