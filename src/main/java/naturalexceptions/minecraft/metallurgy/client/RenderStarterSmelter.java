package naturalexceptions.minecraft.metallurgy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

/**
 * Special renderer for TileEntityStarterSmelter.
 * Renders the workpiece sitting visibly inside the hearth opening,
 * glowing incandescently according to its temperature, with in-world
 * readable thermal feedback.
 */
@SideOnly(Side.CLIENT)
public final class RenderStarterSmelter extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityStarterSmelter)) {
            return;
        }
        TileEntityStarterSmelter smelter = (TileEntityStarterSmelter) te;
        renderVesselContents(smelter, x, y, z);
        ItemStack workpiece = smelter.getHearthWorkpiece();
        if (workpiece == null || workpiece.getItem() == null) {
            return;
        }

        IIcon icon = workpiece.getItem()
            .getIconIndex(workpiece);
        if (icon == null) {
            icon = workpiece.getIconIndex();
        }
        if (icon == null) {
            return;
        }

        int meta = te.getBlockMetadata();
        // 2=North (Z-), 3=South (Z+), 4=West (X-), 5=East (X+)
        int frontSide = (meta == 0) ? 3 : meta;

        bindTexture(TextureMap.locationItemsTexture);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Center on block
        GL11.glTranslated(x + 0.5D, y, z + 0.5D);

        // Rotate based on front face
        switch (frontSide) {
            case 2: // North
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case 4: // West
                GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case 5: // East
                GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case 3: // South (default)
            default:
                break;
        }

        // Now local coordinates: +Z is toward front opening, Y is vertical, X is horizontal across mouth
        double halfW = 0.22D;
        double minX = -halfW;
        double maxX = halfW;
        double minZ = 0.20D;
        double maxZ = 0.48D;
        double yFace = 0.16D;

        int brightness = te.getWorldObj() != null ? te.getWorldObj()
            .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord, te.zCoord, 0) : 15728880;
        int l1 = brightness % 65536;
        int l2 = brightness / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);

        int colorInt = workpiece.getItem()
            .getColorFromItemStack(workpiece, 0);
        float red = (float) (colorInt >> 16 & 255) / 255.0F;
        float green = (float) (colorInt >> 8 & 255) / 255.0F;
        float blue = (float) (colorInt & 255) / 255.0F;
        GL11.glColor4f(red, green, blue, 1.0F);

        Tessellator tessellator = Tessellator.instance;
        double minU = icon.getMinU();
        double maxU = icon.getMaxU();
        double minV = icon.getMinV();
        double maxV = icon.getMaxV();

        // 1. Base workpiece quad (resting in hearth throat, tilted slightly)
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(minX, yFace + 0.04D, minZ, minU, maxV);
        tessellator.addVertexWithUV(maxX, yFace + 0.04D, minZ, maxU, maxV);
        tessellator.addVertexWithUV(maxX, yFace, maxZ, maxU, minV);
        tessellator.addVertexWithUV(minX, yFace, maxZ, minU, minV);
        tessellator.draw();

        // 2. Continuous thermal incandescence glow when hot
        ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
        if (thermal != null && thermal.temperatureKelvin() > 550.0) {
            double temp = thermal.temperatureKelvin();
            float[] inc = MaterialAppearance.getThermalIncandescenceRgba(temp);
            if (inc[3] > 0.01F) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                GL11.glColor4f(inc[0], inc[1], inc[2], inc[3]);
                double yGlow = yFace + 0.002D;

                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(minX, yGlow + 0.04D, minZ, minU, maxV);
                tessellator.addVertexWithUV(maxX, yGlow + 0.04D, minZ, maxU, maxV);
                tessellator.addVertexWithUV(maxX, yGlow, maxZ, maxU, minV);
                tessellator.addVertexWithUV(minX, yGlow, maxZ, minU, minV);
                tessellator.draw();
            }
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();

        // 3. Floating label: only when directly targeted by crosshair with active workpiece, never when idle
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.renderViewEntity != null && !mc.gameSettings.hideGUI && workpiece != null) {
            net.minecraft.util.MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                && mop.blockX == te.xCoord
                && mop.blockY == te.yCoord
                && mop.blockZ == te.zCoord) {
                double dx = mc.renderViewEntity.posX - (te.xCoord + 0.5D);
                double dy = mc.renderViewEntity.posY - (te.yCoord + 0.5D);
                double dz = mc.renderViewEntity.posZ - (te.zCoord + 0.5D);
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq <= 16.0D) {
                    renderHearthLabel(workpiece, thermal, x, y, z);
                }
            }
        }
    }

    private void renderVesselContents(TileEntityStarterSmelter smelter, double x, double y, double z) {
        naturalexceptions.minecraft.multiblock.SmelterVesselGeometry geometry = smelter.vesselGeometry();
        if (geometry == null || smelter.vesselCapacityLitres() <= 0.0) return;
        java.util.List<AggregateLayer> layers = smelter.vesselState()
            .layers();
        if (layers.isEmpty()) return;
        if (geometry.cavityCellCount() == 0) return;
        int half = (geometry.interiorWidth() + 2) / 2;
        int minLocalX = -half + 1, maxLocalX = half - 1;
        int minLocalZ = 1, maxLocalZ = geometry.interiorDepth();
        int[] p00 = geometry.world(minLocalX, 0, minLocalZ);
        int[] pX = geometry.world(maxLocalX, 0, minLocalZ);
        int[] pZ = geometry.world(minLocalX, 0, maxLocalZ);
        int[] pXZ = geometry.world(maxLocalX, 0, maxLocalZ);
        int[] basisX = geometry.world(minLocalX + 1, 0, minLocalZ);
        int[] basisZ = geometry.world(minLocalX, 0, minLocalZ + 1);
        double[] q00 = localCorner(p00, smelter);
        double[] q10 = localCorner(pX, smelter, basisX[0] - p00[0], basisX[2] - p00[2]);
        double[] q01 = localCorner(pZ, smelter, basisZ[0] - p00[0], basisZ[2] - p00[2]);
        double[] q11 = localCorner(
            pXZ,
            smelter,
            basisX[0] - p00[0] + basisZ[0] - p00[0],
            basisX[2] - p00[2] + basisZ[2] - p00[2]);
        double baseY = 0.04D;
        double yCursor = baseY;
        int[] top0 = geometry.world(0, geometry.interiorHeight(), 1);
        int roomLight = smelter.getWorldObj() != null ? smelter.getWorldObj()
            .getLightBrightnessForSkyBlocks(top0[0], top0[1], top0[2], 0) : 0xF000F0;
        // The cavity may be up to three blocks tall; contents stop just under its top.
        double ceiling = geometry.interiorHeight() - 0.06D;
        for (AggregateLayer layer : layers) {
            boolean molten = layer.phaseType() == AggregatePhaseType.MOLTEN_METAL
                || layer.phaseType() == AggregatePhaseType.SLAG;
            boolean charge = layer.phaseType() == AggregatePhaseType.SOLID_CHARGE;
            if (!molten && !charge) continue;
            double layerHeight = geometry.heightForVolume(layer.occupiedVolumeLitres());
            if (layerHeight <= 0.0D || yCursor >= ceiling) continue;
            // A cavity cell holds 25 L, so a few ores' worth of metal is a film; keep every layer visible.
            layerHeight = Math.max(MIN_VISIBLE_LAYER, layerHeight);
            int color = molten
                ? MaterialAppearance.getMoltenColor(layer.materialRef(), layer.stateKey(), layer.temperatureKelvin())
                : darken(MaterialAppearance.getSolidColor(layer.materialRef(), layer.stateKey()), 0.55F);
            // Molten metal and slag glow; unmelted charge is lit like the room around it.
            int light = molten ? 0xF000F0 : roomLight;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            float red = (color >> 16 & 255) / 255.0F;
            float green = (color >> 8 & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glColor4f(red, green, blue, 0.96F);
            Tessellator t = Tessellator.instance;
            double bottom = yCursor;
            double top = Math.min(ceiling, bottom + layerHeight);
            t.startDrawingQuads();
            // The local-to-world transformed corners preserve the true rectangle for all rotations.
            t.addVertex(q00[0], top, q00[1]);
            t.addVertex(q01[0], top, q01[1]);
            t.addVertex(q11[0], top, q11[1]);
            t.addVertex(q10[0], top, q10[1]);
            t.addVertex(q00[0], bottom, q00[1]);
            t.addVertex(q10[0], bottom, q10[1]);
            t.addVertex(q10[0], top, q10[1]);
            t.addVertex(q00[0], top, q00[1]);
            t.addVertex(q11[0], bottom, q11[1]);
            t.addVertex(q01[0], bottom, q01[1]);
            t.addVertex(q01[0], top, q01[1]);
            t.addVertex(q11[0], top, q11[1]);
            t.addVertex(q01[0], bottom, q01[1]);
            t.addVertex(q00[0], bottom, q00[1]);
            t.addVertex(q00[0], top, q00[1]);
            t.addVertex(q01[0], top, q01[1]);
            t.addVertex(q10[0], bottom, q10[1]);
            t.addVertex(q11[0], bottom, q11[1]);
            t.addVertex(q11[0], top, q11[1]);
            t.addVertex(q10[0], top, q10[1]);
            t.draw();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glPopMatrix();
            yCursor = top;
        }
    }

    /** Thinnest layer drawn, in blocks: one texel. */
    private static final double MIN_VISIBLE_LAYER = 1.0D / 16.0D;

    private static int darken(int rgb, float factor) {
        int r = Math.round(((rgb >> 16) & 0xFF) * factor);
        int g = Math.round(((rgb >> 8) & 0xFF) * factor);
        int b = Math.round((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private static double[] localCorner(int[] block, TileEntityStarterSmelter smelter) {
        return new double[] { block[0] - smelter.xCoord, block[2] - smelter.zCoord };
    }

    private static double[] localCorner(int[] block, TileEntityStarterSmelter smelter, int dx, int dz) {
        return new double[] { block[0] - smelter.xCoord + dx, block[2] - smelter.zCoord + dz };
    }

    private void renderHearthLabel(ItemStack workpiece, ActiveThermalState thermal, double x, double y, double z) {
        FontRenderer font = func_147498_b();
        if (font == null) return;

        RenderManager rm = RenderManager.instance;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 0.75D, z + 0.5D);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        float scale = 0.011F;
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        ComponentInstance comp = ItemGenericComponent.getComponent(workpiece);
        MaterialProperties props = ItemGenericComponent.resolveProperties(comp);
        double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
        ItemGenericComponent.ThermalCondition cond = ItemGenericComponent.getThermalCondition(tempK, props);

        String titleStr = "\u00a76Hearth: \u00a7f" + workpiece.getDisplayName();
        String heatStr = "Heat: " + cond.label();

        font.drawStringWithShadow(titleStr, -font.getStringWidth(titleStr) / 2, -10, 0xFFFFFF);
        font.drawStringWithShadow(heatStr, -font.getStringWidth(heatStr) / 2, 0, 0xFFFFFF);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
