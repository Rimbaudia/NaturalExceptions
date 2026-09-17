package naturalexceptions.minecraft.mixin;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.gravity.NEEntityRenderHandler;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

    @Inject(method = "rotateCorpse", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$onRotateCorpse(EntityLivingBase entity, float p_77043_2_, float bodyYaw,
        float partialTicks, CallbackInfo ci) {
        if (NEEntityRenderHandler.shouldApplyCustomOrientation(entity)) {
            NEEntityRenderHandler.applyEntityOrientation(entity, bodyYaw, partialTicks);
            ci.cancel();
        }
    }
}
