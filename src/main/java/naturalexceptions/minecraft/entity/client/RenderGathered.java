package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
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
import naturalexceptions.minecraft.entity.EntityGathered;

/** The Gathered, its faces and seams lit with a pale Stormglass light; and its shades, faint and see-through. */
@SideOnly(Side.CLIENT)
public final class RenderGathered extends RenderLiving {

    static final float SCALE = 1.3F;
    private static final ResourceLocation SKIN = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/gathered.png");
    private static final ResourceLocation GLOW = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/gathered_glow.png");

    public RenderGathered() {
        super(new ModelGathered(), 1.2F);
        setRenderPassModel(new ModelGathered());
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (entity instanceof EntityGathered) BossStatus.setBossStatus((EntityGathered) entity, true);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(SCALE, SCALE, SCALE);
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        if (pass != 0) return -1;
        bindTexture(GLOW);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDepthMask(!entity.isInvisible());
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
        boolean kneeling = entity instanceof EntityGathered && ((EntityGathered) entity).kneeling();
        float glow = kneeling ? 0.3F : 0.75F + 0.25F * (float) Math.sin((entity.ticksExisted + partialTicks) * 0.08);
        GL11.glColor4f(glow, glow, glow, 1.0F);
        return 1;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return SKIN;
    }

    /** A shade: a pale face trailing a wisp of shroud, drawn half see-through. */
    public static final class Shade extends RenderLiving {

        private static final ResourceLocation TEXTURE = new ResourceLocation(
            "naturalexceptions",
            "textures/entity/gathered_shade.png");

        public Shade() {
            super(new ShadeModel(), 0.0F);
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.65F);
        }

        @Override
        public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
            super.doRender(entity, x, y, z, yaw, partialTicks);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return TEXTURE;
        }
    }

    static final class ShadeModel extends ModelBase {

        final ModelRenderer face, wisp;

        ShadeModel() {
            textureWidth = 32;
            textureHeight = 32;
            face = new ModelRenderer(this, 0, 0);
            face.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6);
            face.setRotationPoint(0.0F, 18.0F, 0.0F);
            wisp = new ModelRenderer(this, 0, 12);
            wisp.addBox(-2.0F, 0.0F, 0.0F, 4, 4, 6);
            wisp.setRotationPoint(0.0F, 0.0F, 2.0F);
            face.addChild(wisp);
        }

        @Override
        public void render(Entity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch,
            float scale) {
            face.rotateAngleY = yaw / (180.0F / (float) Math.PI);
            face.rotateAngleX = pitch / (180.0F / (float) Math.PI);
            wisp.rotateAngleX = 0.3F + (float) Math.sin(age * 0.3F) * 0.25F;
            face.render(scale);
        }
    }
}
