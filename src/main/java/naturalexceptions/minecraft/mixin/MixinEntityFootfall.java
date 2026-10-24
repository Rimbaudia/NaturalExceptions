package naturalexceptions.minecraft.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/** Featherstep Slippers: pressure plates and tripwires don't feel their wearer. */
@Mixin(Entity.class)
public abstract class MixinEntityFootfall {

    @Inject(method = "doesEntityNotTriggerPressurePlate", at = @At("HEAD"), cancellable = true)
    private void naturalexceptions$featherstep(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (self instanceof EntityPlayer
            && ItemTreasureGear.worn((EntityPlayer) self, Treasure.FEATHERSTEP_SLIPPERS) > 0) cir.setReturnValue(true);
    }
}
