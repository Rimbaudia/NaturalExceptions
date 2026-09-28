package naturalexceptions.minecraft.multiblock;

/**
 * Indicates development stability of a multiblock structure layout.
 */
public enum MultiblockMaturity {

    EXPERIMENTAL("Experimental Structure", "Experimental structure — layout may change during development."),
    STABLE("Stable Structure", "Standard established multiblock structure.");

    private final String label;
    private final String notice;

    MultiblockMaturity(String label, String notice) {
        this.label = label;
        this.notice = notice;
    }

    public String label() {
        return label;
    }

    public String notice() {
        return notice;
    }

    public boolean isExperimental() {
        return this == EXPERIMENTAL;
    }
}
