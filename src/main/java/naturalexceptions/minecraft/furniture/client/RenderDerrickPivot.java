package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.TileEntityDerrickPivot;

/**
 * A derrick's knee brace, a timber from the mast up under the boom that turns with it; its guy ropes, from the mast's
 * top down to the stakes set round it (fence posts on the ground within
 * {@link TileEntityDerrickPivot#STAKE_REACH} of the pivot); and its boom while it swings: its logs and the sheave's
 * cheeks at its tip, turning about the mast.
 */
@SideOnly(Side.CLIENT)
public final class RenderDerrickPivot extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityDerrickPivot)) return;
        TileEntityDerrickPivot pivot = (TileEntityDerrickPivot) tile;
        int[][] stays = pivot.stays();
        if (stays.length > 0) {
            int light = tile.getWorldObj()
                .getLightBrightnessForSkyBlocks(stays[0][0], stays[0][1] + 1, stays[0][2], 0); // open air over a stake
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            bindTexture(TextureMap.locationBlocksTexture);
            GL11.glPushMatrix();
            GL11.glTranslated(x - tile.xCoord, y - tile.yCoord, z - tile.zCoord);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            double[] head = { tile.xCoord + 0.5, stays[0][3] + 0.85, tile.zCoord + 0.5 };
            for (int[] s : stays) HoistDrawing.cord(
                naturalexceptions.minecraft.furniture.HoistGeometry.ropeIcon(),
                head,
                new double[] { s[0] + 0.5, s[1] + 0.95, s[2] + 0.5 },
                0.035);
            GL11.glPopMatrix();
        }
        int[] brace = pivot.brace();
        if (!pivot.slewing() && brace == null) return;
        double angle = pivot.slewing() ? pivot.angle(pivot.ticksIn(partialTicks))
            : TileEntityDerrickPivot.angleOf(brace[0]);
        int mastTop = pivot.slewing() ? pivot.mastTop() : brace[1];
        IIcon log = Blocks.log.getIcon(2, 1), plank = Blocks.planks.getIcon(1, 1);
        if (mastTop - tile.yCoord >= 3) {
            // Lit as the air it stands in beside the mast, under the boom (the mast's own block is dark inside).
            int lx = tile.xCoord + (int) Math.round(Math.cos(angle)),
                lz = tile.zCoord + (int) Math.round(Math.sin(angle));
            int light = tile.getWorldObj()
                .getLightBrightnessForSkyBlocks(lx, mastTop - 1, lz, 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            bindTexture(TextureMap.locationBlocksTexture);
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5, y + mastTop - tile.yCoord + 0.5, z + 0.5);
            GL11.glRotated(-Math.toDegrees(angle), 0, 1, 0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            // From the mast's face two blocks down to the boom's underside two blocks out, at 45 degrees.
            GL11.glTranslated(1.25, -1.25, 0);
            GL11.glRotated(45, 0, 0, 1);
            double half = 1.06;
            HoistDrawing.box(log, new double[] { -half, -0.14, -0.14, half, 0.14, 0.14 });
            GL11.glPopMatrix();
        }
        if (!pivot.slewing()) return;
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(tile.xCoord, pivot.mastTop(), tile.zCoord, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + pivot.mastTop() - tile.yCoord + 0.5, z + 0.5);
        GL11.glRotated(-Math.toDegrees(angle), 0, 1, 0); // local +x now runs out along the boom
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int length = pivot.length();
        HoistDrawing.box(log, new double[] { 0.5, -0.5, -0.5, length - 0.5, 0.5, 0.5 });
        // The sheave's two cheeks and the bar over its wheel, at the tip.
        double t = length;
        HoistDrawing.box(plank, new double[] { t - 0.4375, -0.375, -0.5, t + 0.4375, 0.5, -0.375 });
        HoistDrawing.box(plank, new double[] { t - 0.4375, -0.375, 0.375, t + 0.4375, 0.5, 0.5 });
        HoistDrawing.box(plank, new double[] { t - 0.5, 0.375, -0.375, t + 0.5, 0.5, 0.375 });
        GL11.glPopMatrix();
    }
}
