package naturalexceptions.minecraft.client;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Vanilla-style hover tooltips for overlays drawn on top of a screen (the screen's own method is protected). */
@SideOnly(Side.CLIENT)
public final class GuiTooltips {

    private static final Method DRAW = find();

    private GuiTooltips() {}

    public static void draw(GuiScreen gui, List<String> lines, int mouseX, int mouseY) {
        if (DRAW == null || gui == null || lines == null || lines.isEmpty()) return;
        try {
            DRAW.invoke(gui, lines, mouseX, mouseY, gui.mc.fontRenderer);
        } catch (ReflectiveOperationException ignored) {}
    }

    private static Method find() {
        for (String name : new String[] { "drawHoveringText" }) {
            try {
                Method method = GuiScreen.class
                    .getDeclaredMethod(name, List.class, int.class, int.class, FontRenderer.class);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException | SecurityException ignored) {}
        }
        return null;
    }
}
