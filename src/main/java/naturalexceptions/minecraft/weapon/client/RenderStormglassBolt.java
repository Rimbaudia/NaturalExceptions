package naturalexceptions.minecraft.weapon.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.weapon.EntityStormglassBolt;

/**
 * A Stormglass bolt in flight: a long faceted crystal spinning point-first along its path, wrapped in a soft violet
 * glow, crackling with short arcs, and drawing a fading ribbon of pale light behind it from where it has been. All of
 * it full-bright and added onto what lies behind, so it reads as light rather than as a solid thing.
 */
@SideOnly(Side.CLIENT)
public final class RenderStormglassBolt extends Render {

    private static final float LENGTH = 0.42F, TAIL = 0.22F, RADIUS = 0.07F;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        GL11.glPushMatrix();
        CrystalDraw.begin();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

        if (entity instanceof EntityStormglassBolt) ribbon((EntityStormglassBolt) entity, x, y, z, partialTicks);

        GL11.glTranslated(x, y, z);
        GL11.glPushMatrix();
        double mx = entity.motionX, my = entity.motionY, mz = entity.motionZ;
        double horizontal = Math.sqrt(mx * mx + mz * mz);
        if (horizontal + Math.abs(my) > 1.0E-4) {
            GL11.glRotatef((float) Math.toDegrees(Math.atan2(mx, mz)), 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((float) -Math.toDegrees(Math.atan2(my, horizontal)), 1.0F, 0.0F, 0.0F);
        }
        GL11.glRotatef((entity.ticksExisted + partialTicks) * 24.0F, 0.0F, 0.0F, 1.0F);
        CrystalDraw.crystal(LENGTH, TAIL, RADIUS, 0.0F);
        GL11.glPopMatrix();
        // Two or three arcs, a new shape every tick.
        CrystalDraw.arcs(entity.getEntityId() * 31L + entity.ticksExisted, 2 + entity.ticksExisted % 2, 0.35F, 0.9F);

        float pulse = 0.85F + 0.15F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.8F);
        GL11.glRotatef(180.0F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        CrystalDraw.glow(0.42F * pulse, 1.0F);

        CrystalDraw.end();
        GL11.glPopMatrix();
    }

    /**
     * The ribbon: two crossed strips through the bolt's last positions, narrowing and fading towards the oldest, so
     * it has width from every side.
     */
    private static void ribbon(EntityStormglassBolt bolt, double x, double y, double z, float partialTicks) {
        double ex = bolt.lastTickPosX + (bolt.posX - bolt.lastTickPosX) * partialTicks;
        double ey = bolt.lastTickPosY + (bolt.posY - bolt.lastTickPosY) * partialTicks;
        double ez = bolt.lastTickPosZ + (bolt.posZ - bolt.lastTickPosZ) * partialTicks;
        int n = 1;
        double[][] points = new double[EntityStormglassBolt.TRAIL + 1][];
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
            t.setColorRGBA_F(0.62F + 0.3F * f, 0.5F + 0.4F * f, 1.0F, 0.55F * f);
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
