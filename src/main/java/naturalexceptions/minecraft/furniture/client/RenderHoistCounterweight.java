package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.HoistGeometry;

/** A counterweight box hanging in its well: the iron-banded crate and its bail (its rope is the windlass's to draw). */
@SideOnly(Side.CLIENT)
public final class RenderHoistCounterweight extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        // Shaded face by face as blocks are, so not lit again by the item lighting (which left the faces turned
        // from its lights near black).
        boolean lit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHTING);
        draw(entity, x, y, z, partialTicks);
        if (lit) GL11.glEnable(GL11.GL_LIGHTING);
    }

    private void draw(Entity entity, double x, double y, double z, float partialTicks) {
        int light = entity.worldObj.getLightBrightnessForSkyBlocks(
            (int) Math.floor(entity.posX),
            (int) Math.floor(entity.posY + 0.5),
            (int) Math.floor(entity.posZ),
            0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x - 0.5, y, z - 0.5);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        double[][] parts = HoistGeometry.counterweight();
        for (int i = 0; i < parts.length; i++)
            HoistDrawing.box(i == 0 ? Blocks.planks.getIcon(1, 1) : Blocks.iron_block.getIcon(1, 0), parts[i]);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
