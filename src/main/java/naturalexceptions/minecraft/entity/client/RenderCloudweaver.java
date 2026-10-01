package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A Cloudweaver: the spider, on its own grey-white skin, its violet eyes glowing in the dark. */
@SideOnly(Side.CLIENT)
public final class RenderCloudweaver extends RenderSpider {

    private static final ResourceLocation SKIN = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/cloudweaver.png");
    private static final ResourceLocation EYES = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/cloudweaver_eyes.png");

    @Override
    protected ResourceLocation getEntityTexture(EntitySpider spider) {
        return SKIN;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SKIN;
    }

    /** Its eyes, drawn bright over the skin as a spider's are. */
    @Override
    protected int shouldRenderPass(EntitySpider spider, int pass, float partialTicks) {
        if (pass != 0) return -1;
        bindTexture(EYES);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(!spider.isInvisible());
        char bright = 61680;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bright % 65536, bright / 65536);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return 1;
    }
}
