package naturalexceptions.minecraft.weapon.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.weapon.EntityLumeniteBolt;

/**
 * A Lumenite bolt in flight: a long faceted crystal spinning point-first along its path, wrapped in a soft glow and
 * drawing a fading ribbon of pale light behind it from where it has been. All of it full-bright and added onto what
 * lies behind, so it reads as light rather than as a solid thing.
 */
@SideOnly(Side.CLIENT)
public final class RenderLumeniteBolt extends Render {

    private static final float LENGTH = 0.42F, TAIL = 0.22F, RADIUS = 0.07F;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        if (entity instanceof EntityLumeniteBolt) ribbon((EntityLumeniteBolt) entity, x, y, z, partialTicks);

        GL11.glTranslated(x, y, z);
        crystal(entity, partialTicks);
        glow(entity, partialTicks);

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /** The crystal: an elongated octahedron, point along the motion, turning on its long axis. */
    private static void crystal(Entity entity, float partialTicks) {
        double mx = entity.motionX, my = entity.motionY, mz = entity.motionZ;
        double horizontal = Math.sqrt(mx * mx + mz * mz);
        GL11.glPushMatrix();
        if (horizontal + Math.abs(my) > 1.0E-4) {
            GL11.glRotatef((float) Math.toDegrees(Math.atan2(mx, mz)), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float) -Math.toDegrees(Math.atan2(my, horizontal)), 1.0F, 0.0F, 0.0F);
        }
        GL11.glRotatef((entity.ticksExisted + partialTicks) * 24.0F, 0.0F, 0.0F, 1.0F);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_TRIANGLES);
        for (int i = 0; i < 4; i++) {
            double a0 = i * Math.PI / 2, a1 = (i + 1) * Math.PI / 2;
            float x0 = (float) Math.cos(a0) * RADIUS, y0 = (float) Math.sin(a0) * RADIUS;
            float x1 = (float) Math.cos(a1) * RADIUS, y1 = (float) Math.sin(a1) * RADIUS;
            boolean lit = i % 2 == 0;
            // The forward facets: white at the point, cyan at the girdle.
            t.setColorRGBA_F(1.0F, 1.0F, 1.0F, 0.95F);
            t.addVertex(0.0, 0.0, LENGTH);
            t.setColorRGBA_F(lit ? 0.62F : 0.36F, lit ? 0.92F : 0.74F, 1.0F, 0.9F);
            t.addVertex(x0, y0, 0.0);
            t.addVertex(x1, y1, 0.0);
            // The rear facets, darker.
            t.setColorRGBA_F(lit ? 0.3F : 0.2F, lit ? 0.62F : 0.46F, lit ? 0.9F : 0.8F, 0.9F);
            t.addVertex(x0, y0, 0.0);
            t.addVertex(x1, y1, 0.0);
            t.setColorRGBA_F(0.7F, 0.95F, 1.0F, 0.95F);
            t.addVertex(0.0, 0.0, -TAIL);
        }
        t.draw();
        GL11.glPopMatrix();
    }

    /** A soft disc of light round the crystal, turned to the eye, gently pulsing. */
    private void glow(Entity entity, float partialTicks) {
        float pulse = 0.85F + 0.15F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.8F);
        GL11.glPushMatrix();
        GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        disc(0.42F * pulse, 0.45F, 0.55F, 0.85F, 1.0F);
        disc(0.18F * pulse, 0.8F, 0.85F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glPopMatrix();
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
     * The ribbon: two crossed strips through the bolt's last positions, narrowing and fading towards the oldest, so
     * it has width from every side.
     */
    private static void ribbon(EntityLumeniteBolt bolt, double x, double y, double z, float partialTicks) {
        double ex = bolt.lastTickPosX + (bolt.posX - bolt.lastTickPosX) * partialTicks;
        double ey = bolt.lastTickPosY + (bolt.posY - bolt.lastTickPosY) * partialTicks;
        double ez = bolt.lastTickPosZ + (bolt.posZ - bolt.lastTickPosZ) * partialTicks;
        int n = 1;
        double[][] points = new double[EntityLumeniteBolt.TRAIL + 1][];
        points[0] = new double[] { x, y, z };
        for (double[] p : bolt.trail) {
            if (p == null) break;
            points[n++] = new double[] { p[0] - ex + x, p[1] - ey + y, p[2] - ez + z };
        }
        if (n < 2) return;
        double dx = points[0][0] - points[n - 1][0], dy = points[0][1] - points[n - 1][1],
            dz = points[0][2] - points[n - 1][2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-3) return;
        dx /= len;
        dy /= len;
        dz /= len;
        // Two directions across the path: one level, one upright.
        double[] across = normal(dz, 0.0, -dx, 1.0, 0.0, 0.0);
        double[] up = normal(
            dy * across[2] - dz * across[1],
            dz * across[0] - dx * across[2],
            dx * across[1] - dy * across[0],
            0.0,
            1.0,
            0.0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        strip(points, n, across);
        strip(points, n, up);
        GL11.glDepthMask(true);
    }

    private static void strip(double[][] points, int n, double[] side) {
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_QUAD_STRIP);
        for (int i = 0; i < n; i++) {
            float f = 1.0F - (float) i / (n - 1);
            double w = 0.09 * f + 0.01;
            t.setColorRGBA_F(0.45F + 0.4F * f, 0.8F + 0.2F * f, 1.0F, 0.55F * f);
            t.addVertex(points[i][0] + side[0] * w, points[i][1] + side[1] * w, points[i][2] + side[2] * w);
            t.addVertex(points[i][0] - side[0] * w, points[i][1] - side[1] * w, points[i][2] - side[2] * w);
        }
        t.draw();
    }

    /** (x, y, z) made a unit vector, or the fallback when it has no length. */
    private static double[] normal(double x, double y, double z, double fx, double fy, double fz) {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-4 ? new double[] { fx, fy, fz } : new double[] { x / len, y / len, z / len };
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
