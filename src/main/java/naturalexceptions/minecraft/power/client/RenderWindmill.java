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
            double a = Math.toRadians(sail * 90.0);
            double c = Math.cos(a), s = Math.sin(a);
            // A spar out from the hub, and a cloth panel along one side of it, a little pitched.
            quadBox(tess, spar, c, s, 0.1, 2.25, -0.04, 0.04, -0.03, 0.03);
            quadBox(tess, cloth, c, s, 0.45, 2.15, 0.04, 0.5, 0.0, 0.02);
        }
        tess.draw();
        GL11.glPopMatrix();
    }

    /**
     * A box along the direction (c, s) in the sail plane, from {@code r0} to {@code r1} out, {@code w0}..{@code w1}
     * across.
     */
    private static void quadBox(Tessellator t, IIcon icon, double c, double s, double r0, double r1, double w0,
        double w1, double d0, double d1) {
        double[][] corners = new double[8][];
        int i = 0;
        for (double r : new double[] { r0, r1 }) for (double w : new double[] { w0, w1 })
            for (double d : new double[] { d0, d1 }) corners[i++] = new double[] { r * c - w * s, r * s + w * c, d };
        int[][] faces = { { 0, 1, 3, 2 }, { 4, 6, 7, 5 }, { 0, 4, 5, 1 }, { 2, 3, 7, 6 }, { 0, 2, 6, 4 },
            { 1, 5, 7, 3 } };
        for (int[] f : faces) face(t, icon, corners[f[0]], corners[f[1]], corners[f[2]], corners[f[3]]);
    }

    private static void box(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double[][] c = { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y1, z0 }, { x0, y1, z0 }, { x0, y0, z1 }, { x1, y0, z1 },
            { x1, y1, z1 }, { x0, y1, z1 } };
        int[][] faces = { { 0, 3, 2, 1 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
            { 1, 2, 6, 5 } };
        for (int[] f : faces) face(t, icon, c[f[0]], c[f[1]], c[f[2]], c[f[3]]);
    }

    private static void face(Tessellator t, IIcon icon, double[] a, double[] b, double[] c, double[] d) {
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        t.addVertexWithUV(a[0], a[1], a[2], u0, v0);
        t.addVertexWithUV(b[0], b[1], b[2], u1, v0);
        t.addVertexWithUV(c[0], c[1], c[2], u1, v1);
        t.addVertexWithUV(d[0], d[1], d[2], u0, v1);
        // and the back, so the cloth shows from behind
        t.addVertexWithUV(d[0], d[1], d[2], u0, v1);
        t.addVertexWithUV(c[0], c[1], c[2], u1, v1);
        t.addVertexWithUV(b[0], b[1], b[2], u1, v0);
        t.addVertexWithUV(a[0], a[1], a[2], u0, v0);
    }
}
