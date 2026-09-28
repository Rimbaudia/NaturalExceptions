package naturalexceptions.minecraft.metallurgy.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.metallurgy.TileEntitySmelterOutlet;

/**
 * Renders the flowing molten metal; static spout geometry belongs to the block renderer.
 */
@SideOnly(Side.CLIENT)
public final class RenderSmelterOutlet extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (!(te instanceof TileEntitySmelterOutlet)) {
            return;
        }
        TileEntitySmelterOutlet outlet = (TileEntitySmelterOutlet) te;
        int facing = outlet.getFacing();

        if (!outlet.isPouring()) return;

        bindTexture(TextureMap.locationBlocksTexture);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Static spout parts are rendered in the chunk and inventory block renderer.

        // 2. Render Flowing Liquid Metal Stream when pouring!
        if (outlet.isPouring()) {
            naturalexceptions.minecraft.fabrication.TileEntityCastingTable targetTable = outlet
                .findTargetCastingTable();
            double targetDistY = 1.0D;
            if (targetTable != null) {
                targetDistY = (double) (outlet.yCoord - targetTable.yCoord);
            }
            renderMoltenStream(
                facing,
                targetDistY,
                outlet.getStreamColor(),
                outlet.getWorldObj() != null ? outlet.getWorldObj()
                    .getTotalWorldTime() : 0L,
                partialTicks);
        }

        GL11.glPopMatrix();
    }

    private void renderMoltenStream(int facing, double targetDistY, int streamColor, long worldTime,
        float partialTicks) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Glowing molten brightness
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        float r = ((streamColor >> 16) & 0xFF) / 255.0F;
        float g = ((streamColor >> 8) & 0xFF) / 255.0F;
        float b = (streamColor & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, 0.95F);

        double streamX = naturalexceptions.minecraft.metallurgy.SmelterOutletGeometry.STREAM_X;
        double streamZ = naturalexceptions.minecraft.metallurgy.SmelterOutletGeometry.STREAM_Z;
        double topY = naturalexceptions.minecraft.metallurgy.SmelterOutletGeometry.STREAM_Y;
        // Reach down to table surface (or cavity top) below
        double bottomY = targetDistY > 0.0D ? (1.0D - targetDistY) + 0.02D : -0.92D;
        double radius = 0.045D;

        IIcon lavaIcon = net.minecraft.init.Blocks.flowing_lava != null
            ? net.minecraft.init.Blocks.flowing_lava.getIcon(0, 0)
            : (net.minecraft.init.Blocks.lava != null ? net.minecraft.init.Blocks.lava.getIcon(0, 0) : null);

        double minU = lavaIcon != null ? lavaIcon.getMinU() : 0.0D;
        double maxU = lavaIcon != null ? lavaIcon.getMaxU() : 1.0D;
        double minV = lavaIcon != null ? lavaIcon.getMinV() : 0.0D;
        double maxV = lavaIcon != null ? lavaIcon.getMaxV() : 1.0D;

        double v1 = minV;
        double v2 = maxV;

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // 1. Four outer column faces (3D cylindrical flow)
        // North face (-Z)
        tess.setNormal(0.0F, 0.0F, -1.0F);
        tess.addVertexWithUV(streamX - radius, bottomY, streamZ - radius, minU, v2);
        tess.addVertexWithUV(streamX + radius, bottomY, streamZ - radius, maxU, v2);
        tess.addVertexWithUV(streamX + radius, topY, streamZ - radius, maxU, v1);
        tess.addVertexWithUV(streamX - radius, topY, streamZ - radius, minU, v1);

        // South face (+Z)
        tess.setNormal(0.0F, 0.0F, 1.0F);
        tess.addVertexWithUV(streamX + radius, bottomY, streamZ + radius, minU, v2);
        tess.addVertexWithUV(streamX - radius, bottomY, streamZ + radius, maxU, v2);
        tess.addVertexWithUV(streamX - radius, topY, streamZ + radius, maxU, v1);
        tess.addVertexWithUV(streamX + radius, topY, streamZ + radius, minU, v1);

        // West face (-X)
        tess.setNormal(-1.0F, 0.0F, 0.0F);
        tess.addVertexWithUV(streamX - radius, bottomY, streamZ + radius, minU, v2);
        tess.addVertexWithUV(streamX - radius, bottomY, streamZ - radius, maxU, v2);
        tess.addVertexWithUV(streamX - radius, topY, streamZ - radius, maxU, v1);
        tess.addVertexWithUV(streamX - radius, topY, streamZ + radius, minU, v1);

        // East face (+X)
        tess.setNormal(1.0F, 0.0F, 0.0F);
        tess.addVertexWithUV(streamX + radius, bottomY, streamZ - radius, minU, v2);
        tess.addVertexWithUV(streamX + radius, bottomY, streamZ + radius, maxU, v2);
        tess.addVertexWithUV(streamX + radius, topY, streamZ + radius, maxU, v1);
        tess.addVertexWithUV(streamX + radius, topY, streamZ - radius, minU, v1);

        // 2. Volumetric internal cross-core
        double diagRadius = radius * 1.3D;
        tess.addVertexWithUV(streamX - diagRadius, bottomY, streamZ - diagRadius, minU, v2);
        tess.addVertexWithUV(streamX + diagRadius, bottomY, streamZ + diagRadius, maxU, v2);
        tess.addVertexWithUV(streamX + diagRadius, topY, streamZ + diagRadius, maxU, v1);
        tess.addVertexWithUV(streamX - diagRadius, topY, streamZ - diagRadius, minU, v1);

        tess.addVertexWithUV(streamX - diagRadius, bottomY, streamZ + diagRadius, minU, v2);
        tess.addVertexWithUV(streamX + diagRadius, bottomY, streamZ - diagRadius, maxU, v2);
        tess.addVertexWithUV(streamX + diagRadius, topY, streamZ - diagRadius, maxU, v1);
        tess.addVertexWithUV(streamX - diagRadius, topY, streamZ + diagRadius, minU, v1);

        // 3. Impact splash pool on receiving table
        double splashRad = radius * 2.2D;
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(streamX - splashRad, bottomY + 0.005D, streamZ - splashRad, minU, minV);
        tess.addVertexWithUV(streamX - splashRad, bottomY + 0.005D, streamZ + splashRad, minU, maxV);
        tess.addVertexWithUV(streamX + splashRad, bottomY + 0.005D, streamZ + splashRad, maxU, maxV);
        tess.addVertexWithUV(streamX + splashRad, bottomY + 0.005D, streamZ - splashRad, maxU, minV);

        tess.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
