package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityCitadelGuard;

/**
 * A Citadel Guard: a man in a Skystone-plated helm and tabard, a halberd upright in his right hand, a tower shield
 * on his left arm, swung round before him when he guards.
 */
@SideOnly(Side.CLIENT)
public final class RenderCitadelGuard extends RenderBiped {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "naturalexceptions",
        "textures/entity/citadel_guard.png");

    public RenderCitadelGuard() {
        super(new Model(), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return TEXTURE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return TEXTURE;
    }

    /** The biped on a 64 x 64 skin: the shield and the halberd's parts drawn from its lower half. */
    static final class Model extends ModelBiped {

        final ModelRenderer shieldSide, shieldFront, pole, blade, spike;

        Model() {
            super(0.0F, 0.0F, 64, 64);
            // At his side, on the outside of his left arm.
            shieldSide = new ModelRenderer(this, 20, 32);
            shieldSide.addBox(3.0F, -3.0F, -5.0F, 1, 15, 10);
            bipedLeftArm.addChild(shieldSide);
            // Up, before his chest.
            shieldFront = new ModelRenderer(this, 0, 32);
            shieldFront.addBox(-4.0F, -1.0F, -5.0F, 8, 15, 1);
            bipedBody.addChild(shieldFront);
            // The halberd, upright through his right fist.
            pole = new ModelRenderer(this, 44, 32);
            pole.addBox(-1.5F, -14.0F, -0.5F, 1, 30, 1);
            bipedRightArm.addChild(pole);
            blade = new ModelRenderer(this, 50, 32);
            blade.addBox(-1.5F, -13.0F, -4.0F, 1, 5, 3);
            bipedRightArm.addChild(blade);
            spike = new ModelRenderer(this, 50, 42);
            spike.addBox(-1.5F, -17.0F, -0.5F, 1, 3, 1);
            bipedRightArm.addChild(spike);
        }

        @Override
        public void setRotationAngles(float swing, float swingAmount, float age, float headYaw, float headPitch,
            float scale, Entity entity) {
            super.setRotationAngles(swing, swingAmount, age, headYaw, headPitch, scale, entity);
            boolean up = entity instanceof EntityCitadelGuard && ((EntityCitadelGuard) entity).shieldUp();
            shieldFront.showModel = up;
            shieldSide.showModel = !up;
            if (up) {
                bipedLeftArm.rotateAngleX = -0.9F;
                bipedLeftArm.rotateAngleY = 0.5F;
            }
            // The halberd held upright, its head over his shoulder, unless he is swinging it.
            if (onGround <= 0.0F) bipedRightArm.rotateAngleX = Math.min(bipedRightArm.rotateAngleX, -0.15F);
        }
    }
}
