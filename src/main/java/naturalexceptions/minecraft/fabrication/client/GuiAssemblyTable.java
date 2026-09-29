package naturalexceptions.minecraft.fabrication.client;

import java.util.Arrays;
import java.util.List;

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
 * Assembly Table: a tool head and a handle go in, the finished tool comes out. Empty slots show what belongs in
 * them, and the arrow fills once the pair makes a tool.
 */
@SideOnly(Side.CLIENT)
public final class GuiAssemblyTable extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/assembly_table.png");
    private static final int ARROW_X = 100;
    private static final int ARROW_Y = 36;
    /** Sprite cells right of the 176-wide screen art. */
    private static final int[] ARROW_FULL = { 176, 0, 22, 15 };
    private static final int[] GHOST_HEAD = { 176, 16 };
    private static final int[] GHOST_HANDLE = { 192, 16 };
    private static final int STATUS_Y = 60;

    private final TileEntityAssemblyTable tileEntity;

    public GuiAssemblyTable(InventoryPlayer playerInv, TileEntityAssemblyTable tileEntity) {
        super(new ContainerAssemblyTable(playerInv, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("Assembly Table", 8, 6, 0x404040);
        fontRendererObj.drawString("Inventory", 8, 72, 0x404040);

        boolean hasHead = tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD) != null;
        boolean hasHandle = tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE) != null;
        ItemStack result = tileEntity.computeResult();
        String status;
        int colour = 0x606060;
        if (result != null) {
            status = result.getDisplayName();
            colour = 0x404040;
        } else if (hasHead && !hasHandle) {
            status = "Add a handle or a stick";
        } else if (!hasHead && hasHandle) {
            status = "Add a tool head";
        } else if (hasHead) {
            status = "These don't fit together";
        } else {
            status = "Add a head and a handle";
        }
        status = fontRendererObj.trimStringToWidth(status, xSize - 16);
        fontRendererObj.drawString(status, (xSize - fontRendererObj.getStringWidth(status)) / 2, STATUS_Y, colour);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD) == null) drawTexturedModalRect(
            guiLeft + ContainerAssemblyTable.HEAD_SLOT_X,
            guiTop + ContainerAssemblyTable.RECIPE_SLOT_Y,
            GHOST_HEAD[0],
            GHOST_HEAD[1],
            16,
            16);
        if (tileEntity.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE) == null) drawTexturedModalRect(
            guiLeft + ContainerAssemblyTable.HANDLE_SLOT_X,
            guiTop + ContainerAssemblyTable.RECIPE_SLOT_Y,
            GHOST_HANDLE[0],
            GHOST_HANDLE[1],
            16,
            16);
        if (tileEntity.computeResult() != null) drawTexturedModalRect(
            guiLeft + ARROW_X,
            guiTop + ARROW_Y,
            ARROW_FULL[0],
            ARROW_FULL[1],
            ARROW_FULL[2],
            ARROW_FULL[3]);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /** Empty input slots explain themselves on hover, including that a whole tool can be taken apart here. */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (mc.thePlayer.inventory.getItemStack() != null) return;
        List<String> lines = null;
        if (emptySlotAt(TileEntityAssemblyTable.SLOT_HEAD, ContainerAssemblyTable.HEAD_SLOT_X, mouseX, mouseY))
            lines = Arrays.asList("Tool head", "§7Or put a whole tool here", "§7to take it apart");
        else if (emptySlotAt(TileEntityAssemblyTable.SLOT_HANDLE, ContainerAssemblyTable.HANDLE_SLOT_X, mouseX, mouseY))
            lines = Arrays.asList("Handle", "§7A handle or a stick");
        if (lines != null) drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
    }

    private boolean emptySlotAt(int slot, int slotX, int mouseX, int mouseY) {
        if (tileEntity.getStackInSlot(slot) != null) return false;
        int x = guiLeft + slotX;
        int y = guiTop + ContainerAssemblyTable.RECIPE_SLOT_Y;
        return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
    }
}
