package naturalexceptions.core.worldgen.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The citadel's buildings read cleanly, can be entered from the front, and turn their stairs with them. */
final class BuildingTemplateTest {

    @Test
    void everyBuildingReadsAndIsABuilding() {
        assertTrue(
            TemplateLibrary.buildings()
                .size() >= 10,
            "a town's worth of buildings");
        for (RoomTemplate t : TemplateLibrary.buildings()) {
            assertTrue(t.building, t + " is drawn as a building");
            assertTrue(t.kinds.contains(RoomTemplate.Kind.CITADEL), t + " belongs to citadels");
        }
        RoomTemplate keep = TemplateLibrary.building("grand_keep");
        assertEquals(25, keep.width());
        assertEquals(25, keep.depth());
        // The catacomb's shaft comes up at (8, 8) of the keep: it is left open.
        assertEquals('.', keep.at(8, 1, 8, 0));
        assertEquals('#', keep.at(8, 0, 8, 0));
    }

    @Test
    void everyBuildingOpensAtTheFront() {
        for (RoomTemplate t : TemplateLibrary.buildings()) {
            int front = t.depth() - 1;
            boolean door = false;
            for (int x = 1; x < t.width() - 1; x++) {
                for (int z : new int[] { front, front - 1 }) {
                    char low = t.at(x, 1, z, 0), high = t.at(x, 2, z, 0);
                    boolean opening = low == 'D' && high == 'D' || low == '.' && high == '.';
                    if (!opening || z != front && t.at(x, 1, front, 0) != '.') continue;
                    char inside = t.at(x, 1, z - 1, 0);
                    door |= inside == '.' || inside == 'D';
                }
            }
            assertTrue(door, t + " can be walked into from the front");
        }
    }

    @Test
    void stairsTurnWithTheBuilding() {
        // '1' rises to the north: vanilla meta 3; a quarter turn clockwise makes it rise to the east (meta 0).
        assertEquals(3, naturalexceptions.core.worldgen.RoomWriterAccess.stairs('1', 0));
        assertEquals(0, naturalexceptions.core.worldgen.RoomWriterAccess.stairs('1', 1));
        assertEquals(2, naturalexceptions.core.worldgen.RoomWriterAccess.stairs('1', 2));
        assertEquals(1, naturalexceptions.core.worldgen.RoomWriterAccess.stairs('1', 3));
        assertEquals(0 | 4, naturalexceptions.core.worldgen.RoomWriterAccess.stairs('6', 0), "upside down");
    }
}
