package naturalexceptions.minecraft.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/**
 * One of the machines the dead city left keeping its watch (its Automatons, the dungeons' Skystone Sentinels). They
 * take whoever wears a Sentry's Crest for one of their own, until that one strikes them.
 */
public interface CitadelMachine {

    /** Whether this machine lets {@code player} pass: a Sentry's Crest, and no blow struck at it. */
    static boolean passes(EntityPlayer player, EntityLivingBase machine) {
        return player != null && machine.getAITarget() != player
            && ItemTreasureGear.worn(player, Treasure.SENTRYS_CREST) > 0;
    }
}
