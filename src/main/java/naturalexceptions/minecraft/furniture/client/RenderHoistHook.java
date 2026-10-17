package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.EntityHoistHook;
import naturalexceptions.minecraft.furniture.HoistModel;

/** A hoist's basket on the move, with its rider standing in it, and the rope it hangs from. */
@SideOnly(Side.CLIENT)
public final class RenderHoistHook extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (!(entity instanceof EntityHoistHook)) return;
        EntityHoistHook hook = (EntityHoistHook) entity;
        double[] feet = HoistModel.feet(hook, partialTicks);
        double ix = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double iy = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double iz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        // The basket's corner, relative to the camera: its floor just under the rider's feet.
        double bx = x + feet[0] - ix - 0.5, by = y + feet[1] - iy - 0.1, bz = z + feet[2] - iz - 0.5;
        int light = entity.worldObj.getLightBrightnessForSkyBlocks(
            (int) Math.floor(feet[0]),
            (int) Math.floor(feet[1] + 1),
            (int) Math.floor(feet[2]),
            0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(bx, by, bz);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        double[][] basket = HoistModel.basket();
        // The basket without its middle post (that is where the rider stands): floor and four sides.
        for (int i = 0; i < basket.length - 1; i++) box(Blocks.planks.getIcon(1, 1), basket[i]);
        // The slings from its corners to the ring over the rider's head, and the rope from the ring up to the boom.
        IIcon rope = Blocks.planks.getIcon(1, 0);
        double ring = RING, top = HoistModel.ropeTop(hook) - (feet[1] - 0.1);
        // (Not for the rider's own eyes in first person: they run right past them.)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        boolean ownEyes = mc.gameSettings.thirdPersonView == 0 && hook.riddenByEntity == mc.renderViewEntity;
        if (!ownEyes)
            for (double[] c : new double[][] { { 0.12, 0.12 }, { 0.88, 0.12 }, { 0.12, 0.88 }, { 0.88, 0.88 } })
                line(rope, new double[] { c[0], 0.6, c[1] }, new double[] { RING_X, ring, 0.5 }, 0.035);
        if (top > ring) box(rope, new double[] { RING_X - 0.05, ring, 0.45, RING_X + 0.05, top, 0.55 });
        GL11.glPopMatrix();
    }

    /** How high over the basket's floor its slings meet the rope: above the rider's head. */
    private static final double RING = 2.3;
    /** The ring a little to one side of the rider's head, so that looking up the rope is a line, not a point. */
    private static final double RING_X = 0.75;

    /** A thin cord from {@code a} to {@code b}: two crossed strips, seen from any side. */
    static void line(IIcon icon, double[] a, double[] b, double half) {
        Tessellator t = Tessellator.instance;
        double u0 = icon.getMinU(), u1 = icon.getInterpolatedU(2), v0 = icon.getMinV(), v1 = icon.getMaxV();
        t.startDrawingQuads();
        t.setColorOpaque_F(0.85F, 0.85F, 0.85F);
        for (double[] o : new double[][] { { half, 0 }, { 0, half } }) {
            t.addVertexWithUV(a[0] - o[0], a[1], a[2] - o[1], u0, v1);
            t.addVertexWithUV(a[0] + o[0], a[1], a[2] + o[1], u1, v1);
            t.addVertexWithUV(b[0] + o[0], b[1], b[2] + o[1], u1, v0);
            t.addVertexWithUV(b[0] - o[0], b[1], b[2] - o[1], u0, v0);
            t.addVertexWithUV(b[0] - o[0], b[1], b[2] - o[1], u0, v0);
            t.addVertexWithUV(b[0] + o[0], b[1], b[2] + o[1], u1, v0);
            t.addVertexWithUV(a[0] + o[0], a[1], a[2] + o[1], u1, v1);
            t.addVertexWithUV(a[0] - o[0], a[1], a[2] - o[1], u0, v1);
        }
        t.draw();
    }

    /** A box in block space, each face its share of {@code icon}, lit from above as blocks are. */
    static void box(IIcon icon, double[] b) {
        Tessellator t = Tessellator.instance;
        double u0 = icon.getInterpolatedU(b[0] * 16), u1 = icon.getInterpolatedU(Math.min(16, b[3] * 16));
        double w0 = icon.getInterpolatedU(b[2] * 16), w1 = icon.getInterpolatedU(Math.min(16, b[5] * 16));
        double v0 = icon.getInterpolatedV(16 - Math.min(16, (b[4] - b[1]) * 16)), v1 = icon.getInterpolatedV(16);
        t.startDrawingQuads();
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(b[0], b[4], b[2], u0, w0);
        t.addVertexWithUV(b[0], b[4], b[5], u0, w1);
        t.addVertexWithUV(b[3], b[4], b[5], u1, w1);
        t.addVertexWithUV(b[3], b[4], b[2], u1, w0);
        t.setColorOpaque_F(0.5F, 0.5F, 0.5F);
        t.setNormal(0, -1, 0);
        t.addVertexWithUV(b[0], b[1], b[5], u0, w1);
        t.addVertexWithUV(b[0], b[1], b[2], u0, w0);
        t.addVertexWithUV(b[3], b[1], b[2], u1, w0);
        t.addVertexWithUV(b[3], b[1], b[5], u1, w1);
        t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        t.setNormal(0, 0, -1);
        t.addVertexWithUV(b[0], b[1], b[2], u1, v1);
        t.addVertexWithUV(b[0], b[4], b[2], u1, v0);
        t.addVertexWithUV(b[3], b[4], b[2], u0, v0);
        t.addVertexWithUV(b[3], b[1], b[2], u0, v1);
        t.setNormal(0, 0, 1);
        t.addVertexWithUV(b[3], b[1], b[5], u1, v1);
        t.addVertexWithUV(b[3], b[4], b[5], u1, v0);
        t.addVertexWithUV(b[0], b[4], b[5], u0, v0);
        t.addVertexWithUV(b[0], b[1], b[5], u0, v1);
        t.setColorOpaque_F(0.6F, 0.6F, 0.6F);
        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(b[0], b[1], b[5], w1, v1);
        t.addVertexWithUV(b[0], b[4], b[5], w1, v0);
        t.addVertexWithUV(b[0], b[4], b[2], w0, v0);
        t.addVertexWithUV(b[0], b[1], b[2], w0, v1);
        t.setNormal(1, 0, 0);
        t.addVertexWithUV(b[3], b[1], b[2], w1, v1);
        t.addVertexWithUV(b[3], b[4], b[2], w1, v0);
        t.addVertexWithUV(b[3], b[4], b[5], w0, v0);
        t.addVertexWithUV(b[3], b[1], b[5], w0, v1);
        t.draw();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
