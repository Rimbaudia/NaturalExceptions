package naturalexceptions.minecraft.fabrication.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.minecraft.fabrication.ContainerMoldAssemblyTable;
import naturalexceptions.minecraft.fabrication.MoldAssemblyLayout;
import naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable;
import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * Mold Assembly Table: a blank mold goes in on the left, a row of shape tiles picks what it becomes, and the shaped
 * mold comes out on the right. Tiles show the mold itself; names are in their tooltips.
 */
public final class GuiMoldAssemblyTable extends GuiContainer {

    private static final ResourceLocation GUI = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/mold_assembly_table.png");
    /** Sprite cells in the texture, right of the 176-wide screen art. */
    private static final int[] TILE_NORMAL = { 176, 0 };
    private static final int[] TILE_HOVER = { 198, 0 };
    private static final int[] TILE_CURRENT = { 220, 0 };
    private static final int[] TILE_UNAVAILABLE = { 176, 22 };
    private static final int[] ARROW_FULL = { 176, 44, 22, 15 };
    private static final int[] GHOST_BLANK = { 198, 44 };

    private final TileEntityMoldAssemblyTable table;

    public GuiMoldAssemblyTable(InventoryPlayer player, TileEntityMoldAssemblyTable table) {
        super(new ContainerMoldAssemblyTable(player, table));
        this.table = table;
        xSize = MoldAssemblyLayout.WIDTH;
        ySize = MoldAssemblyLayout.HEIGHT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        buttonList.clear();
        for (int i = 0; i < TileEntityMoldAssemblyTable.selectableGeometries()
            .size(); i++) {
            int[] at = MoldAssemblyLayout.tile(i);
            buttonList.add(
                new HitArea(
                    i,
                    guiLeft + at[0],
                    guiTop + at[1],
                    MoldAssemblyLayout.TILE_SIZE,
                    MoldAssemblyLayout.TILE_SIZE));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString("Mold Assembly Table", 8, 6, 0x404040);
        String instruction = !table.hasValidBlankInput() ? "Put a Blank Mold in"
            : anyShapeAvailable() ? "Pick a shape" : "Take the shaped mold out";
        fontRendererObj.drawString(
            instruction,
            (xSize - fontRendererObj.getStringWidth(instruction)) / 2,
            MoldAssemblyLayout.INSTRUCTION_Y,
            0x606060);
        fontRendererObj.drawString("Inventory", 8, MoldAssemblyLayout.PLAYER_Y - 11, 0x404040);
        drawShapeIcons();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(GUI);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (table.getStackInSlot(0) == null) {
            drawTexturedModalRect(
                guiLeft + MoldAssemblyLayout.INPUT_X,
                guiTop + MoldAssemblyLayout.MACHINE_Y,
                GHOST_BLANK[0],
                GHOST_BLANK[1],
                16,
                16);
        }
        if (table.hasValidBlankInput()) {
            drawTexturedModalRect(
                guiLeft + MoldAssemblyLayout.ARROW_X,
                guiTop + MoldAssemblyLayout.ARROW_Y,
                ARROW_FULL[0],
                ARROW_FULL[1],
                ARROW_FULL[2],
                ARROW_FULL[3]);
        }
        // Tiles belong to the background so held items and tooltips always draw above them.
        List<GeometryId> shapes = TileEntityMoldAssemblyTable.selectableGeometries();
        for (int i = 0; i < shapes.size(); i++) {
            int[] at = MoldAssemblyLayout.tile(i);
            int x = guiLeft + at[0];
            int y = guiTop + at[1];
            boolean hovered = mx >= x && mx < x + MoldAssemblyLayout.TILE_SIZE
                && my >= y
                && my < y + MoldAssemblyLayout.TILE_SIZE;
            int[] sprite = isCurrent(shapes.get(i)) ? TILE_CURRENT
                : !table.canShape(i) ? TILE_UNAVAILABLE : hovered ? TILE_HOVER : TILE_NORMAL;
            drawTexturedModalRect(
                x,
                y,
                sprite[0],
                sprite[1],
                MoldAssemblyLayout.TILE_SIZE,
                MoldAssemblyLayout.TILE_SIZE);
        }
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** A clickable but invisible button; the tiles are drawn in the background layer. */
    private static final class HitArea extends GuiButton {

        HitArea(int id, int x, int y, int width, int height) {
            super(id, x, y, width, height, "");
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {}
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < buttonList.size(); i++) ((GuiButton) buttonList.get(i)).enabled = table.canShape(i);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (mc.thePlayer.inventory.getItemStack() != null) return;
        List<GeometryId> shapes = TileEntityMoldAssemblyTable.selectableGeometries();
        for (int i = 0; i < shapes.size(); i++) {
            int[] at = MoldAssemblyLayout.tile(i);
            int x = guiLeft + at[0];
            int y = guiTop + at[1];
            if (mouseX >= x && mouseX < x + MoldAssemblyLayout.TILE_SIZE
                && mouseY >= y
                && mouseY < y + MoldAssemblyLayout.TILE_SIZE) {
                List<String> lines = new ArrayList<>();
                lines.add(labelFor(shapes.get(i)));
                lines.add(
                    table.canShape(i) ? "§7Uses one Blank Mold"
                        : table.hasValidBlankInput() ? "§8Take the other mold out first" : "§8Needs a Blank Mold");
                drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
                return;
            }
        }
        int inputX = guiLeft + MoldAssemblyLayout.INPUT_X;
        int inputY = guiTop + MoldAssemblyLayout.MACHINE_Y;
        if (table.getStackInSlot(0) == null && mouseX >= inputX - 1
            && mouseX < inputX + 17
            && mouseY >= inputY - 1
            && mouseY < inputY + 17) {
            drawHoveringText(Collections.singletonList("Blank Mold"), mouseX, mouseY, fontRendererObj);
        }
    }

    /** Shape icons on the tiles, drawn under any held item; unavailable shapes are dimmed. */
    private void drawShapeIcons() {
        List<GeometryId> shapes = TileEntityMoldAssemblyTable.selectableGeometries();
        for (int i = 0; i < shapes.size(); i++) {
            int[] at = MoldAssemblyLayout.tile(i);
            int x = at[0] + 3;
            int y = at[1] + 3;
            ItemStack icon = ItemCastingMold.createStack(shapes.get(i));
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem.getInstance()
                .renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), icon, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (!table.canShape(i) && !isCurrent(shapes.get(i))) drawRect(x, y, x + 16, y + 16, 0x80A0A0A0);
        }
    }

    private boolean anyShapeAvailable() {
        for (int i = 0; i < TileEntityMoldAssemblyTable.selectableGeometries()
            .size(); i++) if (table.canShape(i)) return true;
        return false;
    }

    /** The shape of the mold waiting in the output, shown pressed in. */
    private boolean isCurrent(GeometryId geometry) {
        ItemStack output = table.getStackInSlot(1);
        if (output == null) return false;
        ItemStack shaped = ItemCastingMold.createStack(geometry);
        return output.getItem() == shaped.getItem() && output.getItemDamage() == shaped.getItemDamage()
            && ItemStack.areItemStackTagsEqual(output, shaped);
    }

    private static String labelFor(GeometryId geometry) {
        if (geometry == GeometryId.PICK_HEAD) return "Pick Head Mold";
        String id = geometry.id();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1)
            .replace('_', ' ') + " Mold";
    }
}
