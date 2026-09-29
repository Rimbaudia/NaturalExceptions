package naturalexceptions.minecraft.gear.client;

/**
 * The tabs along the top of the inventory and Gear screens. Inventory and Gear switch screens and sit on the left;
 * Items and Body switch side panels on and off and sit on the right. Geometry follows vanilla's creative tabs.
 */
public enum InventoryTab {

    INVENTORY("Inventory", 0, false),
    GEAR("Gear", 1, false),
    BODY("Body", 4, true),
    ITEMS("Items", 5, true);

    public static final int WIDTH = 28;
    public static final int HEIGHT = 32;
    /** How far a tab rises above the screen's top edge. */
    public static final int RISE = 28;

    private final String label;
    /** Column in vanilla's {@code creative_inventory/tabs.png} whose art this tab uses. */
    private final int column;
    private final boolean toggle;

    InventoryTab(String label, int column, boolean toggle) {
        this.label = label;
        this.column = column;
        this.toggle = toggle;
    }

    public String label() {
        return label;
    }

    public int column() {
        return column;
    }

    /** True for side-panel switches (Items, Body); false for screen tabs (Inventory, Gear). */
    public boolean isToggle() {
        return toggle;
    }

    /** Screen x of the tab: left tabs from the left edge, the last column flush with the right edge. */
    public int x(int guiLeft, int guiWidth) {
        if (column == 5) return guiLeft + guiWidth - WIDTH;
        return guiLeft + WIDTH * column + column;
    }

    public int y(int guiTop) {
        return guiTop - RISE;
    }

    /** The tab under a point, or null. Only the part above the screen's edge counts. */
    public static InventoryTab at(int mouseX, int mouseY, int guiLeft, int guiTop, int guiWidth) {
        for (InventoryTab tab : values()) {
            int x = tab.x(guiLeft, guiWidth);
            int y = tab.y(guiTop);
            if (mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < guiTop) return tab;
        }
        return null;
    }
}
