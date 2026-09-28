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
        "textures/gui/assembly_table.png");
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
                new GuiButton(
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
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
        GL11.glColor4f(1, 1, 1, 1);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF373737);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xFFC6C6C6);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + 2, 0xFFFFFFFF);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + 2, guiTop + ySize - 1, 0xFFFFFFFF);
        mc.getTextureManager()
            .bindTexture(GUI);
        drawTexturedModalRect(guiLeft, guiTop + 103, 0, 83, xSize, 83);
        drawSlotBackground(guiLeft + MoldAssemblyLayout.INPUT_X - 1, guiTop + MoldAssemblyLayout.MACHINE_Y - 1);
        drawSlotBackground(guiLeft + MoldAssemblyLayout.OUTPUT_X - 1, guiTop + MoldAssemblyLayout.MACHINE_Y - 1);
        drawRect(guiLeft + 74, guiTop + 37, guiLeft + 106, guiTop + 41, 0xFF777777);
        drawRect(guiLeft + 101, guiTop + 33, guiLeft + 107, guiTop + 45, 0xFF777777);
    }

    private void drawSlotBackground(int x, int y) {
        drawRect(x, y, x + 18, y + 18, 0xFF373737);
        drawRect(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < buttonList.size(); i++) ((GuiButton) buttonList.get(i)).enabled = table.canShape(i);
        super.drawScreen(mouseX, mouseY, partialTicks);
        int index = 0;
        GeometryId hoveredGeometry = null;
        boolean hoveredEnabled = false;
        for (GeometryId geometry : TileEntityMoldAssemblyTable.selectableGeometries()) {
            int col = index % 3, row = index / 3;
            int buttonX = guiLeft + MoldAssemblyLayout.BUTTON_X + col * MoldAssemblyLayout.BUTTON_COLUMN_SPACING;
            int buttonY = guiTop + MoldAssemblyLayout.BUTTON_Y + row * MoldAssemblyLayout.BUTTON_ROW_SPACING;
            int x = buttonX + 2;
            int y = buttonY + 1;
            boolean enabled = table.canShape(index);
            boolean hovered = mouseX >= buttonX && mouseX < buttonX + MoldAssemblyLayout.BUTTON_WIDTH
                && mouseY >= buttonY
                && mouseY < buttonY + MoldAssemblyLayout.BUTTON_HEIGHT;
            drawShapeButton(
                buttonX,
                buttonY,
                MoldAssemblyLayout.BUTTON_WIDTH,
                MoldAssemblyLayout.BUTTON_HEIGHT,
                enabled,
                hovered);
            ItemStack icon = ItemCastingMold.createStack(geometry);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem.getInstance()
                .renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), icon, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            String label = geometry == GeometryId.PICK_HEAD ? "Pick" : capitalize(geometry.id());
            fontRendererObj.drawString(label, x + 19, y + 3, enabled ? 0x404040 : 0x777777);
            if (hovered) {
                hoveredGeometry = geometry;
                hoveredEnabled = enabled;
            }
            index++;
        }
        if (hoveredGeometry != null) drawShapeTooltip(hoveredGeometry, hoveredEnabled, mouseX, mouseY);
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
