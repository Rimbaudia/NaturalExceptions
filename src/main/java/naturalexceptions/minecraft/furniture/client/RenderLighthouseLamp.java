package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.BlockLighthouseLamp;
import naturalexceptions.minecraft.furniture.TileEntityLighthouseLamp;

/**
 * Draws a Lighthouse Lamp's turning parts: the Fresnel lens round its core (four panels of prism glass, the two that
 * face the beams with bull's-eyes), and the two beams thrown from those bull's-eyes, each a white-hot blade inside a
 * wide pale-violet glow, full strength most of their length and fading only towards their ends. They are drawn
 * without fog, so the haze of distance does not swallow them.
 */
@SideOnly(Side.CLIENT)
public final class RenderLighthouseLamp extends TileEntitySpecialRenderer {

    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/beacon_beam.png");
    private static final ResourceLocation LENS = new ResourceLocation(
        "naturalexceptions",
        "textures/blocks/lighthouse_lens.png");
    private static final ResourceLocation BULLSEYE = new ResourceLocation(
        "naturalexceptions",
        "textures/blocks/lighthouse_lens_bullseye.png");

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (tile.getWorldObj() == null) return;
        boolean lit = BlockLighthouseLamp.lit(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        double turn = Math.toRadians(
            ((tile.getWorldObj()
                .getTotalWorldTime() + (lit ? partialTicks : 0)) * (lit ? TileEntityLighthouseLamp.TURN : 0.0))
                % 360.0);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        lens(turn);
        if (lit) {
            boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            bindTexture(BEAM);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            for (int side = 0; side < 2; side++) {
                double a = turn + side * Math.PI;
                // The wide glow, then the white-hot blade inside it.
                beam(t, a, 0.45, 4.0, 0.85F, 0.8F, 1.0F, 0.9F);
                beam(t, a, 0.12, 0.9, 1.0F, 0.98F, 0.92F, 1.0F);
            }
            t.draw();
            if (fog) GL11.glEnable(GL11.GL_FOG);
        }
        GL11.glDepthMask(true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /** The lens's four panels round the core, turned with the beams; the bull's-eyes face along them. */
    private void lens(double turn) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator t = Tessellator.instance;
        for (int kind = 0; kind < 2; kind++) {
            bindTexture(kind == 0 ? BULLSEYE : LENS);
            t.startDrawingQuads();
            for (int k = kind; k < 4; k += 2) panel(t, turn + k * Math.PI / 2);
            t.draw();
        }
    }

    /** A lens panel facing out along {@code angle}, {@link BlockLighthouseLamp#LENS} from the middle. */
    private static void panel(Tessellator t, double angle) {
        double r = BlockLighthouseLamp.LENS, dx = Math.cos(angle), dz = Math.sin(angle), px = -dz, pz = dx;
        double y0 = BlockLighthouseLamp.LENS_FOOT, y1 = BlockLighthouseLamp.LENS_TOP;
        double cx = dx * r, cz = dz * r;
        t.addVertexWithUV(cx - px * r, y1, cz - pz * r, 0.0, 0.0);
        t.addVertexWithUV(cx + px * r, y1, cz + pz * r, 1.0, 0.0);
        t.addVertexWithUV(cx + px * r, y0, cz + pz * r, 1.0, 1.0);
        t.addVertexWithUV(cx - px * r, y0, cz - pz * r, 0.0, 1.0);
    }

    /**
     * One beam along {@code angle} from the bull's-eye: a flat blade and an upright one crossed, {@code near} wide at
     * the lens and {@code far} wide at its end, at full {@code alpha} for two thirds of its length, then fading out.
     */
    private static void beam(Tessellator t, double angle, double near, double far, float r, float g, float b,
        float alpha) {
        double len = TileEntityLighthouseLamp.BEAM, start = BlockLighthouseLamp.LENS, mid = len * 2 / 3;
        double h = (BlockLighthouseLamp.LENS_FOOT + BlockLighthouseLamp.LENS_TOP) / 2;
        double dx = Math.cos(angle), dz = Math.sin(angle), px = -dz, pz = dx;
        double wMid = near + (far - near) * 2 / 3;
        double[][] spans = { { start, near, alpha, mid, wMid, alpha * 0.8 }, { mid, wMid, alpha * 0.8, len, far, 0 } };
        for (double[] sp : spans) {
            double d0 = sp[0], w0 = sp[1], d1 = sp[3], w1 = sp[4];
            float a0 = (float) sp[2], a1 = (float) sp[5];
            double v0 = d0 / 8, v1 = d1 / 8;
            // Flat.
            vertex(t, dx * d0 + px * w0, h, dz * d0 + pz * w0, 0.0, v0, r, g, b, a0);
            vertex(t, dx * d1 + px * w1, h, dz * d1 + pz * w1, 0.0, v1, r, g, b, a1);
            vertex(t, dx * d1 - px * w1, h, dz * d1 - pz * w1, 1.0, v1, r, g, b, a1);
            vertex(t, dx * d0 - px * w0, h, dz * d0 - pz * w0, 1.0, v0, r, g, b, a0);
            // Upright.
            vertex(t, dx * d0, h + w0, dz * d0, 0.0, v0, r, g, b, a0);
            vertex(t, dx * d1, h + w1, dz * d1, 0.0, v1, r, g, b, a1);
            vertex(t, dx * d1, h - w1, dz * d1, 1.0, v1, r, g, b, a1);
            vertex(t, dx * d0, h - w0, dz * d0, 1.0, v0, r, g, b, a0);
        }
    }

    private static void vertex(Tessellator t, double x, double y, double z, double u, double v, float r, float g,
        float b, float alpha) {
        t.setColorRGBA_F(r, g, b, alpha);
        t.addVertexWithUV(x, y, z, u, v);
    }
}
