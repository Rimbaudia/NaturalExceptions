package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

final class SkyIslandsTest {

    private static final long SEED = 1234567L;

    private static List<SkyIslands.Island> islandsIn(SkyIslands field, int cells) {
        List<SkyIslands.Island> found = new ArrayList<>();
        for (long cx = 0; cx < cells; cx++) for (long cz = 0; cz < cells; cz++) {
            SkyIslands.Island island = field.atCell(cx, cz);
            if (island != null) found.add(island);
        }
        return found;
    }

    @Test
    void rareButThereAndHighInTheSky() {
        SkyIslands field = new SkyIslands(SEED);
        List<SkyIslands.Island> islands = islandsIn(field, 11); // about 4200 x 4200 blocks
        assertTrue(islands.size() >= 20 && islands.size() <= 65, "about a third of cells: " + islands.size());
        for (SkyIslands.Island island : islands) {
            assertTrue(island.centreY() >= SkyIslands.MIN_CENTRE_Y && island.centreY() <= SkyIslands.MAX_CENTRE_Y);
            assertTrue(island.radius() >= SkyIslands.MIN_RADIUS && island.radius() <= SkyIslands.MAX_RADIUS);
            int[] middle = field.column(island, island.centreX(), island.centreZ());
            assertNotNull(middle, "solid in the middle");
            assertTrue(middle[0] >= 110, "never reaches down to the land: " + middle[0]);
            assertTrue(middle[1] - middle[0] >= 5, "a real thickness");
        }
    }

    @Test
    void sameSeedSameIslandsDifferentSeedDifferent() {
        SkyIslands a = new SkyIslands(SEED);
        SkyIslands b = new SkyIslands(SEED);
        SkyIslands c = new SkyIslands(SEED + 1);
        int differences = 0;
        for (long cx = 0; cx < 8; cx++) for (long cz = 0; cz < 8; cz++) {
            SkyIslands.Island x = a.atCell(cx, cz), y = b.atCell(cx, cz), z = c.atCell(cx, cz);
            assertEquals(x == null, y == null);
            if (x != null) assertEquals(x.centreX(), y.centreX());
            if ((x == null) != (z == null) || x != null && z != null && x.centreX() != z.centreX()) differences++;
        }
        assertTrue(differences > 0);
    }

    @Test
    void everyChunkSeesTheIslandsThatReachIt() {
        SkyIslands field = new SkyIslands(SEED);
        SkyIslands.Island island = islandsIn(field, 6).get(0);
        int reach = (int) Math.ceil(island.radius() * 1.3);
        for (int x = island.centreX() - reach; x <= island.centreX() + reach; x += 3) {
            for (int z = island.centreZ() - reach; z <= island.centreZ() + reach; z += 3) {
                int[] direct = field.column(island, x, z);
                if (direct == null) continue;
                int chunkX = Math.floorDiv(x, 16) * 16, chunkZ = Math.floorDiv(z, 16) * 16;
                boolean seen = false;
                for (SkyIslands.Island near : field.near(chunkX, chunkZ, chunkX + 15, chunkZ + 15)) {
                    if (near.cellX() == island.cellX() && near.cellZ() == island.cellZ()) {
                        assertArrayEquals(direct, field.column(near, x, z), "same shape from any chunk");
                        seen = true;
                    }
                }
                assertTrue(seen, "chunk at " + chunkX + "," + chunkZ + " misses an island over it");
            }
        }
        assertNull(
            field.column(island, island.centreX() + (int) (island.radius() * 1.3) + 2, island.centreZ()),
            "nothing past the edge");
    }

    @Test
    void pondTreeAndCacheSitOnTheIsland() {
        SkyIslands field = new SkyIslands(SEED);
        for (SkyIslands.Island island : islandsIn(field, 11)) {
            int[] tree = field.treeSpot(island);
            assertNotNull(field.column(island, tree[0], tree[1]), "tree on land");
            if (island.hasPond()) {
                assertTrue(field.pondAt(island, island.centreX(), island.centreZ()), "pond in the middle");
                assertTrue(!field.pondAt(island, tree[0], tree[1]), "tree not in the pond");
            }
            if (island.hasCache()) {
                int[] cache = field.cacheSpot(island);
                assertNotNull(field.column(island, cache[0], cache[1]), "cache on land");
            }
        }
    }
}
