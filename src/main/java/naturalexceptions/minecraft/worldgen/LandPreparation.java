package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Makes the land around a teleport's landing spot a few chunks a tick before the player is sent there. Arriving in
 * land not yet generated had the server make everything in view at once, a citadel's worth of island and buildings,
 * and it fell tens of seconds behind: creatures and blocks stood still and then lurched. Spread over ticks it is the
 * same work, but the game keeps running, and the player arrives to finished land.
 */
public final class LandPreparation {

    /** Generation allowed in one tick, so the server keeps near its twenty ticks a second. */
    private static final long BUDGET_NANOS = 20_000_000L;
    private static final int REPORT_TICKS = 20;

    private static final Map<UUID, Trip> TRIPS = new HashMap<>();

    private static final class Trip {

        final WorldServer world;
        final double x, y, z;
        final String what;
        final Runnable arrived;
        final List<int[]> chunks;
        int next, ticks;

        Trip(WorldServer world, double x, double y, double z, String what, Runnable arrived, List<int[]> chunks) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.what = what;
            this.arrived = arrived;
            this.chunks = chunks;
        }
    }

    /**
     * Sends {@code player} to (x, y, z) once the land in view of it is made; {@code what} names it in the progress
     * lines ("the citadel's land"), and {@code arrived} runs on arrival. A later trip replaces a pending one.
     */
    public static void start(EntityPlayerMP player, double x, double y, double z, String what, Runnable arrived) {
        WorldServer world = (WorldServer) player.worldObj;
        int view = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance();
        // One ring past the view, so the chunks in view are populated too (population waits for neighbours).
        List<int[]> chunks = nearestFirst((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4, view + 1);
        TRIPS.put(player.getUniqueID(), new Trip(world, x, y, z, what, arrived, chunks));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRIPS.isEmpty()) return;
        long start = System.nanoTime();
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Trip> entry : TRIPS.entrySet()) {
            Trip trip = entry.getValue();
            EntityPlayerMP player = player(entry.getKey());
            if (player == null || player.isDead || player.worldObj != trip.world) {
                done.add(entry.getKey());
                continue;
            }
            while (trip.next < trip.chunks.size() && System.nanoTime() - start < BUDGET_NANOS) {
                int[] c = trip.chunks.get(trip.next++);
                if (!trip.world.theChunkProviderServer.chunkExists(c[0], c[1]))
                    trip.world.theChunkProviderServer.loadChunk(c[0], c[1]);
            }
            if (trip.next >= trip.chunks.size()) {
                player.setPositionAndUpdate(trip.x, trip.y, trip.z);
                trip.arrived.run();
                done.add(entry.getKey());
            } else if (trip.ticks++ % REPORT_TICKS == 0) {
                player.addChatMessage(
                    new ChatComponentText(
                        "§7Preparing " + trip.what + ": " + trip.next * 100 / trip.chunks.size() + "%"));
            }
        }
        for (UUID id : done) TRIPS.remove(id);
    }

    private static EntityPlayerMP player(UUID id) {
        for (Object o : MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (p.getUniqueID()
                .equals(id)) return p;
        }
        return null;
    }

    /** The chunks within {@code reach} of (cx, cz), nearest first, so what is closest to the landing is made first. */
    static List<int[]> nearestFirst(int cx, int cz, int reach) {
        List<int[]> out = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++)
            for (int dz = -reach; dz <= reach; dz++) out.add(new int[] { cx + dx, cz + dz });
        out.sort((a, b) -> Integer.compare(ring(a, cx, cz), ring(b, cx, cz)));
        return out;
    }

    private static int ring(int[] c, int cx, int cz) {
        return Math.max(Math.abs(c[0] - cx), Math.abs(c[1] - cz));
    }
}
