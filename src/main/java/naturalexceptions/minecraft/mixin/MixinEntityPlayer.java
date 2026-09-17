package naturalexceptions.minecraft.mixin;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.gravity.NEGravityService;
import naturalexceptions.minecraft.gravity.NEPlayerMovementHandler;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer {

    @Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$moveEntityWithHeading(float strafe, float forward, CallbackInfo ci) {
        if (NEGravityService.global()
            .isNonstandardGravityActive()) {
            EntityPlayer player = (EntityPlayer) (Object) this;
            NEPlayerMovementHandler.handleMovement(player, strafe, forward);
            ci.cancel();
        }
    }
}
