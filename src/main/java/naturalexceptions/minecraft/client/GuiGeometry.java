package naturalexceptions.minecraft.client;

import java.lang.reflect.Field;

import net.minecraft.client.gui.inventory.GuiContainer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Reads a container screen's protected placement (left, top, width, height) for overlays drawn around it. */
@SideOnly(Side.CLIENT)
public final class GuiGeometry {

    private static final Field GUI_LEFT = field("guiLeft", "field_147003_i");
    private static final Field GUI_TOP = field("guiTop", "field_147009_r");
    private static final Field X_SIZE = field("xSize", "field_146999_f");
    private static final Field Y_SIZE = field("ySize", "field_147000_g");

    private GuiGeometry() {}

    public static int left(GuiContainer gui) {
        return get(GUI_LEFT, gui, Math.max(0, (gui.width - width(gui)) / 2));
    }

    public static int top(GuiContainer gui) {
        return get(GUI_TOP, gui, Math.max(0, (gui.height - height(gui)) / 2));
    }

    public static int width(GuiContainer gui) {
        return get(X_SIZE, gui, 176);
    }

    public static int height(GuiContainer gui) {
        return get(Y_SIZE, gui, 166);
    }

    private static int get(Field field, GuiContainer gui, int fallback) {
        if (field == null || gui == null) return fallback;
        try {
            return field.getInt(gui);
        } catch (IllegalAccessException ignored) {
            return fallback;
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
}
