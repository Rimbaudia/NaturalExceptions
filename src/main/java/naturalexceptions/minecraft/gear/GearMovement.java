package naturalexceptions.minecraft.gear;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.minecraft.item.ItemTreasureGear;
import naturalexceptions.minecraft.network.PacketGearAction;

/**
 * Server side of movement gear: checks the gear is worn, forgives falls it softens, and plays its effect for
 * everyone nearby. The owner's client moves the player (see {@code GearClientEffects}).
 */
public final class GearMovement {

    /** A landing this soon after a glide, climb or cloud jump is forgiven. */
    static final long ASSIST_GRACE_TICKS = 12L;
    private static final long SOUND_GAP_TICKS = 16L;

    private static final Map<EntityPlayer, Long> LAST_ASSIST = new WeakHashMap<>();
    private static final Map<EntityPlayer, Long> LAST_SOUND = new WeakHashMap<>();

    public static void onAction(EntityPlayerMP player, PacketGearAction.Action action) {
        Treasure needed = action == PacketGearAction.Action.DOUBLE_JUMP ? Treasure.CLOUD_FLASK
            : action == PacketGearAction.Action.GLIDE ? Treasure.GLIDER_CLOAK : Treasure.CLIMBING_CLAWS;
        if (ItemTreasureGear.worn(player, needed) == 0) return;
        long now = player.worldObj.getTotalWorldTime();
        player.fallDistance = 0.0F;
        synchronized (LAST_ASSIST) {
            LAST_ASSIST.put(player, now);
        }
        switch (action) {
            case DOUBLE_JUMP:
                burst(player, "cloud", 14, 0.35, 0.02);
                player.worldObj.playSoundAtEntity(player, "naturalexceptions:gear.cloud_jump", 0.8F, 1.0F);
                break;
            case GLIDE:
                if (soundDue(player, now)) player.worldObj.playSoundAtEntity(
                    player,
                    "naturalexceptions:gear.glide",
                    0.4F,
                    0.9F + player.getRNG()
                        .nextFloat() * 0.2F);
                break;
            case CLIMB:
            default:
                if (soundDue(player, now)) player.worldObj.playSoundAtEntity(
                    player,
                    "naturalexceptions:gear.claw_scrape",
                    0.5F,
                    0.9F + player.getRNG()
                        .nextFloat() * 0.3F);
                break;
        }
    }

    private static boolean soundDue(EntityPlayer player, long now) {
        synchronized (LAST_SOUND) {
            Long last = LAST_SOUND.get(player);
            if (last != null && now - last < SOUND_GAP_TICKS) return false;
            LAST_SOUND.put(player, now);
            return true;
        }
    }

    private static void burst(EntityPlayer player, String particle, int count, double spread, double speed) {
        if (player.worldObj instanceof WorldServer) ((WorldServer) player.worldObj)
            .func_147487_a(particle, player.posX, player.posY - 0.2, player.posZ, count, spread, 0.05, spread, speed);
    }

    /** Landing on Cloud, or with Windbloom Tea still working. */
    static boolean softLanding(net.minecraft.entity.EntityLivingBase living) {
        if (naturalexceptions.minecraft.item.ItemWindbloomTea.active(living, living.worldObj.getTotalWorldTime()))
            return true;
        int x = net.minecraft.util.MathHelper.floor_double(living.posX);
        int y = net.minecraft.util.MathHelper.floor_double(living.boundingBox.minY - 0.2);
        int z = net.minecraft.util.MathHelper.floor_double(living.posZ);
        return naturalexceptions.minecraft.registry.NEBlocks.cloud != null
            && living.worldObj.getBlock(x, y, z) == naturalexceptions.minecraft.registry.NEBlocks.cloud;
    }

    /** Healing multiplier from Bands of Mending: half again per band, up to two. */
    public static double mendingMultiplier(int bands) {
        return 1.0 + 0.5 * Math.max(0, Math.min(2, bands));
    }

    /**
     * Soft landings: Cloud underfoot or Windbloom Tea take the whole fall for anyone; for players the Horseshoe
     * Pendant cancels every fall and other gear forgives a landing right after it was used.
     */
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj.isRemote) return;
        net.minecraft.entity.EntityLivingBase living = event.entityLiving;
        if (softLanding(living)) {
            event.setCanceled(true);
            return;
        }
        if (!(living instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) living;
        if (ItemTreasureGear.worn(player, Treasure.HORSESHOE_PENDANT) > 0) {
            event.setCanceled(true);
            return;
        }
        Long last;
        synchronized (LAST_ASSIST) {
            last = LAST_ASSIST.get(player);
        }
        if (last != null && player.worldObj.getTotalWorldTime() - last <= ASSIST_GRACE_TICKS) event.setCanceled(true);
    }
}
