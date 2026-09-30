package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;

/** {@code /ne_landmarks tp citadel} and {@code tp sanctum} find the nearest dungeon of that kind. */
final class CommandLandmarksTest {

    private static double distance(SkyIslands.Island island, int px, int pz) {
        return Math.hypot(island.centreX() - px, island.centreZ() - pz);
    }

    @Test
    void eachKindFindsItsOwnNearestFirst() {
        SkyIslands islands = new SkyIslands(424242L);
        int px = 3000, pz = -2000;
        List<SkyIslands.Island> all = CommandLandmarks
            .nearestDungeons(islands, px, pz, null, CommandLandmarks.WIDE_SEARCH);
        assertTrue(!all.isEmpty(), "dungeons within reach");
        for (SkyDungeon.Kind kind : SkyDungeon.Kind.values()) {
            List<SkyIslands.Island> found = CommandLandmarks
                .nearestDungeons(islands, px, pz, kind, CommandLandmarks.WIDE_SEARCH);
            assertTrue(!found.isEmpty(), "a " + kind + " within reach");
            for (SkyIslands.Island island : found) assertEquals(kind, islands.dungeonKind(island));
            for (int i = 1; i < found.size(); i++)
                assertTrue(distance(found.get(i - 1), px, pz) <= distance(found.get(i), px, pz), "nearest first");
            SkyIslands.Island nearestOfAll = null;
            for (SkyIslands.Island island : all) if (islands.dungeonKind(island) == kind) {
                nearestOfAll = island;
                break;
            }
            assertEquals(
                nearestOfAll.centreX(),
                found.get(0)
                    .centreX(),
                "the nearest of its kind");
        }
    }
}
