package naturalexceptions.core.worldgen.dungeon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The authored rooms read cleanly, keep their doorways open, and say what they hold. */
final class RoomTemplateTest {

    @Test
    void everyRoomReadsAndThereAreEnough() {
        assertTrue(
            TemplateLibrary.all()
                .size() >= 20,
            "a real library of rooms");
        Set<RoomTemplate.Role> covered = EnumSet.noneOf(RoomTemplate.Role.class);
        for (RoomTemplate t : TemplateLibrary.all()) covered.addAll(t.roles);
        assertEquals(EnumSet.allOf(RoomTemplate.Role.class), covered, "every role has rooms");
    }

    @Test
    void turningKeepsEveryBlockAndSwapsTheFootprint() {
        RoomTemplate hall = TemplateLibrary.named("great_hall");
        assertEquals(28, hall.width(0));
        assertEquals(9, hall.depth(0));
        assertEquals(9, hall.width(1));
        assertEquals(28, hall.depth(1));
        for (int r = 0; r < 4; r++) {
            int count = 0;
            for (int x = 0; x < hall.width(r); x++)
                for (int z = 0; z < hall.depth(r); z++) if (hall.at(x, 1, z, r) == '0') count++;
            assertEquals(14, count, "the same pillars whichever way it faces");
        }
        RoomTemplate library = TemplateLibrary.named("library");
        // Turned a quarter clockwise, the north-west corner goes to the north-east.
        assertEquals(library.at(0, 1, 0, 0), library.at(library.width(1) - 1, 1, 0, 1));
    }

    @Test
    void doorLanesStayOpenAndRolesHaveWhatTheyNeed() {
        for (RoomTemplate t : TemplateLibrary.all()) {
            int w = t.width(), d = t.depth();
            int[] mids = t.small ? new int[] { 2 } : new int[t.cellsX];
            if (!t.small) for (int k = 0; k < t.cellsX; k++) mids[k] = RoomTemplate.CELL * k + 4;
            for (int m : mids) for (int o = -1; o <= 1; o++) for (int y = 1; y <= 3; y++) {
                assertTrue(open(t.at(m + o, y, 0, 0)), t + ": north doorway blocked");
                assertTrue(open(t.at(m + o, y, d - 1, 0)), t + ": south doorway blocked");
            }
            int[] midsZ = t.small ? new int[] { 2 } : new int[t.cellsZ];
            if (!t.small) for (int k = 0; k < t.cellsZ; k++) midsZ[k] = RoomTemplate.CELL * k + 4;
            for (int m : midsZ) for (int o = -1; o <= 1; o++) for (int y = 1; y <= 3; y++) {
                assertTrue(open(t.at(0, y, m + o, 0)), t + ": west doorway blocked");
                assertTrue(open(t.at(w - 1, y, m + o, 0)), t + ": east doorway blocked");
            }
            if (t.roles.contains(RoomTemplate.Role.KEY)) assertTrue(t.has('c'), t + ": a key room has a chest");
            if (t.roles.contains(RoomTemplate.Role.VAULT))
                assertTrue(t.has('c') && t.has('C'), t + ": treasure and riches");
            if (t.roles.contains(RoomTemplate.Role.WARDEN)) assertTrue(t.has('W'), t + ": the Warden's seal");
            boolean devices = t.has('^') || t.has('v') || t.has('F') || t.has('%') || t.has('h') || t.has('T');
            assertEquals(t.trapped, devices, t + ": 'trapped' says whether it holds traps");
            if (t.has('^'))
                assertTrue(t.has('v') || t.has('F') || t.has('a') || t.has('T'), t + ": its triggers fire something");
        }
    }

    private static boolean open(char c) {
        return c == '.' || c == 't' || c == 'f' || c == 'w' || c == 'H' || c == 'q';
    }

    @Test
    void badFilesSayWhatIsWrong() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> RoomTemplate.parse("bad", Arrays.asList("name: bad", "roles: hall", "---", "layer 1:", "....")));
        assertTrue(
            e.getMessage()
                .contains("wide"),
            e.getMessage());
    }
}
