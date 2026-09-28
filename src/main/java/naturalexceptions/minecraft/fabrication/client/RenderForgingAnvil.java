package naturalexceptions.minecraft.fabrication.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.fabrication.TileEntityForgingAnvil;
import naturalexceptions.minecraft.item.ItemGenericComponent;

/**
 * Special renderer for TileEntityForgingAnvil.
 * Renders the hosted workpiece resting flat on the anvil striking face,
 * along with dynamic incandescent thermal emission when hot.
 */
@SideOnly(Side.CLIENT)
public final class RenderForgingAnvil extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityForgingAnvil)) {
            return;
        }
        TileEntityForgingAnvil anvil = (TileEntityForgingAnvil) te;
        ItemStack workpiece = anvil.getWorkpiece();
        if (workpiece != null && workpiece.getItem() != null) {
            IIcon icon = workpiece.getItem()
                .getIconIndex(workpiece);
            if (icon == null) {
                icon = workpiece.getIconIndex();
            }
            if (icon != null) {
                bindTexture(TextureMap.locationItemsTexture);

                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
                GL11.glDisable(GL11.GL_CULL_FACE);

                int brightness = te.getWorldObj() != null ? te.getWorldObj()
                    .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord + 1, te.zCoord, 0) : 15728880;
                int l1 = brightness % 65536;
                int l2 = brightness / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);

                int colorInt = workpiece.getItem()
                    .getColorFromItemStack(workpiece, 0);
                float red = (float) (colorInt >> 16 & 255) / 255.0F;
                float green = (float) (colorInt >> 8 & 255) / 255.0F;
                float blue = (float) (colorInt & 255) / 255.0F;
                GL11.glColor4f(red, green, blue, 1.0F);

                double minX = x + 0.22D;
                double maxX = x + 0.78D;
                double minZ = z + 0.22D;
                double maxZ = z + 0.78D;
                double yFace = y + 1.002D;

                Tessellator tessellator = Tessellator.instance;
                double minU = icon.getMinU();
                double maxU = icon.getMaxU();
                double minV = icon.getMinV();
                double maxV = icon.getMaxV();

                // 1. Base workpiece rendering
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(minX, yFace, maxZ, minU, maxV);
                tessellator.addVertexWithUV(maxX, yFace, maxZ, maxU, maxV);
                tessellator.addVertexWithUV(maxX, yFace, minZ, maxU, minV);
                tessellator.addVertexWithUV(minX, yFace, minZ, minU, minV);
                tessellator.draw();

                // 2. Thermal incandescence glow when workpiece is hot (continuous blackbody response)
                ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
                if (thermal != null && thermal.temperatureKelvin() > 550.0) {
                    double temp = thermal.temperatureKelvin();
                    float[] incandescence = naturalexceptions.core.material.MaterialAppearance
                        .getThermalIncandescenceRgba(temp);
                    if (incandescence[3] > 0.01F) {
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                        GL11.glColor4f(incandescence[0], incandescence[1], incandescence[2], incandescence[3]);
                        double yGlow = yFace + 0.001D;

                        tessellator.startDrawingQuads();
                        tessellator.addVertexWithUV(minX, yGlow, maxZ, minU, maxV);
                        tessellator.addVertexWithUV(maxX, yGlow, maxZ, maxU, maxV);
                        tessellator.addVertexWithUV(maxX, yGlow, minZ, maxU, minV);
                        tessellator.addVertexWithUV(minX, yGlow, minZ, minU, minV);
                        tessellator.draw();
                    }
                }

                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }

        // 3. In-world floating target and thermal condition label: only when directly targeted with workpiece
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.renderViewEntity != null && !mc.gameSettings.hideGUI && workpiece != null) {
            net.minecraft.util.MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK
                && mop.blockX == te.xCoord
                && mop.blockY == te.yCoord
                && mop.blockZ == te.zCoord) {
                double dx = mc.renderViewEntity.posX - (te.xCoord + 0.5D);
                double dy = mc.renderViewEntity.posY - (te.yCoord + 1.0D);
                double dz = mc.renderViewEntity.posZ - (te.zCoord + 0.5D);
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq <= 16.0D) {
                    renderFloatingLabel(anvil, workpiece, x, y, z);
                }
            }
        }
    }

    private void renderFloatingLabel(TileEntityForgingAnvil anvil, ItemStack workpiece, double x, double y, double z) {
        net.minecraft.client.gui.FontRenderer font = func_147498_b();
        if (font == null) return;

        net.minecraft.client.renderer.entity.RenderManager rm = net.minecraft.client.renderer.entity.RenderManager.instance;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 1.32D, z + 0.5D);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        float scale = 0.013F;
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        naturalexceptions.core.fabrication.GeometryId target = anvil.getTargetGeometry();
        String targetStr = "\u00a76Target: \u00a7f" + ItemGenericComponent.formatGeometryName(target);

        if (workpiece != null) {
            naturalexceptions.core.fabrication.ComponentInstance comp = ItemGenericComponent.getComponent(workpiece);
            ActiveThermalState thermal = ItemGenericComponent.getThermalState(workpiece);
            naturalexceptions.core.property.MaterialProperties props = ItemGenericComponent.resolveProperties(comp);
            double tempK = thermal != null ? thermal.temperatureKelvin() : 293.15;
            ItemGenericComponent.ThermalCondition cond = ItemGenericComponent.getThermalCondition(tempK, props);

            String stageStr;
            if (comp != null
                && comp.manufacturingState() == naturalexceptions.core.fabrication.ManufacturingState.FINISHED) {
                stageStr = "\u00a7a[Finished]";
            } else if (comp != null && comp.geometryId() == target) {
                stageStr = "\u00a7e[Stage: Planishing]";
            } else {
                stageStr = "\u00a77["
                    + (comp != null ? ItemGenericComponent.formatGeometryName(comp.geometryId()) : "Stock")
                    + " \u2192 "
                    + ItemGenericComponent.formatGeometryName(target)
                    + "]";
            }

            String heatStr = "Heat: " + cond.label();

            font.drawStringWithShadow(targetStr, -font.getStringWidth(targetStr) / 2, -18, 0xFFFFFF);
            font.drawStringWithShadow(stageStr, -font.getStringWidth(stageStr) / 2, -8, 0xFFFFFF);
            font.drawStringWithShadow(heatStr, -font.getStringWidth(heatStr) / 2, 2, 0xFFFFFF);
        } else {
            String emptyStr = "\u00a77(Empty - Place stock)";
            String tipStr = "\u00a78[Sneak-Click to cycle target]";
            font.drawStringWithShadow(targetStr, -font.getStringWidth(targetStr) / 2, -12, 0xFFFFFF);
            font.drawStringWithShadow(emptyStr, -font.getStringWidth(emptyStr) / 2, -2, 0xAAAAAA);
            font.drawStringWithShadow(tipStr, -font.getStringWidth(tipStr) / 2, 8, 0x888888);
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
