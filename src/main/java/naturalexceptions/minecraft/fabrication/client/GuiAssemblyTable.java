package naturalexceptions.minecraft.fabrication.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.fabrication.ContainerAssemblyTable;
import naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable;

/**
 * Client-side graphical user interface for the Assembly Table.
 */
@SideOnly(Side.CLIENT)
public final class GuiAssemblyTable extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/assembly_table.png");

    private final TileEntityAssemblyTable tileEntity;

    public GuiAssemblyTable(InventoryPlayer playerInv, TileEntityAssemblyTable tileEntity) {
        super(new ContainerAssemblyTable(playerInv, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString("Assembly Table", 8, 6, 0x404040);
        this.fontRendererObj.drawString("Inventory", 8, 72, 0x404040);

        // The two input labels share one centered heading; separate labels collide at this scale.
        int inputsCenter = (ContainerAssemblyTable.HEAD_SLOT_X + ContainerAssemblyTable.HANDLE_SLOT_X) / 2 + 8;
        drawCenteredString("Head + Handle", inputsCenter, 18, 0x777777);
        drawCenteredString("Result", ContainerAssemblyTable.RESULT_SLOT_X + 8, 18, 0x777777);
        drawCenteredString("+", 71, 39, 0x777777);
        drawCenteredString("->", 112, 39, 0x777777);

        // Status indicator
        boolean hasHead = tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD) != null;
        boolean hasHandle = tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE) != null;
        boolean hasResult = tileEntity.computeResult() != null;

        if (hasResult) {
            ItemStack res = tileEntity.computeResult();
            String name = res != null ? res.getDisplayName() : "Ready to Assemble";
            drawCenteredString(trim("\u00a72" + name, 136), 88, 57, 0x008000);
        } else if (hasHead && !hasHandle) {
            drawCenteredString("\u00a7eInsert Handle or Stick", 88, 57, 0x888800);
        } else if (!hasHead && hasHandle) {
            drawCenteredString("\u00a7eInsert Tool Head", 88, 57, 0x888800);
        } else {
            drawCenteredString("\u00a77Place Head & Handle", 88, 57, 0x888888);
        }
    }

    private void drawCenteredString(String text, int centerX, int y, int color) {
        this.fontRendererObj.drawString(text, centerX - this.fontRendererObj.getStringWidth(text) / 2, y, color);
    }

    private String trim(String text, int width) {
        String result = text;
        while (!result.isEmpty() && fontRendererObj.getStringWidth(result) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);

        // Clear only the baked arrow. The pixel-font arrow is drawn in the foreground.
        drawRect(k + 99, l + 35, k + 125, l + 52, 0xFFC6C6C6);
        // The baked plus is offset toward the head slot; center it in the input gap.
        drawRect(k + 66, l + 35, k + 76, l + 52, 0xFFC6C6C6);
    }
}
