package naturalexceptions.minecraft.gear;

/**
 * Where everything sits on the Gear screen (texture {@code gui/gear.png}). Slot positions are the 16x16 item origin,
 * as for {@link net.minecraft.inventory.Slot}; the drawn frame is one pixel larger on each side.
 */
public final class GearLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 232;

    /** Framed window in which the player's figure is drawn: x, y, width, height. */
    public static final int[] FIGURE = { 52, 18, 72, 88 };

    public static final int PLAYER_INVENTORY_Y = 150;
    public static final int HOTBAR_Y = 208;

    private static final int LEFT_X = 8;
    private static final int RIGHT_X = 152;
    private static final int RINGS_X = 43;
    private static final int RINGS_LEFT_Y = 112;
    private static final int RINGS_RIGHT_Y = 130;

    private GearLayout() {}

    /** Item origin {x, y} of a gear slot. */
    public static int[] slot(GearSlot slot) {
        switch (slot) {
            case HAT:
                return new int[] { LEFT_X, 18 };
            case FACE:
                return new int[] { LEFT_X, 36 };
            case NECK:
                return new int[] { LEFT_X, 54 };
            case BACK:
                return new int[] { LEFT_X, 72 };
            case BELT:
                return new int[] { LEFT_X, 90 };
            case GLOVE_LEFT:
                return new int[] { RIGHT_X, 18 };
            case GLOVE_RIGHT:
                return new int[] { RIGHT_X, 36 };
            case SHOE_LEFT:
                return new int[] { RIGHT_X, 72 };
            case SHOE_RIGHT:
                return new int[] { RIGHT_X, 90 };
            default:
                int ring = slot.ordinal() - GearSlot.RING_LEFT_THUMB.ordinal();
                boolean right = ring >= 5;
                return new int[] { RINGS_X + (ring % 5) * 18, right ? RINGS_RIGHT_Y : RINGS_LEFT_Y };
        }
    }

    /** Text labels drawn on the screen: {x, y} for "Left"/"Right" beside the ring rows. */
    public static int[] ringLabel(boolean right) {
        return new int[] { LEFT_X, (right ? RINGS_RIGHT_Y : RINGS_LEFT_Y) + 4 };
    }

    /** Small "L"/"R" markers left of the glove and shoe slots. */
    public static int[] sideLabel(GearSlot slot) {
        int[] at = slot(slot);
        return new int[] { at[0] - 8, at[1] + 4 };
    }
}
