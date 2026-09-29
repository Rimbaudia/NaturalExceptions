package naturalexceptions.minecraft.fabrication;

/** Shared slot and button coordinates for the Mold Assembly Table container and GUI. */
public final class MoldAssemblyLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 186;
    public static final int INPUT_X = 35;
    public static final int OUTPUT_X = 125;
    public static final int MACHINE_Y = 31;
    public static final int PLAYER_Y = 104;
    public static final int HOTBAR_Y = 162;
    /** Shape tiles: one row, drawn from the texture's sprite column (x >= 176). */
    public static final int TILE_X = 25;
    public static final int TILE_Y = 56;
    public static final int TILE_SIZE = 22;
    public static final int TILE_STEP = 26;
    public static final int ARROW_X = 77;
    public static final int ARROW_Y = 32;
    public static final int INSTRUCTION_Y = 83;

    /** Screen-relative {x, y} of shape tile {@code index}. */
    public static int[] tile(int index) {
        return new int[] { TILE_X + index * TILE_STEP, TILE_Y };
    }

    private MoldAssemblyLayout() {}
}
