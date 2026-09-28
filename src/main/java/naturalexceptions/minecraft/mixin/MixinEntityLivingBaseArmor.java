package naturalexceptions.minecraft.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import naturalexceptions.minecraft.biology.ArmorPathGate;

/** Other living things: skip vanilla armor for a hit whose armor P10 has already resolved and worn. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseArmor {

    @Inject(method = "applyArmorCalculations", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$applyArmorOnce(DamageSource source, float damage,
        CallbackInfoReturnable<Float> cir) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (self.worldObj != null && ArmorPathGate.consume(self, self.worldObj.getTotalWorldTime())) {
            cir.setReturnValue(damage);
        }
    }
}
