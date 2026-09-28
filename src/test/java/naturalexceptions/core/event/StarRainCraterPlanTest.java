package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class StarRainCraterPlanTest {

    @Test
    void sizesScaleWithSeverity() {
        int[] across = { 3, 5, 7 };
        int[] crust = { 18, 56, 116 };
        int[] core = { 1, 1, 7 };
        for (int severity = 1; severity <= 3; severity++) {
            StarRainCraterPlan plan = StarRainCraterPlan.of(severity, 1234L);
            assertEquals(2 + 2 * severity, plan.radius());
            List<StarRainCraterPlan.Cell> ball = plan.cellsWithRole(StarRainCraterPlan.Role.CRUST);
            ball.addAll(plan.cellsWithRole(StarRainCraterPlan.Role.CORE));
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            for (StarRainCraterPlan.Cell cell : ball) {
                minX = Math.min(minX, cell.dx());
                maxX = Math.max(maxX, cell.dx());
            }
            assertEquals(across[severity - 1], maxX - minX + 1, "ball width at severity " + severity);
            assertEquals(
                crust[severity - 1],
                plan.cellsWithRole(StarRainCraterPlan.Role.CRUST)
                    .size());
            assertEquals(
                core[severity - 1],
                plan.cellsWithRole(StarRainCraterPlan.Role.CORE)
                    .size());
        }
    }

    @Test
    void planIsDeterministicAndCellsNeverOverlap() {
        StarRainCraterPlan a = StarRainCraterPlan.of(3, 42L);
        StarRainCraterPlan b = StarRainCraterPlan.of(3, 42L);
        assertEquals(a.cells(), b.cells());
        Set<String> seen = new HashSet<>();
        for (StarRainCraterPlan.Cell cell : a.cells()) {
            assertTrue(seen.add(cell.dx() + "," + cell.dy() + "," + cell.dz()), "one role per cell");
        }
    }

    @Test
    void ballSitsInTheBowlAndEverythingStaysWithinReach() {
        for (int severity = 1; severity <= 3; severity++) {
            StarRainCraterPlan plan = StarRainCraterPlan.of(severity, 7L * severity);
            for (StarRainCraterPlan.Cell cell : plan.cells()) {
                assertTrue(
                    Math.abs(cell.dx()) <= plan.horizontalReach() && Math.abs(cell.dz()) <= plan.horizontalReach());
                if (cell.role() == StarRainCraterPlan.Role.CORE || cell.role() == StarRainCraterPlan.Role.CRUST) {
                    assertTrue(cell.dy() < 0, "the ball lies below the old surface, inside the crater");
                }
            }
            for (StarRainCraterPlan.RimColumn rim : plan.rim()) {
                double r = Math.sqrt(rim.dx() * rim.dx() + rim.dz() * rim.dz());
                assertTrue(r > plan.radius() - 1.0 && r <= plan.horizontalReach());
            }
            assertFalse(
                plan.rim()
                    .isEmpty());
        }
    }

    @Test
    void coreMassSplitsExactly() {
        long total = StarRainImpactFactory.coreMassMilligrams(3);
        long[] shares = StarRainCraterPlan.splitMilligrams(total, 7);
        long sum = 0;
        for (long share : shares) sum += share;
        assertEquals(total, sum);
        assertEquals(8_000_000L, total);
        assertEquals(2_000_000L, StarRainImpactFactory.coreMassMilligrams(1));
    }
}
