package naturalexceptions.minecraft.power.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.power.TileWindmill;

/** A windmill's four sails, turning as fast as the wind drives them (worked out on this side as on the server). */
@SideOnly(Side.CLIENT)
public final class RenderWindmill extends TileEntitySpecialRenderer {

    /** Degrees a tick at full wind. */
    static final double SPIN = 5.0;
    private static final double[] SAIL_COS = { 1.0, 0.0, -1.0, 0.0 };
    private static final double[] SAIL_SIN = { 0.0, 1.0, 0.0, -1.0 };
    private static final int[][] QUAD_FACES = { { 0, 1, 3, 2 }, { 4, 6, 7, 5 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 },
        { 0, 2, 6, 4 }, { 1, 5, 7, 3 } };
    private static final int[][] BOX_FACES = { { 0, 3, 2, 1 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 }, { 3, 7, 6, 2 },
        { 0, 4, 7, 3 }, { 1, 2, 6, 5 } };

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        if (!(te instanceof TileWindmill)) return;
        TileWindmill mill = (TileWindmill) te;
        double angle = mill.clientAngle(partial, SPIN);
        int facing = mill.facing();
        float turn = facing == 3 ? 180.0F : facing == 4 ? 90.0F : facing == 5 ? -90.0F : 0.0F;
        bindTexture(TextureMap.locationBlocksTexture);
        int light = te.getWorldObj()
            .getLightBrightnessForSkyBlocks(te.xCoord, te.yCoord, te.zCoord, 0);
        float prevLX = OpenGlHelper.lastBrightnessX, prevLY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 10.5 / 16.0, z + 0.5);
        GL11.glRotatef(turn, 0.0F, 1.0F, 0.0F);
        GL11.glTranslated(0.0, 0.0, -8.0 / 16.0);
        GL11.glRotated(angle, 0.0, 0.0, 1.0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        IIcon cloth = Blocks.wool.getIcon(0, 0), spar = Blocks.planks.getIcon(0, 1);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        box(tess, spar, -0.12, -0.12, -0.08, 0.12, 0.12, 0.06);
        for (int sail = 0; sail < 4; sail++) {
            double c = SAIL_COS[sail], s = SAIL_SIN[sail];
            // A spar out from the hub, and a cloth panel along one side of it, a little pitched.
            quadBox(tess, spar, c, s, 0.1, 2.25, -0.04, 0.04, -0.03, 0.03);
            quadBox(tess, cloth, c, s, 0.45, 2.15, 0.04, 0.5, 0.0, 0.02);
        }
        tess.draw();
        GL11.glPopMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLX, prevLY);
    }

    /**
     * A box along the direction (c, s) in the sail plane, from {@code r0} to {@code r1} out, {@code w0}..{@code w1}
     * across.
     */
    private static void quadBox(Tessellator t, IIcon icon, double c, double s, double r0, double r1, double w0,
        double w1, double d0, double d1) {
        double[] cx = { r0 * c - w0 * s, r0 * c - w0 * s, r0 * c - w1 * s, r0 * c - w1 * s, r1 * c - w0 * s,
            r1 * c - w0 * s, r1 * c - w1 * s, r1 * c - w1 * s };
        double[] cy = { r0 * s + w0 * c, r0 * s + w0 * c, r0 * s + w1 * c, r0 * s + w1 * c, r1 * s + w0 * c,
            r1 * s + w0 * c, r1 * s + w1 * c, r1 * s + w1 * c };
        double[] cz = { d0, d1, d0, d1, d0, d1, d0, d1 };
        for (int[] f : QUAD_FACES) face(t, icon, cx, cy, cz, f[0], f[1], f[2], f[3]);
    }

    private static void box(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double[] cx = { x0, x1, x1, x0, x0, x1, x1, x0 };
        double[] cy = { y0, y0, y1, y1, y0, y0, y1, y1 };
        double[] cz = { z0, z0, z0, z0, z1, z1, z1, z1 };
        for (int[] f : BOX_FACES) face(t, icon, cx, cy, cz, f[0], f[1], f[2], f[3]);
    }

    private static void face(Tessellator t, IIcon icon, double[] cx, double[] cy, double[] cz, int a, int b, int c,
        int d) {
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        t.addVertexWithUV(cx[a], cy[a], cz[a], u0, v0);
        t.addVertexWithUV(cx[b], cy[b], cz[b], u1, v0);
        t.addVertexWithUV(cx[c], cy[c], cz[c], u1, v1);
        t.addVertexWithUV(cx[d], cy[d], cz[d], u0, v1);
        // and the back, so the cloth shows from behind
        t.addVertexWithUV(cx[d], cy[d], cz[d], u0, v1);
        t.addVertexWithUV(cx[c], cy[c], cz[c], u1, v1);
        t.addVertexWithUV(cx[b], cy[b], cz[b], u1, v0);
        t.addVertexWithUV(cx[a], cy[a], cz[a], u0, v0);
    }
}
