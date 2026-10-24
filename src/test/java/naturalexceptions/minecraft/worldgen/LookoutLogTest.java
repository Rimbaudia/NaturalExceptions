package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.SkyIslands;

/** The Lookout's Log gives the bearings of the nearest sky-halls, nearest first, and no more than three. */
final class LookoutLogTest {

    @Test
    void theLogNamesTheNearestHallsFirst() {
        SkyIslands islands = new SkyIslands(5150L);
        List<String> bearings = LookoutLog.bearings(islands, 0, 0);
        assertFalse(bearings.isEmpty(), "a hall within sight of the origin");
        assertTrue(bearings.size() <= LookoutLog.MAX_BEARINGS);
        long last = -1;
        for (String bearing : bearings) {
            assertTrue(bearing.startsWith("A Citadel: ") || bearing.startsWith("A Sanctum: "), bearing);
            assertTrue(bearing.endsWith("blocks, high above."), bearing);
            long distance = Long.parseLong(bearing.replaceAll(".*about (\\d+) blocks.*", "$1"));
            assertTrue(distance >= last && distance <= LookoutLog.SIGHT + 50, bearing);
            last = distance;
        }
    }
}
