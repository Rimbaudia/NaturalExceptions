package naturalexceptions.minecraft.knowledge.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gui.client.GuiMaterialNaming;
import naturalexceptions.minecraft.knowledge.recipe.CraftingGridSlot;
import naturalexceptions.minecraft.knowledge.recipe.GameItemRegistry;
import naturalexceptions.minecraft.knowledge.recipe.RecipeCategory;
import naturalexceptions.minecraft.knowledge.recipe.RecipeDisplayType;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipe;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipeRegistry;
import naturalexceptions.minecraft.mixin.GuiContainerCreativeAccessor;

/** JEI-like item and quick recipe overlay drawn over any normal container GUI. */
@SideOnly(Side.CLIENT)
public final class InventoryBrowserOverlay {

    private static final int PANEL_BACKGROUND = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF373737;
    private static final int GUI_TEXT = 0xFF404040;
    private static final int SLOT_FILL = 0x30FFFFFF;
    private static final int SLOT_HOVER = 0x70E4D6A8;
    private static final int CELL = 18;
    private static final InventoryBrowserOverlay INSTANCE = new InventoryBrowserOverlay();

    private String query = "";
    private boolean searchFocused;
    private boolean userEnabled = true;
    private boolean showingRecipes;
    private boolean showingUsages;
    private ItemStack selectedStack;
    private int recipePage;
    private List<RecipeCategory> resultCategories = Collections.emptyList();
    private int categoryIndex;
    private List<GameItemRegistry.IndexedItem> filteredItems = Collections.emptyList();
    private int itemPage;
    private GuiContainer lastContainer;
    private InventoryOverlayLayout.Layout lastLayout;
    private int lastMouseX;
    private int lastMouseY;
    private ItemStack hoveredPanelStack;
    private ItemStack hoveredRecipeStack;
    private ItemStack pendingTooltipStack;
    private int pendingTooltipX;
    private int pendingTooltipY;
    private boolean panelDrawnThisFrame;
    private static final Field GUI_LEFT = field("guiLeft", "field_147003_i");
    private static final Field GUI_TOP = field("guiTop", "field_147009_r");
    private static final Field X_SIZE = field("xSize", "field_146999_f");
    private static final Field Y_SIZE = field("ySize", "field_147000_g");
    private static final Method DRAW_TOOLTIP = tooltipMethod();

    private InventoryBrowserOverlay() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        panelDrawnThisFrame = false;
        if (!(event.gui instanceof GuiContainer)) {
            clearScreenState();
            return;
        }

        GuiContainer gui = (GuiContainer) event.gui;
        if (lastContainer != gui) {
            lastContainer = gui;
            query = "";
            itemPage = 0;
            selectedStack = null;
            showingRecipes = false;
            showingUsages = false;
            searchFocused = false;
            UniversalRecipeRegistry.instance()
                .init();
            GameItemRegistry.instance()
                .init();
            refreshItems();
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) return;
        int left = get(GUI_LEFT, gui, Math.max(0, (gui.width - 176) / 2));
        int top = get(GUI_TOP, gui, Math.max(0, (gui.height - 166) / 2));
        int width = get(X_SIZE, gui, 176);
        int height = get(Y_SIZE, gui, 166);
        lastLayout = InventoryOverlayLayout.calculate(gui.width, gui.height, left, top, width, height);
        lastMouseX = event.mouseX;
        lastMouseY = event.mouseY;

        hoveredPanelStack = null;
    }

    /** Draw at container end when no vanilla slot tooltip was rendered. */
    public static void renderAtContainerTail(GuiContainer gui, int mouseX, int mouseY) {
        if (gui instanceof GuiContainerCreative) return;
        if (!INSTANCE.panelDrawnThisFrame) renderAtTail(gui, mouseX, mouseY);
    }

    /** Draw before a container slot tooltip so the tooltip remains on top of the item panel. */
    public static void renderBeforeContainerTooltip(GuiContainer gui, int mouseX, int mouseY) {
        if (!INSTANCE.panelDrawnThisFrame) renderAtTail(gui, mouseX, mouseY);
    }

    /** Creative tabs draw their own tooltips after the container superclass returns. */
    public static void renderAfterCreativeContainer(GuiContainerCreative gui, int mouseX, int mouseY) {
        if (!INSTANCE.panelDrawnThisFrame) renderAtTail(gui, mouseX, mouseY);
    }

    private static void renderAtTail(GuiContainer gui, int mouseX, int mouseY) {
        InventoryBrowserOverlay overlay = INSTANCE;
        if (gui == null || gui != overlay.lastContainer || overlay.lastLayout == null) return;
        overlay.panelDrawnThisFrame = true;
        overlay.lastMouseX = mouseX;
        overlay.lastMouseY = mouseY;
        overlay.pendingTooltipStack = null;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            // Container item rendering leaves depth testing enabled. Keep that state
            // from clipping or tinting the side panel and its item icons.
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (overlay.lastLayout.isPanelVisible() && overlay.userEnabled) {
                overlay.drawPanel(gui, mouseX, mouseY);
                overlay.renderPendingTooltip(gui);
            } else {
                overlay.drawToggleHint(gui, overlay.lastLayout);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiContainer)) return;

        if (lastContainer != mc.currentScreen || lastLayout == null) return;
        int x = scaledMouseX(mc);
        int y = scaledMouseY(mc);
        if (Mouse.getEventDWheel() != 0 && panelContains(x, y)) {
            if (showingRecipes || showingUsages) {
                recipePage = clamp(recipePage + (Mouse.getEventDWheel() < 0 ? 1 : -1), 0, recipeCount() - 1);
            } else {
                int perPage = pageSize();
                itemPage = clamp(
                    itemPage + (Mouse.getEventDWheel() < 0 ? perPage : -perPage),
                    0,
                    Math.max(0, pageCount() - 1) * perPage);
            }
        }

    }

    /** Handles presses from GuiScreen.mouseClicked, where coordinates use scaled GUI space. */
    public static boolean handleContainerMouseClick(GuiContainer gui, int x, int y, int button) {
        InventoryBrowserOverlay overlay = INSTANCE;
        if (gui == null || gui != overlay.lastContainer || overlay.lastLayout == null) return false;
        if (button >= 0
            && (!overlay.lastLayout.isPanelVisible() || !overlay.userEnabled || !overlay.panelContains(x, y)))
            overlay.searchFocused = false;
        if (!overlay.lastLayout.isPanelVisible() || !overlay.userEnabled) {
            if (tabbed(gui)) return false;
            int toggleY = toggleY(gui, overlay.lastLayout);
            if (x >= overlay.lastLayout.toggleX() && x < overlay.lastLayout.toggleX() + overlay.lastLayout.toggleWidth()
                && y >= toggleY
                && y < toggleY + 12) {
                overlay.userEnabled = !overlay.userEnabled;
                return true;
            }
            return false;
        }
        if (!overlay.panelContains(x, y)) return false;
        if (button < 0) return true;
        int searchX = overlay.lastLayout.panelX() + 5;
        int searchY = overlay.lastLayout.panelY() + 22;
        if (x >= searchX && x < searchX + overlay.lastLayout.panelWidth() - 10 && y >= searchY && y < searchY + 14) {
            overlay.searchFocused = button == 0;
            return true;
        }
        overlay.searchFocused = false;

        if (overlay.showingRecipes || overlay.showingUsages) {
            if (button == 1 || overlay.inBackButton(x, y)) {
                overlay.showingRecipes = false;
                overlay.showingUsages = false;
                overlay.selectedStack = null;
                overlay.recipePage = 0;
            } else if (overlay.inCategoryPrevious(x, y)) {
                overlay.changeCategory(-1);
            } else if (overlay.inCategoryNext(x, y)) {
                overlay.changeCategory(1);
            } else if (overlay.inRecipePrevious(x, y)) {
                overlay.recipePage = Math.max(0, overlay.recipePage - 1);
            } else if (overlay.inRecipeNext(x, y)) {
                overlay.recipePage = Math.min(Math.max(0, overlay.recipeCount() - 1), overlay.recipePage + 1);
            } else if (overlay.hoveredRecipeStack != null && (button == 0 || button == 1)) {
                overlay.open(overlay.hoveredRecipeStack, button == 1);
            }
            return true;
        }

        InventoryOverlayInput.PageAction pageAction = InventoryOverlayInput.pageActionAt(
            overlay.lastLayout.panelX(),
            overlay.lastLayout.panelY(),
            overlay.lastLayout.panelWidth(),
            overlay.lastLayout.panelHeight(),
            x,
            y);
        if (pageAction != InventoryOverlayInput.PageAction.NONE) {
            int perPage = overlay.pageSize();
            if (pageAction == InventoryOverlayInput.PageAction.PREVIOUS) {
                overlay.itemPage = Math.max(0, overlay.itemPage - perPage);
            } else {
                overlay.itemPage = Math.min(Math.max(0, overlay.pageCount() - 1) * perPage, overlay.itemPage + perPage);
            }
            return true;
        }

        ItemStack clicked = overlay.itemAt(x, y);
        if (clicked != null) {
            if (button == 0) overlay.open(clicked, false);
            else if (button == 1) overlay.open(clicked, true);
        }
        return true;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || !(mc.currentScreen instanceof GuiContainer) || lastContainer != mc.currentScreen) return;
        if (!mayRunGlobalKeybinds()) return;
        int key = Keyboard.getEventKey();
        if (!Keyboard.getEventKeyState()) return;

        int recipeKey = NEKnowledgeKeys.keyRecipeLookup == null ? Keyboard.KEY_R
            : NEKnowledgeKeys.keyRecipeLookup.getKeyCode();
        int usageKey = NEKnowledgeKeys.keyUsageLookup == null ? Keyboard.KEY_U
            : NEKnowledgeKeys.keyUsageLookup.getKeyCode();
        if (key == recipeKey || key == usageKey) {
            ItemStack target = hoveredPanelStack;
            if (target == null) target = hoveredContainerStack();
            if (target != null) open(target, key == usageKey);
            return;
        }

    }

    public static boolean handleContainerKeyTyped(GuiContainer gui, char typedChar, int keyCode) {
        InventoryBrowserOverlay overlay = INSTANCE;
        if (gui == null || gui != overlay.lastContainer) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (overlay.searchFocused) {
                overlay.searchFocused = false;
                return true;
            }
            if (overlay.showingRecipes || overlay.showingUsages) {
                overlay.showingRecipes = false;
                overlay.showingUsages = false;
                overlay.selectedStack = null;
                overlay.recipePage = 0;
                return true;
            }
            return false;
        }
        if (overlay.searchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!overlay.query.isEmpty()) overlay.query = overlay.query.substring(0, overlay.query.length() - 1);
                overlay.resetSearch();
                return true;
            }
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                overlay.searchFocused = false;
                return true;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return true;
            if (typedChar >= 32 && typedChar != 127 && overlay.query.length() < 64) {
                overlay.query += typedChar;
                overlay.resetSearch();
            }
            // Search owns printable keys and command keys alike, including R/U/K/E.
            return true;
        }
        // Vanilla creative search (and naming text) must see keys before R/U interception.
        if (hasFocusedContainerText(gui)) return false;
        if (keyCode == Keyboard.KEY_E
            && Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode() == keyCode) {
            overlay.searchFocused = false;
            return false;
        }
        if (!overlay.searchFocused) {
            int recipeKey = NEKnowledgeKeys.keyRecipeLookup == null ? Keyboard.KEY_R
                : NEKnowledgeKeys.keyRecipeLookup.getKeyCode();
            int usageKey = NEKnowledgeKeys.keyUsageLookup == null ? Keyboard.KEY_U
                : NEKnowledgeKeys.keyUsageLookup.getKeyCode();
            return keyCode == recipeKey || keyCode == usageKey;
        }
        return false;
    }

    private static boolean hasFocusedContainerText(GuiScreen gui) {
        if (gui instanceof GuiContainerCreativeAccessor) {
            GuiTextField field = ((GuiContainerCreativeAccessor) gui).naturalexceptions$getSearchField();
            return field != null && field.getVisible() && field.isFocused();
        }
        return gui instanceof GuiMaterialNaming && ((GuiMaterialNaming) gui).isNameFieldFocused();
    }

    /** Text fields and an active recipe subview take priority over global NE shortcuts. */
    public static boolean mayRunGlobalKeybinds() {
        InventoryBrowserOverlay overlay = INSTANCE;
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen gui = mc == null ? null : mc.currentScreen;
        boolean currentOverlay = gui != null && gui == overlay.lastContainer;
        return InventoryOverlayInput.mayRunGlobalShortcut(
            (currentOverlay && overlay.searchFocused) || hasFocusedContainerText(gui),
            currentOverlay && (overlay.showingRecipes || overlay.showingUsages));
    }

    public static boolean isSearchFocused() {
        return InventoryOverlayInput.ownsSearchInput(INSTANCE.searchFocused);
    }

    public static boolean shouldConsumeContainerMouse(GuiContainer gui, int mouseX, int mouseY) {
        InventoryBrowserOverlay overlay = INSTANCE;
        if (gui == null || gui != overlay.lastContainer || overlay.lastLayout == null) return false;
        if (overlay.lastLayout.isPanelVisible() && overlay.userEnabled && overlay.panelContains(mouseX, mouseY))
            return true;
        if (tabbed(gui)) return false;
        int toggleY = toggleY(gui, overlay.lastLayout);
        return mouseX >= overlay.lastLayout.toggleX()
            && mouseX < overlay.lastLayout.toggleX() + overlay.lastLayout.toggleWidth()
            && mouseY >= toggleY
            && mouseY < toggleY + 12;
    }

    private void drawPanel(GuiScreen gui, int mouseX, int mouseY) {
        InventoryOverlayLayout.Layout layout = lastLayout;
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelWidth();
        int h = layout.panelHeight();
        gui.drawRect(x, y, x + w, y + h, PANEL_BORDER);
        gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, PANEL_BACKGROUND);
        gui.drawRect(x + 1, y + 1, x + w - 1, y + 2, 0xFFFFFFFF);
        gui.drawRect(x + 1, y + 1, x + 2, y + h - 1, 0xFFFFFFFF);
        gui.drawRect(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        gui.drawRect(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);
        gui.drawRect(x + 2, y + 18, x + w - 2, y + 19, 0xFF777777);
        gui.drawRect(x + 2, y + 19, x + w - 2, y + 20, 0xFFFFFFFF);

        String title = showingRecipes || showingUsages ? (showingUsages ? "Usages" : "Recipes") : "Items";
        gui.drawCenteredString(gui.mc.fontRenderer, title, x + w / 2, y + 5, GUI_TEXT);

        int sx = x + 5;
        int sy = y + 21;
        int sw = w - 10;
        drawInsetField(gui, sx, sy, sw, 14, searchFocused);
        String shownQuery = query.isEmpty() && !searchFocused ? "Search items..." : query;
        String displayQuery = trimToWidth(gui.mc.fontRenderer, shownQuery, sw - 6);
        gui.mc.fontRenderer.drawString(displayQuery, sx + 3, sy + 3, query.isEmpty() ? 0xFF777777 : GUI_TEXT);

        hoveredPanelStack = null;
        if (showingRecipes || showingUsages) {
            drawRecipes(gui, x, y, w, h, mouseX, mouseY);
        } else {
            drawItems(gui, x, y, w, h, mouseX, mouseY);
        }
    }

    private void drawItems(GuiScreen gui, int x, int y, int w, int h, int mouseX, int mouseY) {
        int pageSize = pageSize();
        int start = itemPage;
        int end = Math.min(filteredItems.size(), start + pageSize);
        int iconLeft = x + (w - lastLayout.columns() * CELL) / 2;
        int gridTop = y + 42;
        int cols = lastLayout.columns();
        for (int i = start; i < end; i++) {
            int local = i - start;
            int cx = iconLeft + (local % cols) * CELL;
            int cy = gridTop + (local / cols) * CELL;
            ItemStack stack = filteredItems.get(i)
                .stack();
            drawCell(gui, stack, cx, cy, mouseX, mouseY);
            if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) {
                hoveredPanelStack = stack;
                drawItemTooltip(gui, stack, mouseX, mouseY);
            }
        }

        String page = pageCount() == 0 ? "0 / 0" : (itemPage / pageSize + 1) + " / " + pageCount();
        drawBeveledButton(gui, x + 4, y + h - 17, 16, 14, itemPage > 0);
        drawBeveledButton(gui, x + w - 20, y + h - 17, 16, 14, itemPage / pageSize + 1 < pageCount());
        gui.drawCenteredString(gui.mc.fontRenderer, page, x + w / 2, y + h - 13, GUI_TEXT);
        gui.mc.fontRenderer.drawString("<", x + 9, y + h - 13, itemPage > 0 ? GUI_TEXT : 0xFF777777);
        gui.mc.fontRenderer
            .drawString(">", x + w - 15, y + h - 13, itemPage / pageSize + 1 < pageCount() ? GUI_TEXT : 0xFF777777);
        if (filteredItems.isEmpty())
            gui.drawCenteredString(gui.mc.fontRenderer, "No matches", x + w / 2, gridTop + 4, 0xFFAAAAAA);
    }

    private void drawRecipes(GuiScreen gui, int x, int y, int w, int h, int mouseX, int mouseY) {
        hoveredRecipeStack = null;
        List<UniversalRecipe> recipes = currentRecipes();
        drawBeveledButton(gui, x + 4, y + 38, 16, 14, true);
        gui.mc.fontRenderer.drawString("<", x + 9, y + 41, GUI_TEXT);
        if (resultCategories.size() > 1) {
            drawBeveledButton(gui, x + w - 35, y + 38, 14, 14, categoryIndex > 0);
            drawBeveledButton(gui, x + w - 20, y + 38, 14, 14, categoryIndex + 1 < resultCategories.size());
            gui.mc.fontRenderer.drawString("<", x + w - 31, y + 41, categoryIndex > 0 ? GUI_TEXT : 0xFF777777);
            gui.mc.fontRenderer.drawString(
                ">",
                x + w - 16,
                y + 41,
                categoryIndex + 1 < resultCategories.size() ? GUI_TEXT : 0xFF777777);
        }
        if (recipes.isEmpty()) {
            gui.mc.fontRenderer.drawString("No recipes found", x + 5, y + 61, GUI_TEXT);
            return;
        }

        recipePage = clamp(recipePage, 0, recipes.size() - 1);
        UniversalRecipe recipe = recipes.get(recipePage);
        String category = trimWithEllipsis(
            gui.mc.fontRenderer,
            recipe.type()
                .category()
                .label(),
            w - 58);
        gui.drawCenteredString(gui.mc.fontRenderer, category, x + w / 2, y + 55, GUI_TEXT);
        gui.drawCenteredString(
            gui.mc.fontRenderer,
            (recipePage + 1) + " / " + recipes.size(),
            x + w / 2,
            y + 68,
            GUI_TEXT);
        int originX = centeredX(x, w, 120);
        int originY = y + 82;
        if (recipe.type() == RecipeDisplayType.CRAFTING_GRID && recipe.grid() != null
            && recipe.operationName()
                .toLowerCase(java.util.Locale.ROOT)
                .contains("shapeless")) {
            int index = 0;
            CraftingGridSlot[][] grid = recipe.grid();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    CraftingGridSlot slot = grid[row][col];
                    if (slot == null || slot.isEmpty()) continue;
                    ItemStack input = slot.getDisplayStack(Minecraft.getSystemTime());
                    int ix = originX + (index % 3) * CELL;
                    int iy = originY + (index / 3) * CELL;
                    drawCell(gui, input, ix, iy, mouseX, mouseY);
                    if (slot.alternatives()
                        .size() > 1) gui.mc.fontRenderer.drawString("+", ix + 11, iy - 1, 0xFFFFFF55);
                    if (hit(mouseX, mouseY, ix, iy, CELL)) {
                        hoveredRecipeStack = input;
                        drawItemTooltip(gui, input, mouseX, mouseY);
                    }
                    index++;
                }
            }
            gui.mc.fontRenderer.drawString("Any arrangement", originX, originY + 61, GUI_TEXT);
            gui.mc.fontRenderer.drawString("->", originX + 70, originY + 22, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 96, originY + 18, mouseX, mouseY);
            if (hit(mouseX, mouseY, originX + 96, originY + 18, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            }
        } else if (recipe.type() == RecipeDisplayType.CRAFTING_GRID && recipe.grid() != null) {
            CraftingGridSlot[][] grid = recipe.grid();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    CraftingGridSlot slot = grid[row][col];
                    ItemStack stack = slot == null ? null : slot.getDisplayStack(Minecraft.getSystemTime());
                    drawCell(gui, stack, originX + col * CELL, originY + row * CELL, mouseX, mouseY);
                    if (stack != null && hit(mouseX, mouseY, originX + col * CELL, originY + row * CELL, CELL)) {
                        hoveredRecipeStack = stack;
                        drawItemTooltip(gui, stack, mouseX, mouseY);
                    }
                }
            }
            gui.mc.fontRenderer.drawString("->", originX + 70, originY + 22, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 96, originY + 18, mouseX, mouseY);
            if (hit(mouseX, mouseY, originX + 96, originY + 18, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            }
        } else if (recipe.type() == RecipeDisplayType.FURNACE) {
            originX = centeredX(x, w, 110);
            ItemStack input = recipe.furnaceInput() == null ? null
                : recipe.furnaceInput()
                    .getDisplayStack(Minecraft.getSystemTime());
            drawCell(gui, input, originX + 4, originY + 8, mouseX, mouseY);
            drawCell(gui, recipe.machine(), originX + 35, originY + 8, mouseX, mouseY);
            gui.mc.fontRenderer.drawString("->", originX + 57, originY + 13, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 82, originY + 8, mouseX, mouseY);
            if (hit(mouseX, mouseY, originX + 4, originY + 8, CELL)) {
                hoveredRecipeStack = input;
                drawItemTooltip(gui, input, mouseX, mouseY);
            }
            if (hit(mouseX, mouseY, originX + 35, originY + 8, CELL)) {
                hoveredRecipeStack = recipe.machine();
                drawItemTooltip(gui, recipe.machine(), mouseX, mouseY);
            }
            if (hit(mouseX, mouseY, originX + 82, originY + 8, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            }
        } else if (recipe.type() == RecipeDisplayType.FURNACE_FUEL) {
            originX = centeredX(x, w, 120);
            ItemStack fuel = recipe.allInputs()
                .isEmpty() ? null
                    : recipe.allInputs()
                        .get(0);
            ItemStack machine = recipe.machine();
            drawCell(gui, fuel, originX + 4, originY + 10, mouseX, mouseY);
            gui.mc.fontRenderer.drawString("->", originX + 28, originY + 15, GUI_TEXT);
            drawCell(gui, machine, originX + 52, originY + 10, mouseX, mouseY);
            gui.mc.fontRenderer.drawString("fuel", originX + 73, originY + 15, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 94, originY + 10, mouseX, mouseY);
            if (hit(mouseX, mouseY, originX + 4, originY + 10, CELL)) {
                hoveredRecipeStack = fuel;
                drawItemTooltip(gui, fuel, mouseX, mouseY);
            } else if (hit(mouseX, mouseY, originX + 52, originY + 10, CELL)) {
                hoveredRecipeStack = machine;
                drawItemTooltip(gui, machine, mouseX, mouseY);
            } else if (hit(mouseX, mouseY, originX + 94, originY + 10, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            }
        } else if (recipe.type() == RecipeDisplayType.BREWING) {
            originX = centeredX(x, w, 140);
            List<ItemStack> inputs = recipe.allInputs();
            ItemStack potion = inputs.size() > 0 ? inputs.get(0) : null;
            ItemStack ingredient = inputs.size() > 1 ? inputs.get(1) : null;
            int inputY = originY + 8;
            drawCell(gui, potion, originX + 4, inputY, mouseX, mouseY);
            gui.mc.fontRenderer.drawString("+", originX + 25, inputY + 5, GUI_TEXT);
            drawCell(gui, ingredient, originX + 38, inputY, mouseX, mouseY);
            gui.mc.fontRenderer.drawString("->", originX + 61, inputY + 5, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 86, inputY, mouseX, mouseY);
            drawCell(gui, recipe.machine(), originX + 52, inputY + 35, mouseX, mouseY);
            gui.drawCenteredString(gui.mc.fontRenderer, "Brewing Stand", x + w / 2, inputY + 40, GUI_TEXT);
            if (hit(mouseX, mouseY, originX + 4, inputY, CELL)) {
                hoveredRecipeStack = potion;
                drawItemTooltip(gui, potion, mouseX, mouseY);
            } else if (hit(mouseX, mouseY, originX + 38, inputY, CELL)) {
                hoveredRecipeStack = ingredient;
                drawItemTooltip(gui, ingredient, mouseX, mouseY);
            } else if (hit(mouseX, mouseY, originX + 86, inputY, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            } else if (hit(mouseX, mouseY, originX + 52, inputY + 35, CELL)) {
                hoveredRecipeStack = recipe.machine();
                drawItemTooltip(gui, recipe.machine(), mouseX, mouseY);
            }
        } else {
            originX = centeredX(x, w, 110);
            int iy = originY;
            for (ItemStack input : recipe.allInputs()) {
                if (iy + CELL > y + h - 32) break;
                drawCell(gui, input, originX, iy, mouseX, mouseY);
                if (hit(mouseX, mouseY, originX, iy, CELL)) {
                    hoveredRecipeStack = input;
                    drawItemTooltip(gui, input, mouseX, mouseY);
                }
                iy += CELL + 2;
            }
            gui.mc.fontRenderer.drawString(
                trimWithEllipsis(gui.mc.fontRenderer, recipe.machineName(), w - 10),
                x + 5,
                originY + 3,
                GUI_TEXT);
            gui.mc.fontRenderer.drawString("->", originX + 26, iy, GUI_TEXT);
            drawCell(gui, recipe.output(), originX + 56, iy - 5, mouseX, mouseY);
            if (hit(mouseX, mouseY, originX + 56, iy - 5, CELL)) {
                hoveredRecipeStack = recipe.output();
                drawItemTooltip(gui, recipe.output(), mouseX, mouseY);
            }
        }

        gui.mc.fontRenderer.drawString(
            trimWithEllipsis(gui.mc.fontRenderer, recipe.operationName(), w - 10),
            x + 5,
            y + h - 31,
            GUI_TEXT);
        gui.mc.fontRenderer.drawString(
            trimWithEllipsis(gui.mc.fontRenderer, showingUsages ? "Right click: back" : "Right click: uses", w - 10),
            x + 5,
            y + h - 14,
            GUI_TEXT);
        if (recipes.size() > 1) {
            drawBeveledButton(gui, x + w - 35, y + h - 18, 14, 14, recipePage > 0);
            drawBeveledButton(gui, x + w - 20, y + h - 18, 14, 14, recipePage < recipes.size() - 1);
            gui.mc.fontRenderer.drawString("<", x + w - 31, y + h - 15, recipePage > 0 ? GUI_TEXT : 0xFF777777);
            gui.mc.fontRenderer
                .drawString(">", x + w - 16, y + h - 15, recipePage < recipes.size() - 1 ? GUI_TEXT : 0xFF777777);
        }
    }

    private void drawCell(GuiScreen gui, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        boolean hovered = hit(mouseX, mouseY, x, y, CELL);
        gui.drawRect(x, y, x + CELL, y + CELL, 0xFF373737);
        gui.drawRect(x + 1, y + 1, x + CELL - 1, y + CELL - 1, hovered ? SLOT_HOVER : SLOT_FILL);
        gui.drawRect(x + 1, y + 1, x + CELL - 1, y + 2, 0xFFFFFFFF);
        gui.drawRect(x + 1, y + 1, x + 2, y + CELL - 1, 0xFFFFFFFF);
        gui.drawRect(x + 1, y + CELL - 2, x + CELL - 1, y + CELL - 1, 0xFF8B8B8B);
        gui.drawRect(x + CELL - 2, y + 1, x + CELL - 1, y + CELL - 1, 0xFF8B8B8B);
        if (stack != null && stack.getItem() != null) {
            RenderItem renderItem = RenderItem.getInstance();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(gui.mc.fontRenderer, gui.mc.getTextureManager(), stack, x + 1, y + 1);
            renderItem.renderItemOverlayIntoGUI(gui.mc.fontRenderer, gui.mc.getTextureManager(), stack, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    private static int centeredX(int x, int panelWidth, int contentWidth) {
        return x + Math.max(5, (panelWidth - contentWidth) / 2);
    }

    private static void drawInsetField(GuiScreen gui, int x, int y, int width, int height, boolean focused) {
        gui.drawRect(x, y, x + width, y + height, 0xFF373737);
        gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, focused ? 0xFFF4F1E9 : 0xFFE8E8E8);
        gui.drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFF777777);
        gui.drawRect(x + 1, y + 1, x + 2, y + height - 1, 0xFF777777);
    }

    private static void drawBeveledButton(GuiScreen gui, int x, int y, int width, int height, boolean enabled) {
        gui.drawRect(x, y, x + width, y + height, 0xFF373737);
        gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, enabled ? 0xFFC6C6C6 : 0xFFAAAAAA);
        gui.drawRect(x + 1, y + 1, x + width - 1, y + 2, enabled ? 0xFFFFFFFF : 0xFFBBBBBB);
        gui.drawRect(x + 1, y + 1, x + 2, y + height - 1, enabled ? 0xFFFFFFFF : 0xFFBBBBBB);
        gui.drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
        gui.drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF555555);
    }

    private void drawItemTooltip(GuiScreen gui, ItemStack stack, int mouseX, int mouseY) {
        if (stack == null) return;
        pendingTooltipStack = stack;
        pendingTooltipX = mouseX;
        pendingTooltipY = mouseY;
    }

    private void renderPendingTooltip(GuiScreen gui) {
        if (pendingTooltipStack == null || gui.mc.thePlayer == null) return;
        int maxWidth = Math.max(80, lastLayout.panelWidth() - 28);
        List<String> tooltip = new ArrayList<>();
        for (String line : pendingTooltipStack.getTooltip(gui.mc.thePlayer, gui.mc.gameSettings.advancedItemTooltips)) {
            List<String> wrapped = gui.mc.fontRenderer.listFormattedStringToWidth(line, maxWidth);
            if (wrapped.isEmpty()) tooltip.add("");
            else tooltip.addAll(wrapped);
        }
        if (tooltip.isEmpty()) return;
        int tooltipWidth = 0;
        for (String line : tooltip) tooltipWidth = Math.max(tooltipWidth, gui.mc.fontRenderer.getStringWidth(line));
        int tooltipX = lastLayout.panelX() + lastLayout.panelWidth() - tooltipWidth - 18;
        tooltipX = clamp(tooltipX, lastLayout.panelX() + 4, gui.width - tooltipWidth - 4);
        int tooltipY = Math.min(pendingTooltipY, gui.height - tooltip.size() * 10 - 8);
        if (DRAW_TOOLTIP == null) return;
        try {
            DRAW_TOOLTIP.invoke(gui, tooltip, tooltipX, Math.max(4, tooltipY), gui.mc.fontRenderer);
        } catch (ReflectiveOperationException ignored) {}
    }

    /** Whether the item panel is switched on (the Items tab or the toggle hint flips it). */
    public static boolean isEnabled() {
        return INSTANCE.userEnabled;
    }

    public static void toggleEnabled() {
        INSTANCE.userEnabled = !INSTANCE.userEnabled;
    }

    /** Screens with inventory tabs switch the panel from their Items tab instead of the toggle hint. */
    private static boolean tabbed(GuiScreen gui) {
        return naturalexceptions.minecraft.gear.client.InventoryTabs.showsTabs(gui);
    }

    private void drawToggleHint(GuiScreen gui, InventoryOverlayLayout.Layout layout) {
        if (layout == null || (layout.isPanelVisible() && userEnabled) || tabbed(gui)) return;
        int y = toggleY(lastContainer, layout);
        drawBeveledButton(gui, layout.toggleX(), y, layout.toggleWidth(), 12, true);
        gui.drawCenteredString(
            gui.mc.fontRenderer,
            userEnabled ? "Items" : "Items +",
            layout.toggleX() + layout.toggleWidth() / 2,
            y + 2,
            GUI_TEXT);
    }

    private void open(ItemStack stack, boolean usages) {
        if (stack == null || stack.getItem() == null) return;
        selectedStack = stack.copy();
        showingUsages = usages;
        showingRecipes = !usages;
        recipePage = 0;
        categoryIndex = 0;
        refreshResultCategories();
        searchFocused = false;
    }

    private List<UniversalRecipe> currentRecipes() {
        if (selectedStack == null) return Collections.emptyList();
        Map<RecipeCategory, List<UniversalRecipe>> groups = resultGroups();
        if (groups.isEmpty()) return Collections.emptyList();
        if (categoryIndex >= resultCategories.size()) categoryIndex = 0;
        return groups.getOrDefault(resultCategories.get(categoryIndex), Collections.emptyList());
    }

    private Map<RecipeCategory, List<UniversalRecipe>> resultGroups() {
        if (selectedStack == null) return Collections.emptyMap();
        UniversalRecipeRegistry registry = UniversalRecipeRegistry.instance();
        List<UniversalRecipe> results = showingUsages ? registry.findUsages(selectedStack)
            : registry.findAcquisitions(selectedStack);
        return UniversalRecipeRegistry.groupByCategory(results);
    }

    private void refreshResultCategories() {
        resultCategories = new ArrayList<>(resultGroups().keySet());
        categoryIndex = clamp(categoryIndex, 0, Math.max(0, resultCategories.size() - 1));
    }

    private void changeCategory(int delta) {
        if (resultCategories.isEmpty()) refreshResultCategories();
        if (resultCategories.isEmpty()) return;
        categoryIndex = (categoryIndex + delta + resultCategories.size()) % resultCategories.size();
        recipePage = 0;
    }

    private int recipeCount() {
        return Math.max(1, currentRecipes().size());
    }

    private ItemStack itemAt(int x, int y) {
        if (lastLayout == null || lastLayout.columns() < 1) return null;
        int index = InventoryOverlayInput.itemIndexAt(
            lastLayout.panelX() + (lastLayout.panelWidth() - lastLayout.columns() * CELL) / 2 - 5,
            lastLayout.panelY(),
            lastLayout.columns(),
            lastLayout.rows(),
            itemPage,
            x,
            y);
        if (index < 0 || index >= filteredItems.size()) return null;
        return filteredItems.get(index)
            .stack();
    }

    private int pageSize() {
        return Math.max(1, lastLayout == null ? 1 : lastLayout.columns() * lastLayout.rows());
    }

    private int pageCount() {
        int size = pageSize();
        return filteredItems.isEmpty() ? 0 : (filteredItems.size() + size - 1) / size;
    }

    private void refreshItems() {
        filteredItems = GameItemRegistry.instance()
            .search(query);
        itemPage = 0;
    }

    private void resetSearch() {
        refreshItems();
    }

    private ItemStack hoveredContainerStack() {
        if (lastContainer == null || lastMouseX < 0 || lastMouseY < 0) return null;
        try {
            for (Object slotObject : lastContainer.inventorySlots.inventorySlots) {
                net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) slotObject;
                int left = get(GUI_LEFT, lastContainer, Math.max(0, (lastContainer.width - 176) / 2));
                int top = get(GUI_TOP, lastContainer, Math.max(0, (lastContainer.height - 166) / 2));
                if (slot.getHasStack() && lastMouseX >= left + slot.xDisplayPosition
                    && lastMouseX < left + slot.xDisplayPosition + 16
                    && lastMouseY >= top + slot.yDisplayPosition
                    && lastMouseY < top + slot.yDisplayPosition + 16)
                    return slot.getStack()
                        .copy();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private boolean panelContains(int x, int y) {
        return lastLayout != null && lastLayout.isPanelVisible()
            && userEnabled
            && x >= lastLayout.panelX()
            && x < lastLayout.panelX() + lastLayout.panelWidth()
            && y >= lastLayout.panelY()
            && y < lastLayout.panelY() + lastLayout.panelHeight();
    }

    private boolean inBackButton(int x, int y) {
        return lastLayout != null && x >= lastLayout.panelX() + 4
            && x < lastLayout.panelX() + 20
            && y >= lastLayout.panelY() + 38
            && y < lastLayout.panelY() + 56;
    }

    private boolean inCategoryPrevious(int x, int y) {
        return lastLayout != null && resultCategories.size() > 1
            && x >= lastLayout.panelX() + lastLayout.panelWidth() - 35
            && x < lastLayout.panelX() + lastLayout.panelWidth() - 21
            && y >= lastLayout.panelY() + 37
            && y < lastLayout.panelY() + 55;
    }

    private boolean inCategoryNext(int x, int y) {
        return lastLayout != null && resultCategories.size() > 1
            && x >= lastLayout.panelX() + lastLayout.panelWidth() - 20
            && x < lastLayout.panelX() + lastLayout.panelWidth() - 6
            && y >= lastLayout.panelY() + 37
            && y < lastLayout.panelY() + 55;
    }

    private boolean inRecipePrevious(int x, int y) {
        return lastLayout != null && x >= lastLayout.panelX() + lastLayout.panelWidth() - 35
            && x < lastLayout.panelX() + lastLayout.panelWidth() - 21
            && y >= lastLayout.panelY() + lastLayout.panelHeight() - 18
            && y < lastLayout.panelY() + lastLayout.panelHeight() - 2;
    }

    private boolean inRecipeNext(int x, int y) {
        return lastLayout != null && x >= lastLayout.panelX() + lastLayout.panelWidth() - 20
            && x < lastLayout.panelX() + lastLayout.panelWidth() - 6
            && y >= lastLayout.panelY() + lastLayout.panelHeight() - 18
            && y < lastLayout.panelY() + lastLayout.panelHeight() - 2;
    }

    private static int toggleY(GuiContainer gui, InventoryOverlayLayout.Layout layout) {
        int top = gui == null ? 18 : get(GUI_TOP, gui, Math.max(0, (gui.height - 166) / 2));
        return clamp(top - 14, 2, 24);
    }

    private static boolean hit(int mouseX, int mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    private static int scaledMouseX(Minecraft mc) {
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return Mouse.getX() * res.getScaledWidth() / mc.displayWidth;
    }

    private static int scaledMouseY(Minecraft mc) {
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        return res.getScaledHeight() - Mouse.getY() * res.getScaledHeight() / mc.displayHeight - 1;
    }

    private static Field field(String... names) {
        for (String name : names) {
            try {
                Field field = GuiContainer.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException | SecurityException ignored) {}
        }
        return null;
    }

    private static Method tooltipMethod() {
        try {
            Method method = GuiScreen.class.getDeclaredMethod(
                "drawHoveringText",
                List.class,
                int.class,
                int.class,
                net.minecraft.client.gui.FontRenderer.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private static int get(Field field, GuiContainer gui, int fallback) {
        if (field == null) return fallback;
        try {
            return field.getInt(gui);
        } catch (IllegalAccessException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trimToWidth(net.minecraft.client.gui.FontRenderer font, String value, int width) {
        String result = value == null ? "" : value;
        while (!result.isEmpty() && font.getStringWidth(result) > width)
            result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String trimWithEllipsis(net.minecraft.client.gui.FontRenderer font, String value, int width) {
        String text = value == null ? "" : value;
        if (font.getStringWidth(text) <= width) return text;
        String ellipsis = "...";
        return trimToWidth(font, text, Math.max(0, width - font.getStringWidth(ellipsis))) + ellipsis;
    }

    private void clearScreenState() {
        lastContainer = null;
        lastLayout = null;
        hoveredPanelStack = null;
        searchFocused = false;
        showingRecipes = false;
        showingUsages = false;
        selectedStack = null;
    }

}
