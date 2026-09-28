package naturalexceptions.minecraft.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketP8Status;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * The bounded, player-anchored ways P8 reaches the world: it tells the event system where players are, refines
 * anomalies only around them, announces season and Blood Night changes once, runs the Blood Night night surge,
 * and keeps each client's small status in sync. No state here is authoritative; all of it is re-derived.
 */
public final class P8WorldPresence {

    /** Status/notice cadence. */
    static final int CHECK_INTERVAL_TICKS = 20;
    /** Blood Night surge cadence per player. */
    static final int SURGE_INTERVAL_TICKS = 200;
    /** Hostiles within this range suppress further surge spawns. */
    static final int SURGE_LOCAL_CAP = 6;
    /** Players within this range of an impact hear and see it fall. */
    public static final double IMPACT_NOTICE_RANGE = 1024.0;

    private final Map<UUID, Long> refinedCell = new HashMap<>();
    private final Map<UUID, Boolean> bloodNightSent = new HashMap<>();
    private NaturalCalendar.Season lastSeason;

    /** Regions currently occupied by overworld players; these witness world-wide events. */
    public static List<RegionKey> witnessedRegions(World world) {
        List<RegionKey> regions = new ArrayList<>();
        if (world == null) return regions;
        for (Object object : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            RegionKey region = RegionKey.fromBlock(MathHelper.floor_double(player.posX),
                MathHelper.floor_double(player.posZ));
            if (!regions.contains(region)) regions.add(region);
        }
        return regions;
    }

    /** True while the sun is down (Minecraft time of day 13000 to 23000). */
    public static boolean isNight(long worldTime) {
        long timeOfDay = Math.floorMod(worldTime, NaturalCalendar.TICKS_PER_DAY);
        return timeOfDay >= 13_000L && timeOfDay < 23_000L;
    }

    public void tick(World world, NEWorldSavedData data, long worldTime) {
        if (world == null || data == null || data.p8() == null || worldTime % CHECK_INTERVAL_TICKS != 0) return;
        P8WorldState p8 = data.p8();

        NaturalCalendar.Season season = NaturalCalendar.at(worldTime)
            .season();
        if (lastSeason != null && lastSeason != season) {
            broadcast(world, "§7" + P8Presentation.seasonBeginsMessage(season));
        }
        lastSeason = season;

        for (Object object : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            int x = MathHelper.floor_double(player.posX);
            int z = MathHelper.floor_double(player.posZ);
            UUID id = player.getUniqueID();

            // Anomalies are refined only where a player actually is, once per 1024-block cell visited.
            long cell = (((long) Math.floorDiv(x, 1024)) << 32) ^ (Math.floorDiv(z, 1024) & 0xffffffffL);
            Long previousCell = refinedCell.get(id);
            if (previousCell == null || previousCell != cell) {
                refinedCell.put(id, cell);
                if (p8.ensureAnomaliesNear(x, z)) data.markDirty();
            }

            boolean blood = p8.bloodNightActiveAt(x, z, worldTime);
            Boolean sent = bloodNightSent.get(id);
            if (sent == null || sent != blood) {
                bloodNightSent.put(id, blood);
                if (sent != null) {
                    player.addChatMessage(
                        new ChatComponentText(
                            blood ? P8Presentation.bloodNightBeginsMessage() : P8Presentation.bloodNightEndsMessage()));
                }
                if (player instanceof EntityPlayerMP) {
                    try {
                        NENetwork.CHANNEL.sendTo(new PacketP8Status(blood), (EntityPlayerMP) player);
                    } catch (RuntimeException notConnected) {
                        // Test or not-yet-connected players simply receive the status on the next change.
                    }
                }
            }

            if (blood && isNight(worldTime)
                && Math.floorMod(worldTime + (id.hashCode() & 0xff), SURGE_INTERVAL_TICKS) < CHECK_INTERVAL_TICKS) {
                surgeNear(world, player);
            }
        }
    }

    /** One restrained extra hostile, in the dark, out of sight range, when few are already about. */
    private static void surgeNear(World world, EntityPlayer player) {
        AxisAlignedBB around = AxisAlignedBB.getBoundingBox(
            player.posX - 32,
            player.posY - 16,
            player.posZ - 32,
            player.posX + 32,
            player.posY + 16,
            player.posZ + 32);
        int hostiles = 0;
        for (Object object : world.getEntitiesWithinAABB(net.minecraft.entity.EntityLiving.class, around)) {
            if (object instanceof IMob) hostiles++;
        }
        if (hostiles >= SURGE_LOCAL_CAP) return;
        double angle = world.rand.nextDouble() * Math.PI * 2.0;
        double radius = 18.0 + world.rand.nextDouble() * 10.0;
        int x = MathHelper.floor_double(player.posX + Math.cos(angle) * radius);
        int z = MathHelper.floor_double(player.posZ + Math.sin(angle) * radius);
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) return;
        int y = world.getHeightValue(x, z);
        if (world.getBlockLightValue(x, y, z) > 7) return;
        EntityZombie zombie = new EntityZombie(world);
        zombie.setLocationAndAngles(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360.0F, 0.0F);
        if (!zombie.getCanSpawnHere()) return;
        zombie.onSpawnWithEgg(null);
        world.spawnEntityInWorld(zombie);
    }

    /** Every overworld player in range learns roughly where a star just fell. */
    public static void announceImpact(World world, StarRainImpact impact) {
        if (world == null || impact == null) return;
        for (Object object : world.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            double dx = impact.x() + 0.5 - player.posX;
            double dz = impact.z() + 0.5 - player.posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > IMPACT_NOTICE_RANGE) continue;
            // Close witnesses of a loaded site get the local impact notice from the materializer instead.
            if (distance < 48.0 && world.getChunkProvider()
                .chunkExists(impact.x() >> 4, impact.z() >> 4)) continue;
            player.addChatMessage(new ChatComponentText(P8Presentation.distantImpactMessage(dx, dz)));
            float volume = (float) Math.max(0.15, 1.0 - distance / IMPACT_NOTICE_RANGE);
            world.playSoundAtEntity(player, "ambient.weather.thunder", volume, 1.6F);
        }
    }

    public void forget(UUID player) {
        refinedCell.remove(player);
        bloodNightSent.remove(player);
    }

    private static void broadcast(World world, String message) {
        for (Object object : world.playerEntities) {
            ((EntityPlayer) object).addChatMessage(new ChatComponentText(message));
        }
    }
}
