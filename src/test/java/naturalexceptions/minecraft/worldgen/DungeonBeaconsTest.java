package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;

/** The Sky Key's pull: every dungeon's gate and key chest can be found again, and the way is told plainly. */
final class DungeonBeaconsTest {

    @Test
    void everyDungeonHasAGateAndAKeyChestToPointAt() {
        int dungeons = 0;
        for (long s = 1; s <= 4; s++) {
            SkyIslands field = new SkyIslands(s * 7919L);
            for (long cx = 0; cx < 3; cx++) for (long cz = 0; cz < 3; cz++) {
                SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
                if (island == null) continue;
                SkyDungeon dungeon = SkyDungeon.of(field, island);
                DungeonBeacons.Beacon beacon = DungeonBeacons.locate(dungeon);
                assertNotNull(beacon, dungeon.kind() + " at " + island.centreX() + "," + island.centreZ());
                assertTrue(beacon.chest[1] != beacon.gate[1] || beacon.chest[0] != beacon.gate[0], "two places");
                dungeons++;
            }
        }
        assertTrue(dungeons >= 5, "enough dungeons: " + dungeons);
    }

    @Test
    void theWayIsToldInCompassPointsAndLevels() {
        assertEquals("north", DungeonBeacons.compass(0, -10));
        assertEquals("east", DungeonBeacons.compass(10, 0));
        assertEquals("south-west", DungeonBeacons.compass(-10, 10));
        assertEquals(
            "north-east, 32 blocks, two levels down",
            DungeonBeacons.describe(0.5, 100, 0.5, new int[] { 15, 76, -15 }));
        assertTrue(
            DungeonBeacons.describe(0.5, 100, 0.5, new int[] { 0, 100, 0 })
                .startsWith("right here"));
        assertTrue(
            DungeonBeacons.describe(0.5, 100, 0.5, new int[] { 20, 101, 0 })
                .endsWith("on this level"));
        assertTrue(
            DungeonBeacons.describe(0.5, 100, 0.5, new int[] { 20, 112, 0 })
                .endsWith("a level up"));
    }
}
