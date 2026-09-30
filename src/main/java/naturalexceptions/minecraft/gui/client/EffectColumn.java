package naturalexceptions.minecraft.gui.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.client.GuiGeometry;
import naturalexceptions.minecraft.client.GuiTooltips;
import naturalexceptions.minecraft.mixin.GuiContainerCreativeAccessor;

/**
 * Potion effects beside the inventory as a column of small icons, in place of vanilla's wide boxes. Vanilla pushes
 * the whole inventory aside to make room for its boxes (so the item browser loses its side) and draws them after the
 * container (so they cover the browser while an item tooltip shows); here the inventory stays centred, and the icons
 * are drawn last, with the name and time left on hover.
 */
@SideOnly(Side.CLIENT)
public final class EffectColumn {

    /** A cell's size and the gap between cells and from the inventory. */
    public static final int CELL = 20, GAP = 2;

    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final Field SHOWS_EFFECTS = find();
    private static final EffectColumn INSTANCE = new EffectColumn();

    private EffectColumn() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    private static Field find() {
        try {
            Field field = InventoryEffectRenderer.class.getDeclaredField("field_147045_u");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** How many columns {@code effects} cells take beside a screen {@code guiHeight} tall. */
    public static int columns(int effects, int guiHeight) {
        if (effects <= 0) return 0;
        int perColumn = Math.max(1, (guiHeight + GAP) / (CELL + GAP));
        return (effects + perColumn - 1) / perColumn;
    }

    /** The width the column takes left of the inventory, gap included; 0 when there are no effects to show. */
    public static int width(GuiScreen gui) {
        if (!(gui instanceof InventoryEffectRenderer)) return 0;
        Collection<?> effects = effects();
        int columns = columns(effects.size(), GuiGeometry.height((GuiContainer) gui));
        return columns == 0 ? 0 : columns * (CELL + GAP) + GAP;
    }

    @SuppressWarnings("unchecked")
    private static Collection<PotionEffect> effects() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.thePlayer == null ? java.util.Collections.<PotionEffect>emptyList()
            : mc.thePlayer.getActivePotionEffects();
    }

    /** Undoes vanilla's shove: the inventory back in the middle, and what it placed beside it moved with it. */
    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof InventoryEffectRenderer) || SHOWS_EFFECTS == null) return;
        GuiContainer gui = (GuiContainer) event.gui;
        try {
            if (!SHOWS_EFFECTS.getBoolean(gui)) return;
            SHOWS_EFFECTS.setBoolean(gui, false);
        } catch (IllegalAccessException e) {
            return;
        }
        int shift = (gui.width - GuiGeometry.width(gui)) / 2 - GuiGeometry.left(gui);
        if (shift == 0) return;
        GuiGeometry.setLeft(gui, GuiGeometry.left(gui) + shift);
        for (Object button : event.buttonList) ((GuiButton) button).xPosition += shift;
        if (gui instanceof GuiContainerCreative) {
            net.minecraft.client.gui.GuiTextField search = ((GuiContainerCreativeAccessor) gui)
                .naturalexceptions$getSearchField();
            if (search != null) search.xPosition += shift;
        }
    }

    /** Drawn after everything the screen draws, before the Body panel puts an item's tooltip back on top. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof InventoryEffectRenderer)) return;
        List<PotionEffect> effects = new ArrayList<>(effects());
        if (effects.isEmpty()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Minecraft mc = gui.mc;
        int top = GuiGeometry.top(gui), height = GuiGeometry.height(gui);
        int perColumn = Math.max(1, (height + GAP) / (CELL + GAP));
        int right = GuiGeometry.left(gui) - GAP;
        List<String> tooltip = null;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            for (int i = 0; i < effects.size(); i++) {
                PotionEffect effect = effects.get(i);
                int x = right - (i / perColumn + 1) * (CELL + GAP) + GAP, y = top + (i % perColumn) * (CELL + GAP);
                boolean ending = effect.getDuration() < 200 && !effect.getIsPotionDurationMax();
                // Fading in and out over its last ten seconds, as vanilla's boxes do not but a player expects.
                boolean dim = ending && effect.getDuration() / 10 % 2 == 0;
                cell(x, y, dim);
                Potion potion = Potion.potionTypes[effect.getPotionID()];
                if (potion != null) {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, dim ? 0.45F : 1.0F);
                    if (potion.hasStatusIcon()) {
                        mc.getTextureManager()
                            .bindTexture(INVENTORY);
                        int index = potion.getStatusIconIndex();
                        gui.drawTexturedModalRect(x + 1, y + 1, index % 8 * 18, 198 + index / 8 * 18, 18, 18);
                    } else {
                        String initial = I18n.format(potion.getName())
                            .substring(0, 1);
                        mc.fontRenderer.drawStringWithShadow(
                            initial,
                            x + (CELL - mc.fontRenderer.getStringWidth(initial)) / 2,
                            y + 6,
                            0xFFFFFF);
                    }
                    if (event.mouseX >= x && event.mouseX < x + CELL
                        && event.mouseY >= y
                        && event.mouseY < y + CELL) tooltip = describe(potion, effect);
                }
            }
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (tooltip != null) GuiTooltips.draw(gui, tooltip, event.mouseX, event.mouseY);
        } finally {
            GL11.glPopAttrib();
        }
    }

    /** A bevelled slot, like the inventory's own, darker while an ending effect blinks. */
    private static void cell(int x, int y, boolean dim) {
        Gui.drawRect(x, y, x + CELL, y + CELL, 0xFF373737);
        Gui.drawRect(x + 1, y + 1, x + CELL, y + CELL, 0xFFFFFFFF);
        Gui.drawRect(x + 1, y + 1, x + CELL - 1, y + CELL - 1, dim ? 0xFF6B6B6B : 0xFF8B8B8B);
    }

    private static List<String> describe(Potion potion, PotionEffect effect) {
        List<String> lines = new ArrayList<>();
        String name = I18n.format(potion.getName());
        if (effect.getAmplifier() > 0) name += " " + I18n.format("enchantment.level." + (effect.getAmplifier() + 1));
        lines.add((potion.isBadEffect() ? "§c" : "§f") + name);
        lines.add("§7" + Potion.getDurationString(effect));
        return lines;
    }
}
