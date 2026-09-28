package naturalexceptions.minecraft.metallurgy;

/**
 * What the single canonical casting path (smelter, side-wall outlet, casting table with a mold below the spout)
 * can do right now, in player language.
 */
public enum SmelterOutletState {

    STRUCTURE_INCOMPLETE("waiting on structure", "The smelter walls are incomplete, so nothing is processed or poured."),
    NO_OUTLET("no outlet fitted", "Place a Smelter Outlet against the outside of a wall, level with the controller, spout facing out."),
    NOTHING_MOLTEN("nothing molten yet", "Keep the fire fed; metal can only be poured once it is mostly liquid."),
    NO_TABLE("no table under spout", "Place a Casting Table directly below the outlet spout."),
    NO_MOLD("table has no mold", "Put an empty mold on the Casting Table under the spout."),
    MOLD_FULL("mold is full", "Take the filled mold off the Casting Table (tongs help while it is hot)."),
    POURING("pouring", "Metal is flowing from the spout into the mold."),
    READY("ready to pour", "Right-click the outlet to open the tap and pour into the mold below.");

    private final String shortText;
    private final String hint;

    SmelterOutletState(String shortText, String hint) {
        this.shortText = shortText;
        this.hint = hint;
    }

    /** Compact label for the smelter screen. */
    public String shortText() {
        return shortText;
    }

    /** One sentence explaining what to do about it. */
    public String hint() {
        return hint;
    }

    public boolean canPour() {
        return this == READY;
    }
}
