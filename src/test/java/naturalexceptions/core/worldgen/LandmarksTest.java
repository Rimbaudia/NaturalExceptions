package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

final class LandmarksTest {

    private static final long SEED = 987654321L;

    private static Landmarks.Landmark first(Landmarks field, Landmarks.Kind kind) {
        for (long cx = 0; cx < 40; cx++) for (long cz = 0; cz < 40; cz++) {
            Landmarks.Landmark landmark = field.atCell(cx, cz);
            if (landmark != null && landmark.kind() == kind) return landmark;
        }
        throw new AssertionError("no " + kind);
    }

    @Test
    void rareAndVaried() {
        Landmarks field = new Landmarks(SEED);
        Map<Landmarks.Kind, Integer> kinds = new EnumMap<>(Landmarks.Kind.class);
        int total = 0;
        for (long cx = 0; cx < 8; cx++) for (long cz = 0; cz < 8; cz++) { // 4096 x 4096 blocks
            Landmarks.Landmark landmark = field.atCell(cx, cz);
            if (landmark == null) continue;
            total++;
            kinds.merge(landmark.kind(), 1, Integer::sum);
        }
        assertTrue(total >= 20 && total <= 44, "about half the cells: " + total);
        assertEquals(Landmarks.Kind.values().length, kinds.size(), "every kind shows up: " + kinds);
    }

    @Test
    void chasmNarrowsToNothingAtItsEndsAndHasALavaFloorDepth() {
        Landmarks field = new Landmarks(SEED);
        Landmarks.Landmark chasm = first(field, Landmarks.Kind.CHASM);
        double[] mid = chasm.path()[chasm.path().length / 2];
        double[] middle = field.chasm(chasm, (int) Math.floor(mid[0]), (int) Math.floor(mid[1]));
        assertNotNull(middle, "open in the middle");
        assertTrue(middle[0] > 2.0, "wide in the middle");
        double[][] path = chasm.path();
        double[] end = path[0];
        double[] atEnd = field.chasm(chasm, (int) Math.floor(end[0]), (int) Math.floor(end[1]));
        assertTrue(atEnd == null || atEnd[0] < 1.0, "pinches out at the end");
        assertTrue(Landmarks.chasmOpen(new double[] { 6.0, 0.0 }, Landmarks.CHASM_FLOOR_Y, 70), "open to the floor");
        assertFalse(Landmarks.chasmOpen(new double[] { 6.0, 0.0 }, Landmarks.CHASM_FLOOR_Y - 1, 70), "not below");
        assertFalse(Landmarks.chasmOpen(new double[] { 6.0, 5.0 }, Landmarks.CHASM_FLOOR_Y, 70), "walls lean in");
        assertTrue(Landmarks.chasmOpen(new double[] { 6.0, 5.0 }, 69, 70), "wide at the top");
    }

    @Test
    void spiresRiseHighAndTaper() {
        Landmarks field = new Landmarks(SEED);
        Landmarks.Landmark spires = first(field, Landmarks.Kind.SPIRES);
        Landmarks.Pillar pillar = spires.pillars()
            .get(0);
        assertTrue(pillar.topY >= 100, "tall: " + pillar.topY);
        assertTrue(field.spireAt(spires, pillar.x, 70, pillar.z, 64), "solid near its foot");
        assertFalse(field.spireAt(spires, pillar.x, pillar.topY + 1, pillar.z, 64), "nothing above the top");
        assertFalse(field.spireAt(spires, pillar.x, 64, pillar.z, 64), "starts above the ground");
    }

    @Test
    void sinkholeDropsIntoAFloodedCavern() {
        Landmarks field = new Landmarks(SEED);
        Landmarks.Landmark hole = first(field, Landmarks.Kind.SINKHOLE);
        assertTrue(field.sinkholeOpen(hole, hole.x(), 60, hole.z(), 70), "shaft open");
        assertTrue(field.sinkholeOpen(hole, hole.x(), Landmarks.CAVERN_FLOOR_Y + 3, hole.z(), 70), "cavern open");
        assertFalse(field.sinkholeOpen(hole, hole.x(), Landmarks.CAVERN_FLOOR_Y - 1, hole.z(), 70), "floor below");
        int wide = hole.x() + (int) (hole.width() * 1.3);
        assertFalse(field.sinkholeOpen(hole, wide, 50, hole.z(), 70), "shaft has walls");
        assertTrue(
            field.sinkholeOpen(hole, wide, Landmarks.CAVERN_FLOOR_Y + 2, hole.z(), 70),
            "cavern is wider than the shaft");
    }

    @Test
    void archSpansAboveTheGround() {
        Landmarks field = new Landmarks(SEED);
        Landmarks.Landmark arch = first(field, Landmarks.Kind.ARCH);
        int crown = Landmarks.ARCH_BASE_Y + (int) Math.round(arch.height());
        assertTrue(Landmarks.archAt(arch, arch.x(), crown, arch.z(), 64), "rock at the crown");
        assertFalse(Landmarks.archAt(arch, arch.x(), 66, arch.z(), 64), "open beneath the middle");
    }

    @Test
    void nearFindsEveryLandmarkThatReachesAChunk() {
        Landmarks field = new Landmarks(SEED);
        Landmarks.Landmark chasm = first(field, Landmarks.Kind.CHASM);
        for (double[] point : chasm.path()) {
            int x = (int) Math.floor(point[0]), z = (int) Math.floor(point[1]);
            int cx = Math.floorDiv(x, 16) * 16, cz = Math.floorDiv(z, 16) * 16;
            boolean seen = false;
            for (Landmarks.Landmark near : field.near(cx, cz, cx + 15, cz + 15))
                seen |= near.cellX() == chasm.cellX() && near.cellZ() == chasm.cellZ();
            assertTrue(seen, "chunk " + cx + "," + cz + " on the chasm's path must see it");
        }
    }
}
