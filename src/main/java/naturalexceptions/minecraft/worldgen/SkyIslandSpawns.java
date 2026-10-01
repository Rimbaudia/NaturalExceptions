package naturalexceptions.minecraft.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.core.worldgen.SkyIslands;

/**
 * Nothing spawns of itself on the sky islands, by night or by day: their creatures are the dungeons' own, set there by
 * their spawners and alcoves, and the citadels' guards. (Natural spawning asks for its list of creatures through
 * {@link WorldEvent.PotentialSpawns}; spawners do not, so they are untouched.)
 */
public final class SkyIslandSpawns {

    /** Islands' undersides, by column (Integer.MAX_VALUE where there is no island), for the columns last asked about. */
    private final Map<Long, Integer> undersides = new LinkedHashMap<Long, Integer>(512, 0.75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
            return size() > 4096;
        }
    };
    private long seed = Long.MIN_VALUE;

    @SubscribeEvent
    public void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        if (event.world == null || event.world.provider.dimensionId != 0) return;
        SkyIslands field = DungeonBeacons.field(event.world);
        if (forbids(field, event.x, event.y, event.z)) event.setCanceled(true);
    }

    /** Whether a creature may not spawn of itself at (x, y, z): on or in a sky island. */
    synchronized boolean forbids(SkyIslands field, int x, int y, int z) {
        if (field.seed() != seed) {
            undersides.clear();
            seed = field.seed();
        }
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        Integer bottom = undersides.get(key);
        if (bottom == null) {
            bottom = underside(field, x, z);
            undersides.put(key, bottom);
        }
        return y >= bottom - 2;
    }

    /** The lowest underside of any island over the column (x, z), or Integer.MAX_VALUE if none is. */
    static int underside(SkyIslands field, int x, int z) {
        int bottom = Integer.MAX_VALUE;
        for (SkyIslands.Island island : field.near(x, z, x, z)) {
            int[] column = field.column(island, x, z);
            if (column != null) bottom = Math.min(bottom, column[0]);
        }
        return bottom;
    }
}
