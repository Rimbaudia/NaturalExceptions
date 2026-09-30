package naturalexceptions.minecraft.furniture.client;

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
 * Draws a Lighthouse Lamp's two beams: long pale-violet blades of light turning about the lamp, bright at the lamp
 * and fading to nothing at their ends, laid in light over what they cross.
 */
@SideOnly(Side.CLIENT)
public final class RenderLighthouseLamp extends TileEntitySpecialRenderer {

    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (tile.getWorldObj() == null
            || !BlockLighthouseLamp.lit(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord)) return;
        double turn = Math.toRadians(
            ((tile.getWorldObj()
                .getTotalWorldTime() + partialTicks) * TileEntityLighthouseLamp.TURN) % 360.0);
        bindTexture(BEAM);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDepthMask(false);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        for (int side = 0; side < 2; side++) beam(t, turn + side * Math.PI);
        t.draw();
        GL11.glDepthMask(true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    /** One beam along {@code angle}: a flat blade and an upright one crossed, so it shows from every side. */
    private static void beam(Tessellator t, double angle) {
        double len = TileEntityLighthouseLamp.BEAM, near = 0.4, far = 3.0;
        double dx = Math.cos(angle), dz = Math.sin(angle), px = -dz, pz = dx;
        // Flat: widening from the lamp out to its end.
        vertex(t, px * near, 0.0, pz * near, 0.0, 0.0, 0.55F);
        vertex(t, dx * len + px * far, 0.0, dz * len + pz * far, 0.0, len / 8, 0.0F);
        vertex(t, dx * len - px * far, 0.0, dz * len - pz * far, 1.0, len / 8, 0.0F);
        vertex(t, -px * near, 0.0, -pz * near, 1.0, 0.0, 0.55F);
        // Upright.
        vertex(t, 0.0, near, 0.0, 0.0, 0.0, 0.55F);
        vertex(t, dx * len, far, dz * len, 0.0, len / 8, 0.0F);
        vertex(t, dx * len, -far, dz * len, 1.0, len / 8, 0.0F);
        vertex(t, 0.0, -near, 0.0, 1.0, 0.0, 0.55F);
    }

    private static void vertex(Tessellator t, double x, double y, double z, double u, double v, float alpha) {
        t.setColorRGBA_F(0.85F, 0.8F, 1.0F, alpha);
        t.addVertexWithUV(x, y, z, u, v);
    }
}
