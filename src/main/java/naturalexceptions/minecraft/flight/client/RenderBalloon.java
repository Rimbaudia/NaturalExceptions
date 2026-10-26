package naturalexceptions.minecraft.flight.client;

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

/**
 * A hot-air balloon: a wicker basket, a brazier hung over it on four ropes, and the envelope above, twelve gores of red
 * and white cloth swelling out from the mouth to the crown. Shaded by hand (lit from above), as blocks are.
 */
@SideOnly(Side.CLIENT)
public final class RenderBalloon extends Render {

    static final int GORES = 12;
    /** The envelope's outline: {height over the basket's floor, radius}, mouth to crown. */
    static final double[][] PROFILE = { { 2.5, 0.65 }, { 3.0, 1.3 }, { 3.7, 1.9 }, { 4.6, 2.3 }, { 5.6, 2.4 },
        { 6.5, 2.2 }, { 7.2, 1.7 }, { 7.7, 1.0 }, { 7.95, 0.35 }, { 8.0, 0.0 } };
    static final double BASKET = 0.75, WALL = 0.08, RIM = 0.9;

    public RenderBalloon() {
        shadowSize = 0.8F;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
        bindEntityTexture(entity);
        int light = entity.getBrightnessForRender(partial);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        IIcon wicker = Blocks.planks.getIcon(0, 0), iron = Blocks.iron_block.getIcon(0, 0);
        IIcon rope = Blocks.wool.getIcon(0, 12);
        // The basket: a floor and four walls, open on top.
        box(t, wicker, -BASKET, 0.0, -BASKET, BASKET, WALL, BASKET);
        box(t, wicker, -BASKET, WALL, -BASKET, BASKET, RIM, -BASKET + WALL);
        box(t, wicker, -BASKET, WALL, BASKET - WALL, BASKET, RIM, BASKET);
        box(t, wicker, -BASKET, WALL, -BASKET + WALL, -BASKET + WALL, RIM, BASKET - WALL);
        box(t, wicker, BASKET - WALL, WALL, -BASKET + WALL, BASKET, RIM, BASKET - WALL);
        // The brazier, hung under the mouth.
        box(t, iron, -0.2, 1.95, -0.2, 0.2, 2.25, 0.2);
        // Four ropes from the basket's corners up to the mouth, and four from the mouth down to the brazier.
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + 90 * k), c = Math.cos(a), s = Math.sin(a);
            double corner = (BASKET - WALL / 2) * Math.sqrt(2);
            line(t, rope, c * corner, RIM, s * corner, c * PROFILE[0][1], PROFILE[0][0], s * PROFILE[0][1]);
            line(t, rope, c * PROFILE[0][1], PROFILE[0][0], s * PROFILE[0][1], c * 0.2, 2.25, s * 0.2);
        }
        // The envelope.
        for (int g = 0; g < GORES; g++) {
            IIcon cloth = Blocks.wool.getIcon(0, g % 2 == 0 ? 14 : 0);
            double a0 = 2 * Math.PI * g / GORES, a1 = 2 * Math.PI * (g + 1) / GORES;
            for (int i = 0; i + 1 < PROFILE.length; i++) {
                double y0 = PROFILE[i][0], r0 = PROFILE[i][1], y1 = PROFILE[i + 1][0], r1 = PROFILE[i + 1][1];
                // Lit from above: a face turned up is bright, one turned down dim.
                double slope = (r0 - r1) / Math.max(1e-3, Math.hypot(r1 - r0, y1 - y0));
                float shade = (float) (0.72 + 0.28 * slope) * (0.9F + 0.1F * (float) Math.cos((a0 + a1) / 2));
                t.setColorOpaque_F(shade, shade, shade);
                face(
                    t,
                    cloth,
                    new double[] { Math.cos(a0) * r0, y0, Math.sin(a0) * r0 },
                    new double[] { Math.cos(a1) * r0, y0, Math.sin(a1) * r0 },
                    new double[] { Math.cos(a1) * r1, y1, Math.sin(a1) * r1 },
                    new double[] { Math.cos(a0) * r1, y1, Math.sin(a0) * r1 });
            }
        }
        t.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /** A rope from one point to another: two thin strips, crossed. */
    private static void line(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double w = 0.03;
        t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        face(
            t,
            icon,
            new double[] { x0 - w, y0, z0 },
            new double[] { x0 + w, y0, z0 },
            new double[] { x1 + w, y1, z1 },
            new double[] { x1 - w, y1, z1 });
        face(
            t,
            icon,
            new double[] { x0, y0, z0 - w },
            new double[] { x0, y0, z0 + w },
            new double[] { x1, y1, z1 + w },
            new double[] { x1, y1, z1 - w });
    }

    private static void box(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double[][] c = { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y1, z0 }, { x0, y1, z0 }, { x0, y0, z1 }, { x1, y0, z1 },
            { x1, y1, z1 }, { x0, y1, z1 } };
        int[][] faces = { { 0, 3, 2, 1 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 }, { 3, 7, 6, 2 }, { 0, 4, 7, 3 },
            { 1, 2, 6, 5 } };
        float[] shades = { 0.8F, 0.8F, 0.5F, 1.0F, 0.6F, 0.6F };
        for (int i = 0; i < faces.length; i++) {
            int[] f = faces[i];
            t.setColorOpaque_F(shades[i], shades[i], shades[i]);
            face(t, icon, c[f[0]], c[f[1]], c[f[2]], c[f[3]]);
        }
    }

    /** A quad, drawn from both sides (the envelope is seen from the basket inside it too). */
    private static void face(Tessellator t, IIcon icon, double[] a, double[] b, double[] c, double[] d) {
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        t.addVertexWithUV(a[0], a[1], a[2], u0, v1);
        t.addVertexWithUV(b[0], b[1], b[2], u1, v1);
        t.addVertexWithUV(c[0], c[1], c[2], u1, v0);
        t.addVertexWithUV(d[0], d[1], d[2], u0, v0);
        t.addVertexWithUV(d[0], d[1], d[2], u0, v0);
        t.addVertexWithUV(c[0], c[1], c[2], u1, v0);
        t.addVertexWithUV(b[0], b[1], b[2], u1, v1);
        t.addVertexWithUV(a[0], a[1], a[2], u0, v1);
    }
}
