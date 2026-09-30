package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntitySkystoneSentinel;

/** A Skystone Sentinel: a biped in Skystone plates, a head taller than a man, fists raised over its head to slam. */
@SideOnly(Side.CLIENT)
public final class RenderSkystoneSentinel extends RenderBiped {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/skystone_sentinel.png");
    static final float SCALE = 1.15F;

    public RenderSkystoneSentinel() {
        super(new Model(), 0.6F);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(SCALE, SCALE, SCALE);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return TEXTURE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }

    /** The biped, with both arms thrown up overhead while it winds up a Ground Slam. */
    static final class Model extends ModelBiped {

        @Override
        public void setRotationAngles(float swing, float swingAmount, float age, float headYaw, float headPitch,
            float scale, Entity entity) {
            super.setRotationAngles(swing, swingAmount, age, headYaw, headPitch, scale, entity);
            if (entity instanceof EntitySkystoneSentinel && ((EntitySkystoneSentinel) entity).windingUp()) {
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -2.9F;
                bipedRightArm.rotateAngleZ = 0.2F;
                bipedLeftArm.rotateAngleZ = -0.2F;
            }
        }
    }
}
