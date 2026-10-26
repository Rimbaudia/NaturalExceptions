package naturalexceptions.minecraft.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.core.worldgen.Updrafts;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketUpdraft;

/**
 * The updrafts beside the sky islands (see {@link Updrafts}), on the server: a player standing in one is told so (their
 * client carries them up), and the mist drifting up each column near a player shows where it stands.
 */
public final class UpdraftWinds {

    /** How often each player is looked at, in ticks. */
    static final long CHECK_TICKS = 10;
    /** How near a column must be to a player for them to see its mist. */
    static final int SEEN = 48;

    private static Updrafts updrafts;
    private final Map<UUID, Long> lastChecked = new HashMap<>();

    static synchronized Updrafts updrafts(SkyIslands field) {
        if (updrafts == null || updrafts.field() != field) updrafts = new Updrafts(field);
        return updrafts;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || !(event.entityLiving instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        if (player.worldObj.provider.dimensionId != 0) return;
        // A player is updated once per movement packet, not once a tick: pace by the clock.
        long now = player.worldObj.getTotalWorldTime();
        Long checked = lastChecked.get(player.getUniqueID());
        if (checked != null && now - checked < CHECK_TICKS && now >= checked) return;
        lastChecked.put(player.getUniqueID(), now);
        SkyIslands field = DungeonBeacons.field(player.worldObj);
        Updrafts drafts = updrafts(field);
        Updrafts.Column in = drafts.at(player.posX, player.boundingBox.minY, player.posZ);
        if (in != null) {
            NENetwork.CHANNEL.sendTo(new PacketUpdraft(in.top), player);
            grounded(player);
        }
        mist(player, drafts, field);
    }

    /**
     * Rising with nothing underfoot, a player would be taken for flying and sent off a server that doesn't allow it:
     * the air carrying them up counts as footing.
     */
    private static void grounded(EntityPlayerMP player) {
        NetHandlerPlayServer handler = player.playerNetServerHandler;
        if (handler == null) return;
        try {
            ObfuscationReflectionHelper
                .setPrivateValue(NetHandlerPlayServer.class, handler, 0, "field_147365_f", "floatingTickCount");
        } catch (RuntimeException ignored) {
            // A renamed field costs only the kick it would have spared.
        }
        player.fallDistance = 0.0F;
    }

    /** Wisps of mist drifting up the columns near the player, about their height. */
    private static void mist(EntityPlayerMP player, Updrafts drafts, SkyIslands field) {
        if (!(player.worldObj instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.worldObj;
        int x = (int) Math.floor(player.posX), z = (int) Math.floor(player.posZ);
        for (SkyIslands.Island island : field.near(x - SEEN, z - SEEN, x + SEEN, z + SEEN)) {
            Updrafts.Column column = drafts.of(island);
            if (column == null) continue;
            double dx = column.x - player.posX, dz = column.z - player.posZ;
            if (dx * dx + dz * dz > (double) SEEN * SEEN) continue;
            for (int i = 0; i < 6; i++) {
                double y = player.posY + (world.rand.nextDouble() - 0.4) * 24.0;
                if (y < Updrafts.BASE_Y || y > column.top) continue;
                double a = world.rand.nextDouble() * Math.PI * 2, r = world.rand.nextDouble() * Updrafts.RADIUS;
                // Count 0: one wisp, moving along the offset (straight up) at the given speed.
                world.func_147487_a(
                    "cloud",
                    column.x + Math.cos(a) * r,
                    y,
                    column.z + Math.sin(a) * r,
                    0,
                    0.0,
                    1.0,
                    0.0,
                    0.25);
            }
        }
    }

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) lastChecked.remove(event.player.getUniqueID());
    }
}
