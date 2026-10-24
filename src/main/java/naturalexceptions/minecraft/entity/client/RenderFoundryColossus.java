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
import naturalexceptions.minecraft.entity.EntityFoundryColossus;

/** The Foundry Colossus, its fire glowing through its grille, eyes and crucible; low asleep, out when doused. */
@SideOnly(Side.CLIENT)
public final class RenderFoundryColossus extends RenderLiving {

    static final float SCALE = 1.5F;
    private static final ResourceLocation SKIN = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/foundry_colossus.png");
    private static final ResourceLocation GLOW = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/foundry_colossus_glow.png");

    public RenderFoundryColossus() {
        super(new ModelFoundryColossus(), 1.4F);
        setRenderPassModel(new ModelFoundryColossus());
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityFoundryColossus
            && ((EntityFoundryColossus) entity).state() != EntityFoundryColossus.ASLEEP)
            BossStatus.setBossStatus((EntityFoundryColossus) entity, true);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(SCALE, SCALE, SCALE);
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        if (pass != 0) return -1;
        int state = entity instanceof EntityFoundryColossus ? ((EntityFoundryColossus) entity).state()
            : EntityFoundryColossus.AWAKE;
        bindTexture(GLOW);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(!entity.isInvisible());
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        float glow = state == EntityFoundryColossus.ASLEEP ? 0.15F
            : state == EntityFoundryColossus.QUENCHED ? 0.35F
                : 0.85F + 0.15F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.25);
        GL11.glColor4f(glow, glow * 0.95F, glow * 0.9F, 1.0F);
        return 1;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SKIN;
    }
}
