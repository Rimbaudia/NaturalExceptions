package naturalexceptions.minecraft.gear;

/** What part of the body a worn item sits on. Items declare one; gear slots accept one. */
public enum GearKind {

    HAT("Hat"),
    FACE("Face"),
    NECK("Neck"),
    BACK("Back"),
    BELT("Belt"),
    GLOVE("Glove"),
    SHOE("Shoe"),
    RING("Ring");

    private final String label;

    GearKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Icon name of the grey silhouette shown in an empty slot of this kind. */
    public String slotIcon() {
        return "naturalexceptions:gear_slot_" + name().toLowerCase(java.util.Locale.ROOT);
    }
}
