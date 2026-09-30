package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.ContainerStorage;

/** The chest screen, sized to a crate's, shelf's or counter's rows. */
@SideOnly(Side.CLIENT)
public final class GuiStorage extends GuiContainer {

    private static final ResourceLocation CHEST = new ResourceLocation("textures/gui/container/generic_54.png");
    private final IInventory player, storage;
    private final int rows;

    public GuiStorage(IInventory player, IInventory storage) {
        super(new ContainerStorage(player, storage));
        this.player = player;
        this.storage = storage;
        this.rows = storage.getSizeInventory() / 9;
        allowUserInput = false;
        ySize = 114 + rows * 18;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format(storage.getInventoryName()), 8, 6, 4210752);
        fontRendererObj.drawString(I18n.format(player.getInventoryName()), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(CHEST);
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, rows * 18 + 17);
        drawTexturedModalRect(x, y + rows * 18 + 17, 0, 126, xSize, 96);
    }
}
