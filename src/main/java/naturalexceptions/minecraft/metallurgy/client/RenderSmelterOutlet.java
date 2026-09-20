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
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Renders the Smelter Outlet spout nozzle and the animated flowing molten metal stream.
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

        IIcon brickIcon = NEBlocks.refractoryBrick != null ? NEBlocks.refractoryBrick.getIcon(0, 0) : null;
        if (brickIcon == null) {
            return;
        }

        bindTexture(TextureMap.locationBlocksTexture);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // 1. Render Spout Geometry (Nozzle channel)
        renderSpout(facing, brickIcon);

        // 2. Render Flowing Liquid Metal Stream when pouring!
        if (outlet.isPouring()) {
            renderMoltenStream(
                facing,
                outlet.getWorldObj() != null ? outlet.getWorldObj()
                    .getTotalWorldTime() : 0L,
                partialTicks);
        }

        GL11.glPopMatrix();
    }

    private void renderSpout(int facing, IIcon icon) {
        Tessellator tess = Tessellator.instance;
        double minU = icon.getMinU();
        double maxU = icon.getMaxU();
        double minV = icon.getMinV();
        double maxV = icon.getMaxV();

        // Dimensions of the spout channel
        double minX = 0.30D, maxX = 0.70D;
        double minY = 0.25D, maxY = 0.55D;
        double minZ = 0.30D, maxZ = 0.70D;

        if (facing == 2) { // North (spout points -Z)
            minZ = 0.0D;
            maxZ = 0.85D;
        } else if (facing == 3) { // South (spout points +Z)
            minZ = 0.15D;
            maxZ = 1.0D;
        } else if (facing == 4) { // West (spout points -X)
            minX = 0.0D;
            maxX = 0.85D;
        } else if (facing == 5) { // East (spout points +X)
            minX = 0.15D;
            maxX = 1.0D;
        }

        tess.startDrawingQuads();
        // Top
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(minX, maxY, minZ, minU, minV);
        tess.addVertexWithUV(minX, maxY, maxZ, minU, maxV);
        tess.addVertexWithUV(maxX, maxY, maxZ, maxU, maxV);
        tess.addVertexWithUV(maxX, maxY, minZ, maxU, minV);

        // Bottom
        tess.setNormal(0.0F, -1.0F, 0.0F);
        tess.addVertexWithUV(minX, minY, maxZ, minU, maxV);
        tess.addVertexWithUV(minX, minY, minZ, minU, minV);
        tess.addVertexWithUV(maxX, minY, minZ, maxU, minV);
        tess.addVertexWithUV(maxX, minY, maxZ, maxU, maxV);

        // Sides
        tess.setNormal(-1.0F, 0.0F, 0.0F);
        tess.addVertexWithUV(minX, minY, minZ, minU, maxV);
        tess.addVertexWithUV(minX, minY, maxZ, maxU, maxV);
        tess.addVertexWithUV(minX, maxY, maxZ, maxU, minV);
        tess.addVertexWithUV(minX, maxY, minZ, minU, minV);

        tess.setNormal(1.0F, 0.0F, 0.0F);
        tess.addVertexWithUV(maxX, minY, maxZ, minU, maxV);
        tess.addVertexWithUV(maxX, minY, minZ, maxU, maxV);
        tess.addVertexWithUV(maxX, maxY, minZ, maxU, minV);
        tess.addVertexWithUV(maxX, maxY, maxZ, minU, minV);

        tess.setNormal(0.0F, 0.0F, -1.0F);
        tess.addVertexWithUV(maxX, minY, minZ, minU, maxV);
        tess.addVertexWithUV(minX, minY, minZ, maxU, maxV);
        tess.addVertexWithUV(minX, maxY, minZ, maxU, minV);
        tess.addVertexWithUV(maxX, maxY, minZ, minU, minV);

        tess.setNormal(0.0F, 0.0F, 1.0F);
        tess.addVertexWithUV(minX, minY, maxZ, minU, maxV);
        tess.addVertexWithUV(maxX, minY, maxZ, maxU, maxV);
        tess.addVertexWithUV(maxX, maxY, maxZ, maxU, minV);
        tess.addVertexWithUV(minX, maxY, maxZ, minU, minV);

        tess.draw();
    }

    private void renderMoltenStream(int facing, long worldTime, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);

        // Glowing molten lava brightness
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        GL11.glColor4f(1.0F, 0.55F, 0.12F, 0.95F);

        double streamX = 0.5D + TileEntitySmelterOutlet.getFrontOffsetX(facing) * 0.4D;
        double streamZ = 0.5D + TileEntitySmelterOutlet.getFrontOffsetZ(facing) * 0.4D;
        double topY = 0.35D;
        double bottomY = -0.92D; // Reaches directly onto the casting table surface below!
        double radius = 0.06D;

        float anim = (float) ((worldTime % 40L) + partialTicks) * 0.08F;

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // Cross quads representing the flowing cylindrical stream
        // Face 1: X-aligned
        tess.addVertexWithUV(streamX - radius, bottomY, streamZ, 0.0D, 1.0D + anim);
        tess.addVertexWithUV(streamX + radius, bottomY, streamZ, 1.0D, 1.0D + anim);
        tess.addVertexWithUV(streamX + radius, topY, streamZ, 1.0D, 0.0D + anim);
        tess.addVertexWithUV(streamX - radius, topY, streamZ, 0.0D, 0.0D + anim);

        // Face 2: Z-aligned
        tess.addVertexWithUV(streamX, bottomY, streamZ - radius, 0.0D, 1.0D + anim);
        tess.addVertexWithUV(streamX, bottomY, streamZ + radius, 1.0D, 1.0D + anim);
        tess.addVertexWithUV(streamX, topY, streamZ + radius, 1.0D, 0.0D + anim);
        tess.addVertexWithUV(streamX, topY, streamZ - radius, 0.0D, 0.0D + anim);

        tess.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
