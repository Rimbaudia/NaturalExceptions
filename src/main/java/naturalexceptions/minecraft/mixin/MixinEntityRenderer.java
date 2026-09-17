package naturalexceptions.minecraft.mixin;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.gravity.NECameraHandler;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void naturalexceptions$onOrientCamera(float partialTicks, CallbackInfo ci) {
        NECameraHandler.applyAlignment(partialTicks);
    }
}
