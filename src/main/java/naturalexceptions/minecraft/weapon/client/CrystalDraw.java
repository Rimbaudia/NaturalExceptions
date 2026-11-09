package naturalexceptions.minecraft.weapon.client;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Stormglass drawn as light: faceted crystals, soft glows and crackling arcs, untextured and full-bright. Callers set
 * up the GL state ({@link #begin}/{@link #end}) and the transform; everything here draws round the origin.
 */
@SideOnly(Side.CLIENT)
public final class CrystalDraw {

    /** Stormglass's colours, as {r, g, b}: the point, the lit and shaded girdle, the lit and shaded rear, the back. */
    private static final float[] TIP = { 1.0F, 1.0F, 1.0F }, LIT = { 0.8F, 0.7F, 1.0F },
        SHADE = { 0.56F, 0.42F, 0.92F }, REAR_LIT = { 0.44F, 0.32F, 0.78F }, REAR_SHADE = { 0.3F, 0.2F, 0.62F },
        BACK = { 0.86F, 0.8F, 1.0F };

    private static final float[] GIRDLE_COS = { 1.0F, 0.0F, -1.0F, 0.0F, 1.0F };
    private static final float[] GIRDLE_SIN = { 0.0F, 1.0F, 0.0F, -1.0F, 0.0F };
    private static final double[] DISC_COS = new double[17];
    private static final double[] DISC_SIN = new double[17];

    static {
        for (int i = 0; i <= 16; i++) {
            double a = i * Math.PI / 8;
            DISC_COS[i] = Math.cos(a);
            DISC_SIN[i] = Math.sin(a);
        }
    }

    private CrystalDraw() {}

    /**
     * Untextured, unlit, two-sided and blended; {@link #end} undoes it without pushing attribute bits.
     */
    public static void begin() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    public static void end() {
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glLineWidth(1.0F);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * A crystal along +Z: a four-sided point {@code length} long ahead, a shorter one {@code tail} behind,
     * {@code radius}
     * round the girdle; {@code bright} (0 to 1) whitens it as it charges or flares.
     */
    public static void crystal(float length, float tail, float radius, float bright) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < 4; i++) {
            float x0 = GIRDLE_COS[i] * radius, y0 = GIRDLE_SIN[i] * radius;
            float x1 = GIRDLE_COS[i + 1] * radius, y1 = GIRDLE_SIN[i + 1] * radius;
            boolean lit = (i & 1) == 0;
            colour(t, TIP, bright, 0.95F);
            t.addVertex(0.0, 0.0, length);
            colour(t, lit ? LIT : SHADE, bright, 0.9F);
            t.addVertex(x0, y0, 0.0);
            t.addVertex(x1, y1, 0.0);
            colour(t, lit ? REAR_LIT : REAR_SHADE, bright, 0.9F);
            t.addVertex(x0, y0, 0.0);
            t.addVertex(x1, y1, 0.0);
            colour(t, BACK, bright, 0.95F);
            t.addVertex(0.0, 0.0, -tail);
        }
        t.draw();
    }

    private static void colour(Tessellator t, float[] c, float bright, float alpha) {
        t.setColorRGBA_F(
            c[0] + (1.0F - c[0]) * bright,
            c[1] + (1.0F - c[1]) * bright,
            c[2] + (1.0F - c[2]) * bright,
            alpha);
    }

    /**
     * A soft violet glow in the XY plane (turn it to the eye first): an outer haze and a bright heart, added onto
     * what lies behind.
     */
    public static void glow(float radius, float strength) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        emitDiscTriangles(t, radius, 0.45F * strength, 0.7F, 0.56F, 1.0F);
        emitDiscTriangles(t, radius * 0.43F, 0.8F * strength, 0.93F, 0.88F, 1.0F);
        t.draw();
        GL11.glDepthMask(true);
    }

    private static void emitDiscTriangles(Tessellator t, float radius, float alpha, float r, float g, float b) {
        for (int i = 0; i < 16; i++) {
            t.setColorRGBA_F(r, g, b, alpha);
            t.addVertex(0.0, 0.0, 0.0);
            t.setColorRGBA_F(r, g, b, 0.0F);
            t.addVertex(DISC_COS[i] * radius, DISC_SIN[i] * radius, 0.0);
            t.addVertex(DISC_COS[i + 1] * radius, DISC_SIN[i + 1] * radius, 0.0);
        }
    }

    /**
     * {@code count} short jagged arcs of storm-light leaping out from near the origin to about {@code reach}; a new
     * shape each time {@code seed} changes (give it the tick), so they flicker.
     */
    public static void arcs(long seed, int count, float reach, float alpha) {
        if (count <= 0) return;
        long state = seed ^ 0x9E3779B97F4A7C15L;
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_LINES);
        int steps = 4;
        for (int n = 0; n < count; n++) {
            state = mix64(state);
            double yaw = toUnitDouble(state) * (Math.PI * 2.0);
            state = mix64(state);
            double pitch = (toUnitDouble(state) - 0.5) * Math.PI;
            double cosPitch = Math.cos(pitch);
            double dx = cosPitch * Math.cos(yaw), dy = Math.sin(pitch), dz = cosPitch * Math.sin(yaw);
            double prevX = dx * (reach * 0.25);
            double prevY = dy * (reach * 0.25);
            double prevZ = dz * (reach * 0.25);
            float prevFade = 1.0F;
            double jitter = reach * 0.18;
            for (int s = 1; s <= steps; s++) {
                double along = reach * (0.25 + 0.75 * s / steps);
                state = mix64(state);
                double jx = (toUnitDouble(state) - 0.5) * jitter;
                state = mix64(state);
                double jy = (toUnitDouble(state) - 0.5) * jitter;
                state = mix64(state);
                double jz = (toUnitDouble(state) - 0.5) * jitter;
                double curX = dx * along + jx;
                double curY = dy * along + jy;
                double curZ = dz * along + jz;
                float curFade = 1.0F - (float) s / (steps + 1);
                t.setColorRGBA_F(0.9F, 0.84F, 1.0F, alpha * prevFade);
                t.addVertex(prevX, prevY, prevZ);
                t.setColorRGBA_F(0.9F, 0.84F, 1.0F, alpha * curFade);
                t.addVertex(curX, curY, curZ);
                prevX = curX;
                prevY = curY;
                prevZ = curZ;
                prevFade = curFade;
            }
        }
        t.draw();
        GL11.glDepthMask(true);
    }

    private static long mix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double toUnitDouble(long z) {
        return (z >>> 11) * 0x1.0p-53;
    }
}
