package naturalexceptions.minecraft.knowledge.client;

/** Coordinate hit testing shared by the container overlay and its tests. */
public final class InventoryOverlayInput {

    public enum KeyboardOwner {
        SEARCH_FIELD,
        OVERLAY_SUBVIEW,
        CONTAINER,
        GLOBAL
    }

    public enum PageAction {
        NONE,
        PREVIOUS,
        NEXT
    }

    private InventoryOverlayInput() {}

    public static KeyboardOwner keyboardOwner(boolean searchFocused, boolean subviewOpen, boolean containerOpen) {
        if (searchFocused) return KeyboardOwner.SEARCH_FIELD;
        if (subviewOpen) return KeyboardOwner.OVERLAY_SUBVIEW;
        if (containerOpen) return KeyboardOwner.CONTAINER;
        return KeyboardOwner.GLOBAL;
    }

    public static boolean ownsSearchInput(boolean searchFocused) {
        return keyboardOwner(searchFocused, false, true) == KeyboardOwner.SEARCH_FIELD;
    }

    public static boolean mayRunGlobalShortcut(boolean searchFocused, boolean subviewOpen) {
        KeyboardOwner owner = keyboardOwner(searchFocused, subviewOpen, true);
        return owner == KeyboardOwner.CONTAINER || owner == KeyboardOwner.GLOBAL;
    }

    public static int itemIndexAt(int panelX, int panelY, int columns, int rows, int firstIndex, int mouseX,
        int mouseY) {
        int localX = mouseX - (panelX + 5);
        int localY = mouseY - (panelY + 42);
        if (columns <= 0 || rows <= 0 || localX < 0 || localY < 0) return -1;
        int column = localX / 18;
        int row = localY / 18;
        if (column >= columns || row >= rows) return -1;
        return firstIndex + row * columns + column;
    }

    public static PageAction pageActionAt(int panelX, int panelY, int panelWidth, int panelHeight, int mouseX,
        int mouseY) {
        int buttonY = panelY + panelHeight - 17;
        if (mouseY < buttonY || mouseY >= buttonY + 14) return PageAction.NONE;
        if (mouseX >= panelX + 4 && mouseX < panelX + 20) return PageAction.PREVIOUS;
        if (mouseX >= panelX + panelWidth - 20 && mouseX < panelX + panelWidth - 4) return PageAction.NEXT;
        return PageAction.NONE;
    }
}
