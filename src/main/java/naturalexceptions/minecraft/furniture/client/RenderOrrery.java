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

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        long time = tile.getWorldObj() != null ? tile.getWorldObj()
            .getWorldTime() : 0L;
        float day = ((time % 24000L) + partialTicks) / 24000.0F;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        arm(day * 360.0F, 0.36F, 0.6F, MOON, 0.07F);
        arm(-day * 90.0F + 140.0F, 0.46F, 0.52F, STAR, 0.05F);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /** A thin arm from the stem out to {@code reach} at {@code height}, carrying a small body. */
    private static void arm(float angle, float reach, float height, float[] body, float size) {
        GL11.glPushMatrix();
        GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);
        box(0.0F, height - 0.015F, -0.015F, reach, height + 0.015F, 0.015F, BRASS);
        box(reach - 0.012F, height - 0.12F, -0.012F, reach + 0.012F, height, 0.012F, BRASS);
        box(reach - size, height - 0.12F - size * 2, -size, reach + size, height - 0.12F, size, body);
        GL11.glPopMatrix();
    }

    private static void box(float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        float[] shades = { 1.0F, 0.55F, 0.8F, 0.8F, 0.7F, 0.7F };
        float[][][] faces = { { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } },
            { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 }, { x0, y0, z1 } },
            { { x0, y0, z0 }, { x0, y1, z0 }, { x1, y1, z0 }, { x1, y0, z0 } },
            { { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 } },
            { { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 }, { x0, y1, z0 } },
            { { x1, y0, z0 }, { x1, y1, z0 }, { x1, y1, z1 }, { x1, y0, z1 } } };
        for (int f = 0; f < 6; f++) {
            t.setColorOpaque_F(c[0] * shades[f], c[1] * shades[f], c[2] * shades[f]);
            for (float[] v : faces[f]) t.addVertex(v[0], v[1], v[2]);
        }
        t.draw();
    }
}
