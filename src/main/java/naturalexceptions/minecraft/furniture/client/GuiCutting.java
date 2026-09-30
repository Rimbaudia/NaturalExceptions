package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.ContainerCutting;
import naturalexceptions.minecraft.furniture.TileEntityCuttingBench;

/** A cutting bench's screen: the piece on the left, an arrow, and what it can become. */
@SideOnly(Side.CLIENT)
public final class GuiCutting extends GuiContainer {

    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("textures/gui/container/furnace.png");
    private final IInventory player;
    private final TileEntityCuttingBench bench;

    public GuiCutting(IInventory player, TileEntityCuttingBench bench) {
        super(new ContainerCutting(player, bench));
        this.player = player;
        this.bench = bench;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format(bench.getInventoryName()), 8, 6, 4210752);
        fontRendererObj.drawString(I18n.format(player.getInventoryName()), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        // The chest screen's frame, emptied of its slots above; then a slot under the piece and under each choice.
        mc.getTextureManager()
            .bindTexture(CHEST);
        drawTexturedModalRect(x, y, 0, 0, xSize, 17);
        for (int row = 17; row < 70; row++) drawTexturedModalRect(x, y + row, 0, 10, xSize, 1); // plain panel
        drawTexturedModalRect(x, y + 70, 0, 126, xSize, 96);
        int slotU = 7, slotV = 17;
        ContainerCutting container = (ContainerCutting) inventorySlots;
        int inputY = container.fired() ? ContainerCutting.FIRED_INPUT_Y : ContainerCutting.INPUT_Y;
        drawTexturedModalRect(x + ContainerCutting.INPUT_X - 1, y + inputY - 1, slotU, slotV, 18, 18);
        if (container.fired())
            drawTexturedModalRect(x + ContainerCutting.INPUT_X - 1, y + ContainerCutting.FUEL_Y - 1, slotU, slotV, 18, 18);
        for (int i = 0; i < ContainerCutting.CHOICES; i++) drawTexturedModalRect(
            x + ContainerCutting.CHOICE_X - 1 + (i % ContainerCutting.ACROSS) * 18,
            y + ContainerCutting.CHOICE_Y - 1 + (i / ContainerCutting.ACROSS) * 18,
            slotU,
            slotV,
            18,
            18);
        mc.getTextureManager()
            .bindTexture(FURNACE);
        drawTexturedModalRect(x + 49, y + 34, 176, 14, 24, 17); // the arrow, full
        if (container.fired() && container.hot()) // the fire, between the piece and its fuel
            drawTexturedModalRect(x + ContainerCutting.INPUT_X + 1, y + 37, 176, 0, 14, 14);
    }
}
