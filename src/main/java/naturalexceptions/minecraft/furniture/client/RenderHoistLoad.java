package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.EntityHoistLoad;
import naturalexceptions.minecraft.furniture.HoistGeometry;

/**
 * A hoist's basket on its rope: the basket, its four slings up to the ring, and the rope from the ring straight up to
 * the far side of the sheave's wheel. The rider does not see the slings that run past their own eyes.
 */
@SideOnly(Side.CLIENT)
public final class RenderHoistLoad extends Render {

    /** How high over the basket's floor its slings meet: above a rider's head. */
    private static final double RING = 2.1;

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
        if (!(entity instanceof EntityHoistLoad)) return;
        EntityHoistLoad load = (EntityHoistLoad) entity;
        double ix = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double iy = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double iz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        int light = entity.worldObj
            .getLightBrightnessForSkyBlocks((int) Math.floor(ix), (int) Math.floor(iy + 1), (int) Math.floor(iz), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslated(x - 0.5, y, z - 0.5); // the basket's corner, its floor level with the entity's foot
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        double[][] basket = HoistGeometry.basket();
        for (int i = 0; i < basket.length - 1; i++) HoistDrawing.box(Blocks.planks.getIcon(1, 1), basket[i]);
        // Where the rope comes down: under the far edge of the wheel, a little off the basket's middle.
        double[] away = HoistGeometry.away(load);
        double ringX = 0.5 + away[0] * HoistGeometry.WHEEL, ringZ = 0.5 + away[2] * HoistGeometry.WHEEL;
        IIcon rope = HoistGeometry.ropeIcon();
        Minecraft mc = Minecraft.getMinecraft();
        boolean ownEyes = mc.gameSettings.thirdPersonView == 0 && load.riddenByEntity == mc.renderViewEntity;
        if (!ownEyes)
            for (double[] c : new double[][] { { 0.12, 0.12 }, { 0.88, 0.12 }, { 0.12, 0.88 }, { 0.88, 0.88 } })
                HoistDrawing.cord(rope, new double[] { c[0], 0.6, c[1] }, new double[] { ringX, RING, ringZ }, 0.03);
        double top = load.sheave()[1] - iy; // the axle's height over the basket's floor
        if (top > RING)
            HoistDrawing.cord(rope, new double[] { ringX, RING, ringZ }, new double[] { ringX, top, ringZ }, 0.05);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TextureMap.locationBlocksTexture;
    }
}
