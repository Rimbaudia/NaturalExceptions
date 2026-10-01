package naturalexceptions.minecraft.entity;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** What the sky's creatures do that is not theirs to do in their own update: the shock of a Stormglass arrow. */
public final class SkyMobEvents {

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.source.getSourceOfDamage() instanceof EntityStormArrow) || event.entityLiving.worldObj.isRemote)
            return;
        event.entityLiving.addPotionEffect(
            new PotionEffect(
                Potion.moveSlowdown.id,
                EntityStormArrow.SHOCK_TICKS,
                EntityStormArrow.SHOCK_AMPLIFIER));
        event.entityLiving.worldObj.playSoundAtEntity(event.entityLiving, "fireworks.blast", 0.5F, 1.8F);
        if (event.entityLiving.worldObj instanceof WorldServer)
            ((WorldServer) event.entityLiving.worldObj).func_147487_a(
                "fireworksSpark",
                event.entityLiving.posX,
                event.entityLiving.posY + event.entityLiving.height * 0.6,
                event.entityLiving.posZ,
                12,
                0.3,
                0.4,
                0.3,
                0.08);
    }
}
