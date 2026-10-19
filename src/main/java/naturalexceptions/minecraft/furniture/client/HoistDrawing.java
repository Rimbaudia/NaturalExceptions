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

    /**
     * A cord from {@code a} to {@code b}, {@code half} its half-width: a square rope, solid from any side, its ends
     * closed.
     */
    static void cord(IIcon icon, double[] a, double[] b, double half) {
        tint = tintFor(icon);
        double[] d = { b[0] - a[0], b[1] - a[1], b[2] - a[2] };
        double length = Math.sqrt(dot(d, d));
        if (length < 1e-6) return;
        double[] along = scale(d, 1.0 / length);
        // Two directions square to the cord: one level where it can be, the other square to both.
        double[] e1 = Math.abs(along[1]) > 0.99 ? new double[] { 1, 0, 0 }
            : normal(cross(along, new double[] { 0, 1, 0 }));
        double[] e2 = normal(cross(along, e1));
        e1 = scale(e1, half);
        e2 = scale(e2, half);
        double u0 = icon.getMinU(), u1 = icon.getInterpolatedU(2), v0 = icon.getMinV(), v1 = icon.getMaxV();
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        double[][] corners = { add(e1, e2, 1, 1), add(e1, e2, -1, 1), add(e1, e2, -1, -1), add(e1, e2, 1, -1) };
        for (int k = 0; k < 4; k++) {
            double[] c0 = corners[k], c1 = corners[(k + 1) % 4];
            double[] out = normal(new double[] { c0[0] + c1[0], c0[1] + c1[1], c0[2] + c1[2] });
            quad(
                t,
                new double[][] { plus(a, c0), plus(a, c1), plus(b, c1), plus(b, c0) },
                new double[][] { { u0, v1 }, { u1, v1 }, { u1, v0 }, { u0, v0 } },
                out);
        }
        for (int end = 0; end < 2; end++) {
            double[] at = end == 0 ? a : b;
            double[] out = end == 0 ? scale(along, -1) : along;
            quad(
                t,
                new double[][] { plus(at, corners[0]), plus(at, corners[1]), plus(at, corners[2]),
                    plus(at, corners[3]) },
                new double[][] { { u0, v0 }, { u1, v0 }, { u1, v1 }, { u0, v1 } },
                out);
        }
        t.draw();
    }

    /**
     * A round part about the local x axis ({@code aboutX}) or z axis: a {@code sides}-sided prism of {@code radius}
     * from {@code from} to {@code to} along it, with its two end faces. One solid, so nothing overlaps or flickers.
     */
    static void prism(IIcon icon, int sides, double radius, double from, double to, boolean aboutX) {
        tint = tintFor(icon);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        double vTop = icon.getInterpolatedV(16 - Math.min(16, (to - from) * 16)), vBottom = icon.getMaxV();
        double[] axis = aboutX ? new double[] { 1, 0, 0 } : new double[] { 0, 0, 1 };
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            double uA = icon.getInterpolatedU(16.0 * i / sides), uB = icon.getInterpolatedU(16.0 * (i + 1) / sides);
            double[] p0 = rim(radius, a0, from, aboutX), p1 = rim(radius, a1, from, aboutX),
                q0 = rim(radius, a0, to, aboutX), q1 = rim(radius, a1, to, aboutX);
            quad(
                t,
                new double[][] { p0, p1, q1, q0 },
                new double[][] { { uA, vBottom }, { uB, vBottom }, { uB, vTop }, { uA, vTop } },
                rim(1.0, (a0 + a1) / 2, 0, aboutX));
            for (int end = 0; end < 2; end++) {
                double at = end == 0 ? from : to;
                double[] c = rim(0, 0, at, aboutX), e0 = rim(radius, a0, at, aboutX), e1 = rim(radius, a1, at, aboutX);
                quad(
                    t,
                    new double[][] { c, e0, e1, e1 },
                    new double[][] { capUV(icon, 0, 0), capUV(icon, Math.cos(a0), Math.sin(a0)),
                        capUV(icon, Math.cos(a1), Math.sin(a1)), capUV(icon, Math.cos(a1), Math.sin(a1)) },
                    end == 0 ? scale(axis, -1) : axis);
            }
        }
        t.draw();
    }

    /**
     * A ring about the local z axis: {@code sides} segments between {@code inner} and {@code outer} radius,
     * {@code half}
     * either side of z = 0; a sheave's rim.
     */
    static void ring(IIcon icon, int sides, double inner, double outer, double half) {
        tint = tintFor(icon);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        double u0 = icon.getMinU(), u1 = icon.getInterpolatedU(16 * Math.min(1, 2 * half)), vA = icon.getMinV();
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides, mid = (a0 + a1) / 2;
            double v1 = icon.getInterpolatedV(16 * Math.min(1, outer * (a1 - a0)));
            double[] o0 = rim(outer, a0, 0, false), o1 = rim(outer, a1, 0, false), i0 = rim(inner, a0, 0, false),
                i1 = rim(inner, a1, 0, false);
            double[] front = { 0, 0, half }, back = { 0, 0, -half };
            double[][] uv = { { u0, vA }, { u1, vA }, { u1, v1 }, { u0, v1 } };
            quad(
                t,
                new double[][] { plus(o0, back), plus(o0, front), plus(o1, front), plus(o1, back) },
                uv,
                rim(1, mid, 0, false));
            quad(
                t,
                new double[][] { plus(i0, back), plus(i0, front), plus(i1, front), plus(i1, back) },
                uv,
                rim(-1, mid, 0, false));
            double vR = icon.getInterpolatedV(16 * Math.min(1, outer - inner));
            double[][] face = { { u0, vA }, { u1, vA }, { u1, vR }, { u0, vR } };
            quad(
                t,
                new double[][] { plus(i0, front), plus(o0, front), plus(o1, front), plus(i1, front) },
                face,
                new double[] { 0, 0, 1 });
            quad(
                t,
                new double[][] { plus(i0, back), plus(o0, back), plus(o1, back), plus(i1, back) },
                face,
                new double[] { 0, 0, -1 });
        }
        t.draw();
    }

    /**
     * One face, wound to face {@code out} whichever order its corners came in, and shaded by which way it faces as
     * blocks are (lit from above, darker beneath and to the sides).
     */
    /** Rope is drawn from plank grain, tinted the brown of hemp. */
    private static final float[] HEMP = { 0.62F, 0.5F, 0.34F }, PLAIN = { 1.0F, 1.0F, 1.0F };
    private static float[] tint = PLAIN;

    private static float[] tintFor(IIcon icon) {
        return icon == naturalexceptions.minecraft.furniture.HoistGeometry.ropeIcon() ? HEMP : PLAIN;
    }

    private static void quad(Tessellator t, double[][] v, double[][] uv, double[] out) {
        double[] n = cross(minus(v[1], v[0]), minus(v[2], v[0]));
        if (dot(n, n) < 1e-18) n = cross(minus(v[2], v[0]), minus(v[3], v[0]));
        boolean reverse = dot(n, out) < 0;
        double[] o = normal(out);
        float shade = (float) (o[1] >= 0 ? 0.8 + 0.2 * o[1] : 0.8 + 0.3 * o[1]) - (float) (0.2 * Math.abs(o[0]));
        t.setColorOpaque_F(shade * tint[0], shade * tint[1], shade * tint[2]);
        t.setNormal((float) o[0], (float) o[1], (float) o[2]);
        for (int k = 0; k < 4; k++) {
            int i = reverse ? 3 - k : k;
            t.addVertexWithUV(v[i][0], v[i][1], v[i][2], uv[i][0], uv[i][1]);
        }
    }

    /** A point at {@code radius} and {@code angle} round the axis, {@code at} along it. */
    private static double[] rim(double radius, double angle, double at, boolean aboutX) {
        double c = radius * Math.cos(angle), s = radius * Math.sin(angle);
        return aboutX ? new double[] { at, c, s } : new double[] { c, s, at };
    }

    private static double[] capUV(IIcon icon, double c, double s) {
        return new double[] { icon.getInterpolatedU(8 + 6 * c), icon.getInterpolatedV(8 + 6 * s) };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static double[] minus(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    private static double[] plus(double[] a, double[] b) {
        return new double[] { a[0] + b[0], a[1] + b[1], a[2] + b[2] };
    }

    private static double[] scale(double[] a, double k) {
        return new double[] { a[0] * k, a[1] * k, a[2] * k };
    }

    private static double[] normal(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1e-12 ? new double[] { 0, 1, 0 } : scale(a, 1 / l);
    }

    private static double[] add(double[] e1, double[] e2, double k1, double k2) {
        return new double[] { e1[0] * k1 + e2[0] * k2, e1[1] * k1 + e2[1] * k2, e1[2] * k1 + e2[2] * k2 };
    }
}
