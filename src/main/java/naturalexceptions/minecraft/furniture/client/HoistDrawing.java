package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Boxes and cords for drawing a hoist's moving parts, in whatever space the GL matrix is in. */
@SideOnly(Side.CLIENT)
final class HoistDrawing {

    private HoistDrawing() {}

    /** A box {minX, minY, minZ, maxX, maxY, maxZ}, each face its share of {@code icon}, shaded as blocks are. */
    static void box(IIcon icon, double[] b) {
        Tessellator t = Tessellator.instance;
        double u0 = icon.getInterpolatedU(clamp(b[0]) * 16), u1 = icon.getInterpolatedU(clamp(b[3]) * 16);
        double w0 = icon.getInterpolatedU(clamp(b[2]) * 16), w1 = icon.getInterpolatedU(clamp(b[5]) * 16);
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

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v - Math.floor(v)));
    }

    /** A cord from {@code a} to {@code b}, {@code half} its half-width: two crossed strips, seen from any side. */
    static void cord(IIcon icon, double[] a, double[] b, double half) {
        Tessellator t = Tessellator.instance;
        double u0 = icon.getMinU(), u1 = icon.getInterpolatedU(2), v0 = icon.getMinV(), v1 = icon.getMaxV();
        double dx = b[0] - a[0], dz = b[2] - a[2], flat = Math.sqrt(dx * dx + dz * dz);
        // Strips across the cord: one level, one standing (for a vertical cord, one along x and one along z).
        double[][] across = flat < 1e-6 ? new double[][] { { half, 0, 0 }, { 0, 0, half } }
            : new double[][] { { -dz / flat * half, 0, dx / flat * half }, { 0, half, 0 } };
        t.startDrawingQuads();
        t.setColorOpaque_F(0.85F, 0.85F, 0.85F);
        for (double[] o : across) {
            t.addVertexWithUV(a[0] - o[0], a[1] - o[1], a[2] - o[2], u0, v1);
            t.addVertexWithUV(a[0] + o[0], a[1] + o[1], a[2] + o[2], u1, v1);
            t.addVertexWithUV(b[0] + o[0], b[1] + o[1], b[2] + o[2], u1, v0);
            t.addVertexWithUV(b[0] - o[0], b[1] - o[1], b[2] - o[2], u0, v0);
            t.addVertexWithUV(b[0] - o[0], b[1] - o[1], b[2] - o[2], u0, v0);
            t.addVertexWithUV(b[0] + o[0], b[1] + o[1], b[2] + o[2], u1, v0);
            t.addVertexWithUV(a[0] + o[0], a[1] + o[1], a[2] + o[2], u1, v1);
            t.addVertexWithUV(a[0] - o[0], a[1] - o[1], a[2] - o[2], u0, v1);
        }
        t.draw();
    }
}
