package naturalexceptions.minecraft.gear;

/**
 * Where everything sits on the Gear screen (texture {@code gui/gear.png}). Slot positions are the 16x16 item origin,
 * as for {@link net.minecraft.inventory.Slot}; the drawn frame is one pixel larger on each side.
 *
 * <p>
 * Like vanilla's armour column beside the player, the body slots (hat, face, neck, back, belt) stand against the
 * left of the figure window and gloves and shoes against its right; each hand's five rings form an outer column on
 * its own side.
 */
public final class GearLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 200;

    /** Framed window in which the player's figure is drawn: x, y, width, height. */
    public static final int[] FIGURE = { 62, 18, 52, 88 };

    public static final int PLAYER_INVENTORY_Y = 118;
    public static final int HOTBAR_Y = 176;

    private static final int LEFT_RINGS_X = 21;
    private static final int BODY_X = 43;
    private static final int HANDS_FEET_X = 117;
    private static final int RIGHT_RINGS_X = 139;
    private static final int TOP_Y = 18;

    private GearLayout() {}

    /** Item origin {x, y} of a gear slot. */
    public static int[] slot(GearSlot slot) {
        switch (slot) {
            case HAT:
                return new int[] { BODY_X, TOP_Y };
            case FACE:
                return new int[] { BODY_X, TOP_Y + 18 };
            case NECK:
                return new int[] { BODY_X, TOP_Y + 36 };
            case BACK:
                return new int[] { BODY_X, TOP_Y + 54 };
            case BELT:
                return new int[] { BODY_X, TOP_Y + 72 };
            case GLOVE_LEFT:
                return new int[] { HANDS_FEET_X, TOP_Y };
            case GLOVE_RIGHT:
                return new int[] { HANDS_FEET_X, TOP_Y + 18 };
            case SHOE_LEFT:
                return new int[] { HANDS_FEET_X, TOP_Y + 54 };
            case SHOE_RIGHT:
                return new int[] { HANDS_FEET_X, TOP_Y + 72 };
            default:
                int ring = slot.ordinal() - GearSlot.RING_LEFT_THUMB.ordinal();
                boolean right = ring >= 5;
                return new int[] { right ? RIGHT_RINGS_X : LEFT_RINGS_X, TOP_Y + (ring % 5) * 18 };
        }
    }
}
