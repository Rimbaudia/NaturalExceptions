package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Rising air stands just clear of each island it serves, from low down to over the top, and the same each time. */
final class UpdraftsTest {

    @Test
    void columnsStandClearOfTheirIslandsAndTopOutOverThem() {
        SkyIslands field = new SkyIslands(20261026L);
        Updrafts drafts = new Updrafts(field);
        int islands = 0, with = 0, grand = 0;
        for (SkyIslands.Island island : field.near(-6000, -6000, 6000, 6000)) {
            islands++;
            Updrafts.Column column = drafts.of(island);
            if (island.tier() == SkyIslands.Tier.GRAND) {
                grand++;
                assertNotNull(column, "every great island has its updraft");
            }
            if (column == null) continue;
            with++;
            assertSame(column, drafts.of(island), "worked out once, and the same after");
            assertTrue(column.top > island.centreY() + 1, "it tops out over the island");
            // Its whole width clear of the island's rock and cloud.
            for (double a = 0; a < Math.PI * 2; a += Math.PI / 4) {
                int x = (int) Math.floor(column.x + Math.cos(a) * Updrafts.RADIUS);
                int z = (int) Math.floor(column.z + Math.sin(a) * Updrafts.RADIUS);
                assertNull(field.column(island, x, z), "the column runs into the island's rock");
                assertNull(
                    field.cloud(island, x, z),
                    "the column runs into the island's cloud: reach " + field.reach(island, x, z) + " radius "
                        + island.radius() + " column off " + Math.hypot(column.x - island.centreX(), column.z - island.centreZ()));
            }
            // And near enough the rock to step off onto it at the top, with no cloud between.
            double dx = column.x - island.centreX(), dz = column.z - island.centreZ(), off = Math.sqrt(dx * dx + dz * dz);
            int land = -1;
            for (double t = 1.0; t <= 12.0; t += 0.5) {
                int x = (int) Math.floor(column.x - dx / off * t), z = (int) Math.floor(column.z - dz / off * t);
                if (field.column(island, x, z) != null) {
                    land = (int) t;
                    break;
                }
                assertNull(field.cloud(island, x, z), "cloud between the column and the island's rock");
            }
            assertTrue(land > 0 && land <= 8, "the island's rock is a few steps from the column's top: " + land);
            assertTrue(column.holds(column.x, island.centreY(), column.z));
            assertTrue(!column.holds(column.x, column.top + 1, column.z));
            assertTrue(!column.holds(column.x + Updrafts.RADIUS + 0.5, island.centreY(), column.z));
        }
        assertTrue(islands > 20 && grand > 0, "enough islands to judge: " + islands + ", great " + grand);
        assertTrue(with > grand && with < islands, "some islands have rising air, not all: " + with + " of " + islands);
    }
}
