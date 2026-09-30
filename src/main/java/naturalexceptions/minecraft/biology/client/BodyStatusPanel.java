package naturalexceptions.minecraft.biology.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.minecraft.biology.BodyStatusSnapshot;
import naturalexceptions.minecraft.biology.BodyTreatment;
import naturalexceptions.minecraft.biology.BodyWords;
import naturalexceptions.minecraft.client.GuiGeometry;
import naturalexceptions.minecraft.client.GuiTooltips;
import naturalexceptions.minecraft.gui.client.EffectColumn;
import naturalexceptions.minecraft.item.ItemBandage;
import naturalexceptions.minecraft.knowledge.client.InventoryOverlayLayout;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketBodyStatus;
import naturalexceptions.minecraft.network.PacketTreatRegion;

/**
 * The Body panel beside the inventory and Gear screens (Body tab): a figure whose parts are coloured by how hurt they
 * are, how your blood, energy, water, legs and hands are holding up, and a short list of what is wrong in plain
 * words. Drop a field bandage on a wounded part to dress or splint it.
 */
@SideOnly(Side.CLIENT)
public final class BodyStatusPanel {

    private static final ResourceLocation SPRITES = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/body_panel.png");
    private static final int PANEL_BACKGROUND = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF373737;
    private static final int GUI_TEXT = 0xFF404040;
    private static final int MUTED_TEXT = 0xFF707070;
    private static final int WARN_TEXT = 0xFF9A2A1E;
    private static final int BAR_BACK = 0xFF8B8B8B;
    private static final int LINE_HEIGHT = 11;
    private static final int ICON = 9;

    /** Icon cells along v = 64 in the sprite sheet (see tools/textures/generate_interface_art.py). */
    private static final int ICON_BLOOD = 0;
    private static final int ICON_BURN = 1;
    private static final int ICON_BONE = 2;
    private static final int ICON_LUNGS = 3;
    private static final int ICON_DRESSING = 4;
    private static final int ICON_ENERGY = 5;
    private static final int ICON_WATER = 6;
    private static final int ICON_MOVING = 7;
    private static final int ICON_HANDS = 8;

    private static final BodyStatusPanel INSTANCE = new BodyStatusPanel();

    /** Open/closed survives closing and reopening the inventory for the rest of the session. */
    private boolean open;
    private int scroll;
    private BodyStatusLayout lastLayout;
    private GuiContainer lastGui;

    private BodyStatusPanel() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static boolean isOpen() {
        return INSTANCE.open;
    }

    public static void toggle() {
        INSTANCE.open = !INSTANCE.open;
        INSTANCE.scroll = 0;
        Mouse.getDWheel(); // drop wheel movement made while the panel was closed
    }

    /** True when the body has something worth looking at (an open injury or harmful exposure). */
    public static boolean needsAttention() {
        BodyStatusSnapshot body = PacketBodyStatus.clientLatest;
        return body != null && needsAttention(body);
    }

    static boolean needsAttention(BodyStatusSnapshot body) {
        return !body.injuries()
            .isEmpty() || body.exposurePenalty() >= 0.05;
    }

    /** The screens the panel appears beside. */
    static boolean hosts(GuiScreen gui) {
        return naturalexceptions.minecraft.gear.client.InventoryTabs.showsTabs(gui);
    }

    /** Beside the inventory, and left of the potion effects' column when there is one. */
    private static BodyStatusLayout layoutFor(GuiContainer gui) {
        int left = GuiGeometry.left(gui) - EffectColumn.width(gui);
        return BodyStatusLayout.calculate(gui.width, gui.height, left, GuiGeometry.top(gui), GuiGeometry.height(gui));
    }

    // --- Events ----------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        // The item browser keeps clear of the effects' column, and of this panel when it is open.
        int effects = EffectColumn.width(event.gui);
        if (!hosts(event.gui)) {
            InventoryOverlayLayout.reserveLeft(effects);
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        InventoryOverlayLayout
            .reserveLeft(open ? layoutFor(gui).reservedLeftWidth(GuiGeometry.left(gui)) : effects);
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!open || !hosts(event.gui)) {
            lastLayout = null;
            return;
        }
        GuiContainer gui = (GuiContainer) event.gui;
        BodyStatusLayout layout = layoutFor(gui);
        lastLayout = layout;
        lastGui = gui;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            List<String> tooltip = draw(gui, layout, event.mouseX, event.mouseY);
            if (tooltip != null && !tooltip.isEmpty()) GuiTooltips.draw(gui, tooltip, event.mouseX, event.mouseY);
            else if (!layout.panelContains(event.mouseX, event.mouseY))
                redrawItemTooltip(gui, event.mouseX, event.mouseY);
        } finally {
            GL11.glPopAttrib();
        }
    }

    /**
     * The screen drew its item tooltip before the panel was drawn over it; draw the tooltip of the item under the
     * mouse again, on top, so the panel never hides it.
     */
    @SuppressWarnings("unchecked")
    private static void redrawItemTooltip(GuiContainer gui, int mouseX, int mouseY) {
        if (gui.mc.thePlayer == null || gui.mc.thePlayer.inventory.getItemStack() != null) return;
        int left = GuiGeometry.left(gui), top = GuiGeometry.top(gui);
        for (net.minecraft.inventory.Slot slot : (List<net.minecraft.inventory.Slot>) gui.inventorySlots.inventorySlots) {
            int x = left + slot.xDisplayPosition, y = top + slot.yDisplayPosition;
            if (mouseX < x - 1 || mouseX >= x + 17 || mouseY < y - 1 || mouseY >= y + 17) continue;
            ItemStack stack = slot.getStack();
            if (stack == null || !slot.func_111238_b()) return;
            List<String> raw = stack.getTooltip(gui.mc.thePlayer, gui.mc.gameSettings.advancedItemTooltips);
            List<String> lines = new java.util.ArrayList<>();
            for (int i = 0; i < raw.size(); i++)
                lines.add(i == 0 ? stack.getRarity().rarityColor + raw.get(i) : "\u00a77" + raw.get(i));
            GuiTooltips.draw(gui, lines, mouseX, mouseY);
            return;
        }
    }

    /**
     * Called from the container click hook. A click on the open panel never reaches the screen underneath (so a held
     * item is not thrown into the world); with a bandage held, a click on a wounded part treats it.
     */
    public static boolean handleClick(GuiContainer gui, int mouseX, int mouseY, int button) {
        if (!consumes(gui, mouseX, mouseY)) return false;
        ItemStack held = gui.mc.thePlayer.inventory.getItemStack();
        BodyStatusSnapshot body = PacketBodyStatus.clientLatest;
        String path = INSTANCE.lastLayout.regionAt(mouseX, mouseY);
        if (button == 0 && held != null && held.getItem() instanceof ItemBandage && body != null && path != null) {
            String id = regionId(body, path);
            if (id != null && BodyTreatment.treatable(body, id))
                NENetwork.CHANNEL.sendToServer(new PacketTreatRegion(id));
        }
        return true;
    }

    /** Mouse presses and releases over the open panel belong to it. */
    public static boolean consumes(GuiContainer gui, int mouseX, int mouseY) {
        BodyStatusLayout layout = INSTANCE.lastLayout;
        return INSTANCE.open && layout != null && INSTANCE.lastGui == gui && layout.panelContains(mouseX, mouseY);
    }

    // --- Drawing ---------------------------------------------------------------------------------------------

    private List<String> draw(GuiContainer gui, BodyStatusLayout layout, int mouseX, int mouseY) {
        FontRenderer font = gui.mc.fontRenderer;
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelWidth();
        drawPanelFrame(x, y, w, layout.panelHeight());

        BodyStatusSnapshot body = PacketBodyStatus.clientLatest;
        if (body == null) {
            font.drawString("Body", x + 7, y + 7, GUI_TEXT);
            font.drawString("Checking\u2026", x + 7, y + 20, MUTED_TEXT);
            return null;
        }

        String status = BodyWords.status(body);
        font.drawString(status, x + 7, y + 7, statusColour(status));
        drawBar(x + 7, y + 17, w - 14, 2, body.condition(), BodyStatusLayout.harmColour(1.0F - body.condition()));

        ItemStack held = gui.mc.thePlayer.inventory.getItemStack();
        boolean bandage = held != null && held.getItem() instanceof ItemBandage;
        List<String> figureTip = drawFigure(gui, layout, body, bandage, mouseX, mouseY);
        List<String> vitalsTip = drawVitals(gui, layout, body, mouseX, mouseY);
        drawList(gui, layout, body, bandage, mouseX, mouseY);
        return figureTip != null ? figureTip : vitalsTip;
    }

    private static List<String> drawFigure(GuiContainer gui, BodyStatusLayout layout, BodyStatusSnapshot body,
        boolean bandage, int mouseX, int mouseY) {
        String hovered = layout.panelContains(mouseX, mouseY) ? layout.regionAt(mouseX, mouseY) : null;
        List<String> tooltip = null;
        float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0));
        for (String path : BodyStatusLayout.dollRegions()) {
            int[] r = layout.regionRect(path);
            String id = regionId(body, path);
            BodyStatusSnapshot.Region region = id == null ? null : body.region(id);
            int tint = region == null ? 0xFF9A9A9A : BodyStatusLayout.harmColour(body.regionHarm(id));
            drawPiece(gui, layout, path, tint);
            if (region != null && region.fractured()) drawCrack(r);
            if (id != null && body.regionBleeding(id))
                Gui.drawRect(r[0] + r[2] - 4, r[1] + 2, r[0] + r[2] - 2, r[1] + 5, 0xFF9C0A0A);
            boolean treatable = bandage && id != null && BodyTreatment.treatable(body, id);
            if (treatable) outline(r, (Math.round(pulse * 200) + 55) << 24 | 0xFFFFFF);
            if (path.equals(hovered)) {
                outline(r, 0xFFFFFFFF);
                tooltip = regionTooltip(body, id != null ? id : path, bandage, treatable);
            }
        }
        return tooltip;
    }

    private static void drawPiece(GuiContainer gui, BodyStatusLayout layout, String path, int tint) {
        int cell = BodyStatusLayout.pieceIndex(path);
        if (cell < 0) return;
        gui.mc.getTextureManager()
            .bindTexture(SPRITES);
        enableSpriteBlending();
        GL11.glColor4f(((tint >> 16) & 0xFF) / 255.0F, ((tint >> 8) & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, 1.0F);
        gui.drawTexturedModalRect(
            layout.dollX(),
            layout.dollY(),
            cell * 32,
            0,
            BodyStatusLayout.DOLL_WIDTH,
            BodyStatusLayout.DOLL_HEIGHT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawCrack(int[] r) {
        int cx = r[0] + r[2] / 2;
        for (int i = 3; i < r[3] - 3; i += 2) {
            int dx = (i / 2) % 2 == 0 ? -1 : 0;
            Gui.drawRect(cx + dx, r[1] + i, cx + dx + 1, r[1] + i + 2, 0xFF1E1E1E);
        }
    }

    private static List<String> regionTooltip(BodyStatusSnapshot body, String id, boolean bandage, boolean treatable) {
        List<String> lines = new ArrayList<>();
        lines.add(BodyStatusSnapshot.label(id));
        List<BodyStatusSnapshot.Injury> injuries = new ArrayList<>();
        for (BodyStatusSnapshot.Injury injury : body.groupedInjuries()) if (injury.regionId()
            .equals(id)) injuries.add(injury);
        for (BodyStatusSnapshot.Injury injury : injuries) lines.add("\u00a77" + BodyWords.injury(body, injury));
        BodyStatusSnapshot.Region region = body.region(id);
        if (injuries.isEmpty())
            lines.add(region != null && region.integrity() < 0.98F ? "\u00a77Sore, healing" : "\u00a7aUnhurt");
        if (bandage) lines.add(treatable ? "\u00a7eClick to use the bandage here" : "\u00a78Nothing here to dress");
        return lines;
    }

    private static List<String> drawVitals(GuiContainer gui, BodyStatusLayout layout, BodyStatusSnapshot body,
        int mouseX, int mouseY) {
        List<Vital> vitals = new ArrayList<>();
        if (body.canBleed()) vitals.add(new Vital("Blood", ICON_BLOOD, ratio(body.blood(), body.bloodCapacity())));
        vitals.add(new Vital("Energy", ICON_ENERGY, ratio(body.energy(), body.energyCapacity())));
        vitals.add(new Vital("Water", ICON_WATER, ratio(body.hydration(), body.hydrationCapacity())));
        vitals.add(new Vital("Moving", ICON_MOVING, body.movement()));
        vitals.add(new Vital("Hands", ICON_HANDS, body.handling()));
        List<String> tooltip = null;
        for (int i = 0; i < vitals.size(); i++) {
            Vital vital = vitals.get(i);
            int[] at = layout.vitalRow(i);
            drawIcon(gui, vital.icon, at[0], at[1]);
            drawBar(
                at[0] + ICON + 3,
                at[1] + 3,
                at[2] - ICON - 3,
                3,
                vital.value,
                BodyStatusLayout.harmColour(1.0F - vital.value));
            if (mouseX >= at[0] && mouseX < at[0] + at[2]
                && mouseY >= at[1]
                && mouseY < at[1] + BodyStatusLayout.VITAL_ROW_HEIGHT) {
                tooltip = new ArrayList<>();
                tooltip.add(vital.name + " " + BodyStatusSnapshot.percent(vital.value));
            }
        }
        return tooltip;
    }

    private static final class Vital {

        final String name;
        final int icon;
        final float value;

        Vital(String name, int icon, float value) {
            this.name = name;
            this.icon = icon;
            this.value = value;
        }
    }

    private void drawList(GuiContainer gui, BodyStatusLayout layout, BodyStatusSnapshot body, boolean bandage,
        int mouseX, int mouseY) {
        FontRenderer font = gui.mc.fontRenderer;
        int x = layout.panelX() + 7;
        int textX = x + ICON + 3;
        int right = layout.panelX() + layout.panelWidth() - 7;
        int width = right - 2 - textX;
        int top = layout.listTop();
        int bottom = layout.listBottom();
        List<Line> lines = new ArrayList<>();

        for (BodyStatusSnapshot.Injury injury : body.groupedInjuries()) {
            boolean urgent = injury.severity() >= 0.5F && injury.treatment() <= 0.0F;
            addWrapped(
                font,
                lines,
                iconFor(injury),
                BodyWords.injury(body, injury),
                width,
                urgent ? WARN_TEXT : GUI_TEXT);
            addWrapped(font, lines, -1, BodyWords.healing(injury), width, MUTED_TEXT);
        }
        boolean bleeding = false;
        for (BodyStatusSnapshot.Injury injury : body.injuries())
            bleeding |= injury.bleeding() > 0.05F && injury.treatment() <= 0.0F;
        if (bleeding) addWrapped(
            font,
            lines,
            -1,
            "Styptic Salve slows the bleeding; a dressing closes the wound; Iron Tonic makes blood back.",
            width,
            MUTED_TEXT);
        for (BodyStatusSnapshot.Exposure exposure : body.exposures()) {
            String level = BodyWords.level(exposure);
            boolean urgent = "severe".equals(level) || "moderate".equals(level);
            String text = BodyWords.exposure(exposure, materialName(exposure.material())) + " \u2014 " + level;
            addWrapped(font, lines, ICON_LUNGS, text, width, urgent ? WARN_TEXT : GUI_TEXT);
        }
        if (body.inhalationProtection() > 0.0F) addWrapped(font, lines, -1, "Mask on", width, MUTED_TEXT);
        if (lines.isEmpty()) addWrapped(font, lines, -1, "Nothing wrong.", width, MUTED_TEXT);

        if (bandage) bottom -= LINE_HEIGHT + 2;
        int visible = Math.max(1, (bottom - top) / LINE_HEIGHT);
        int maxScroll = Math.max(0, lines.size() - visible);
        if (layout.panelContains(mouseX, mouseY) && mouseY >= top && mouseY < bottom) {
            int wheel = Mouse.getDWheel();
            if (wheel != 0) scroll += wheel < 0 ? 1 : -1;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        Gui.drawRect(x - 1, top - 4, right, top - 3, 0xFF9A9A9A);
        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            Line line = lines.get(scroll + i);
            int ly = top + i * LINE_HEIGHT;
            drawIcon(gui, line.icon, x, ly - 1);
            font.drawString(line.text, textX, ly, line.colour);
        }
        if (maxScroll > 0) {
            int trackX = right - 1;
            int track = bottom - top;
            int thumb = Math.max(6, track * visible / lines.size());
            int thumbY = top + (track - thumb) * scroll / maxScroll;
            Gui.drawRect(trackX, top, trackX + 2, bottom, 0xFFAAAAAA);
            Gui.drawRect(trackX, thumbY, trackX + 2, thumbY + thumb, 0xFF555555);
        }
        if (bandage) {
            int hintY = layout.listBottom() - LINE_HEIGHT + 2;
            Gui.drawRect(x - 1, hintY - 3, right, hintY - 2, 0xFF9A9A9A);
            drawIcon(gui, ICON_DRESSING, x, hintY - 1);
            boolean any = false;
            for (BodyStatusSnapshot.Injury injury : body.injuries()) any |= injury.treatment() <= 0.0F;
            font.drawString(
                font.trimStringToWidth(any ? "Drop it on a wound" : "No wounds to dress", width),
                textX,
                hintY,
                any ? GUI_TEXT : MUTED_TEXT);
        }
    }

    private static int iconFor(BodyStatusSnapshot.Injury injury) {
        if (injury.treatment() > 0.0F) return ICON_DRESSING;
        if (injury.kind() == BiologicalInjury.Kind.BURN) return ICON_BURN;
        if (injury.kind() == BiologicalInjury.Kind.STRUCTURAL) return ICON_BONE;
        return ICON_BLOOD;
    }

    /** One list line: an optional icon (only on an entry's first line) and text. */
    private static final class Line {

        final int icon;
        final String text;
        final int colour;

        Line(int icon, String text, int colour) {
            this.icon = icon;
            this.text = text;
            this.colour = colour;
        }
    }

    private static void addWrapped(FontRenderer font, List<Line> lines, int icon, String text, int width, int colour) {
        boolean first = true;
        for (Object part : font.listFormattedStringToWidth(text, width)) {
            lines.add(new Line(first ? icon : -1, String.valueOf(part), colour));
            first = false;
        }
    }

    private static void drawIcon(GuiContainer gui, int index, int x, int y) {
        if (gui == null || index < 0) return;
        gui.mc.getTextureManager()
            .bindTexture(SPRITES);
        enableSpriteBlending();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        gui.drawTexturedModalRect(x, y, index * ICON, 64, ICON, ICON);
    }

    /** {@code Gui.drawRect} leaves blending off; sprites need it for their transparent edges. */
    private static void enableSpriteBlending() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
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

    private static int statusColour(String status) {
        switch (status) {
            case "Healthy":
                return 0xFF3C6E2E;
            case "Hurt":
                return 0xFF8A6418;
            case "Badly hurt":
                return 0xFFA8461C;
            default:
                return 0xFFA0201A;
        }
    }

    private static float ratio(float value, float capacity) {
        return capacity <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, value / capacity));
    }

    private static void drawBar(int x, int y, int width, int height, float fraction, int colour) {
        Gui.drawRect(x, y, x + width, y + height, BAR_BACK);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) Gui.drawRect(x, y, x + filled, y + height, colour);
    }

    private static void outline(int[] r, int colour) {
        Gui.drawRect(r[0], r[1], r[0] + r[2], r[1] + 1, colour);
        Gui.drawRect(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], colour);
        Gui.drawRect(r[0], r[1] + 1, r[0] + 1, r[1] + r[3] - 1, colour);
        Gui.drawRect(r[0] + r[2] - 1, r[1] + 1, r[0] + r[2], r[1] + r[3] - 1, colour);
    }

    /** Vanilla container frame: black outline, white top-left and dark bottom-right bevel. */
    private static void drawPanelFrame(int x, int y, int width, int height) {
        Gui.drawRect(x + 1, y, x + width - 1, y + height, PANEL_BORDER);
        Gui.drawRect(x, y + 1, x + width, y + height - 1, PANEL_BORDER);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_BACKGROUND);
        Gui.drawRect(x + 1, y + 1, x + width - 2, y + 3, 0xFFFFFFFF);
        Gui.drawRect(x + 1, y + 1, x + 3, y + height - 2, 0xFFFFFFFF);
        Gui.drawRect(x + 3, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
        Gui.drawRect(x + width - 3, y + 3, x + width - 1, y + height - 1, 0xFF555555);
    }
}
