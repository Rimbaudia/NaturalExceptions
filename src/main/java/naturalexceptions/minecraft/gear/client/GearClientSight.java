package naturalexceptions.minecraft.gear.client;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/** The Diver's Bell Helm's glass port: water stays clear far further than the eye alone sees it. */
@SideOnly(Side.CLIENT)
public final class GearClientSight {

    /** Vanilla water fog is 0.1 (0.05 with Water Breathing). */
    static final float CLEAR_WATER_FOG = 0.015F;

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (event.block == null || event.block.getMaterial() != Material.water
            || !(event.entity instanceof EntityPlayer)) return;
        if (ItemTreasureGear.worn((EntityPlayer) event.entity, Treasure.DIVERS_BELL_HELM) == 0) return;
        event.density = CLEAR_WATER_FOG;
        event.setCanceled(true);
    }
}
