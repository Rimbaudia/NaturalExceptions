package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntitySkyWarden;

/** Draws the Sky Warden 1.3 times player size, its core and visor glowing (and its cracks once Unbound). */
@SideOnly(Side.CLIENT)
public final class RenderSkyWarden extends RenderLiving {

    static final float SCALE = 1.3F;
    private static final ResourceLocation SKIN = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/sky_warden.png");
    private static final ResourceLocation GLOW = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/sky_warden_glow.png");
    private static final ResourceLocation CRACKS = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/sky_warden_cracks.png");

    public RenderSkyWarden() {
        super(new ModelSkyWarden(), 1.0F);
        setRenderPassModel(new ModelSkyWarden());
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntitySkyWarden) BossStatus.setBossStatus((EntitySkyWarden) entity, true);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(SCALE, SCALE, SCALE);
    }

    /** Pass 0: the core and visor, full bright. Pass 1: the cracks, once it is Unbound, pulsing. */
    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        if (pass > 1 || pass == 1 && !(entity instanceof EntitySkyWarden && ((EntitySkyWarden) entity).unbound()))
            return -1;
        bindTexture(pass == 0 ? GLOW : CRACKS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(!entity.isInvisible());
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        float glow = pass == 0 ? 0.8F + 0.2F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.12)
            : 0.6F + 0.4F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.3);
        GL11.glColor4f(glow, glow, glow, 1.0F);
        return 1;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SKIN;
    }
}
