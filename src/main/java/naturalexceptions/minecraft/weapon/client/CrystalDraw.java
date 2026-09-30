package naturalexceptions.minecraft.weapon.client;

import java.util.Random;

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

    private CrystalDraw() {}

    /**
     * Untextured, unlit, two-sided and blended; with {@code GL_ENABLE_BIT} and friends pushed so {@link #end} undoes
     * it.
     */
    public static void begin() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    public static void end() {
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopAttrib();
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
            double a0 = i * Math.PI / 2, a1 = (i + 1) * Math.PI / 2;
            float x0 = (float) Math.cos(a0) * radius, y0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius, y1 = (float) Math.sin(a1) * radius;
            boolean lit = i % 2 == 0;
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
        disc(radius, 0.45F * strength, 0.7F, 0.56F, 1.0F);
        disc(radius * 0.43F, 0.8F * strength, 0.93F, 0.88F, 1.0F);
        GL11.glDepthMask(true);
    }

    private static void disc(float radius, float alpha, float r, float g, float b) {
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLE_FAN);
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertex(0.0, 0.0, 0.0);
        t.setColorRGBA_F(r, g, b, 0.0F);
        for (int i = 0; i <= 16; i++) {
            double a = i * Math.PI / 8;
            t.addVertex(Math.cos(a) * radius, Math.sin(a) * radius, 0.0);
        }
        t.draw();
    }

    /**
     * {@code count} short jagged arcs of storm-light leaping out from near the origin to about {@code reach}; a new
     * shape each time {@code seed} changes (give it the tick), so they flicker.
     */
    public static void arcs(long seed, int count, float reach, float alpha) {
        Random random = new Random(seed);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        Tessellator t = Tessellator.instance;
        for (int n = 0; n < count; n++) {
            double yaw = random.nextDouble() * Math.PI * 2, pitch = (random.nextDouble() - 0.5) * Math.PI;
            double dx = Math.cos(pitch) * Math.cos(yaw), dy = Math.sin(pitch), dz = Math.cos(pitch) * Math.sin(yaw);
            t.startDrawing(GL11.GL_LINE_STRIP);
            int steps = 4;
            for (int s = 0; s <= steps; s++) {
                double along = reach * (0.25 + 0.75 * s / steps);
                double jitter = s == 0 ? 0.0 : reach * 0.18;
                float fade = 1.0F - (float) s / (steps + 1);
                t.setColorRGBA_F(0.9F, 0.84F, 1.0F, alpha * fade);
                t.addVertex(
                    dx * along + (random.nextDouble() - 0.5) * jitter,
                    dy * along + (random.nextDouble() - 0.5) * jitter,
                    dz * along + (random.nextDouble() - 0.5) * jitter);
            }
            t.draw();
        }
        GL11.glDepthMask(true);
    }
}
