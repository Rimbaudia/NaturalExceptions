package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.TileEntityHoist;

/**
 * A windlass's rope, hanging from the derrick's boom down to its basket at the foot of the quarry. While the basket is
 * away on a ride, the ride draws it instead.
 */
@SideOnly(Side.CLIENT)
public final class RenderHoistRope extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityHoist)) return;
        int[] rope = ((TileEntityHoist) tile).rope(); // basket x, y, z and the rope's top
        if (rope == null) return;
        int light = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(rope[0], rope[1] + 1, rope[2], 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x + rope[0] - tile.xCoord, y + rope[1] - tile.yCoord, z + rope[2] - tile.zCoord);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHoistHook
            .box(Blocks.planks.getIcon(1, 0), new double[] { 0.45, 1.0, 0.45, 0.55, rope[3] - rope[1], 0.55 });
        GL11.glPopMatrix();
    }
}
