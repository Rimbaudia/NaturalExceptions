package naturalexceptions.minecraft.metallurgy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.metallurgy.AggregateLayer;

/**
 * Client-side graphical user interface for the Starter Smelter.
 */
@SideOnly(Side.CLIENT)
public final class GuiStarterSmelter extends GuiContainer {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/gui/starter_smelter.png");

    private final TileEntityStarterSmelter tileEntity;

    public GuiStarterSmelter(InventoryPlayer playerInv, TileEntityStarterSmelter tileEntity) {
        super(new ContainerStarterSmelter(playerInv, tileEntity));
        this.tileEntity = tileEntity;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        // Button 0: Pour Metal (Action 0)
        this.buttonList.add(new GuiButton(0, this.guiLeft + 60, this.guiTop + 47, 32, 18, "Pour"));
        // Button 1: Skim Slag (Action 1)
        this.buttonList.add(new GuiButton(1, this.guiLeft + 94, this.guiTop + 47, 32, 18, "Skim"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, button.id);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title
        this.fontRendererObj.drawString("Starter Smelter", 8, 6, 0x404040);

        // Status
        if (tileEntity.isStructureValid()) {
            this.fontRendererObj.drawString("\u00a72Formed", 134, 6, 0x008000);
        } else {
            this.fontRendererObj.drawString("\u00a74Broken", 130, 6, 0xAA0000);
        }

        // Monitor info inside the dark display area (x=62 to 125, y=19 to 44)
        if (tileEntity.vesselState()
            .fuelState()
            .isBurning()) {
            int ticks = tileEntity.vesselState()
                .fuelState()
                .remainingTicks();
            this.fontRendererObj.drawString("\u00a76" + ticks + "t", 64, 21, 0xFFFFFF);
        } else {
            this.fontRendererObj.drawString("\u00a77Cold", 64, 21, 0x888888);
        }

        double maxTemp = 293.15;
        for (AggregateLayer l : tileEntity.vesselState()
            .layers()) {
            if (l.temperatureKelvin() > maxTemp) {
                maxTemp = l.temperatureKelvin();
            }
        }

        if (tileEntity.vesselState()
            .layers()
            .isEmpty()) {
            this.fontRendererObj.drawString("\u00a78Empty", 64, 32, 0x666666);
        } else {
            int color = maxTemp > 1350.0 ? 0xFFAA00 : 0xAAAAAA;
            String tempStr = String.format("%.0fK", maxTemp);
            this.fontRendererObj.drawString(tempStr, 64, 32, color);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        int k = this.guiLeft;
        int l = this.guiTop;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Hover over monitor area (x between guiLeft + 61 and guiLeft + 120, y between guiTop + 18 and guiTop + 44)
        if (mouseX >= this.guiLeft + 61 && mouseX <= this.guiLeft + 120
            && mouseY >= this.guiTop + 18
            && mouseY <= this.guiTop + 44) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a76--- Smelter Vessel Contents ---");
            tooltip.add(
                "\u00a77Total Charge: " + tileEntity.vesselState()
                    .totalMass());
            tooltip.add(
                "\u00a77Burn Ticks: " + tileEntity.vesselState()
                    .fuelState()
                    .remainingTicks());

            List<AggregateLayer> layers = tileEntity.vesselState()
                .layers();
            if (layers.isEmpty()) {
                tooltip.add("\u00a78(Crucible is empty)");
            } else {
                for (int i = 0; i < layers.size(); i++) {
                    AggregateLayer l = layers.get(i);
                    String phaseCol = l.phaseType() == naturalexceptions.core.metallurgy.AggregatePhaseType.MOLTEN_METAL
                        ? "\u00a7a"
                        : "\u00a7e";
                    tooltip.add(
                        String.format(
                            "%s[%d] %s: %s @ %.1f K (%s)",
                            phaseCol,
                            i,
                            l.phaseType(),
                            l.mass(),
                            l.temperatureKelvin(),
                            l.physicalPhase()));
                }
            }

            if (tileEntity.vesselState()
                .lining()
                .isDamaged()) {
                tooltip.add(
                    String.format(
                        "\u00a7cLining Wear: %.1f%%",
                        tileEntity.vesselState()
                            .lining()
                            .damage() * 100.0));
            }

            this.drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }
}
