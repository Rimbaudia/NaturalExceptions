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
import naturalexceptions.minecraft.fabrication.TileEntityCastingTable;
import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * Renders the hosted casting mold sitting flat on top of the casting table.
 * Uses direct Tessellator rendering bound to the items atlas to guarantee
 * 100% reliable rendering without entity distance-culling or interpolation desync.
 */
@SideOnly(Side.CLIENT)
public final class RenderCastingTable extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntityCastingTable)) {
            return;
        }
        TileEntityCastingTable table = (TileEntityCastingTable) te;
        ItemStack mold = table.getMold();
        if (mold == null || mold.getItem() == null) {
            return;
        }

        IIcon icon = mold.getItem()
            .getIconIndex(mold);
        if (icon == null) {
            icon = mold.getIconIndex();
        }
        if (icon == null) {
            return;
        }

        bindTexture(TextureMap.locationItemsTexture);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        int brightness = te.getWorldObj() != null ? te.getWorldObj()
            .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord + 1, te.zCoord, 0) : 15728880;
        int l1 = brightness % 65536;
        int l2 = brightness / 65536;

        // --- Pass 0: Base Mold Clay Body ---
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        IIcon baseIcon = mold.getItem()
            .getIcon(mold, 0);
        if (baseIcon == null) {
            baseIcon = mold.getIconIndex();
        }

        double minX = x + 0.08D;
        double maxX = x + 0.92D;
        double minZ = z + 0.08D;
        double maxZ = z + 0.92D;
        double yBase = y + 1.001D; // Resting flat on top of the casting table
        double yRim = y + 1.025D; // Top clay mold rim height

        double cavMinX = minX + 0.04D;
        double cavMaxX = maxX - 0.04D;
        double cavMinZ = minZ + 0.04D;
        double cavMaxZ = maxZ - 0.04D;

        Tessellator tessellator = Tessellator.instance;

        boolean hasMetal = ItemCastingMold.hasMetal(mold);
        naturalexceptions.core.metallurgy.MaterialQuantity filled = ItemCastingMold.getFilledMass(mold);
        naturalexceptions.core.metallurgy.MaterialQuantity req = ItemCastingMold.getRequiredMass(mold);
        double fillFraction = 1.0;
        if (req.isPositive() && !ItemCastingMold.isFilled(mold)) {
            fillFraction = Math.max(0.20D, Math.min(0.95D, (double) filled.milligrams() / (double) req.milligrams()));
        }
        // Metal sits recessed below the mold rim (yRim = 1.025D)
        double yMetal = hasMetal ? y + 1.004D + (fillFraction * 0.016D) : yBase;

        // 1. Exterior side walls and base floor of the clay mold body
        if (baseIcon != null) {
            double minU = baseIcon.getMinU();
            double maxU = baseIcon.getMaxU();
            double minV = baseIcon.getMinV();
            double maxV = baseIcon.getMaxV();
            double uW = maxU - minU;
            double vH = maxV - minV;

            // Base floor (inside cavity)
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            tessellator.addVertexWithUV(minX, yBase, minZ, minU, minV);
            tessellator.addVertexWithUV(minX, yBase, maxZ, minU, maxV);
            tessellator.addVertexWithUV(maxX, yBase, maxZ, maxU, maxV);
            tessellator.addVertexWithUV(maxX, yBase, minZ, maxU, minV);
            tessellator.draw();

            // Exterior walls: a thin strip of the texture, proportional to the wall height, not the whole icon.
            double wallV = maxV - vH * Math.min(1.0, (yRim - yBase) / Math.max(1.0e-6, maxX - minX));
            tessellator.startDrawingQuads();
            // North exterior wall (-Z)
            tessellator.setNormal(0.0F, 0.0F, -1.0F);
            GL11.glColor4f(0.75F, 0.72F, 0.68F, 1.0F);
            tessellator.addVertexWithUV(minX, yBase, minZ, minU, maxV);
            tessellator.addVertexWithUV(maxX, yBase, minZ, maxU, maxV);
            tessellator.addVertexWithUV(maxX, yRim, minZ, maxU, wallV);
            tessellator.addVertexWithUV(minX, yRim, minZ, minU, wallV);

            // South exterior wall (+Z)
            tessellator.setNormal(0.0F, 0.0F, 1.0F);
            GL11.glColor4f(0.85F, 0.82F, 0.78F, 1.0F);
            tessellator.addVertexWithUV(minX, yRim, maxZ, minU, wallV);
            tessellator.addVertexWithUV(maxX, yRim, maxZ, maxU, wallV);
            tessellator.addVertexWithUV(maxX, yBase, maxZ, maxU, maxV);
            tessellator.addVertexWithUV(minX, yBase, maxZ, minU, maxV);

            // West exterior wall (-X)
            tessellator.setNormal(-1.0F, 0.0F, 0.0F);
            GL11.glColor4f(0.65F, 0.62F, 0.58F, 1.0F);
            tessellator.addVertexWithUV(minX, yBase, minZ, minU, maxV);
            tessellator.addVertexWithUV(minX, yRim, minZ, minU, wallV);
            tessellator.addVertexWithUV(minX, yRim, maxZ, maxU, wallV);
            tessellator.addVertexWithUV(minX, yBase, maxZ, maxU, maxV);

            // East exterior wall (+X)
            tessellator.setNormal(1.0F, 0.0F, 0.0F);
            GL11.glColor4f(0.70F, 0.68F, 0.64F, 1.0F);
            tessellator.addVertexWithUV(maxX, yBase, maxZ, maxU, maxV);
            tessellator.addVertexWithUV(maxX, yRim, maxZ, maxU, wallV);
            tessellator.addVertexWithUV(maxX, yRim, minZ, minU, wallV);
            tessellator.addVertexWithUV(maxX, yBase, minZ, minU, maxV);
            tessellator.draw();

            // Mold top rim perimeter strips (leaving cavity opening in center)
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            tessellator.startDrawingQuads();
            tessellator.setNormal(0.0F, 1.0F, 0.0F);
            // North rim strip
            tessellator.addVertexWithUV(minX, yRim, minZ, minU, minV);
            tessellator.addVertexWithUV(minX, yRim, cavMinZ, minU, minV + vH * 0.05D);
            tessellator.addVertexWithUV(maxX, yRim, cavMinZ, maxU, minV + vH * 0.05D);
            tessellator.addVertexWithUV(maxX, yRim, minZ, maxU, minV);

            // South rim strip
            tessellator.addVertexWithUV(minX, yRim, cavMaxZ, minU, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(minX, yRim, maxZ, minU, maxV);
            tessellator.addVertexWithUV(maxX, yRim, maxZ, maxU, maxV);
            tessellator.addVertexWithUV(maxX, yRim, cavMaxZ, maxU, maxV - vH * 0.05D);

            // West rim strip
            tessellator.addVertexWithUV(minX, yRim, cavMinZ, minU, minV + vH * 0.05D);
            tessellator.addVertexWithUV(minX, yRim, cavMaxZ, minU, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(cavMinX, yRim, cavMaxZ, minU + uW * 0.05D, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(cavMinX, yRim, cavMinZ, minU + uW * 0.05D, minV + vH * 0.05D);

            // East rim strip
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMinZ, maxU - uW * 0.05D, minV + vH * 0.05D);
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMaxZ, maxU - uW * 0.05D, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(maxX, yRim, cavMaxZ, maxU, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(maxX, yRim, cavMinZ, maxU, minV + vH * 0.05D);
            tessellator.draw();

            // Inner cavity bevels sloping down to metal surface (or base floor)
            GL11.glColor4f(0.55F, 0.52F, 0.48F, 0.90F);
            tessellator.startDrawingQuads();
            // North inner bevel
            tessellator.addVertexWithUV(cavMinX, yRim, cavMinZ, minU, minV);
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMinZ, maxU, minV);
            tessellator.addVertexWithUV(cavMaxX, yMetal, cavMinZ, maxU, minV + vH * 0.05D);
            tessellator.addVertexWithUV(cavMinX, yMetal, cavMinZ, minU, minV + vH * 0.05D);

            // South inner bevel
            tessellator.addVertexWithUV(cavMinX, yMetal, cavMaxZ, minU, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(cavMaxX, yMetal, cavMaxZ, maxU, maxV - vH * 0.05D);
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMaxZ, maxU, maxV);
            tessellator.addVertexWithUV(cavMinX, yRim, cavMaxZ, minU, maxV);

            // West inner bevel
            tessellator.addVertexWithUV(cavMinX, yRim, cavMinZ, minU, minV);
            tessellator.addVertexWithUV(cavMinX, yMetal, cavMinZ, minU, minV + vH * 0.05D);
            tessellator.addVertexWithUV(cavMinX, yMetal, cavMaxZ, minU + uW * 0.05D, maxV);
            tessellator.addVertexWithUV(cavMinX, yRim, cavMaxZ, minU, maxV);

            // East inner bevel
            tessellator.addVertexWithUV(cavMaxX, yMetal, cavMinZ, maxU - uW * 0.05D, minV);
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMinZ, maxU, minV);
            tessellator.addVertexWithUV(cavMaxX, yRim, cavMaxZ, maxU, maxV);
            tessellator.addVertexWithUV(cavMaxX, yMetal, cavMaxZ, maxU - uW * 0.05D, maxV);
            tessellator.draw();
        }

        // --- Pass 1: Recessed Cavity Fill (Molten / Solidified Metal) ---
        if (hasMetal) {
            boolean isSolidified = ItemCastingMold.isSolidified(mold);
            if (!isSolidified) {
                // Molten metal glows brightly with maximum lightmap brightness
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            } else {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);
            }

            int color = mold.getItem()
                .getColorFromItemStack(mold, 1);
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            IIcon fillIcon = mold.getItem()
                .getIcon(mold, 1);
            if (fillIcon != null) {
                double minU = fillIcon.getMinU();
                double maxU = fillIcon.getMaxU();
                double minV = fillIcon.getMinV();
                double maxV = fillIcon.getMaxV();

                // Recessed cast metal surface
                GL11.glColor4f(r, g, b, 1.0F);
                tessellator.startDrawingQuads();
                tessellator.setNormal(0.0F, 1.0F, 0.0F);
                tessellator.addVertexWithUV(minX, yMetal, minZ, minU, minV);
                tessellator.addVertexWithUV(minX, yMetal, maxZ, minU, maxV);
                tessellator.addVertexWithUV(maxX, yMetal, maxZ, maxU, maxV);
                tessellator.addVertexWithUV(maxX, yMetal, minZ, maxU, minV);
                tessellator.draw();
            }
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
