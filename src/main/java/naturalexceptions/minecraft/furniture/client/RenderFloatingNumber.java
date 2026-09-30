package naturalexceptions.minecraft.furniture.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.furniture.EntityFloatingNumber;

/** Draws a damage number turned to the viewer, gold and bold, fading as it rises. */
@SideOnly(Side.CLIENT)
public final class RenderFloatingNumber extends Render {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityFloatingNumber number = (EntityFloatingNumber) entity;
        FontRenderer font = getFontRendererFromRenderManager();
        float age = (number.ticksExisted + partialTicks) / EntityFloatingNumber.LIFE;
        int alpha = (int) (255 * Math.max(0.1F, 1.0F - age * age));
        String text = number.text();
        float scale = 0.035F;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        font.drawStringWithShadow("§l" + text, -font.getStringWidth("§l" + text) / 2, 0, alpha << 24 | 0xFFC830);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return null;
    }
}
