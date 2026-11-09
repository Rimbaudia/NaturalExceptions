package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Orrery's moving parts: a brass arm carrying the moon (pale, at the sun's height) round once a day, in step
 * with the real one, and a longer arm with a small blue wandering star turning slowly the other way.
 */
@SideOnly(Side.CLIENT)
public final class RenderOrrery extends TileEntitySpecialRenderer {

    private static final float[] BRASS = { 0.85F, 0.66F, 0.24F }, MOON = { 0.92F, 0.92F, 0.86F },
        STAR = { 0.55F, 0.62F, 0.95F };
    private static final float[] SHADES = { 1.0F, 0.55F, 0.8F, 0.8F, 0.7F, 0.7F };

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        long time = tile.getWorldObj() != null ? tile.getWorldObj()
            .getWorldTime() : 0L;
        float day = ((time % 24000L) + partialTicks) / 24000.0F;
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        arm(day * 360.0F, 0.36F, 0.6F, MOON, 0.07F);
        arm(-day * 90.0F + 140.0F, 0.46F, 0.52F, STAR, 0.05F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    /** A thin arm from the stem out to {@code reach} at {@code height}, carrying a small body. */
    private static void arm(float angle, float reach, float height, float[] body, float size) {
        GL11.glPushMatrix();
        GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        box(t, 0.0F, height - 0.015F, -0.015F, reach, height + 0.015F, 0.015F, BRASS);
        box(t, reach - 0.012F, height - 0.12F, -0.012F, reach + 0.012F, height, 0.012F, BRASS);
        box(t, reach - size, height - 0.12F - size * 2, -size, reach + size, height - 0.12F, size, body);
        t.draw();
        GL11.glPopMatrix();
    }

    private static void box(Tessellator t, float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        t.setColorOpaque_F(c[0] * SHADES[0], c[1] * SHADES[0], c[2] * SHADES[0]);
        t.addVertex(x0, y1, z0);
        t.addVertex(x0, y1, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y1, z0);
        t.setColorOpaque_F(c[0] * SHADES[1], c[1] * SHADES[1], c[2] * SHADES[1]);
        t.addVertex(x0, y0, z0);
        t.addVertex(x1, y0, z0);
        t.addVertex(x1, y0, z1);
        t.addVertex(x0, y0, z1);
        t.setColorOpaque_F(c[0] * SHADES[2], c[1] * SHADES[2], c[2] * SHADES[2]);
        t.addVertex(x0, y0, z0);
        t.addVertex(x0, y1, z0);
        t.addVertex(x1, y1, z0);
        t.addVertex(x1, y0, z0);
        t.setColorOpaque_F(c[0] * SHADES[3], c[1] * SHADES[3], c[2] * SHADES[3]);
        t.addVertex(x0, y0, z1);
        t.addVertex(x1, y0, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x0, y1, z1);
        t.setColorOpaque_F(c[0] * SHADES[4], c[1] * SHADES[4], c[2] * SHADES[4]);
        t.addVertex(x0, y0, z0);
        t.addVertex(x0, y0, z1);
        t.addVertex(x0, y1, z1);
        t.addVertex(x0, y1, z0);
        t.setColorOpaque_F(c[0] * SHADES[5], c[1] * SHADES[5], c[2] * SHADES[5]);
        t.addVertex(x1, y0, z0);
        t.addVertex(x1, y1, z0);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y0, z1);
    }
}
