package naturalexceptions.minecraft.gear.client;

import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.biology.client.BodyStatusPanel;
import naturalexceptions.minecraft.client.GuiGeometry;
import naturalexceptions.minecraft.client.GuiTooltips;
import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketOpenGear;
import naturalexceptions.minecraft.registry.NEItems;

/** Draws and handles the Inventory / Gear / Body / Items tabs on the survival inventory and the Gear screen. */
@SideOnly(Side.CLIENT)
public final class InventoryTabs {

    private static final ResourceLocation TABS = new ResourceLocation(
        "textures/gui/container/creative_inventory/tabs.png");
    private static final RenderItem ITEM_RENDER = new RenderItem();

    private InventoryTabs() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new InventoryTabs());
    }

    /** Screens that carry the tabs. */
    public static boolean showsTabs(GuiScreen gui) {
        return gui instanceof GuiInventory || gui instanceof GuiGear;
    }

    static boolean selected(InventoryTab tab, GuiScreen gui) {
        switch (tab) {
            case INVENTORY:
                return gui instanceof GuiInventory;
            case GEAR:
                return gui instanceof GuiGear;
            case BODY:
                return BodyStatusPanel.isOpen();
            case ITEMS:
            default:
                return InventoryBrowserOverlay.isEnabled();
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!showsTabs(event.gui)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        Minecraft mc = gui.mc;
        int left = GuiGeometry.left(gui);
        int top = GuiGeometry.top(gui);
        int width = GuiGeometry.width(gui);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            for (InventoryTab tab : InventoryTab.values()) {
                boolean on = selected(tab, gui);
                int x = tab.x(left, width);
                int y = tab.y(top);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager()
                    .bindTexture(TABS);
                // A raised tab joins the screen's edge; a lowered one stops short of it, as if tucked behind.
                gui.drawTexturedModalRect(x, y, tab.column() * 28, on ? 32 : 0, InventoryTab.WIDTH, on ? 32 : 28);
                drawIcon(mc, iconFor(tab), x + 6, y + (on ? 9 : 10));
                if (tab == InventoryTab.BODY && !on && BodyStatusPanel.needsAttention())
                    Gui.drawRect(x + 20, y + 5, x + 23, y + 8, 0xFFC0302A);
            }
            InventoryTab hovered = InventoryTab.at(event.mouseX, event.mouseY, left, top, width);
            if (hovered != null && mc.thePlayer.inventory.getItemStack() == null) {
                GuiTooltips.draw(gui, Collections.singletonList(hovered.label()), event.mouseX, event.mouseY);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    /** Called from the container click hook; returns true when a tab took the click. */
    public static boolean handleClick(GuiContainer gui, int mouseX, int mouseY, int button) {
        if (button != 0 || !showsTabs(gui)) return false;
        InventoryTab tab = InventoryTab
            .at(mouseX, mouseY, GuiGeometry.left(gui), GuiGeometry.top(gui), GuiGeometry.width(gui));
        if (tab == null) return false;
        Minecraft mc = gui.mc;
        boolean holding = mc.thePlayer.inventory.getItemStack() != null;
        switch (tab) {
            case INVENTORY:
                if (gui instanceof GuiGear && !holding) {
                    mc.thePlayer.closeScreen();
                    mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
                }
                break;
            case GEAR:
                if (gui instanceof GuiInventory && !holding) NENetwork.CHANNEL.sendToServer(new PacketOpenGear());
                break;
            case BODY:
                BodyStatusPanel.toggle();
                break;
            case ITEMS:
            default:
                InventoryBrowserOverlay.toggleEnabled();
                break;
        }
        mc.getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        return true;
    }

    private static ItemStack iconFor(InventoryTab tab) {
        switch (tab) {
            case INVENTORY:
                return new ItemStack(Blocks.chest);
            case GEAR:
                return NEItems.exposureMask != null ? new ItemStack(NEItems.exposureMask)
                    : new ItemStack(Items.leather_helmet);
            case BODY:
                return NEItems.bandage != null ? new ItemStack(NEItems.bandage) : new ItemStack(Items.apple);
            case ITEMS:
            default:
                return new ItemStack(Items.book);
        }
    }

    private static void drawIcon(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null || stack.getItem() == null) return;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        ITEM_RENDER.zLevel = 100.0F;
        ITEM_RENDER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        ITEM_RENDER.zLevel = 0.0F;
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
    }
}
