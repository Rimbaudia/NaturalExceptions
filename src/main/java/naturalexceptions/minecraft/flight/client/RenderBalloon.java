package naturalexceptions.minecraft.flight.client;

import net.minecraft.client.renderer.GLAllocation;
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

    private static final double[] GORE_COS = new double[GORES + 1];
    private static final double[] GORE_SIN = new double[GORES + 1];
    private static final double[] ROPE_COS = new double[4];
    private static final double[] ROPE_SIN = new double[4];
    private static final float[][] GORE_SHADE = new float[GORES][PROFILE.length - 1];
    private static final int[][] BOX_FACES = { { 0, 3, 2, 1 }, { 4, 5, 6, 7 }, { 0, 1, 5, 4 }, { 3, 7, 6, 2 },
        { 0, 4, 7, 3 }, { 1, 2, 6, 5 } };
    private static final float[] BOX_SHADES = { 0.8F, 0.8F, 0.5F, 1.0F, 0.6F, 0.6F };

    static {
        for (int g = 0; g <= GORES; g++) {
            double a = 2 * Math.PI * g / GORES;
            GORE_COS[g] = Math.cos(a);
            GORE_SIN[g] = Math.sin(a);
        }
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + 90 * k);
            ROPE_COS[k] = Math.cos(a);
            ROPE_SIN[k] = Math.sin(a);
        }
        for (int g = 0; g < GORES; g++) {
            double a0 = 2 * Math.PI * g / GORES, a1 = 2 * Math.PI * (g + 1) / GORES;
            float goreFactor = 0.9F + 0.1F * (float) Math.cos((a0 + a1) / 2);
            for (int i = 0; i + 1 < PROFILE.length; i++) {
                double y0 = PROFILE[i][0], r0 = PROFILE[i][1], y1 = PROFILE[i + 1][0], r1 = PROFILE[i + 1][1];
                double slope = (r0 - r1) / Math.max(1e-3, Math.hypot(r1 - r0, y1 - y0));
                GORE_SHADE[g][i] = (float) (0.72 + 0.28 * slope) * goreFactor;
            }
        }
    }

    private int displayList = -1;
    private float cachedWickerU = -1.0F;

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
        float prevLX = OpenGlHelper.lastBrightnessX, prevLY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        IIcon wicker = Blocks.planks.getIcon(0, 0);
        if (wicker != null) {
            if (displayList == -1) {
                displayList = GLAllocation.generateDisplayLists(1);
            }
            if (wicker.getMinU() != cachedWickerU) {
                cachedWickerU = wicker.getMinU();
                GL11.glNewList(displayList, GL11.GL_COMPILE_AND_EXECUTE);
                emitBalloonGeometry(wicker);
                GL11.glEndList();
            } else {
                GL11.glCallList(displayList);
            }
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLX, prevLY);
    }

    private static void emitBalloonGeometry(IIcon wicker) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        IIcon iron = Blocks.iron_block.getIcon(0, 0);
        IIcon rope = Blocks.wool.getIcon(0, 12);
        IIcon redCloth = Blocks.wool.getIcon(0, 14);
        IIcon whiteCloth = Blocks.wool.getIcon(0, 0);
        // The basket: a floor and four walls, open on top.
        box(t, wicker, -BASKET, 0.0, -BASKET, BASKET, WALL, BASKET);
        box(t, wicker, -BASKET, WALL, -BASKET, BASKET, RIM, -BASKET + WALL);
        box(t, wicker, -BASKET, WALL, BASKET - WALL, BASKET, RIM, BASKET);
        box(t, wicker, -BASKET, WALL, -BASKET + WALL, -BASKET + WALL, RIM, BASKET - WALL);
        box(t, wicker, BASKET - WALL, WALL, -BASKET + WALL, BASKET, RIM, BASKET - WALL);
        // The brazier, hung under the mouth.
        box(t, iron, -0.2, 1.95, -0.2, 0.2, 2.25, 0.2);
        // Four ropes from the basket's corners up to the mouth, and four from the mouth down to the brazier.
        double corner = (BASKET - WALL / 2) * Math.sqrt(2);
        for (int k = 0; k < 4; k++) {
            double c = ROPE_COS[k], s = ROPE_SIN[k];
            line(t, rope, c * corner, RIM, s * corner, c * PROFILE[0][1], PROFILE[0][0], s * PROFILE[0][1]);
            line(t, rope, c * PROFILE[0][1], PROFILE[0][0], s * PROFILE[0][1], c * 0.2, 2.25, s * 0.2);
        }
        // The envelope.
        for (int g = 0; g < GORES; g++) {
            IIcon cloth = g % 2 == 0 ? redCloth : whiteCloth;
            double c0 = GORE_COS[g], s0 = GORE_SIN[g], c1 = GORE_COS[g + 1], s1 = GORE_SIN[g + 1];
            for (int i = 0; i + 1 < PROFILE.length; i++) {
                double y0 = PROFILE[i][0], r0 = PROFILE[i][1], y1 = PROFILE[i + 1][0], r1 = PROFILE[i + 1][1];
                float shade = GORE_SHADE[g][i];
                t.setColorOpaque_F(shade, shade, shade);
                face(
                    t,
                    cloth,
                    c0 * r0,
                    y0,
                    s0 * r0,
                    c1 * r0,
                    y0,
                    s1 * r0,
                    c1 * r1,
                    y1,
                    s1 * r1,
                    c0 * r1,
                    y1,
                    s0 * r1);
            }
        }
        t.draw();
    }

    /** A rope from one point to another: two thin strips, crossed. */
    private static void line(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double w = 0.03;
        t.setColorOpaque_F(0.8F, 0.8F, 0.8F);
        face(t, icon, x0 - w, y0, z0, x0 + w, y0, z0, x1 + w, y1, z1, x1 - w, y1, z1);
        face(t, icon, x0, y0, z0 - w, x0, y0, z0 + w, x1, y1, z1 + w, x1, y1, z1 - w);
    }

    private static void box(Tessellator t, IIcon icon, double x0, double y0, double z0, double x1, double y1,
        double z1) {
        double[] cx = { x0, x1, x1, x0, x0, x1, x1, x0 };
        double[] cy = { y0, y0, y1, y1, y0, y0, y1, y1 };
        double[] cz = { z0, z0, z0, z0, z1, z1, z1, z1 };
        for (int i = 0; i < BOX_FACES.length; i++) {
            int[] f = BOX_FACES[i];
            t.setColorOpaque_F(BOX_SHADES[i], BOX_SHADES[i], BOX_SHADES[i]);
            int a = f[0], b = f[1], c = f[2], d = f[3];
            face(t, icon, cx[a], cy[a], cz[a], cx[b], cy[b], cz[b], cx[c], cy[c], cz[c], cx[d], cy[d], cz[d]);
        }
    }

    /** A quad, drawn from both sides (the envelope is seen from the basket inside it too). */
    private static void face(Tessellator t, IIcon icon, double ax, double ay, double az, double bx, double by,
        double bz, double cx, double cy, double cz, double dx, double dy, double dz) {
        double u0 = icon.getMinU(), u1 = icon.getMaxU(), v0 = icon.getMinV(), v1 = icon.getMaxV();
        t.addVertexWithUV(ax, ay, az, u0, v1);
        t.addVertexWithUV(bx, by, bz, u1, v1);
        t.addVertexWithUV(cx, cy, cz, u1, v0);
        t.addVertexWithUV(dx, dy, dz, u0, v0);
        t.addVertexWithUV(dx, dy, dz, u0, v0);
        t.addVertexWithUV(cx, cy, cz, u1, v0);
        t.addVertexWithUV(bx, by, bz, u1, v1);
        t.addVertexWithUV(ax, ay, az, u0, v1);
    }
}
