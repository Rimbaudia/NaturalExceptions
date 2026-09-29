package naturalexceptions.minecraft.knowledge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client event handler intercepting hotkeys (K, R, U) both in-game and inside GUI screens.
 */
@SideOnly(Side.CLIENT)
public final class NEKnowledgeKeyHandler {

    private static ItemStack lastHoveredStack;
    private static long lastHoveredTime;

    private static boolean wasRecipeKeyDown = false;
    private static boolean wasUsageKeyDown = false;
    private static boolean wasOpenKeyDown = false;

    private static boolean registered = false;

    private NEKnowledgeKeyHandler() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        NEKnowledgeKeys.register();
        NEKnowledgeKeyHandler handler = new NEKnowledgeKeyHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
        InventoryBrowserOverlay.register();
    }

    /**
     * Handles in-game world hotkeys when no GUI is open.
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        // Container GUIs resolve R/U from their currently hovered slot in
        // DrawScreenEvent.Post. The hand-stack path is only for world play.
        if (mc.currentScreen != null) return;

        if (NEKnowledgeKeys.keyOpenBrowser != null && NEKnowledgeKeys.keyOpenBrowser.isPressed()) {
            mc.displayGuiScreen(new GuiKnowledgeBrowser());
        } else if (NEKnowledgeKeys.keyRecipeLookup != null && NEKnowledgeKeys.keyRecipeLookup.isPressed()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null) {
                mc.displayGuiScreen(GuiKnowledgeBrowser.forAcquisitions(held));
            } else {
                mc.displayGuiScreen(new GuiKnowledgeBrowser());
            }
        } else if (NEKnowledgeKeys.keyUsageLookup != null && NEKnowledgeKeys.keyUsageLookup.isPressed()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null) {
                mc.displayGuiScreen(GuiKnowledgeBrowser.forUsages(held));
            } else {
                mc.displayGuiScreen(new GuiKnowledgeBrowser());
            }
        }
    }

    /** Only item screens take the K/R/U hotkeys; every other screen (chat above all) takes letters as text. */
    static boolean acceptsHotkeys(net.minecraft.client.gui.GuiScreen screen) {
        return screen instanceof net.minecraft.client.gui.inventory.GuiContainer;
    }

    /**
     * Records the hovered ItemStack and displays subtle keybind hints.
     */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event == null || event.itemStack == null) return;

        lastHoveredStack = event.itemStack.copy();
        lastHoveredTime = Minecraft.getSystemTime();

        if (event.showAdvancedItemTooltips) {
            event.toolTip.add("\u00a78[R] Recipes  [U] Usages");
        }
    }

    /**
     * Intercepts hotkeys when hovering over items inside container GUIs (inventory, chest, etc.).
     */
    @SubscribeEvent
    public void onGuiDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) return;
        if (mc.currentScreen instanceof GuiKnowledgeBrowser) return;

        int rKey = NEKnowledgeKeys.keyRecipeLookup != null ? NEKnowledgeKeys.keyRecipeLookup.getKeyCode()
            : Keyboard.KEY_R;
        int uKey = NEKnowledgeKeys.keyUsageLookup != null ? NEKnowledgeKeys.keyUsageLookup.getKeyCode()
            : Keyboard.KEY_U;
        int kKey = NEKnowledgeKeys.keyOpenBrowser != null ? NEKnowledgeKeys.keyOpenBrowser.getKeyCode()
            : Keyboard.KEY_K;

        boolean rDown = rKey > 0 && Keyboard.isKeyDown(rKey);
        boolean uDown = uKey > 0 && Keyboard.isKeyDown(uKey);
        boolean kDown = kKey > 0 && Keyboard.isKeyDown(kKey);

        boolean freshR = rDown && !wasRecipeKeyDown;
        boolean freshU = uDown && !wasUsageKeyDown;
        boolean freshK = kDown && !wasOpenKeyDown;

        // Hotkeys belong to item screens only. Chat, signs, books, menus and every other screen take letters as
        // text: typing "k" in chat must never open the browser.
        if (!acceptsHotkeys(mc.currentScreen)) {
            wasRecipeKeyDown = rDown;
            wasUsageKeyDown = uDown;
            wasOpenKeyDown = kDown;
            return;
        }

        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer
            && !InventoryBrowserOverlay.mayRunGlobalKeybinds()) {
            // The overlay search/subview owns keyboard input; keep edge state in sync
            // so a held key cannot fire immediately when focus changes.
            wasRecipeKeyDown = rDown;
            wasUsageKeyDown = uDown;
            wasOpenKeyDown = kDown;
            return;
        }

        wasRecipeKeyDown = rDown;
        wasUsageKeyDown = uDown;
        wasOpenKeyDown = kDown;

        if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
            if (freshK) mc.displayGuiScreen(new GuiKnowledgeBrowser());
            return;
        }

        long now = Minecraft.getSystemTime();
        boolean hasHovered = lastHoveredStack != null && (now - lastHoveredTime < 350);

        if (freshR && hasHovered) {
            mc.displayGuiScreen(GuiKnowledgeBrowser.forAcquisitions(lastHoveredStack));
        } else if (freshU && hasHovered) {
            mc.displayGuiScreen(GuiKnowledgeBrowser.forUsages(lastHoveredStack));
        } else if (freshK) {
            mc.displayGuiScreen(new GuiKnowledgeBrowser());
        }
    }
}
