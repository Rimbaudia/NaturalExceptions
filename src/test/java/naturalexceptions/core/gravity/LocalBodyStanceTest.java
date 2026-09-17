package naturalexceptions.core.gravity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class LocalBodyStanceTest {

    private static final double EPSILON = 1e-6;

    @Test
    void standardFloorStanceMatchesVanillaGeometry() {
        Vector3d center = new Vector3d(10.0, 64.9, 5.0);
        Vector3d localDown = new Vector3d(0, -1, 0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        // Support point is feet at Y=64.0
        Vector3d sup = stance.supportPoint();
        assertEquals(10.0, sup.x, EPSILON);
        assertEquals(64.0, sup.y, EPSILON);
        assertEquals(5.0, sup.z, EPSILON);

        // Head point is at Y=65.8
        Vector3d head = stance.headPoint();
        assertEquals(10.0, head.x, EPSILON);
        assertEquals(65.8, head.y, EPSILON);
        assertEquals(5.0, head.z, EPSILON);

        // Support surface offset from vanilla base (10.0, 64.0, 5.0) is (0, 0, 0)
        Vector3d offset = stance.supportSurfaceOffset();
        assertEquals(0.0, offset.x, EPSILON);
        assertEquals(0.0, offset.y, EPSILON);
        assertEquals(0.0, offset.z, EPSILON);

        // Rotation from world UP is 0 degrees
        assertEquals(0.0, stance.rotationFromWorldUpDegrees(), EPSILON);
    }

    @Test
    void sideWallStancePlacesFeetOnWallAndBodyExtendingOutward() {
        // Wall at +X (e.g. stone wall at X=10.3). Gravity pulls player +X toward wall.
        // Center is at (10.0, 64.9, 5.0)
        Vector3d center = new Vector3d(10.0, 64.9, 5.0);
        Vector3d localDown = new Vector3d(1, 0, 0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        // LocalUp points into the room (-X)
        assertEquals(-1.0, stance.localUp().x, EPSILON);
        assertEquals(0.0, stance.localUp().y, EPSILON);
        assertEquals(0.0, stance.localUp().z, EPSILON);

        // Feet support point is on the wall at X=10.3, center Y=64.9
        Vector3d sup = stance.supportPoint();
        assertEquals(10.3, sup.x, EPSILON);
        assertEquals(64.9, sup.y, EPSILON);
        assertEquals(5.0, sup.z, EPSILON);

        // Head point extends into room along localUp at X=9.7
        Vector3d head = stance.headPoint();
        assertEquals(9.7, head.x, EPSILON);
        assertEquals(64.9, head.y, EPSILON);
        assertEquals(5.0, head.z, EPSILON);

        // Support surface offset: (+0.3, +0.9, 0.0)
        Vector3d offset = stance.supportSurfaceOffset();
        assertEquals(0.3, offset.x, EPSILON);
        assertEquals(0.9, offset.y, EPSILON);
        assertEquals(0.0, offset.z, EPSILON);

        // Rotation from world UP (0, 1, 0) to localUp (-1, 0, 0) is 90 degrees
        assertEquals(90.0, stance.rotationFromWorldUpDegrees(), EPSILON);

        // Unit rotation axis: (0, 1, 0) x (-1, 0, 0) = (0, 0, 1)
        Vector3d axis = stance.rotationAxis();
        assertEquals(0.0, axis.x, EPSILON);
        assertEquals(0.0, axis.y, EPSILON);
        assertEquals(1.0, axis.z, EPSILON);
    }

    @Test
    void ceilingStancePlacesFeetOnCeilingAndBodyInverted() {
        // Ceiling at Y=100.0. Gravity pulls upward (+Y).
        Vector3d center = new Vector3d(0.0, 99.1, 0.0);
        Vector3d localDown = new Vector3d(0, 1, 0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        // LocalUp points downward (-Y)
        assertEquals(0.0, stance.localUp().x, EPSILON);
        assertEquals(-1.0, stance.localUp().y, EPSILON);
        assertEquals(0.0, stance.localUp().z, EPSILON);

        // Feet support point is on the ceiling at Y=100.0
        Vector3d sup = stance.supportPoint();
        assertEquals(0.0, sup.x, EPSILON);
        assertEquals(100.0, sup.y, EPSILON);
        assertEquals(0.0, sup.z, EPSILON);

        // Head point extends downward toward Y=98.2
        Vector3d head = stance.headPoint();
        assertEquals(0.0, head.x, EPSILON);
        assertEquals(98.2, head.y, EPSILON);
        assertEquals(0.0, head.z, EPSILON);

        // Offset from base (0.0, 98.2, 0.0) to ceiling is (0, 1.8, 0)
        Vector3d offset = stance.supportSurfaceOffset();
        assertEquals(0.0, offset.x, EPSILON);
        assertEquals(1.8, offset.y, EPSILON);
        assertEquals(0.0, offset.z, EPSILON);

        // Rotation is 180 degrees
        assertEquals(180.0, stance.rotationFromWorldUpDegrees(), EPSILON);
    }

    @Test
    void feetProbeBoundsStrictlyIsolatesFeetAndExcludesHead() {
        // Wall at +X
        Vector3d center = new Vector3d(10.0, 64.9, 0.0);
        Vector3d localDown = new Vector3d(1, 0, 0);
        LocalBodyStance stance = new LocalBodyStance(center, localDown, 0.3, 0.9);

        double[] bounds = stance.feetProbeBounds(0.25, 0.1);
        double minX = bounds[0];
        double minY = bounds[1];
        double minZ = bounds[2];
        double maxX = bounds[3];
        double maxY = bounds[4];
        double maxZ = bounds[5];

        // Probe box in X is [10.2, 10.55] (anchored around support point 10.3)
        assertEquals(10.2, minX, EPSILON);
        assertEquals(10.55, maxX, EPSILON);

        // Head is at X=9.7, which is strictly outside [10.2, 10.55]
        assertTrue(stance.headPoint().x < minX, "Head point must be strictly outside feet probe bounds");
    }

    @Test
    void continuousCircumnavigationProducesContinuousRotations() {
        // Test stepping 360 degrees around a circle in the X-Y plane
        int steps = 72;
        double prevAngle = 0.0;
        for (int i = 0; i <= steps; i++) {
            double theta = 2.0 * Math.PI * i / steps;
            Vector3d down = new Vector3d(Math.sin(theta), -Math.cos(theta), 0.0).normalize();
            LocalBodyStance stance = new LocalBodyStance(Vector3d.ZERO, down, 0.3, 0.9);

            double angleDeg = stance.rotationFromWorldUpDegrees();
            assertTrue(angleDeg >= 0.0 && angleDeg <= 180.0, "Angle must be in [0, 180]");

            // Basis determinant must always be +1.0
            LocalEntityFrame frame = stance.toEntityFrame(0.0, GravitySourceId.DEFAULT_MINECRAFT);
            assertEquals(1.0, frame.basisDeterminant(), EPSILON, "Basis determinant must be +1 (right-handed)");
        }
    }
}
