package naturalexceptions.minecraft.gear;

/** The nineteen places a player can wear gear: one per body part, two gloves and shoes, a ring per finger. */
public enum GearSlot {

    HAT(GearKind.HAT, "Hat"),
    FACE(GearKind.FACE, "Face"),
    NECK(GearKind.NECK, "Neck"),
    BACK(GearKind.BACK, "Back"),
    BELT(GearKind.BELT, "Belt"),
    GLOVE_LEFT(GearKind.GLOVE, "Left glove"),
    GLOVE_RIGHT(GearKind.GLOVE, "Right glove"),
    SHOE_LEFT(GearKind.SHOE, "Left shoe"),
    SHOE_RIGHT(GearKind.SHOE, "Right shoe"),
    RING_LEFT_THUMB(GearKind.RING, "Left thumb ring"),
    RING_LEFT_INDEX(GearKind.RING, "Left index ring"),
    RING_LEFT_MIDDLE(GearKind.RING, "Left middle ring"),
    RING_LEFT_RING(GearKind.RING, "Left ring-finger ring"),
    RING_LEFT_LITTLE(GearKind.RING, "Left little ring"),
    RING_RIGHT_THUMB(GearKind.RING, "Right thumb ring"),
    RING_RIGHT_INDEX(GearKind.RING, "Right index ring"),
    RING_RIGHT_MIDDLE(GearKind.RING, "Right middle ring"),
    RING_RIGHT_RING(GearKind.RING, "Right ring-finger ring"),
    RING_RIGHT_LITTLE(GearKind.RING, "Right little ring");

    public static final int COUNT = values().length;

    private final GearKind kind;
    private final String label;

    GearSlot(GearKind kind, String label) {
        this.kind = kind;
        this.label = label;
    }

    public GearKind kind() {
        return kind;
    }

    public String label() {
        return label;
    }

    public static GearSlot byIndex(int index) {
        GearSlot[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
