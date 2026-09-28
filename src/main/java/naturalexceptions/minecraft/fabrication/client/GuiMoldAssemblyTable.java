package naturalexceptions.minecraft.fabrication.client;

import java.util.Arrays;

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

public final class GuiMoldAssemblyTable extends GuiContainer {

    private static final ResourceLocation GUI = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/mold_assembly_table.png");
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
            int col = i % 3, row = i / 3;
            buttonList.add(
                new HitArea(
                    i,
                    guiLeft + MoldAssemblyLayout.BUTTON_X + col * MoldAssemblyLayout.BUTTON_COLUMN_SPACING,
                    guiTop + MoldAssemblyLayout.BUTTON_Y + row * MoldAssemblyLayout.BUTTON_ROW_SPACING,
                    MoldAssemblyLayout.BUTTON_WIDTH,
                    MoldAssemblyLayout.BUTTON_HEIGHT,
                    ""));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString("Mold Assembly Table", 8, 6, 0x404040);
        fontRendererObj.drawString("Input", 31, 18, 0x555555);
        fontRendererObj.drawString("Output", 116, 18, 0x555555);
        String instruction = table.hasValidBlankInput()
            ? table.getStackInSlot(1) == null ? "Choose shape (1 blank)" : "Take output to change shape"
            : "Insert Blank Mold into input";
        fontRendererObj.drawString(trim(instruction, 160), 8, 51, 0x404040);
        fontRendererObj.drawString("Inventory", 8, 93, 0x404040);
        drawShapeLabels();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(GUI);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        // Shape buttons belong to the background so held items and tooltips always draw above them.
        for (int i = 0; i < TileEntityMoldAssemblyTable.selectableGeometries()
            .size(); i++) {
            int buttonX = guiLeft + MoldAssemblyLayout.BUTTON_X + (i % 3) * MoldAssemblyLayout.BUTTON_COLUMN_SPACING;
            int buttonY = guiTop + MoldAssemblyLayout.BUTTON_Y + (i / 3) * MoldAssemblyLayout.BUTTON_ROW_SPACING;
            boolean hovered = mx >= buttonX && mx < buttonX + MoldAssemblyLayout.BUTTON_WIDTH
                && my >= buttonY
                && my < buttonY + MoldAssemblyLayout.BUTTON_HEIGHT;
            drawShapeButton(
                buttonX,
                buttonY,
                MoldAssemblyLayout.BUTTON_WIDTH,
                MoldAssemblyLayout.BUTTON_HEIGHT,
                table.canShape(i),
                hovered);
        }
    }

    /** A clickable but invisible button; the shape buttons are drawn in the background layer. */
    private static final class HitArea extends GuiButton {

        HitArea(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY) {}
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < buttonList.size(); i++) ((GuiButton) buttonList.get(i)).enabled = table.canShape(i);
        super.drawScreen(mouseX, mouseY, partialTicks);
        int index = 0;
        for (GeometryId geometry : TileEntityMoldAssemblyTable.selectableGeometries()) {
            int buttonX = guiLeft + MoldAssemblyLayout.BUTTON_X
                + (index % 3) * MoldAssemblyLayout.BUTTON_COLUMN_SPACING;
            int buttonY = guiTop + MoldAssemblyLayout.BUTTON_Y + (index / 3) * MoldAssemblyLayout.BUTTON_ROW_SPACING;
            boolean hovered = mouseX >= buttonX && mouseX < buttonX + MoldAssemblyLayout.BUTTON_WIDTH
                && mouseY >= buttonY
                && mouseY < buttonY + MoldAssemblyLayout.BUTTON_HEIGHT;
            if (hovered && mc.thePlayer.inventory.getItemStack() == null) {
                drawShapeTooltip(geometry, table.canShape(index), mouseX, mouseY);
            }
            index++;
        }
    }

    /** Shape icons and labels, drawn under any held item. */
    private void drawShapeLabels() {
        int index = 0;
        for (GeometryId geometry : TileEntityMoldAssemblyTable.selectableGeometries()) {
            int x = MoldAssemblyLayout.BUTTON_X + (index % 3) * MoldAssemblyLayout.BUTTON_COLUMN_SPACING + 2;
            int y = MoldAssemblyLayout.BUTTON_Y + (index / 3) * MoldAssemblyLayout.BUTTON_ROW_SPACING + 1;
            boolean enabled = table.canShape(index);
            ItemStack icon = ItemCastingMold.createStack(geometry);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem.getInstance()
                .renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), icon, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            String label = geometry == GeometryId.PICK_HEAD ? "Pick" : capitalize(geometry.id());
            fontRendererObj.drawString(label, x + 19, y + 5, enabled ? 0x404040 : 0x777777);
            index++;
        }
    }

    private void drawShapeTooltip(GeometryId geometry, boolean enabled, int mouseX, int mouseY) {
        java.util.List<String> lines = Arrays
            .asList(labelFor(geometry), enabled ? "Consumes one Blank Mold" : "Add or take molds first");
        int width = 0;
        for (String line : lines) width = Math.max(width, fontRendererObj.getStringWidth(line));
        int x = Math.min(mouseX + 12, guiLeft + xSize - width - 12);
        x = Math.max(guiLeft + 4, x);
        int y = Math.min(mouseY + 12, guiTop + ySize - lines.size() * 10 - 10);
        y = Math.max(guiTop + 4, y);
        drawHoveringText(lines, x, y, fontRendererObj);
    }

    private String trim(String text, int width) {
        String result = text;
        while (!result.isEmpty() && fontRendererObj.getStringWidth(result) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String labelFor(GeometryId geometry) {
        if (geometry == GeometryId.PICK_HEAD) return "Pick Head Mold";
        return capitalize(geometry.id()) + " Mold";
    }

    private void drawShapeButton(int x, int y, int width, int height, boolean enabled, boolean hovered) {
        int face = enabled && hovered ? 0xFFE4D6A8 : enabled ? 0xFFC6C6C6 : 0xFFB0B0B0;
        int highlight = enabled ? 0xFFFFFFFF : 0xFFD0D0D0;
        drawRect(x, y, x + width, y + height, 0xFF373737);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, face);
        drawRect(x + 1, y + 1, x + width - 1, y + 2, highlight);
        drawRect(x + 1, y + 1, x + 2, y + height - 1, highlight);
        drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
        drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF555555);
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value
            : Character.toUpperCase(value.charAt(0)) + value.substring(1)
                .replace('_', ' ');
    }
}
