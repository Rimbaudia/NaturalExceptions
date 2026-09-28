package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.minecraft.knowledge.client.InventoryOverlayInput;
import naturalexceptions.minecraft.knowledge.client.InventoryOverlayLayout;
import naturalexceptions.minecraft.knowledge.recipe.RecipeCategory;
import naturalexceptions.minecraft.knowledge.recipe.RecipeDisplayType;

final class InventoryOverlayLayoutTest {

    @Test
    void placesPanelBesideVanillaInventoryWithoutCoveringIt() {
        InventoryOverlayLayout.Layout layout = InventoryOverlayLayout.calculate(854, 480, 339, 155, 176, 166);

        assertTrue(layout.isPanelVisible());
        assertEquals(InventoryOverlayLayout.Side.RIGHT, layout.side());
        assertEquals(339 + 176 + 5, layout.panelX());
        assertEquals(854, layout.panelX() + layout.panelWidth());
        assertEquals(0, layout.panelY());
        assertEquals(480, layout.panelHeight());
        assertEquals(7, layout.columns());
        assertTrue(layout.rows() > 0);
    }

    @Test
    void keepsACompactItemPanelVisibleBesideAnInventoryInAWindowedScreen() {
        InventoryOverlayLayout.Layout layout = InventoryOverlayLayout.calculate(430, 256, 127, 45, 176, 166);

        assertTrue(layout.isPanelVisible());
        assertEquals(InventoryOverlayLayout.Side.RIGHT, layout.side());
        assertEquals(122, layout.panelWidth());
        assertEquals(6, layout.columns());
        assertEquals(308, layout.panelX());
    }

    @Test
    void usesCompactToggleWhenNeitherSideCanFitThePanel() {
        InventoryOverlayLayout.Layout layout = InventoryOverlayLayout.calculate(260, 240, 42, 37, 176, 166);

        assertFalse(layout.isPanelVisible());
        assertTrue(layout.hasToggle());
        assertTrue(layout.toggleX() >= 0);
        assertTrue(layout.toggleX() + layout.toggleWidth() <= 260);
    }

    @Test
    void mapsPanelClicksToTheVisibleItemAndPageControls() {
        assertEquals(23, InventoryOverlayInput.itemIndexAt(200, 30, 5, 4, 10, 260, 111));
        assertEquals(-1, InventoryOverlayInput.itemIndexAt(200, 30, 5, 4, 10, 200, 71));
        assertEquals(
            InventoryOverlayInput.PageAction.PREVIOUS,
            InventoryOverlayInput.pageActionAt(200, 30, 158, 240, 207, 257));
        assertEquals(
            InventoryOverlayInput.PageAction.NEXT,
            InventoryOverlayInput.pageActionAt(200, 30, 158, 240, 345, 257));
        assertEquals(
            InventoryOverlayInput.PageAction.NONE,
            InventoryOverlayInput.pageActionAt(200, 30, 158, 240, 207, 100));
    }

    @Test
    void focusedSearchOwnsRUKInsteadOfGlobalShortcuts() {
        assertTrue(InventoryOverlayInput.ownsSearchInput(true));
        assertFalse(InventoryOverlayInput.mayRunGlobalShortcut(true, false));
        assertFalse(InventoryOverlayInput.mayRunGlobalShortcut(true, true));
    }

    @Test
    void unfocusedSearchLeavesGlobalShortcutsAvailableOutsideOverlaySubview() {
        assertTrue(InventoryOverlayInput.mayRunGlobalShortcut(false, false));
        assertFalse(InventoryOverlayInput.mayRunGlobalShortcut(false, true));
    }

    @Test
    void recipeDisplayTypesMapToSemanticProcessCategories() {
        assertEquals(RecipeCategory.CRAFTING, RecipeDisplayType.CRAFTING_GRID.category());
        assertEquals(RecipeCategory.SMELTING, RecipeDisplayType.FURNACE.category());
        assertEquals(RecipeCategory.FUEL, RecipeDisplayType.FURNACE_FUEL.category());
        assertEquals(RecipeCategory.BREWING, RecipeDisplayType.BREWING.category());
        assertEquals(RecipeCategory.WORLD_INTERACTION, RecipeDisplayType.WORLD_ACTION.category());
        assertEquals(RecipeCategory.NATURAL_EXCEPTIONS_PROCESS, RecipeDisplayType.PROCESS.category());
        assertEquals(RecipeCategory.MACHINE_PROCESSING, RecipeDisplayType.MACHINE_PROCESS.category());
    }
}
