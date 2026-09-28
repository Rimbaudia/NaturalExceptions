package naturalexceptions.minecraft.metallurgy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;

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
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Title
        this.fontRendererObj.drawString("Starter Smelter", 8, 6, 0x404040);

        // Status
        if (tileEntity.isStructureValid()) {
            this.fontRendererObj.drawString("\u00a72Formed", 134, 6, 0x008000);
        } else {
            this.fontRendererObj.drawString("\u00a74Broken", 130, 6, 0xAA0000);
        }

        double maxTemp = 293.15;
        for (AggregateLayer layer : tileEntity.vesselState()
            .layers()) maxTemp = Math.max(maxTemp, layer.temperatureKelvin());
        String capacity = fillBand(tileEntity.fillFraction());
        this.fontRendererObj.drawString(trim(String.format("%s  %s", heatBand(maxTemp), capacity), 70), 58, 55,
            maxTemp > 600 ? 0xAA5500 : 0x404040);
        this.fontRendererObj.drawString(
            trim(
                String.format(
                    "Fill %s  Fuel %s",
                    fillBand(tileEntity.fillFraction()),
                    tileEntity.vesselState()
                        .fuelState()
                        .isBurning() ? "burning" : "idle"),
                70),
            58,
            65,
            0x404040);
        this.fontRendererObj.drawString(trim("Outlet " + compactOutletStatus(), 70), 58, 75, 0x404040);
    }

    private String compactOutletStatus() {
        String status = tileEntity.outletStatus();
        if ("NO ACCESSIBLE LIQUID".equals(status)) return "no liquid";
        if ("RECEIVER MISSING".equals(status)) return "need receiver";
        if ("RECEIVER FULL".equals(status)) return "receiver full";
        return status.toLowerCase(java.util.Locale.ROOT);
    }

    private String trim(String text, int width) {
        String result = text;
        while (!result.isEmpty() && fontRendererObj.getStringWidth(result) > width)
            result = result.substring(0, result.length() - 1);
        return result;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager()
            .bindTexture(GUI_TEXTURE);
        int k = this.guiLeft;
        int l = this.guiTop;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
        drawLayerStack(k + 63, l + 12, 60, 37);
    }

    private void drawLayerStack(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, 0xFF373737);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFB8B8B8);
        double capacity = tileEntity.vesselCapacityLitres();
        if (capacity <= 0.0) return;
        int bottom = y + height - 1;
        for (AggregateLayer layer : tileEntity.vesselState()
            .layers()) {
            int segment = (int) Math.ceil(height * layer.occupiedVolumeLitres() / capacity);
            if (segment <= 0) continue;
            int color = layer.phaseType() == AggregatePhaseType.SLAG ? 0xFF51463B
                : MaterialAppearance.getMoltenColor(layer.materialRef(), layer.stateKey(), layer.temperatureKelvin())
                    | 0xFF000000;
            int top = Math.max(y + 1, bottom - segment);
            drawRect(x + 1, top, x + width - 1, bottom, color);
            bottom = top;
            if (bottom <= y + 1) break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Hover over monitor area (x between guiLeft + 61 and guiLeft + 120, y between guiTop + 18 and guiTop + 44)
        if (mouseX >= this.guiLeft + 61 && mouseX <= this.guiLeft + 125
            && mouseY >= this.guiTop + 10
            && mouseY <= this.guiTop + 50) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("\u00a76--- Smelter contents ---");
            tooltip.add("\u00a77Charge: " + fillBand(tileEntity.fillFraction()));
            if (tileEntity.vesselState()
                .fuelState()
                .isBurning()) {
                tooltip.add(
                    "\u00a77Fuel: \u00a7f" + tileEntity.vesselState()
                        .fuelState()
                        .fuelLabel());
            }
            tooltip.add("\u00a77Heat: " + heatBand(hottestLayerTemperature()));

            List<AggregateLayer> layers = tileEntity.vesselState()
                .layers();
            if (layers.isEmpty()) {
                tooltip.add("\u00a78(Crucible is empty)");
            } else {
                for (int i = 0; i < layers.size(); i++) {
                    AggregateLayer l = layers.get(i);
                    String phase = l.phaseType() == AggregatePhaseType.SLAG ? "dark residue"
                        : l.physicalPhase() == naturalexceptions.core.material.MaterialPhase.LIQUID ? "liquid metal"
                            : "solid metal";
                    String materialName = naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(
                        this.mc.thePlayer, l.materialRef(), l.stateKey());
                    tooltip.add(String.format("\u00a77Layer %d: %s, %s, %s", i + 1, materialName, phase,
                        heatBand(l.temperatureKelvin())));
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

    private double hottestLayerTemperature() {
        double hottest = 293.15;
        for (AggregateLayer layer : tileEntity.vesselState().layers()) hottest = Math.max(hottest, layer.temperatureKelvin());
        return hottest;
    }

    private static String fillBand(double fill) {
        if (fill <= 0.01) return "empty";
        if (fill < 0.30) return "low";
        if (fill < 0.70) return "partly filled";
        if (fill < 0.97) return "nearly full";
        return "full";
    }

    private static String heatBand(double kelvin) {
        if (kelvin < 330) return "cool";
        if (kelvin < 500) return "warm";
        if (kelvin < 800) return "hot";
        if (kelvin < 1100) return "glowing hot";
        return "molten heat";
    }
}
