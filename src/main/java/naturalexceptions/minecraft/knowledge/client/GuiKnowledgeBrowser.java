package naturalexceptions.minecraft.knowledge.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;
import naturalexceptions.minecraft.knowledge.recipe.CraftingGridSlot;
import naturalexceptions.minecraft.knowledge.recipe.GameItemRegistry;
import naturalexceptions.minecraft.knowledge.recipe.RecipeDisplayType;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipe;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipeRegistry;
import naturalexceptions.minecraft.multiblock.MultiblockDefinition;
import naturalexceptions.minecraft.multiblock.MultiblockPattern;
import naturalexceptions.minecraft.multiblock.MultiblockRole;

/**
 * Universal in-game recipe and knowledge browser.
 * Combines JEI/NEI-style automatic recipe discovery across all loaded mods
 * with rich authoritative Natural Exceptions metallurgical intelligence.
 */
@SideOnly(Side.CLIENT)
public final class GuiKnowledgeBrowser extends GuiScreen {

    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 230;

    // Tabs
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_RECIPES = 1;
    public static final int TAB_USAGES = 2;
    public static final int TAB_STRUCTURE = 3;

    private int activeTab = TAB_OVERVIEW;

    private GuiTextField searchField;
    private final Deque<ItemStack> navigationHistory = new ArrayDeque<>();
    private List<GameItemRegistry.IndexedItem> filteredItems = new ArrayList<>();
    private ItemStack activeStack;
    private KnowledgeEntry activeKnowledgeEntry;

    private List<UniversalRecipe> activeRecipes = new ArrayList<>();
    private List<UniversalRecipe> activeUsages = new ArrayList<>();
    private int currentRecipePage = 0;
    private int currentUsagePage = 0;

    private int listPage = 0;
    private static final int ITEMS_PER_PAGE = 7;

    // Scrolling in right content pane
    private int scrollY = 0;
    private int maxScrollY = 0;
    private ItemStack pendingTooltip;

    // Multiblock layer selector
    private int multiblockLayerIndex = 0;

    // Filter modes for left list
    private int filterMode = 0; // 0=All Items, 1=Natural Exceptions, 2=Minecraft, 3=Rich NE Topics
    private static final String[] FILTER_LABELS = { "All Items", "@naturalexceptions", "@minecraft", "NE Topics" };

    public GuiKnowledgeBrowser() {
        this(null, false, null);
    }

    public GuiKnowledgeBrowser(ItemStack targetStack, boolean isUsageLookup, String initialEntryId) {
        KnowledgeRegistry.instance()
            .init();
        UniversalRecipeRegistry.instance()
            .init();
        GameItemRegistry.instance()
            .init();

        if (targetStack != null) {
            this.activeStack = targetStack.copy();
            this.activeTab = isUsageLookup ? TAB_USAGES : TAB_RECIPES;
        } else if (initialEntryId != null) {
            this.activeKnowledgeEntry = KnowledgeRegistry.instance()
                .getEntry(initialEntryId);
            if (activeKnowledgeEntry != null && activeKnowledgeEntry.iconStack() != null) {
                this.activeStack = activeKnowledgeEntry.iconStack();
            }
            this.activeTab = TAB_OVERVIEW;
        }
    }

    public static GuiKnowledgeBrowser forAcquisitions(ItemStack stack) {
        return new GuiKnowledgeBrowser(stack, false, null);
    }

    public static GuiKnowledgeBrowser forUsages(ItemStack stack) {
        return new GuiKnowledgeBrowser(stack, true, null);
    }

    public static GuiKnowledgeBrowser forEntry(String entryId) {
        return new GuiKnowledgeBrowser(null, false, entryId);
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        this.searchField = new GuiTextField(this.fontRendererObj, guiLeft + 8, guiTop + 8, 114, 14);
        this.searchField.setMaxStringLength(35);
        this.searchField.setEnableBackgroundDrawing(true);
        this.searchField.setTextColor(0xFFFFFF);

        updateFilteredItems();

        if (activeStack == null && !filteredItems.isEmpty()) {
            activeStack = filteredItems.get(0)
                .stack();
        }

        refreshActiveStackData();
        rebuildButtons();
    }

    private void refreshActiveStackData() {
        if (activeStack != null) {
            activeRecipes = UniversalRecipeRegistry.instance()
                .findAcquisitions(activeStack);
            activeUsages = UniversalRecipeRegistry.instance()
                .findUsages(activeStack);

            // Check if there is an authored KnowledgeEntry matching this item
            activeKnowledgeEntry = null;
            for (KnowledgeEntry e : KnowledgeRegistry.instance()
                .getAllEntries()) {
                if (ProcessStep.isMatching(e.iconStack(), activeStack)) {
                    activeKnowledgeEntry = e;
                    break;
                }
            }
        } else {
            activeRecipes = Collections.emptyList();
            activeUsages = Collections.emptyList();
            activeKnowledgeEntry = null;
        }

        currentRecipePage = 0;
        currentUsagePage = 0;
        scrollY = 0;
    }

    private void rebuildButtons() {
        this.buttonList.clear();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // Filter button
        this.buttonList.add(new GuiButton(1, guiLeft + 8, guiTop + 24, 114, 14, FILTER_LABELS[filterMode]));

        // Paging for item list
        this.buttonList.add(new GuiButton(10, guiLeft + 8, guiTop + 208, 20, 14, "<"));
        this.buttonList.add(new GuiButton(11, guiLeft + 102, guiTop + 208, 20, 14, ">"));

        // Header navigation buttons
        this.buttonList.add(new GuiButton(20, guiLeft + 300, guiTop + 6, 45, 14, "< Back"));
        this.buttonList.add(new GuiButton(21, guiLeft + 350, guiTop + 6, 20, 14, "X"));

        // Tab buttons
        int tabX = guiLeft + 132;
        int tabY = guiTop + 30;

        String acqLabel = "Obtain (" + activeRecipes.size() + ")";
        String useLabel = "Usages (" + activeUsages.size() + ")";

        this.buttonList.add(new GuiButton(30, tabX, tabY, 54, 14, "Overview"));
        this.buttonList.add(new GuiButton(31, tabX + 56, tabY, 64, 14, acqLabel));
        this.buttonList.add(new GuiButton(32, tabX + 122, tabY, 62, 14, useLabel));

        if (activeKnowledgeEntry != null && activeKnowledgeEntry.hasMultiblock()) {
            this.buttonList.add(new GuiButton(33, tabX + 186, tabY, 56, 14, "Structure"));
        }

        // Paging controls for Recipes and Usages
        if (activeTab == TAB_RECIPES && activeRecipes.size() > 1) {
            this.buttonList.add(new GuiButton(40, guiLeft + 134, guiTop + 208, 20, 14, "<"));
            this.buttonList.add(new GuiButton(41, guiLeft + 348, guiTop + 208, 20, 14, ">"));
        } else if (activeTab == TAB_USAGES && activeUsages.size() > 1) {
            this.buttonList.add(new GuiButton(40, guiLeft + 134, guiTop + 208, 20, 14, "<"));
            this.buttonList.add(new GuiButton(41, guiLeft + 348, guiTop + 208, 20, 14, ">"));
        }

        // Multiblock layer selector buttons
        if (activeTab == TAB_STRUCTURE && activeKnowledgeEntry != null && activeKnowledgeEntry.hasMultiblock()) {
            this.buttonList.add(new GuiButton(50, guiLeft + 134, guiTop + 48, 20, 14, "<"));
            this.buttonList.add(new GuiButton(51, guiLeft + 348, guiTop + 48, 20, 14, ">"));
        }
    }

    private void updateFilteredItems() {
        String query = searchField != null ? searchField.getText()
            .trim() : "";

        if (filterMode == 3) { // NE Topics
            List<KnowledgeEntry> entries = KnowledgeRegistry.instance()
                .search(query);
            List<GameItemRegistry.IndexedItem> list = new ArrayList<>();
            for (KnowledgeEntry e : entries) {
                // Do not turn the browser into an omniscient catalog of undiscovered NE topics.
                if (e.iconStack() != null && hasLearnedEntry(e)) {
                    list.add(new GameItemRegistry.IndexedItem(e.iconStack(), e.title(), "naturalexceptions", e.id()));
                }
            }
            filteredItems = list;
        } else {
            String effectiveQuery = query;
            if (filterMode == 1 && !query.contains("@")) {
                effectiveQuery = "@naturalexceptions " + query;
            } else if (filterMode == 2 && !query.contains("@")) {
                effectiveQuery = "@minecraft " + query;
            }
            filteredItems = GameItemRegistry.instance()
                .search(effectiveQuery);
        }

        int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
        if (listPage > maxPage) listPage = maxPage;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        switch (button.id) {
            case 1: // Cycle filter
                filterMode = (filterMode + 1) % FILTER_LABELS.length;
                listPage = 0;
                updateFilteredItems();
                rebuildButtons();
                break;
            case 10: // Item list page prev
                if (listPage > 0) listPage--;
                break;
            case 11: // Item list page next
                int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
                if (listPage < maxPage) listPage++;
                break;
            case 20: // Back
                if (!navigationHistory.isEmpty()) {
                    selectStack(navigationHistory.pop(), false);
                }
                break;
            case 21: // Close
                this.mc.displayGuiScreen(null);
                break;
            case 30: // Overview tab
                activeTab = TAB_OVERVIEW;
                scrollY = 0;
                rebuildButtons();
                break;
            case 31: // Recipes tab
                activeTab = TAB_RECIPES;
                currentRecipePage = 0;
                scrollY = 0;
                rebuildButtons();
                break;
            case 32: // Usages tab
                activeTab = TAB_USAGES;
                currentUsagePage = 0;
                scrollY = 0;
                rebuildButtons();
                break;
            case 33: // Structure tab
                activeTab = TAB_STRUCTURE;
                multiblockLayerIndex = 0;
                scrollY = 0;
                rebuildButtons();
                break;
            case 40: // Recipe/Usage prev
                if (activeTab == TAB_RECIPES && currentRecipePage > 0) currentRecipePage--;
                if (activeTab == TAB_USAGES && currentUsagePage > 0) currentUsagePage--;
                scrollY = 0;
                break;
            case 41: // Recipe/Usage next
                if (activeTab == TAB_RECIPES && currentRecipePage < activeRecipes.size() - 1) currentRecipePage++;
                if (activeTab == TAB_USAGES && currentUsagePage < activeUsages.size() - 1) currentUsagePage++;
                scrollY = 0;
                break;
            case 50: // Multiblock layer prev
                if (multiblockLayerIndex > 0) multiblockLayerIndex--;
                break;
            case 51: // Multiblock layer next
                if (activeKnowledgeEntry != null && activeKnowledgeEntry.hasMultiblock()) {
                    int layerCount = activeKnowledgeEntry.multiblock()
                        .getLayerNumbers()
                        .size();
                    if (multiblockLayerIndex < layerCount - 1) multiblockLayerIndex++;
                }
                break;
        }
    }

    public void selectStack(ItemStack stack, boolean pushHistory) {
        if (stack == null) return;
        if (pushHistory && activeStack != null) {
            navigationHistory.push(activeStack.copy());
        }
        this.activeStack = stack.copy();
        refreshActiveStackData();

        // If currently in Recipes/Usages and none exist, fallback to Overview
        if (activeTab == TAB_RECIPES && activeRecipes.isEmpty() && !activeUsages.isEmpty()) {
            activeTab = TAB_USAGES;
        } else if (activeTab == TAB_USAGES && activeUsages.isEmpty() && !activeRecipes.isEmpty()) {
            activeTab = TAB_RECIPES;
        }

        rebuildButtons();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            int guiLeft = (this.width - GUI_WIDTH) / 2;
            int guiTop = (this.height - GUI_HEIGHT) / 2;
            int direction = wheel > 0 ? -1 : 1;
            if (mouseX >= guiLeft && mouseX <= guiLeft + 126 && mouseY >= guiTop + 42 && mouseY <= guiTop + 203) {
                int maxPage = Math.max(0, (filteredItems.size() - 1) / ITEMS_PER_PAGE);
                listPage = Math.max(0, Math.min(maxPage, listPage + direction));
            } else if (mouseX >= guiLeft + 130 && mouseY >= guiTop + 48 && mouseY <= guiTop + 204) {
                scrollY = TextLayoutHelper.clampScroll(scrollY + direction * 16, maxScrollY);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this.searchField != null && this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            this.listPage = 0;
            updateFilteredItems();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }

        // R and U keys
        if (keyCode == Keyboard.KEY_R) {
            activeTab = TAB_RECIPES;
            currentRecipePage = 0;
            scrollY = 0;
            rebuildButtons();
            return;
        } else if (keyCode == Keyboard.KEY_U) {
            activeTab = TAB_USAGES;
            currentUsagePage = 0;
            scrollY = 0;
            rebuildButtons();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, button);
        }

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // Left Item List clicks
        int listX = guiLeft + 8;
        int listY = guiTop + 42;
        int listWidth = 114;
        int itemHeight = 23;

        int startIndex = listPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int rowY = listY + (i - startIndex) * itemHeight;
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= rowY && mouseY < rowY + itemHeight) {
                ItemStack clicked = filteredItems.get(i)
                    .stack();
                if (button == 1) { // Right-click = Usages
                    selectStack(clicked, true);
                    activeTab = TAB_USAGES;
                    rebuildButtons();
                } else { // Left-click = Recipes/Overview
                    selectStack(clicked, true);
                }
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        // Background
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xEE1E1E1E);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + GUI_WIDTH - 1, guiTop + GUI_HEIGHT - 1, 0xEE2A2A2A);

        // Divider
        drawRect(guiLeft + 128, guiTop + 4, guiLeft + 130, guiTop + GUI_HEIGHT - 4, 0xFF444444);

        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }

        drawLeftItemList(guiLeft, guiTop, mouseX, mouseY);

        if (activeStack != null) {
            drawHeader(guiLeft, guiTop);

            int contentLeft = guiLeft + 134;
            int contentTop = guiTop + 48;
            int contentWidth = 238;
            int contentHeight = 156;

            pendingTooltip = null;
            startScissor(contentLeft, contentTop, contentWidth, contentHeight);

            switch (activeTab) {
                case TAB_OVERVIEW:
                    drawOverviewTab(contentLeft, contentTop - scrollY, contentWidth, mouseX, mouseY);
                    break;
                case TAB_RECIPES:
                    drawRecipesTab(
                        contentLeft,
                        contentTop - scrollY,
                        contentWidth,
                        activeRecipes,
                        currentRecipePage,
                        "Recipe",
                        mouseX,
                        mouseY);
                    break;
                case TAB_USAGES:
                    drawRecipesTab(
                        contentLeft,
                        contentTop - scrollY,
                        contentWidth,
                        activeUsages,
                        currentUsagePage,
                        "Usage",
                        mouseX,
                        mouseY);
                    break;
                case TAB_STRUCTURE:
                    drawStructureTab(contentLeft, contentTop - scrollY, contentWidth, mouseX, mouseY);
                    break;
            }

            endScissor();
            if (pendingTooltip != null) renderToolTip(pendingTooltip, mouseX, mouseY);

            // Draw scrollbar if content exceeds pane
            if (maxScrollY > 0) {
                int trackHeight = contentHeight;
                int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + maxScrollY));
                int thumbY = contentTop + (scrollY * (trackHeight - thumbHeight)) / maxScrollY;
                drawRect(
                    contentLeft + contentWidth + 1,
                    contentTop,
                    contentLeft + contentWidth + 3,
                    contentTop + trackHeight,
                    0xFF333333);
                drawRect(
                    contentLeft + contentWidth + 1,
                    thumbY,
                    contentLeft + contentWidth + 3,
                    thumbY + thumbHeight,
                    0xFFAAAAAA);
            }
            drawRecipePageIndicator(guiLeft, guiTop);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawLeftItemList(int guiLeft, int guiTop, int mouseX, int mouseY) {
        int listX = guiLeft + 8;
        int listY = guiTop + 42;
        int listWidth = 114;
        int itemHeight = 23;

        int startIndex = listPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            GameItemRegistry.IndexedItem item = filteredItems.get(i);
            int rowY = listY + (i - startIndex) * itemHeight;

            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth
                && mouseY >= rowY
                && mouseY < rowY + itemHeight;
            boolean isSelected = activeStack != null && ProcessStep.isMatching(item.stack(), activeStack);

            int bgColor = isSelected ? 0xFF3F3F5F : (isHovered ? 0xFF333333 : 0xFF222222);
            drawRect(listX, rowY, listX + listWidth, rowY + itemHeight - 1, bgColor);

            renderItemStack(item.stack(), listX + 2, rowY + 3);

            String title = item.displayName();
            if (fontRendererObj.getStringWidth(title) > 88) {
                title = fontRendererObj.trimStringToWidth(title, 84) + "..";
            }
            int textColor = isSelected ? 0xFFFF55 : 0xFFFFFF;
            fontRendererObj.drawString(title, listX + 22, rowY + 3, textColor);
            if (isHovered) {
                drawHoveringText(
                    java.util.Arrays.asList(item.displayName(), "\u00a78@" + item.modId()),
                    mouseX,
                    mouseY,
                    fontRendererObj);
            }
        }

        int totalPages = Math.max(1, (filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        String pageStr = (listPage + 1) + " / " + totalPages;
        int strWidth = fontRendererObj.getStringWidth(pageStr);
        fontRendererObj.drawString(pageStr, guiLeft + 8 + (114 - strWidth) / 2, guiTop + 211, 0xAAAAAA);
    }

    private void drawHeader(int guiLeft, int guiTop) {
        int headerX = guiLeft + 134;
        int headerY = guiTop + 8;

        renderItemStack(activeStack, headerX, headerY);

        String title = activeKnowledgeEntry != null ? activeKnowledgeEntry.title() : activeStack.getDisplayName();
        if (activeKnowledgeEntry != null && !hasLearnedEntry(activeKnowledgeEntry)) {
            title = isMaterialEntry(activeKnowledgeEntry) ? "Material record" : "Unrecorded topic";
        } else if (isMaterialEntry(activeKnowledgeEntry) && !hasLearnedMaterialTitle(activeKnowledgeEntry)) {
            title = "Material record";
        } else if (isMaterialEntry(activeKnowledgeEntry)) {
            String label = learnedMaterialLabel(activeKnowledgeEntry);
            if (label != null) title = label;
        }
        if (fontRendererObj.getStringWidth(title) > 130) {
            title = fontRendererObj.trimStringToWidth(title, 125) + "..";
        }
        fontRendererObj.drawString("\u00a7l" + title, headerX + 22, headerY + 1, 0xFFFFAA);

        String modName = activeKnowledgeEntry == null ? "Item Details"
            : hasLearnedEntry(activeKnowledgeEntry) ? "Personal knowledge" : "No notes recorded";
        fontRendererObj.drawString("\u00a78" + modName, headerX + 22, headerY + 11, 0xAAAAAA);

        if (activeKnowledgeEntry != null && activeKnowledgeEntry.isExperimental()) {
            drawRect(guiLeft + 230, headerY, guiLeft + 295, headerY + 12, 0xCCAA0000);
            fontRendererObj.drawString("\u00a7lEXPERIMENTAL", guiLeft + 233, headerY + 2, 0xFFFFFF);
        }
    }

    private void drawOverviewTab(int x, int y, int width, int mouseX, int mouseY) {
        int curY = y;

        if (activeKnowledgeEntry != null) {
            if (isMaterialEntry(activeKnowledgeEntry)) {
                drawLearnedMaterialFacts(activeKnowledgeEntry, x, curY, width);
            } else {
                drawLearnedEntryFacts(activeKnowledgeEntry, x, curY, width);
            }
        } else {
            // Vanilla or mod item overview
            curY = drawWrapped("\u00a76" + activeStack.getDisplayName(), x + 2, curY, width - 4, 0xFFFF77);
            curY = drawWrapped(
                "\u00a77Registered Item: \u00a7f" + activeStack.getItem()
                    .getUnlocalizedName(),
                x + 2,
                curY,
                width - 4,
                0xAAAAAA);
            curY = drawWrapped(
                "\u00a77Stack Size: \u00a7f" + activeStack.getMaxStackSize(),
                x + 2,
                curY,
                width - 4,
                0xAAAAAA);

            if (activeStack.isItemStackDamageable()) {
                curY = drawWrapped(
                    "\u00a77Max Durability: \u00a7f" + activeStack.getMaxDamage(),
                    x + 2,
                    curY,
                    width - 4,
                    0xAAAAAA);
            }

            curY += 6;
            curY = drawWrapped("\u00a7eCrafting & Usage Routes:\u00a7r", x + 2, curY, width - 4, 0xFFFF77);
            curY = drawWrapped(
                "\u2022 " + activeRecipes.size() + " way(s) to craft/obtain this item.",
                x + 6,
                curY,
                width - 8,
                0xDDDDDD);
            curY = drawWrapped(
                "\u2022 " + activeUsages.size() + " recipe(s) consume this item.",
                x + 6,
                curY,
                width - 8,
                0xDDDDDD);
            curY += 4;
        }

        maxScrollY = Math.max(0, (curY - y) - 156);
    }

    private boolean isMaterialEntry(KnowledgeEntry entry) {
        return entry != null && entry.category() == naturalexceptions.minecraft.knowledge.KnowledgeCategory.MATERIALS;
    }

    private naturalexceptions.core.knowledge.KnowledgeSubjectRef materialSubject(KnowledgeEntry entry) {
        if (entry == null) return null;
        String path = entry.id().startsWith("material_") ? entry.id().substring("material_".length()) : "";
        if (path.isEmpty()) return null;
        return naturalexceptions.core.knowledge.KnowledgeSubjectRef.material(
            naturalexceptions.core.material.MaterialRef.authored("naturalexceptions", path));
    }

    private boolean hasLearnedMaterialTitle(KnowledgeEntry entry) {
        naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = materialSubject(entry);
        if (subject == null || mc.thePlayer == null) return false;
        naturalexceptions.core.knowledge.PlayerKnowledge knowledge =
            naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties.get(mc.thePlayer).knowledge();
        return knowledge.aliasFor(subject) != null || knowledge.bestPrecision(subject) != null;
    }

    private naturalexceptions.core.knowledge.KnowledgeSubjectRef entrySubject(KnowledgeEntry entry) {
        if (entry == null) return null;
        return naturalexceptions.core.knowledge.KnowledgeSubjectRef.parse("entry|naturalexceptions:" + entry.id());
    }

    private boolean hasLearnedEntry(KnowledgeEntry entry) {
        if (isMaterialEntry(entry)) return hasLearnedMaterialTitle(entry);
        naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = entrySubject(entry);
        return subject != null && mc.thePlayer != null && !naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties
            .get(mc.thePlayer).knowledge().evidenceFor(subject).isEmpty();
    }

    private void drawLearnedEntryFacts(KnowledgeEntry entry, int x, int y, int width) {
        naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = entrySubject(entry);
        java.util.List<naturalexceptions.core.knowledge.KnowledgeEvidence> evidence = subject == null || mc.thePlayer == null
            ? java.util.Collections.<naturalexceptions.core.knowledge.KnowledgeEvidence>emptyList()
            : naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties.get(mc.thePlayer).knowledge().evidenceFor(subject);
        if (evidence.isEmpty()) {
            drawWrapped("No documented information recorded. Read a source or investigate this process to add evidence.",
                x + 2, y, width - 4, 0xDDDDDD);
            return;
        }
        int curY = y;
        for (naturalexceptions.core.knowledge.KnowledgeEvidence fact : evidence) {
            curY = drawWrapped("\u00a7e" + fact.facet() + "\u00a7r: " + fact.statement(), x + 2, curY,
                width - 4, 0xDDDDDD) + 2;
            curY = drawWrapped("\u00a78Source: " + fact.provenance().name().toLowerCase(Locale.ROOT)
                + " / precision: " + fact.precision().name().toLowerCase(Locale.ROOT), x + 6, curY,
                width - 8, 0xAAAAAA) + 4;
        }
    }

    private String learnedMaterialLabel(KnowledgeEntry entry) {
        naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = materialSubject(entry);
        if (subject == null || mc.thePlayer == null) return null;
        java.util.List<naturalexceptions.core.knowledge.KnowledgeEvidence> evidence =
            naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties.get(mc.thePlayer).knowledge().evidenceFor(subject);
        for (naturalexceptions.core.knowledge.KnowledgeEvidence fact : evidence) {
            if ("classification".equals(fact.facet())) return fact.statement();
        }
        return naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties.get(mc.thePlayer).knowledge().aliasFor(subject);
    }

    private void drawLearnedMaterialFacts(KnowledgeEntry entry, int x, int y, int width) {
        naturalexceptions.core.knowledge.KnowledgeSubjectRef subject = materialSubject(entry);
        java.util.List<naturalexceptions.core.knowledge.KnowledgeEvidence> evidence = subject == null || mc.thePlayer == null
            ? java.util.Collections.<naturalexceptions.core.knowledge.KnowledgeEvidence>emptyList()
            : naturalexceptions.minecraft.knowledge.PlayerKnowledgeProperties.get(mc.thePlayer).knowledge().evidenceFor(subject);
        int curY = y;
        if (evidence.isEmpty()) {
            drawWrapped("No documented information recorded. Inspect a sample or read a field source to add evidence.",
                x + 2, curY, width - 4, 0xDDDDDD);
            return;
        }
        for (naturalexceptions.core.knowledge.KnowledgeEvidence fact : evidence) {
            curY = drawWrapped("\u00a7e" + fact.facet() + "\u00a7r: " + fact.statement(), x + 2, curY,
                width - 4, 0xDDDDDD) + 2;
            curY = drawWrapped("\u00a78Source: " + fact.provenance().name().toLowerCase(Locale.ROOT)
                + " / precision: " + fact.precision().name().toLowerCase(Locale.ROOT), x + 6, curY,
                width - 8, 0xAAAAAA) + 4;
        }
    }

    private void drawRecipesTab(int x, int y, int width, List<UniversalRecipe> recipes, int pageIndex, String modeLabel,
        int mouseX, int mouseY) {
        int startY = y;
        int curY = y;

        if (recipes == null || recipes.isEmpty()) {
            fontRendererObj.drawString(
                "\u00a77No recorded " + modeLabel.toLowerCase(Locale.ROOT) + " recipes.",
                x + 4,
                curY + 20,
                0x888888);
            maxScrollY = 0;
            return;
        }

        UniversalRecipe recipe = recipes.get(Math.min(pageIndex, recipes.size() - 1));

        // Header: Operation + Station
        renderItemStack(recipe.machine(), x + 2, curY);
        int headerTextY = drawWrapped("\u00a7e" + recipe.operationName(), x + 22, curY + 1, width - 26, 0xFFFF55);
        headerTextY = drawWrapped("\u00a78Station: " + recipe.machineName(), x + 22, headerTextY, width - 26, 0xAAAAAA);
        curY = Math.max(curY + 22, headerTextY + 2);

        // Render recipe based on type
        if (recipe.type() == RecipeDisplayType.CRAFTING_GRID) {
            drawCraftingGrid(x + 10, curY, recipe, mouseX, mouseY);
            curY += 64;
        } else if (recipe.type() == RecipeDisplayType.FURNACE) {
            drawFurnaceRecipe(x + 10, curY, recipe, mouseX, mouseY);
            curY += 56;
        } else {
            drawProcessRecipe(x + 6, curY, recipe, mouseX, mouseY);
            curY += 48;
        }

        // Notes and conditions wrapped
        if (!recipe.notes()
            .isEmpty()) {
            fontRendererObj.drawString("\u00a76Recipe Notes:\u00a7r", x + 4, curY, 0xFFAA00);
            curY += 10;

            for (String note : recipe.notes()) {
                List<String> noteLines = TextLayoutHelper.wrap(fontRendererObj, note, width - 12);
                for (String nl : noteLines) {
                    fontRendererObj.drawString("\u00a77\u2022 \u00a7f" + nl, x + 8, curY, 0xDDDDDD);
                    curY += 9;
                }
            }
        }

        maxScrollY = Math.max(0, (curY - startY) - 156);

    }

    private void drawRecipePageIndicator(int guiLeft, int guiTop) {
        List<UniversalRecipe> recipes = activeTab == TAB_RECIPES ? activeRecipes : activeUsages;
        int page = activeTab == TAB_RECIPES ? currentRecipePage : currentUsagePage;
        if (activeTab != TAB_RECIPES && activeTab != TAB_USAGES || recipes.size() < 2) return;
        String label = (page + 1) + " / " + recipes.size();
        int labelWidth = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, guiLeft + 134 + (234 - labelWidth) / 2, guiTop + 211, 0xAAAAAA);
    }

    private void drawCraftingGrid(int x, int y, UniversalRecipe recipe, int mouseX, int mouseY) {
        long timeMs = Minecraft.getSystemTime();

        // Draw 3x3 slots
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slotX = x + c * 19;
                int slotY = y + r * 19;

                drawRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF373737);
                drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF1E1E1E);

                CraftingGridSlot slot = recipe.grid() != null ? recipe.grid()[r][c] : null;
                if (slot != null && !slot.isEmpty()) {
                    ItemStack display = slot.getDisplayStack(timeMs);
                    if (display != null) {
                        renderItemStack(display, slotX + 1, slotY + 1);
                        if (slot.alternatives()
                            .size() > 1) {
                            fontRendererObj.drawStringWithShadow("\u00a7e+", slotX + 11, slotY + 1, 0xFFFF55);
                        }
                        if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                            pendingTooltip = display;
                        }
                    }
                }
            }
        }

        // Draw crafting arrow
        fontRendererObj.drawString("\u00a76==>\u00a7r", x + 64, y + 23, 0xFFAA00);

        // Draw output slot
        int outX = x + 96;
        int outY = y + 18;
        drawRect(outX - 1, outY - 1, outX + 23, outY + 23, 0xFFFFD700);
        drawRect(outX, outY, outX + 22, outY + 22, 0xFF1E1E1E);

        if (recipe.output() != null) {
            renderItemStack(recipe.output(), outX + 3, outY + 3);
            if (mouseX >= outX && mouseX <= outX + 22 && mouseY >= outY && mouseY <= outY + 22) {
                pendingTooltip = recipe.output();
            }
        }
    }

    private void drawFurnaceRecipe(int x, int y, UniversalRecipe recipe, int mouseX, int mouseY) {
        // Input slot
        int inX = x + 10;
        int inY = y + 4;
        drawRect(inX, inY, inX + 18, inY + 18, 0xFF373737);
        drawRect(inX + 1, inY + 1, inX + 17, inY + 17, 0xFF1E1E1E);

        if (recipe.furnaceInput() != null) {
            ItemStack in = recipe.furnaceInput()
                .getDisplayStack(Minecraft.getSystemTime());
            if (in != null) {
                renderItemStack(in, inX + 1, inY + 1);
                if (mouseX >= inX && mouseX <= inX + 18 && mouseY >= inY && mouseY <= inY + 18) {
                    pendingTooltip = in;
                }
            }
        }

        // Flame and arrow
        fontRendererObj.drawString("\u00a7c\u2668\u00a7r", inX + 5, inY + 22, 0xFF5555);
        fontRendererObj.drawString("\u00a76====>\u00a7r", x + 38, y + 14, 0xFFAA00);

        // Output slot
        int outX = x + 84;
        int outY = y + 8;
        drawRect(outX - 1, outY - 1, outX + 23, outY + 23, 0xFFFFD700);
        drawRect(outX, outY, outX + 22, outY + 22, 0xFF1E1E1E);

        if (recipe.output() != null) {
            renderItemStack(recipe.output(), outX + 3, outY + 3);
            if (mouseX >= outX && mouseX <= outX + 22 && mouseY >= outY && mouseY <= outY + 22) {
                pendingTooltip = recipe.output();
            }
        }
    }

    private void drawProcessRecipe(int x, int y, UniversalRecipe recipe, int mouseX, int mouseY) {
        drawRect(x, y, x + 226, y + 40, 0xFF222222);

        // Inputs
        int itemX = x + 6;
        for (ItemStack in : recipe.allInputs()) {
            renderItemStack(in, itemX, y + 12);
            if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= y + 12 && mouseY <= y + 28) {
                pendingTooltip = in;
            }
            itemX += 20;
        }

        // Center Arrow
        fontRendererObj.drawString("\u00a76==>\u00a7r", x + 96, y + 16, 0xFFAA00);

        // Output
        if (recipe.output() != null) {
            renderItemStack(recipe.output(), x + 130, y + 12);
            if (mouseX >= x + 130 && mouseX <= x + 146 && mouseY >= y + 12 && mouseY <= y + 28) {
                pendingTooltip = recipe.output();
            }
        }
    }

    private void drawStructureTab(int x, int y, int width, int mouseX, int mouseY) {
        if (activeKnowledgeEntry == null || !activeKnowledgeEntry.hasMultiblock()) return;
        MultiblockDefinition mb = activeKnowledgeEntry.multiblock();

        List<Integer> layers = mb.getLayerNumbers();
        if (layers.isEmpty()) return;

        int currentLayer = layers.get(Math.min(multiblockLayerIndex, layers.size() - 1));

        String layerLabel = formatLayerLabel(currentLayer);
        int labelWidth = fontRendererObj.getStringWidth(layerLabel);
        int labelBottom;
        if (labelWidth <= width - 4) {
            fontRendererObj.drawString("\u00a76" + layerLabel, x + (width - labelWidth) / 2, y + 2, 0xFFAA00);
            labelBottom = y + 12;
        } else {
            labelBottom = drawWrapped("\u00a76" + layerLabel, x + 2, y + 2, width - 4, 0xFFAA00);
        }

        List<MultiblockPattern> layerPatterns = mb.getPatternsInLayer(currentLayer);
        int minX = 0;
        int maxX = 0;
        int minZ = 0;
        int maxZ = 0;
        if (!layerPatterns.isEmpty()) {
            minX = layerPatterns.stream()
                .mapToInt(MultiblockPattern::dx)
                .min()
                .getAsInt();
            maxX = layerPatterns.stream()
                .mapToInt(MultiblockPattern::dx)
                .max()
                .getAsInt();
            minZ = layerPatterns.stream()
                .mapToInt(MultiblockPattern::dz)
                .min()
                .getAsInt();
            maxZ = layerPatterns.stream()
                .mapToInt(MultiblockPattern::dz)
                .max()
                .getAsInt();
        }
        int spanX = Math.max(1, maxX - minX + 1);
        int spanZ = Math.max(1, maxZ - minZ + 1);
        int cellSize = Math.max(14, Math.min(22, 106 / spanX));
        int gridOriginX = x + 8 - minX * cellSize;
        int gridOriginY = Math.max(y + 18, labelBottom + 6);
        int contentEndY = gridOriginY + spanZ * cellSize;

        ItemStack hoveredBlockStack = null;
        String hoveredBlockTooltip = null;

        for (MultiblockPattern pattern : layerPatterns) {
            int cellX = gridOriginX + pattern.dx() * cellSize;
            int cellY = gridOriginY + (pattern.dz() - minZ) * cellSize;

            MultiblockRole role = pattern.role();
            int frameColor = 0xFF888888;
            if (role == MultiblockRole.REQUIRED_CONTROLLER) frameColor = 0xFFFFD700;
            else if (role == MultiblockRole.REQUIRED_AIR) frameColor = 0xFF00E5FF;
            else if (role == MultiblockRole.OPTIONAL_ATTACHMENT) frameColor = 0xFF00FF00;
            else if (role == MultiblockRole.RELATED_EXTERNAL) frameColor = 0xFFFF00FF;

            drawRect(cellX, cellY, cellX + cellSize - 1, cellY + cellSize - 1, 0x44222222);
            drawRect(cellX, cellY, cellX + cellSize - 1, cellY + 1, frameColor);
            drawRect(cellX, cellY + cellSize - 2, cellX + cellSize - 1, cellY + cellSize - 1, frameColor);
            drawRect(cellX, cellY, cellX + 1, cellY + cellSize - 1, frameColor);
            drawRect(cellX + cellSize - 2, cellY, cellX + cellSize - 1, cellY + cellSize - 1, frameColor);

            if (pattern.displayStack() != null) {
                renderItemStack(pattern.displayStack(), cellX + 3, cellY + 3);
            } else if (role == MultiblockRole.REQUIRED_AIR) {
                fontRendererObj.drawString("\u00a7bAir", cellX + 3, cellY + 7, 0x00E5FF);
            }

            if (mouseX >= cellX && mouseX <= cellX + cellSize && mouseY >= cellY && mouseY <= cellY + cellSize) {
                hoveredBlockStack = pattern.displayStack();
                hoveredBlockTooltip = pattern.blockName() + " (" + role.displayLabel() + ")";
            }
        }

        // Dynamic required blocks list
        int reqX = x + 122;
        int reqY = gridOriginY;
        int reqWidth = width - (reqX - x) - 2;
        if (reqWidth < 58) {
            reqX = x + Math.min(width - 70, 112);
            reqWidth = width - (reqX - x) - 2;
        }
        reqY = drawWrapped("\u00a7e\u00a7lRequired Blocks:\u00a7r", reqX, reqY, reqWidth, 0xFFFF55);

        for (MultiblockDefinition.RequiredBlockEntry req : mb.getRequiredBlockEntries()) {
            reqY = drawWrapped("\u00a7f" + req.count() + "x \u00a77" + req.name(), reqX, reqY, reqWidth, 0xDDDDDD);
        }

        reqY += 6;
        reqY = drawWrapped("\u00a7a\u00a7lAttachments:\u00a7r", reqX, reqY, reqWidth, 0x55FF55);
        for (MultiblockPattern pattern : mb.patterns()) {
            if (pattern.role() == MultiblockRole.OPTIONAL_ATTACHMENT
                || pattern.role() == MultiblockRole.RELATED_EXTERNAL) {
                String label = pattern.blockName() + " (dx="
                    + pattern.dx()
                    + ", dy="
                    + pattern.dy()
                    + ", dz="
                    + pattern.dz()
                    + ")";
                reqY = drawWrapped("\u00a77\u2022 " + label, reqX, reqY, reqWidth, 0xDDDDDD);
            }
        }

        if (hoveredBlockStack != null && hoveredBlockTooltip != null) pendingTooltip = hoveredBlockStack;
        contentEndY = Math.max(contentEndY, reqY);
        maxScrollY = Math.max(0, contentEndY - (y + 156));
    }

    private static String formatLayerLabel(int dy) {
        switch (dy) {
            case -1:
                return "Layer 1: Hearth Base (dy = -1)";
            case 0:
                return "Layer 2: Crucible Chamber (dy = 0)";
            case 1:
                return "Layer 3: Upper Rim & Flue (dy = 1)";
            default:
                return "Layer (dy = " + dy + ")";
        }
    }

    private void startScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = res.getScaleFactor();

        int scissorX = x * scale;
        int scissorY = mc.displayHeight - (y + height) * scale;
        int scissorWidth = width * scale;
        int scissorHeight = height * scale;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderItemStack(ItemStack stack, int x, int y) {
        if (stack == null) return;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), stack, x, y);
        itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    private int drawWrapped(String text, int x, int y, int maxWidth, int color) {
        List<String> lines = TextLayoutHelper.wrap(fontRendererObj, text, maxWidth);
        for (String line : lines) {
            fontRendererObj.drawString(line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
}
