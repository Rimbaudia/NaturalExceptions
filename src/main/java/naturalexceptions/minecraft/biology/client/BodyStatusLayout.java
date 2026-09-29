package naturalexceptions.minecraft.biology.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Screen placement for the Body panel beside the inventory or Gear screen, and the figure inside it. Pure geometry
 * so it can be tested without a client. The figure's pieces are drawn from {@code gui/body_panel.png}.
 */
public final class BodyStatusLayout {

    public static final int PANEL_WIDTH = 140;
    static final int MIN_PANEL_WIDTH = 112;
    static final int MARGIN = 5;
    /** Shortest panel: header, figure and a few list lines. */
    static final int MIN_PANEL_HEIGHT = 150;
    /** Figure is drawn on a 16 x 32 unit grid (a Minecraft skin's proportions), {@link #DOLL_SCALE} px per unit. */
    public static final int DOLL_SCALE = 2;
    public static final int DOLL_WIDTH = 16 * DOLL_SCALE;
    public static final int DOLL_HEIGHT = 32 * DOLL_SCALE;
    /** Height of the header (status word, condition bar) above the figure. */
    public static final int HEADER_HEIGHT = 24;

    /** Region path to {x, y, w, h} in doll units. Seen from the front, so the right arm is on the viewer's left. */
    private static final Map<String, int[]> DOLL = new LinkedHashMap<>();

    static {
        DOLL.put("head", new int[] { 4, 0, 8, 8 });
        DOLL.put("torso", new int[] { 4, 8, 8, 12 });
        DOLL.put("right_arm", new int[] { 0, 8, 4, 12 });
        DOLL.put("left_arm", new int[] { 12, 8, 4, 12 });
        DOLL.put("right_leg", new int[] { 4, 20, 4, 12 });
        DOLL.put("left_leg", new int[] { 8, 20, 4, 12 });
    }

    /** Vitals rows beside the figure: blood, energy, water, moving, hands. */
    public static final int VITAL_ROWS = 5;
    public static final int VITAL_ROW_HEIGHT = 13;

    private final int panelX;
    private final int panelY;
    private final int panelWidth;
    private final int panelHeight;
    private final boolean overlaysScreenEdge;

    private BodyStatusLayout(int panelX, int panelY, int panelWidth, int panelHeight, boolean overlaysScreenEdge) {
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.overlaysScreenEdge = overlaysScreenEdge;
    }

    /** Beside the screen at {@code guiLeft, guiTop}, as tall as the screen ({@code guiHeight}). */
    public static BodyStatusLayout calculate(int screenWidth, int screenHeight, int guiLeft, int guiTop,
        int guiHeight) {
        int room = guiLeft - MARGIN - 2;
        int width;
        int x;
        boolean overlay = false;
        if (room >= PANEL_WIDTH) {
            width = PANEL_WIDTH;
            x = guiLeft - MARGIN - width;
        } else if (room >= MIN_PANEL_WIDTH) {
            width = room;
            x = 2;
        } else {
            width = Math.min(PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, screenWidth - 4));
            x = 2;
            overlay = true;
        }
        int height = Math.max(MIN_PANEL_HEIGHT, guiHeight);
        int y = Math.max(2, Math.min(guiTop, screenHeight - height - 2));
        return new BodyStatusLayout(x, y, width, height, overlay);
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

    /** True when there was no room beside the inventory and the panel covers part of it. */
    public boolean overlaysScreenEdge() {
        return overlaysScreenEdge;
    }

    /** Width left of the inventory that the item browser must not use while the panel is open. */
    public int reservedLeftWidth(int guiLeft) {
        return overlaysScreenEdge ? 0 : Math.max(0, guiLeft - panelX);
    }

    public int dollX() {
        return panelX + 8;
    }

    public int dollY() {
        return panelY + HEADER_HEIGHT;
    }

    /** {x, y, width} of a vitals row (icon, then bar) beside the figure. */
    public int[] vitalRow(int index) {
        int x = dollX() + DOLL_WIDTH + 8;
        return new int[] { x, dollY() + index * VITAL_ROW_HEIGHT, panelX + panelWidth - 8 - x };
    }

    /** Top of the injury/exposure list. */
    public int listTop() {
        return dollY() + DOLL_HEIGHT + 7;
    }

    public int listBottom() {
        return panelY + panelHeight - 6;
    }

    public boolean panelContains(int x, int y) {
        return x >= panelX && x < panelX + panelWidth && y >= panelY && y < panelY + panelHeight;
    }

    /** Region paths the figure draws, in drawing order. */
    public static Iterable<String> dollRegions() {
        return DOLL.keySet();
    }

    /** Cell of a region's piece in {@code gui/body_panel.png} (32 px wide cells along the top), or -1. */
    public static int pieceIndex(String regionPath) {
        int i = 0;
        for (String region : DOLL.keySet()) {
            if (region.equals(regionPath)) return i;
            i++;
        }
        return -1;
    }

    /** Screen rectangle {x, y, w, h} of a region's block in the figure, or null if the figure has no such part. */
    public int[] regionRect(String regionPath) {
        int[] unit = DOLL.get(regionPath);
        if (unit == null) return null;
        return new int[] { dollX() + unit[0] * DOLL_SCALE, dollY() + unit[1] * DOLL_SCALE, unit[2] * DOLL_SCALE,
            unit[3] * DOLL_SCALE };
    }

    /** The region path under a screen point, or null. */
    public String regionAt(int x, int y) {
        for (String region : DOLL.keySet()) {
            int[] r = regionRect(region);
            if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) return region;
        }
        return null;
    }

    /** Sound green through amber to red as a region's harm goes from 0 to 1. Opaque ARGB. */
    public static int harmColour(float harm) {
        float h = Math.max(0.0F, Math.min(1.0F, harm));
        if (h < 0.02F) return 0xFF6FA35A;
        int from;
        int to;
        float t;
        if (h < 0.5F) {
            from = 0x9BB24A;
            to = 0xE0A030;
            t = h / 0.5F;
        } else {
            from = 0xE0A030;
            to = 0xB8322A;
            t = (h - 0.5F) / 0.5F;
        }
        return 0xFF000000 | lerp(from, to, t);
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
