package naturalexceptions.minecraft.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.ISpecialArmor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import naturalexceptions.minecraft.biology.ArmorPathGate;

/** Players: skip Forge's armor step for a hit whose armor P10 has already resolved and worn. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerArmor {

    @Redirect(
        method = "damageEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/common/ISpecialArmor$ArmorProperties;ApplyArmor(Lnet/minecraft/entity/EntityLivingBase;[Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/DamageSource;D)F",
            remap = false))
    private float naturalexceptions$applyArmorOnce(EntityLivingBase entity, ItemStack[] inventory, DamageSource source,
        double damage) {
        if (entity.worldObj != null && ArmorPathGate.consume(entity, entity.worldObj.getTotalWorldTime())) {
            return (float) damage;
        }
        return ISpecialArmor.ArmorProperties.ApplyArmor(entity, inventory, source, damage);
    }
}
