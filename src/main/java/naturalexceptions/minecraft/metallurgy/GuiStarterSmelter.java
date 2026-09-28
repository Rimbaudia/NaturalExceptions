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

    /** Heat gauge spans ambient to a white-hot hearth. */
    private static final double GAUGE_MIN_K = 293.15;
    private static final double GAUGE_MAX_K = 2000.0;
    /** Fire gauge reads full at the auto-feed buffer (about 160 s of burning). */
    private static final int FIRE_GAUGE_TICKS = 3200;

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString("Starter Smelter", 8, 6, 0x404040);
        String structure = tileEntity.isStructureValid() ? "Formed" : "Incomplete";
        int structureColour = tileEntity.isStructureValid() ? 0x2E7D32 : 0xAA0000;
        this.fontRendererObj.drawString(structure, 168 - fontRendererObj.getStringWidth(structure), 6, structureColour);

        this.fontRendererObj.drawString("Feed", 8, 24, 0x404040);
        this.fontRendererObj.drawString("Fuel", 8, 58, 0x404040);

        double hottest = hottestLayerTemperature();
        this.fontRendererObj
            .drawString(trim("Heat: " + heatBand(hottest), 110), 58, 54, hottest > 800 ? 0xAA5500 : 0x404040);
        this.fontRendererObj.drawString(trim("Fire: " + fireText(), 110), 58, 64, 0x404040);
        SmelterOutletState state = tileEntity.outletState();
        int stateColour = state == SmelterOutletState.READY || state == SmelterOutletState.POURING ? 0x2E7D32
            : 0x404040;
        this.fontRendererObj.drawString(trim("Tap: " + state.shortText(), 110), 58, 74, stateColour);
    }

    private String fireText() {
        int ticks = tileEntity.vesselState()
            .fuelState()
            .remainingTicks();
        if (ticks <= 0)
            return tileEntity.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL) != null ? "catching" : "out, add fuel";
        return "burning, ~" + Math.max(1, ticks / 20) + "s";
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
        drawHeatGauge(k + 134, l + 13, 7, 35);
        drawFireGauge(k + 151, l + 13, 7, 35);
    }

    private void drawHeatGauge(int x, int y, int width, int height) {
        double hottest = hottestLayerTemperature();
        double fraction = Math.max(0.0, Math.min(1.0, (hottest - GAUGE_MIN_K) / (GAUGE_MAX_K - GAUGE_MIN_K)));
        int filled = (int) Math.round(fraction * height);
        if (filled <= 0) return;
        float[] rgba = MaterialAppearance.getThermalIncandescenceRgba(Math.max(900.0, hottest));
        int glow = 0xFF000000 | (int) (rgba[0] * 255) << 16 | (int) (rgba[1] * 255) << 8 | (int) (rgba[2] * 255);
        int colour = hottest < 800 ? 0xFF8A3B24 : glow;
        drawRect(x, y + height - filled, x + width, y + height, colour);
    }

    private void drawFireGauge(int x, int y, int width, int height) {
        int ticks = tileEntity.vesselState()
            .fuelState()
            .remainingTicks();
        int filled = (int) Math.round(Math.min(1.0, ticks / (double) FIRE_GAUGE_TICKS) * height);
        if (filled <= 0) return;
        drawRect(x, y + height - filled, x + width, y + height, 0xFFE0701C);
        drawRect(x, y + height - filled, x + width, Math.min(y + height, y + height - filled + 2), 0xFFFFC23A);
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
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        List<String> tooltip = new ArrayList<>();
        if (x >= 61 && x <= 125 && y >= 10 && y <= 50) {
            contentsTooltip(tooltip);
        } else if (x >= 133 && x <= 142 && y >= 12 && y <= 49) {
            tooltip.add("\u00a76Heat");
            tooltip.add("\u00a77Hottest contents: " + heatBand(hottestLayerTemperature()));
            tooltip.add("\u00a78Copper flows once it reaches molten heat.");
        } else if (x >= 150 && x <= 159 && y >= 12 && y <= 49) {
            tooltip.add("\u00a76Fire");
            tooltip.add("\u00a77" + fireText());
            if (tileEntity.vesselState()
                .fuelState()
                .isBurning()) {
                tooltip.add(
                    "\u00a77Burning " + tileEntity.vesselState()
                        .fuelState()
                        .fuelLabel());
            }
            tooltip.add("\u00a78Fuel in the slot is added as the fire burns down.");
        } else if (x >= 56 && x <= 170 && y >= 72 && y <= 83) {
            SmelterOutletState state = tileEntity.outletState();
            tooltip.add("\u00a76Pouring");
            tooltip.add("\u00a77" + state.hint());
            tooltip.add("\u00a78Molten metal leaves only through the outlet spout.");
        } else if (x >= 110 && y >= 4 && y <= 15 && !tileEntity.isStructureValid()) {
            tooltip.add("\u00a7cThe structure is not complete");
            if (tileEntity.getStructureValidationResult() != null && tileEntity.getStructureValidationResult()
                .failureReason() != null) {
                tooltip.add(
                    "\u00a77" + tileEntity.getStructureValidationResult()
                        .failureReason());
            }
            tooltip.add("\u00a78See the Knowledge Browser (K) for the layout.");
        }
        if (!tooltip.isEmpty()) this.drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
    }

    private void contentsTooltip(List<String> tooltip) {
        tooltip.add("\u00a76Crucible");
        tooltip.add("\u00a77" + capitalise(fillBand(tileEntity.fillFraction())));
        List<AggregateLayer> layers = tileEntity.vesselState()
            .layers();
        if (layers.isEmpty()) {
            tooltip.add("\u00a78Empty. Put ore or scrap metal in the feed slot.");
        } else {
            for (int i = layers.size() - 1; i >= 0; i--) {
                AggregateLayer l = layers.get(i);
                String state = l.phaseType() == AggregatePhaseType.SLAG ? "slag; sneak-use an empty ladle to skim"
                    : l.phaseType() == AggregatePhaseType.SOLID_CHARGE ? "unmelted charge"
                        : l.isPourable() ? "molten, can be poured" : "softening, not yet pourable";
                String materialName = l.phaseType() == AggregatePhaseType.SLAG ? "Slag"
                    : naturalexceptions.minecraft.knowledge.KnowledgePresentation
                        .materialName(this.mc.thePlayer, l.materialRef(), l.stateKey());
                tooltip.add(String.format("\u00a7f%s\u00a77: %s", materialName, state));
            }
        }
        if (tileEntity.vesselState()
            .lining()
            .isDamaged()) {
            tooltip.add("\u00a7cThe refractory lining is showing heat damage.");
        }
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private double hottestLayerTemperature() {
        double hottest = 293.15;
        for (AggregateLayer layer : tileEntity.vesselState()
            .layers()) hottest = Math.max(hottest, layer.temperatureKelvin());
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
