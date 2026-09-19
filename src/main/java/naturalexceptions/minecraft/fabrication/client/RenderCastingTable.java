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

        boolean isFilled = ItemCastingMold.isFilled(mold);
        boolean isSolidified = ItemCastingMold.isSolidified(mold);

        if (isFilled && !isSolidified) {
            // Molten metal glows brightly with maximum lightmap brightness
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        } else {
            int brightness = te.getWorldObj() != null ? te.getWorldObj()
                .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord + 1, te.zCoord, 0) : 15728880;
            int l1 = brightness % 65536;
            int l2 = brightness / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) l1, (float) l2);
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        double minU = icon.getMinU();
        double maxU = icon.getMaxU();
        double minV = icon.getMinV();
        double maxV = icon.getMaxV();

        // The mold sits centered on top of the casting table: y + 1.002 to avoid z-fighting
        double minX = x + 0.08D;
        double maxX = x + 0.92D;
        double minZ = z + 0.08D;
        double maxZ = z + 0.92D;
        double yPos = y + 1.002D;

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.addVertexWithUV(minX, yPos, minZ, minU, minV);
        tessellator.addVertexWithUV(minX, yPos, maxZ, minU, maxV);
        tessellator.addVertexWithUV(maxX, yPos, maxZ, maxU, maxV);
        tessellator.addVertexWithUV(maxX, yPos, minZ, maxU, minV);
        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
