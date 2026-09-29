package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** What sky islands are like on top: kinds, plants, clouds, the waterfall and the shrine. */
final class SkyIslandLifeTest {

    private static final long SEED = 2024L;

    private static List<SkyIslands.Island> islands(SkyIslands field, int cells) {
        List<SkyIslands.Island> found = new ArrayList<>();
        for (long cx = 0; cx < cells; cx++) for (long cz = 0; cz < cells; cz++) {
            SkyIslands.Island island = field.atCell(cx, cz);
            if (island != null) found.add(island);
        }
        return found;
    }

    private static SkyIslands.Island first(SkyIslands field, SkyIslands.Kind kind, boolean withShrine) {
        for (SkyIslands.Island island : islands(field, 30)) {
            if (island.kind() == kind && (!withShrine || field.shrineKind(island) != null)) return island;
        }
        throw new AssertionError("no " + kind);
    }

    @Test
    void everyKindTurnsUp() {
        SkyIslands field = new SkyIslands(SEED);
        Map<SkyIslands.Kind, Integer> kinds = new EnumMap<>(SkyIslands.Kind.class);
        for (SkyIslands.Island island : islands(field, 20)) kinds.merge(island.kind(), 1, Integer::sum);
        assertEquals(SkyIslands.Kind.values().length, kinds.size(), "all kinds: " + kinds);
        for (int count : kinds.values()) assertTrue(count >= 8, "none is rare: " + kinds);
    }

    @Test
    void plantsGrowOnlyOnDryLandAndEachKindLooksItsPart() {
        SkyIslands field = new SkyIslands(SEED);
        for (SkyIslands.Kind kind : SkyIslands.Kind.values()) {
            SkyIslands.Island island = first(field, kind, false);
            Map<SkyIslands.Decor, Integer> decor = new EnumMap<>(SkyIslands.Decor.class);
            int rock = 0, columns = 0;
            int r = (int) (island.radius() * 1.3);
            for (int x = island.centreX() - r; x <= island.centreX() + r; x++) {
                for (int z = island.centreZ() - r; z <= island.centreZ() + r; z++) {
                    SkyIslands.Decor d = field.decor(island, x, z);
                    if (d != SkyIslands.Decor.NONE) {
                        assertNotNull(field.column(island, x, z), "no plant in the air");
                        assertTrue(!field.pondAt(island, x, z) && !field.waterfallAt(island, x, z), "none in water");
                        assertTrue(!field.shrineCovers(island, x, z), "none inside the shrine");
                        decor.merge(d, 1, Integer::sum);
                    }
                    if (field.column(island, x, z) != null) {
                        columns++;
                        if (field.surface(island, x, z) != SkyIslands.Surface.GRASS) rock++;
                    }
                }
            }
            if (kind == SkyIslands.Kind.CRAG) assertTrue(rock > columns / 2, "a crag is mostly bare rock");
            else {
                assertEquals(0, rock, kind + " is grassy");
                assertTrue(decor.getOrDefault(SkyIslands.Decor.TALL_GRASS, 0) > 10, kind + " has grass: " + decor);
            }
            if (kind == SkyIslands.Kind.MEADOW) {
                int flowers = decor.getOrDefault(SkyIslands.Decor.RED_FLOWER, 0)
                    + decor.getOrDefault(SkyIslands.Decor.YELLOW_FLOWER, 0);
                assertTrue(flowers > 5, "a meadow flowers");
                assertTrue(decor.getOrDefault(SkyIslands.Decor.WINDBLOOM, 0) > 0, "Windbloom grows in meadows");
            }
        }
    }

    @Test
    void groveHasSeveralTreesAndTheyStandOnLand() {
        SkyIslands field = new SkyIslands(SEED);
        SkyIslands.Island grove = first(field, SkyIslands.Kind.GROVE, false);
        List<int[]> trees = field.treeSpots(grove);
        assertTrue(trees.size() >= 2, "a grove is a stand of trees: " + trees.size());
        for (int[] spot : trees) assertNotNull(field.column(grove, spot[0], spot[1]));
        assertTrue(
            field.treeSpots(first(field, SkyIslands.Kind.CRAG, false))
                .isEmpty(),
            "crags are bare");
    }

    @Test
    void lakesSpillOverTheEdgeAsAWaterfall() {
        SkyIslands field = new SkyIslands(SEED);
        SkyIslands.Island lake = first(field, SkyIslands.Kind.LAKE, false);
        assertTrue(field.pondAt(lake, lake.centreX(), lake.centreZ()), "a pond in the middle");
        int[] lip = field.waterfallLip(lake);
        assertNotNull(lip, "the channel reaches out");
        assertTrue(
            field.reach(lake, lip[0], lip[1]) > 0.8,
            "the lip is at the edge: " + field.reach(lake, lip[0], lip[1]));
    }

    @Test
    void cloudsHangAtTheRimAndUnderneath() {
        SkyIslands field = new SkyIslands(SEED);
        SkyIslands.Island island = first(field, SkyIslands.Kind.MEADOW, false);
        int rim = 0, under = 0;
        int r = (int) (island.radius() * 1.4);
        for (int x = island.centreX() - r; x <= island.centreX() + r; x++) {
            for (int z = island.centreZ() - r; z <= island.centreZ() + r; z++) {
                int[] cloud = field.cloud(island, x, z);
                if (cloud == null) continue;
                double reach = field.reach(island, x, z);
                assertTrue(reach >= 0.25 && reach < SkyIslands.CLOUD_RIM_OUTER, "clouds only at the rim or under it");
                if (reach >= SkyIslands.CLOUD_RIM_INNER) rim++;
                else under++;
            }
        }
        assertTrue(rim > 10, "a cloud rim: " + rim);
        assertTrue(under > 0, "puffs underneath");
    }

    @Test
    void shrinesSitOnTheIslandWithTheCacheInside() {
        SkyIslands field = new SkyIslands(SEED);
        int seen = 0;
        for (SkyIslands.Island island : islands(field, 30)) {
            SkyIslands.ShrineKind kind = field.shrineKind(island);
            if (kind == null) continue;
            seen++;
            int[] origin = field.shrineOrigin(island);
            assertNotNull(origin);
            int chests = 0;
            for (SkyIslands.Piece piece : field.shrinePieces(island)) {
                if (piece.block == SkyIslands.Piece.Block.CHEST) chests++;
                if (piece.dy == 0 && piece.block == SkyIslands.Piece.Block.SKYSTONE) assertNotNull(
                    field.column(island, origin[0] + piece.dx, origin[2] + piece.dz),
                    kind + " floor over the edge");
            }
            assertEquals(1, chests, kind + " holds the cache");
            int[] chest = field.shrineChest(island);
            assertTrue(chest[1] > origin[1], "the chest stands in the shrine, above the ground");
        }
        assertTrue(seen >= 10, "shrines are common on big islands: " + seen);
    }
}
