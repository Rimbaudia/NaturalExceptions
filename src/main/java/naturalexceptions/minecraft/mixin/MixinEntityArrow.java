package naturalexceptions.minecraft.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import naturalexceptions.minecraft.gravity.NEProjectileHelper;

@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;F)V", at = @At("RETURN"))
    private void naturalexceptions$onArrowInit(World world, EntityLivingBase shooter, float speed, CallbackInfo ci) {
        NEProjectileHelper.alignArrowLaunch((EntityArrow) (Object) this, shooter, speed * 1.5F);
    }
}
