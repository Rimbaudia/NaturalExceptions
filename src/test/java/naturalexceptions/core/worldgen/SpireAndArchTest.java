package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.HermitLookout.Part;

/**
 * The spires carry a Hermit's lookout that can be climbed into and lived in; some arches keep a Gale Core in their
 * keystone, and every arch is a Wind Gate underneath its span.
 */
final class SpireAndArchTest {

    private static List<Landmarks.Landmark> all(Landmarks field, Landmarks.Kind kind) {
        List<Landmarks.Landmark> found = new ArrayList<>();
        for (Landmarks.Landmark landmark : field.near(-30000, -30000, 30000, 30000))
            if (landmark.kind() == kind) found.add(landmark);
        return found;
    }

    @Test
    void theLookoutStandsOnTheTallestPillarWideEnoughToBuildOn() {
        Landmarks field = new Landmarks(1234L);
        int carried = 0;
        List<Landmarks.Landmark> spires = all(field, Landmarks.Kind.SPIRES);
        assertFalse(spires.isEmpty());
        for (Landmarks.Landmark cluster : spires) {
            Landmarks.Pillar chosen = HermitLookout.pillar(cluster);
            if (chosen == null) {
                for (Landmarks.Pillar pillar : cluster.pillars())
                    assertTrue(pillar.radius < HermitLookout.MIN_PILLAR_RADIUS, "none was wide enough");
                continue;
            }
            carried++;
            assertTrue(chosen.radius >= HermitLookout.MIN_PILLAR_RADIUS);
            for (Landmarks.Pillar pillar : cluster.pillars())
                if (pillar.radius >= HermitLookout.MIN_PILLAR_RADIUS) assertTrue(pillar.topY <= chosen.topY);
            assertEquals(chosen.topY + 1, HermitLookout.floorY(chosen));
            assertTrue(HermitLookout.floorY(chosen) + HermitLookout.ROOF_HEIGHT < 256);
        }
        assertTrue(carried > spires.size() / 2, "most clusters carry one: " + carried + " of " + spires.size());
        assertNull(HermitLookout.pillar(all(field, Landmarks.Kind.ARCH).get(0)), "an arch has no lookout");
    }

    @Test
    void theLookoutCanBeClimbedIntoSleptInAndSearched() {
        for (double column : new double[] { 0.0, 0.2, 0.5, 0.99 }) {
            for (double block : new double[] { 0.0, 0.3, 0.6, 0.99 }) {
                assertEquals(Part.HATCH, HermitLookout.partAt(0, 0, 0, column, block), "the hatch over the shaft");
                assertEquals(Part.NONE, HermitLookout.partAt(0, -1, 0, column, block), "the shaft is open below it");
                assertEquals(Part.BED_FOOT, HermitLookout.partAt(1, 1, 1, column, block));
                assertEquals(Part.BED_HEAD, HermitLookout.partAt(1, 1, 2, column, block), "head south of the foot");
                assertEquals(Part.CHEST, HermitLookout.partAt(-1, 1, 1, column, block));
                for (int dy = 1; dy < HermitLookout.ROOF_HEIGHT; dy++)
                    for (int[] corner : new int[][] { { 2, 2 }, { -2, 2 }, { 2, -2 }, { -2, -2 } })
                        assertEquals(Part.POST, HermitLookout.partAt(corner[0], dy, corner[1], column, block));
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        Part floor = HermitLookout.partAt(dx, 0, dz, column, block);
                        Part above = HermitLookout.partAt(dx, 1, dz, column, block);
                        boolean solidFloor = floor == Part.FLOOR || floor == Part.FLOOR_MOSSY
                            || floor == Part.FLOOR_CRACKED
                            || floor == Part.HATCH;
                        if (Math.sqrt(dx * dx + dz * dz) <= HermitLookout.INNER_RADIUS)
                            assertTrue(solidFloor, "the lived-in floor is whole at " + dx + "," + dz);
                        if (above == Part.PARAPET || above == Part.POST
                            || above == Part.BED_FOOT
                            || above == Part.CHEST) assertTrue(solidFloor, "nothing stands on a broken floor");
                    }
                }
            }
        }
    }

    @Test
    void aThirdOrSoOfTheArchesKeepAGaleCoreInTheirKeystone() {
        Landmarks field = new Landmarks(98765L);
        List<Landmarks.Landmark> arches = all(field, Landmarks.Kind.ARCH);
        assertTrue(arches.size() > 40, "enough arches to count: " + arches.size());
        int keystones = 0;
        for (Landmarks.Landmark arch : arches) if (field.hasGaleKeystone(arch)) keystones++;
        double share = keystones / (double) arches.size();
        assertTrue(share > 0.2 && share < 0.5, "share " + share);
    }

    @Test
    void theWindGateIsUnderTheSpanNotOnItNorByTheLegs() {
        Landmarks field = new Landmarks(98765L);
        int checked = 0;
        for (Landmarks.Landmark arch : all(field, Landmarks.Kind.ARCH)) {
            int[] middle = Landmarks.archMiddle(arch);
            double x = middle[0] + 0.5, z = middle[1] + 0.5;
            int keystone = -1;
            for (int y = 0; y < 256 && keystone < 0; y++)
                if (Landmarks.archAt(arch, middle[0], y, middle[1], 0)) keystone = y;
            assertTrue(keystone > 0, "the span passes over its middle");
            assertNotNull(Landmarks.archMiddle(arch));
            assertTrue(Landmarks.underSpan(arch, x, keystone - 3, z), "just under the keystone");
            assertFalse(Landmarks.underSpan(arch, x, keystone + 1, z), "in the rock is not under it");
            assertFalse(Landmarks.underSpan(arch, x, keystone - 60, z), "nor far below");
            assertFalse(Landmarks.underSpan(arch, arch.x() + 500, keystone - 3, arch.z() + 500), "nor elsewhere");
            checked++;
        }
        assertTrue(checked > 40);
    }
}
