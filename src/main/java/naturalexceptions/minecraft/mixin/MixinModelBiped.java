package naturalexceptions.minecraft.mixin;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gravity.NEEntityRenderHandler;

@SideOnly(Side.CLIENT)
@Mixin(ModelBiped.class)
public abstract class MixinModelBiped {

    @Inject(method = "setRotationAngles", at = @At("RETURN"))
    private void naturalexceptions$onSetRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
        float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
        if (entity instanceof EntityLivingBase
            && NEEntityRenderHandler.shouldApplyCustomOrientation((EntityLivingBase) entity)) {
            NEEntityRenderHandler.applyModelAngles((ModelBiped) (Object) this, (EntityLivingBase) entity);
        }
    }
}
