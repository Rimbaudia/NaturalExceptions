package naturalexceptions.minecraft.gear.client;

import java.util.Collections;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.ContainerGear;
import naturalexceptions.minecraft.gear.GearLayout;

/** The Gear screen: what you wear, around a figure of yourself, above your inventory. */
@SideOnly(Side.CLIENT)
public final class GuiGear extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("naturalexceptions", "textures/gui/gear.png");
    private static final int TEXT = 0x404040;

    private float mouseX;
    private float mouseY;

    public GuiGear(EntityPlayer player) {
        super(new ContainerGear(player));
        xSize = GearLayout.WIDTH;
        ySize = GearLayout.HEIGHT;
    }

    /** The screen is tall; keep room above it for the tabs whenever the window allows. */
    @Override
    public void initGui() {
        super.initGui();
        guiTop = Math.max(guiTop, Math.min(InventoryTab.RISE + 2, height - ySize));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawEmptySlotTooltip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRendererObj.drawString("Gear", 8, 6, TEXT);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager()
            .bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        int[] frame = GearLayout.FIGURE;
        int footX = guiLeft + frame[0] + frame[2] / 2;
        int footY = guiTop + frame[1] + frame[3] - 6;
        GuiInventory.func_147046_a(footX, footY, 38, footX - mouseX, footY - 64 - mouseY, mc.thePlayer);
    }

    /** Names an empty gear slot under the mouse ("Left glove"), so the silhouettes need no legend. */
    private void drawEmptySlotTooltip(int mx, int my) {
        if (mc.thePlayer.inventory.getItemStack() != null) return;
        for (Object entry : inventorySlots.inventorySlots) {
            if (!(entry instanceof ContainerGear.GearSlotView)) continue;
            Slot slot = (Slot) entry;
            if (slot.getHasStack()) continue;
            int x = guiLeft + slot.xDisplayPosition;
            int y = guiTop + slot.yDisplayPosition;
            if (mx >= x - 1 && mx < x + 17 && my >= y - 1 && my < y + 17) {
                drawHoveringText(
                    Collections.singletonList(
                        ((ContainerGear.GearSlotView) slot).gearSlot()
                            .label()),
                    mx,
                    my,
                    fontRendererObj);
                return;
            }
        }
    }
}
