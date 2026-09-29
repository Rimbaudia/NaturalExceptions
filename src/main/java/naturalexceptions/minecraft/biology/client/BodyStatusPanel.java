package naturalexceptions.minecraft.biology.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.biology.BodyStatusSnapshot;
import naturalexceptions.minecraft.knowledge.client.InventoryOverlayLayout;
import naturalexceptions.minecraft.network.PacketBodyStatus;

/**
 * The Body panel: a "Body" tab above the player inventory opens a panel on its left showing each body region on a
 * figure, open injuries, poisoning and other exposure, reserves and how well the body moves and handles things.
 */
@SideOnly(Side.CLIENT)
public final class BodyStatusPanel {

    private static final int BUTTON_ID = 0x4E45B0;
    private static final int PANEL_BACKGROUND = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF373737;
    private static final int GUI_TEXT = 0xFF404040;
    private static final int MUTED_TEXT = 0xFF6A6A6A;
    private static final int WARN_TEXT = 0xFF9A2A1E;
    private static final int BAR_BACK = 0xFF8B8B8B;
    private static final int LINE_HEIGHT = 10;

    private static final BodyStatusPanel INSTANCE = new BodyStatusPanel();
    private static final Field GUI_LEFT = field("guiLeft", "field_147003_i");
    private static final Field GUI_TOP = field("guiTop", "field_147009_r");
    private static final Method DRAW_TOOLTIP = tooltipMethod();

    /** Open/closed survives closing and reopening the inventory for the rest of the session. */
    private boolean open;
    private int scroll;
    /** The tab on the current inventory screen. */
    private BodyTab tab;

    private BodyStatusPanel() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    /** The tab above the inventory that opens and closes the panel. */
    private final class BodyTab extends GuiButton {

        BodyTab(int x, int y) {
            super(BUTTON_ID, x, y, BodyStatusLayout.TAB_WIDTH, BodyStatusLayout.TAB_HEIGHT, "Body");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hover = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
            drawBevel(xPosition, yPosition, width, height, !open);
            if (hover)
                Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, 0x30FFFFFF);
            String text = open ? "Body \u00ab" : "Body";
            mc.fontRenderer.drawString(
                text,
                xPosition + (width - mc.fontRenderer.getStringWidth(text)) / 2,
                yPosition + 2,
                GUI_TEXT);
            BodyStatusSnapshot body = PacketBodyStatus.clientLatest;
            if (!open && body != null && needsAttention(body)) {
                Gui.drawRect(xPosition + width - 5, yPosition + 2, xPosition + width - 2, yPosition + 5, 0xFFC0302A);
            }
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiInventory)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        BodyStatusLayout layout = layoutFor(gui);
        tab = new BodyTab(layout.tabX(), layout.tabY());
        event.buttonList.add(tab);
        Mouse.getDWheel(); // drop wheel movement made before the screen opened
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.button instanceof BodyTab)) return;
        open = !open;
        scroll = 0;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiInventory)) {
            InventoryOverlayLayout.reserveLeft(0);
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        BodyStatusLayout layout = layoutFor(gui);
        InventoryOverlayLayout.reserveLeft(open ? layout.reservedLeftWidth(get(GUI_LEFT, gui, 0)) : 0);
        // Potion effects move the inventory after it is laid out; keep the tab on its corner.
        if (tab != null) {
            tab.xPosition = layout.tabX();
            tab.yPosition = layout.tabY();
        }
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!open || !(event.gui instanceof GuiInventory)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        BodyStatusLayout layout = layoutFor(gui);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            List<String> tooltip = draw(gui, layout, event.mouseX, event.mouseY);
            if (tooltip != null && !tooltip.isEmpty()) drawTooltip(gui, tooltip, event.mouseX, event.mouseY);
        } finally {
            GL11.glPopAttrib();
        }
    }

    private List<String> draw(GuiScreen gui, BodyStatusLayout layout, int mouseX, int mouseY) {
        FontRenderer font = gui.mc.fontRenderer;
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelWidth();
        int h = layout.panelHeight();
        drawBevel(x, y, w, h, true);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BACKGROUND);
        font.drawString("Body", x + 6, y + 6, GUI_TEXT);

        BodyStatusSnapshot body = PacketBodyStatus.clientLatest;
        if (body == null) {
            font.drawString("Checking your body\u2026", x + 6, y + 22, MUTED_TEXT);
            return null;
        }

        String condition = "Condition " + BodyStatusSnapshot.percent(body.condition());
        font.drawString(
            condition,
            x + w - 6 - font.getStringWidth(condition),
            y + 6,
            conditionColour(body.condition()));
        drawBar(x + 6, y + 17, w - 12, 3, body.condition(), BodyStatusLayout.harmColour(1.0F - body.condition()));

        List<String> tooltip = drawFigure(font, layout, body, mouseX, mouseY);
        drawVitals(font, layout, body);
        drawList(font, layout, body, mouseX, mouseY);
        return tooltip;
    }

    private List<String> drawFigure(FontRenderer font, BodyStatusLayout layout, BodyStatusSnapshot body, int mouseX,
        int mouseY) {
        String hovered = layout.panelContains(mouseX, mouseY) ? layout.regionAt(mouseX, mouseY) : null;
        List<String> tooltip = null;
        for (String path : BodyStatusLayout.dollRegions()) {
            int[] r = layout.regionRect(path);
            String id = regionId(body, path);
            BodyStatusSnapshot.Region region = id == null ? null : body.region(id);
            float harm = id == null ? 0.0F : body.regionHarm(id);
            int fill = region == null ? 0xFF9A9A9A : BodyStatusLayout.harmColour(harm);
            boolean broken = region != null && region.fractured();
            Gui.drawRect(r[0], r[1], r[0] + r[2], r[1] + r[3], broken ? 0xFF1E1E1E : 0xFF4A4A4A);
            Gui.drawRect(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1, fill);
            if (broken) {
                // A jagged crack across a broken part.
                int cx = r[0] + r[2] / 2;
                for (int i = 2; i < r[3] - 2; i += 2) {
                    int dx = (i / 2) % 2 == 0 ? -1 : 1;
                    Gui.drawRect(cx + dx, r[1] + i, cx + dx + 1, r[1] + i + 2, 0xFF1E1E1E);
                }
            }
            if (id != null && body.regionBleeding(id))
                Gui.drawRect(r[0] + r[2] - 4, r[1] + 1, r[0] + r[2] - 1, r[1] + 4, 0xFF9C0A0A);
            if (id != null && body.regionBurned(id))
                Gui.drawRect(r[0] + 1, r[1] + r[3] - 4, r[0] + 4, r[1] + r[3] - 1, 0xFFFF8C1A);
            if (path.equals(hovered)) {
                Gui.drawRect(r[0], r[1], r[0] + r[2], r[1] + 1, 0xFFFFFFFF);
                Gui.drawRect(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], 0xFFFFFFFF);
                Gui.drawRect(r[0], r[1], r[0] + 1, r[1] + r[3], 0xFFFFFFFF);
                Gui.drawRect(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], 0xFFFFFFFF);
                tooltip = regionTooltip(body, id != null ? id : path);
            }
        }
        return tooltip;
    }

    private static List<String> regionTooltip(BodyStatusSnapshot body, String id) {
        List<String> lines = new ArrayList<>();
        lines.add(BodyStatusSnapshot.label(id));
        BodyStatusSnapshot.Region region = body.region(id);
        if (region != null) {
            lines.add(
                "\u00a77Structure " + BodyStatusSnapshot.percent(region.integrity())
                    + (region.fractured() ? " \u00a7c(broken)" : ""));
        }
        List<BodyStatusSnapshot.Injury> injuries = body.injuriesIn(id);
        for (BodyStatusSnapshot.Injury injury : injuries) lines.add("\u00a7e" + BodyStatusSnapshot.injuryLine(injury));
        if (injuries.isEmpty() && (region == null || region.integrity() > 0.98F)) lines.add("\u00a7aUnhurt");
        return lines;
    }

    private void drawVitals(FontRenderer font, BodyStatusLayout layout, BodyStatusSnapshot body) {
        int x = layout.dollX() + BodyStatusLayout.DOLL_WIDTH + 8;
        int right = layout.panelX() + layout.panelWidth() - 6;
        int width = Math.max(20, right - x);
        int y = layout.dollY() - 1;
        y = vital(font, x, y, width, "Moving", body.movement());
        y = vital(font, x, y, width, "Hands", body.handling());
        if (body.canBleed()) y = vital(font, x, y, width, "Blood", ratio(body.blood(), body.bloodCapacity()));
        y = vital(font, x, y, width, "Energy", ratio(body.energy(), body.energyCapacity()));
        vital(font, x, y, width, "Water", ratio(body.hydration(), body.hydrationCapacity()));
    }

    private static int vital(FontRenderer font, int x, int y, int width, String label, float fraction) {
        String value = BodyStatusSnapshot.percent(fraction);
        font.drawString(label, x, y, GUI_TEXT);
        font.drawString(value, x + width - font.getStringWidth(value), y, fraction < 0.35F ? WARN_TEXT : MUTED_TEXT);
        drawBar(x, y + 9, width, 2, fraction, BodyStatusLayout.harmColour(1.0F - fraction));
        return y + 13;
    }

    private void drawList(FontRenderer font, BodyStatusLayout layout, BodyStatusSnapshot body, int mouseX, int mouseY) {
        int x = layout.panelX() + 6;
        int width = layout.panelWidth() - 12;
        int top = layout.listTop();
        int bottom = layout.listBottom();
        List<Integer> colours = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        addHeading(lines, colours, "Injuries");
        if (body.injuries()
            .isEmpty()) addWrapped(font, lines, colours, "No injuries.", width, MUTED_TEXT);
        for (BodyStatusSnapshot.Injury injury : body.injuries()) {
            addWrapped(
                font,
                lines,
                colours,
                BodyStatusSnapshot.label(injury.regionId()) + ": " + BodyStatusSnapshot.injuryLine(injury),
                width,
                injury.severity() >= 0.5F ? WARN_TEXT : GUI_TEXT);
        }
        lines.add("");
        colours.add(GUI_TEXT);

        addHeading(lines, colours, "Poisoning & exposure");
        if (body.exposures()
            .isEmpty()) addWrapped(font, lines, colours, "No exposure.", width, MUTED_TEXT);
        for (BodyStatusSnapshot.Exposure exposure : body.exposures()) {
            addWrapped(font, lines, colours, materialName(exposure.material()), width, GUI_TEXT);
            addWrapped(
                font,
                lines,
                colours,
                "  " + BodyStatusSnapshot.exposureDetail(exposure),
                width,
                exposure.conditionPenalty() >= 0.05 ? WARN_TEXT : MUTED_TEXT);
        }
        if (body.inhalationProtection() > 0.0F) {
            addWrapped(
                font,
                lines,
                colours,
                "Filter mask worn: breathing " + BodyStatusSnapshot.percent(body.inhalationProtection())
                    + " protected.",
                width,
                MUTED_TEXT);
        }

        int visible = Math.max(1, (bottom - top) / LINE_HEIGHT);
        int maxScroll = Math.max(0, lines.size() - visible);
        if (mouseX >= layout.panelX() && mouseX < layout.panelX() + layout.panelWidth()
            && mouseY >= top
            && mouseY < bottom) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) scroll += wheel < 0 ? 1 : -1;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        Gui.drawRect(x - 2, top - 4, x + width + 2, top - 3, 0xFF8B8B8B);
        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            String line = lines.get(scroll + i);
            font.drawString(line, x, top + i * LINE_HEIGHT, colours.get(scroll + i));
        }
        if (maxScroll > 0) {
            // Thin scrollbar on the right edge when the list is longer than the panel.
            int trackX = layout.panelX() + layout.panelWidth() - 4;
            int track = bottom - top;
            int thumb = Math.max(6, track * visible / lines.size());
            int thumbY = top + (track - thumb) * scroll / maxScroll;
            Gui.drawRect(trackX, top, trackX + 2, bottom, 0xFFAAAAAA);
            Gui.drawRect(trackX, thumbY, trackX + 2, thumbY + thumb, 0xFF555555);
        }
    }

    private static void addHeading(List<String> lines, List<Integer> colours, String text) {
        lines.add("\u00a7l" + text);
        colours.add(GUI_TEXT);
    }

    private static void addWrapped(FontRenderer font, List<String> lines, List<Integer> colours, String text, int width,
        int colour) {
        for (Object part : font.listFormattedStringToWidth(text, width)) {
            lines.add(String.valueOf(part));
            colours.add(colour);
        }
    }

    private static String materialName(String materialId) {
        String path = materialId.contains(":") ? materialId.substring(materialId.indexOf(':') + 1) : materialId;
        String key = "material.naturalexceptions." + path;
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocal(key)
            : BodyStatusSnapshot.materialFallbackName(materialId);
    }

    /** Snapshot region id for a figure part ("left_arm" matches "naturalexceptions:left_arm"). */
    private static String regionId(BodyStatusSnapshot body, String path) {
        for (BodyStatusSnapshot.Region region : body.regions()) {
            String id = region.id();
            if (id.equals(path) || id.endsWith(":" + path)) return id;
        }
        return null;
    }

    static boolean needsAttention(BodyStatusSnapshot body) {
        return !body.injuries()
            .isEmpty() || body.exposurePenalty() >= 0.05;
    }

    private static int conditionColour(float condition) {
        return condition < 0.35F ? WARN_TEXT : GUI_TEXT;
    }

    private static float ratio(float value, float capacity) {
        return capacity <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, value / capacity));
    }

    private static void drawBar(int x, int y, int width, int height, float fraction, int colour) {
        Gui.drawRect(x, y, x + width, y + height, BAR_BACK);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) Gui.drawRect(x, y, x + filled, y + height, colour);
    }

    private static void drawBevel(int x, int y, int width, int height, boolean raised) {
        Gui.drawRect(x, y, x + width, y + height, PANEL_BORDER);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, raised ? PANEL_BACKGROUND : 0xFFAAAAAA);
        int light = raised ? 0xFFFFFFFF : 0xFF555555;
        int dark = raised ? 0xFF555555 : 0xFFFFFFFF;
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + 2, light);
        Gui.drawRect(x + 1, y + 1, x + 2, y + height - 1, light);
        Gui.drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, dark);
        Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, dark);
    }

    private static BodyStatusLayout layoutFor(GuiContainer gui) {
        int left = get(GUI_LEFT, gui, Math.max(0, (gui.width - 176) / 2));
        int top = get(GUI_TOP, gui, Math.max(0, (gui.height - 166) / 2));
        return BodyStatusLayout.calculate(gui.width, gui.height, left, top);
    }

    private static void drawTooltip(GuiScreen gui, List<String> lines, int mouseX, int mouseY) {
        if (DRAW_TOOLTIP == null) return;
        try {
            DRAW_TOOLTIP.invoke(gui, lines, mouseX, mouseY, gui.mc.fontRenderer);
        } catch (ReflectiveOperationException ignored) {}
    }

    private static Method tooltipMethod() {
        try {
            Method method = GuiScreen.class
                .getDeclaredMethod("drawHoveringText", List.class, int.class, int.class, FontRenderer.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static Field field(String deobf, String srg) {
        for (String name : new String[] { deobf, srg }) {
            try {
                Field f = GuiContainer.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (ReflectiveOperationException | SecurityException ignored) {}
        }
        return null;
    }

    private static int get(Field field, GuiContainer gui, int fallback) {
        if (field == null) return fallback;
        try {
            return field.getInt(gui);
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }
}
