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
        List<SkyIslands.Island> islands = islandsIn(field, 11); // about 2800 x 2800 blocks
        assertTrue(islands.size() >= 35 && islands.size() <= 75, "a little under half the cells: " + islands.size());
        for (SkyIslands.Island island : islands) {
            assertTrue(island.centreY() >= SkyIslands.MIN_CENTRE_Y && island.centreY() <= SkyIslands.MAX_CENTRE_Y);
            assertTrue(island.radius() >= SkyIslands.MIN_RADIUS && island.radius() <= SkyIslands.MAX_RADIUS);
            int[] middle = field.column(island, island.centreX(), island.centreZ());
            assertNotNull(middle, "solid in the middle");
            assertTrue(middle[0] >= 100, "never reaches down into the hills: " + middle[0]);
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

    private static int count(SkyIslands field, SkyIslands.Tier tier, int blocks) {
        int n = 0, cells = blocks / tier.cell();
        for (long cx = 0; cx < cells; cx++)
            for (long cz = 0; cz < cells; cz++) if (field.atCell(tier, cx, cz) != null) n++;
        return n;
    }

    @Test
    void mostlySmallSomeMediumAndGrandDungeonIslandsAreFew() {
        SkyIslands field = new SkyIslands(SEED);
        int span = 6144; // a 6 x 6 km square
        int small = count(field, SkyIslands.Tier.SMALL, span);
        int medium = count(field, SkyIslands.Tier.MEDIUM, span);
        int grand = count(field, SkyIslands.Tier.GRAND, span);
        assertTrue(small > 6 * medium, "small islands are the common ones: " + small + " vs " + medium);
        assertTrue(medium > 5 * grand, "medium are much commoner than grand: " + medium + " vs " + grand);
        assertTrue(grand >= 3 && grand <= 14, "a few dungeon islands: " + grand);
        for (long cx = 0; cx < 4; cx++) for (long cz = 0; cz < 4; cz++) {
            SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
            if (island == null) continue;
            assertTrue(island.hasDungeon() && !island.hasCache(), "a grand island has a dungeon, not a heap cache");
            assertNotNull(field.dungeonKind(island));
        }
    }

    @Test
    void cachesAreRareOnSmallIslandsAndNoneHasAShrine() {
        SkyIslands field = new SkyIslands(SEED);
        int small = 0, smallCaches = 0, medium = 0, mediumCaches = 0;
        for (long cx = 0; cx < 48; cx++) for (long cz = 0; cz < 48; cz++) {
            SkyIslands.Island island = field.atCell(SkyIslands.Tier.SMALL, cx, cz);
            if (island != null) {
                small++;
                if (island.hasCache()) smallCaches++;
                assertNull(field.dungeonKind(island));
            }
            if (cx < 16 && cz < 16) {
                island = field.atCell(SkyIslands.Tier.MEDIUM, cx, cz);
                if (island != null) {
                    medium++;
                    if (island.hasCache()) mediumCaches++;
                    assertNull(field.dungeonKind(island));
                }
            }
        }
        assertTrue(smallCaches < small * 0.1, "one small island in twenty: " + smallCaches + " / " + small);
        assertTrue(mediumCaches > medium * 0.15 && mediumCaches < medium * 0.45, mediumCaches + " / " + medium);
    }

    @Test
    void everyTierIsFoundByNear() {
        SkyIslands field = new SkyIslands(SEED);
        for (SkyIslands.Tier tier : SkyIslands.Tier.values()) {
            SkyIslands.Island island = null;
            for (long c = 0; island == null && c < 40; c++) island = field.atCell(tier, c, c);
            assertNotNull(island, tier.name());
            boolean seen = false;
            int x = island.centreX(), z = island.centreZ();
            for (SkyIslands.Island near : field.near(x - 8, z - 8, x + 8, z + 8))
                seen |= near.tier() == tier && near.cellX() == island.cellX() && near.cellZ() == island.cellZ();
            assertTrue(seen, tier + " island is seen by the chunk over its middle");
        }
    }

    @Test
    void stormglassHangsUnderTheBigIslandsOnly() {
        SkyIslands field = new SkyIslands(SEED);
        int columns = 0, hanging = 0, veins = 0, rock = 0;
        for (SkyIslands.Island island : islandsIn(field, 11)) {
            int r = (int) island.radius();
            for (int x = island.centreX() - r; x <= island.centreX() + r; x += 2) {
                for (int z = island.centreZ() - r; z <= island.centreZ() + r; z += 2) {
                    int[] column = field.column(island, x, z);
                    if (column == null) continue;
                    int length = field.stormglassHang(island, x, z);
                    assertTrue(length >= 0 && length <= 3, "a point is one to three blocks long");
                    if (island.tier() == SkyIslands.Tier.SMALL) {
                        assertEquals(0, length, "small islands grow none");
                        continue;
                    }
                    columns++;
                    if (length > 0) hanging++;
                    for (int y = column[0]; y <= column[1]; y++) {
                        rock++;
                        if (field.stormglassVein(x, y, z, column[0])) {
                            veins++;
                            assertTrue(y < column[0] + SkyIslands.STORMGLASS_DEPTH, "veins lie near the underside");
                        }
                    }
                }
            }
        }
        double share = hanging / (double) columns;
        assertTrue(share > 0.02 && share < 0.06, "about one column in twenty-five: " + share);
        assertTrue(veins > 0 && veins < rock / 20, "veins turn up but are rare: " + veins + " of " + rock);
    }
}
