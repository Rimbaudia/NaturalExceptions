package naturalexceptions.minecraft.entity.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntityGaleHusk;

/** The sky's biped creatures drawn on their own skins: the Gale Husk as a zombie, the Stormbone Archer as a skeleton. */
@SideOnly(Side.CLIENT)
public final class RenderSkyMob extends RenderBiped {

    private final ResourceLocation texture;

    private RenderSkyMob(ModelBiped model, String texture) {
        super(model, 0.5F);
        this.texture = new ResourceLocation("naturalexceptions", "textures/entity/" + texture + ".png");
    }

    public static RenderSkyMob galeHusk() {
        return new RenderSkyMob(new HuskModel(), "gale_husk");
    }

    public static RenderSkyMob stormboneArcher() {
        return new RenderSkyMob(new ArcherModel(), "stormbone_archer");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        return texture;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return texture;
    }

    /** A zombie's reach, crouched low when it gathers itself to leap. */
    static final class HuskModel extends ModelZombie {

        @Override
        public void setRotationAngles(float swing, float swingAmount, float age, float headYaw, float headPitch,
            float scale, Entity entity) {
            super.setRotationAngles(swing, swingAmount, age, headYaw, headPitch, scale, entity);
            if (entity instanceof EntityGaleHusk && ((EntityGaleHusk) entity).crouching()) {
                bipedRightLeg.rotateAngleX = -0.6F;
                bipedLeftLeg.rotateAngleX = -0.6F;
                bipedRightArm.rotateAngleX = bipedLeftArm.rotateAngleX = -0.8F;
                bipedBody.rotateAngleX = 0.4F;
            }
        }
    }

    /** A skeleton (thin limbs), its bow always drawn and ready. */
    static final class ArcherModel extends ModelSkeleton {

        @Override
        public void setLivingAnimations(EntityLivingBase entity, float swing, float swingAmount, float partialTicks) {
            aimedBow = true; // vanilla's would cast it to a vanilla skeleton
        }
    }
}
