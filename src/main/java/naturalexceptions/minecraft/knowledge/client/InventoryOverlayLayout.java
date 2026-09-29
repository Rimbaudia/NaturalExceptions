package naturalexceptions.minecraft.knowledge.client;

/** Screen-space placement for the inventory-side item browser. */
public final class InventoryOverlayLayout {

    public enum Side {
        LEFT,
        RIGHT,
        COMPACT
    }

    public static final class Layout {

        private final Side side;
        private final int panelX;
        private final int panelY;
        private final int panelWidth;
        private final int panelHeight;
        private final int columns;
        private final int rows;
        private final int toggleX;
        private final int toggleWidth;
        private final boolean panelVisible;

        private Layout(Side side, int panelX, int panelY, int panelWidth, int panelHeight, int columns, int rows,
            int toggleX, int toggleWidth, boolean panelVisible) {
            this.side = side;
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
            this.columns = columns;
            this.rows = rows;
            this.toggleX = toggleX;
            this.toggleWidth = toggleWidth;
            this.panelVisible = panelVisible;
        }

        public Side side() {
            return side;
        }

        public int panelX() {
            return panelX;
        }

        public int panelY() {
            return panelY;
        }

        public int panelWidth() {
            return panelWidth;
        }

        public int panelHeight() {
            return panelHeight;
        }

        public int columns() {
            return columns;
        }

        public int rows() {
            return rows;
        }

        public int toggleX() {
            return toggleX;
        }

        public int toggleWidth() {
            return toggleWidth;
        }

        public boolean hasToggle() {
            return true;
        }

        public boolean isPanelVisible() {
            return panelVisible;
        }
    }

    /** Enough room for the compact crafting recipe row, search field, and panel borders. */
    private static final int MINIMUM_WIDTH = 120;
    private static final int MAXIMUM_COLUMNS = 7;
    private static final int MARGIN = 5;
    private static final int TITLE_HEIGHT = 40;
    private static final int CELL_SIZE = 18;

    /** Width another panel (the Body panel) currently occupies left of the GUI; the browser stays out of it. */
    private static volatile int reservedLeftWidth;

    private InventoryOverlayLayout() {}

    public static void reserveLeft(int width) {
        reservedLeftWidth = Math.max(0, width);
    }

    public static Layout calculate(int screenWidth, int screenHeight, int guiLeft, int guiTop, int guiWidth,
        int guiHeight) {
        return calculate(screenWidth, screenHeight, guiLeft, guiTop, guiWidth, guiHeight, reservedLeftWidth);
    }

    public static Layout calculate(int screenWidth, int screenHeight, int guiLeft, int guiTop, int guiWidth,
        int guiHeight, int reservedLeft) {
        int rightSpace = Math.max(0, screenWidth - (guiLeft + guiWidth) - MARGIN);
        int leftSpace = Math.max(0, guiLeft - MARGIN - Math.max(0, reservedLeft));

        Side side = Side.COMPACT;
        int available = 0;
        if (rightSpace >= MINIMUM_WIDTH) {
            side = Side.RIGHT;
            available = rightSpace;
        } else if (leftSpace >= MINIMUM_WIDTH) {
            side = Side.LEFT;
            available = leftSpace;
        }

        int toggleWidth = Math.min(54, Math.max(36, screenWidth - 4));
        if (side == Side.COMPACT) {
            int toggleX = clamp(guiLeft + guiWidth - toggleWidth, 2, Math.max(2, screenWidth - toggleWidth - 2));
            return new Layout(side, 0, 0, 0, 0, 0, 0, toggleX, toggleWidth, false);
        }

        int width = available;
        int x = side == Side.RIGHT ? guiLeft + guiWidth + MARGIN : guiLeft - MARGIN - Math.max(0, reservedLeft) - width;
        int panelHeight = screenHeight;
        int y = 0;
        int columns = Math.min(MAXIMUM_COLUMNS, Math.max(3, (width - 10) / CELL_SIZE));
        int rows = Math.max(1, (panelHeight - TITLE_HEIGHT - 20) / CELL_SIZE);
        return new Layout(side, x, y, width, panelHeight, columns, rows, x, toggleWidth, true);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
