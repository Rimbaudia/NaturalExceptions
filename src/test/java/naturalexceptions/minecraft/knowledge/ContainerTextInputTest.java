package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import naturalexceptions.minecraft.gui.client.GuiMaterialNaming;
import naturalexceptions.minecraft.knowledge.client.InventoryBrowserOverlay;
import naturalexceptions.minecraft.knowledge.client.InventoryOverlayLayout;
import naturalexceptions.minecraft.mixin.GuiContainerCreativeAccessor;

final class ContainerTextInputTest {

    private Minecraft previousMinecraft;

    @BeforeEach
    void prepareMinecraft() throws Exception {
        Field field = Minecraft.class.getDeclaredField("theMinecraft");
        field.setAccessible(true);
        previousMinecraft = (Minecraft) field.get(null);
        field.set(null, allocate(Minecraft.class));
    }

    @AfterEach
    void clearOverlay() throws Exception {
        Field minecraft = Minecraft.class.getDeclaredField("theMinecraft");
        minecraft.setAccessible(true);
        minecraft.set(null, previousMinecraft);
        setOverlay("lastContainer", null);
        setOverlay("lastLayout", null);
        setOverlay("searchFocused", false);
        setOverlay("showingRecipes", false);
    }

    @Test
    void creativeSearchReceivesRecipeAndUsageLetters() throws Exception {
        CreativeScreen gui = allocate(CreativeScreen.class);
        gui.search = focusedField();
        setOverlay("lastContainer", gui);
        Minecraft.getMinecraft().currentScreen = gui;
        assertFalse(InventoryBrowserOverlay.mayRunGlobalKeybinds());
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'r', Keyboard.KEY_R));
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'u', Keyboard.KEY_U));
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'k', Keyboard.KEY_K));
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'e', Keyboard.KEY_E));
    }

    @Test
    void namingTextReceivesUnhandledKeysInsteadOfOverlayShortcuts() throws Exception {
        GuiMaterialNaming gui = allocate(GuiMaterialNaming.class);
        Field field = GuiMaterialNaming.class.getDeclaredField("nameField");
        field.setAccessible(true);
        field.set(gui, focusedField());
        setOverlay("lastContainer", gui);
        Minecraft.getMinecraft().currentScreen = gui;
        assertFalse(InventoryBrowserOverlay.mayRunGlobalKeybinds());
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'r', Keyboard.KEY_R));
    }

    @Test
    void unfocusedAndHiddenCreativeSearchLeaveShortcutsAvailable() throws Exception {
        CreativeScreen gui = allocate(CreativeScreen.class);
        gui.search = focusedField();
        setOverlay("lastContainer", gui);
        Minecraft.getMinecraft().currentScreen = gui;
        gui.search.setFocused(false);
        assertTrue(InventoryBrowserOverlay.mayRunGlobalKeybinds());
        assertTrue(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'r', Keyboard.KEY_R));
        gui.search.setFocused(true);
        gui.search.setVisible(false);
        assertTrue(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'u', Keyboard.KEY_U));
    }

    @Test
    void clickingBackInVanillaInventoryReleasesOverlaySearch() throws Exception {
        CreativeScreen gui = allocate(CreativeScreen.class);
        gui.search = focusedField();
        setOverlay("lastContainer", gui);
        Minecraft.getMinecraft().currentScreen = gui;
        setOverlay("lastLayout", InventoryOverlayLayout.calculate(854, 480, 339, 155, 176, 166));
        setOverlay("searchFocused", true);
        assertFalse(InventoryBrowserOverlay.handleContainerMouseClick(gui, 400, 180, 0));
        assertFalse(InventoryBrowserOverlay.isSearchFocused());
        assertFalse(InventoryBrowserOverlay.mayRunGlobalKeybinds());
        assertFalse(InventoryBrowserOverlay.handleContainerKeyTyped(gui, 'r', Keyboard.KEY_R));
    }

    @Test
    void explicitOverlaySearchKeepsPriorityForEscape() throws Exception {
        CreativeScreen gui = allocate(CreativeScreen.class);
        gui.search = focusedField();
        setOverlay("lastContainer", gui);
        Minecraft.getMinecraft().currentScreen = gui;
        setOverlay("searchFocused", true);
        assertTrue(InventoryBrowserOverlay.handleContainerKeyTyped(gui, '\u001b', Keyboard.KEY_ESCAPE));
        assertFalse(InventoryBrowserOverlay.isSearchFocused());
    }

    @Test
    void enterInOverlaySearchOnNamingScreenOnlyReleasesSearchFocus() throws Exception {
        GuiMaterialNaming gui = allocate(GuiMaterialNaming.class);
        Field field = GuiMaterialNaming.class.getDeclaredField("nameField");
        field.setAccessible(true);
        field.set(gui, new GuiTextField(null, 0, 0, 100, 16));
        setOverlay("lastContainer", gui);
        setOverlay("searchFocused", true);
        java.lang.reflect.Method keyTyped = GuiMaterialNaming.class
            .getDeclaredMethod("keyTyped", char.class, int.class);
        keyTyped.setAccessible(true);
        keyTyped.invoke(gui, '\n', Keyboard.KEY_RETURN);
        assertFalse(InventoryBrowserOverlay.isSearchFocused());
    }

    private static GuiTextField focusedField() {
        GuiTextField field = new GuiTextField(null, 0, 0, 100, 16);
        field.setFocused(true);
        return field;
    }

    private static void setOverlay(String name, Object value) throws Exception {
        Field instance = InventoryBrowserOverlay.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        Field field = InventoryBrowserOverlay.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance.get(null), value);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }

    // Unit tests do not run the Mixin transformer; provide its accessor contract explicitly.
    private static final class CreativeScreen extends GuiContainerCreative implements GuiContainerCreativeAccessor {

        private GuiTextField search;

        private CreativeScreen() {
            super(null);
        }

        @Override
        public GuiTextField naturalexceptions$getSearchField() {
            return search;
        }
    }
}
