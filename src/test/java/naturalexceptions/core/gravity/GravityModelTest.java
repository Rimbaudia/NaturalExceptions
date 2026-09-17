package naturalexceptions.core.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GravityModelTest {

    private static final double EPSILON = 1e-9;

    @Test
    void vectorArithmeticBehavesCorrectly() {
        Vector3d v1 = new Vector3d(1.0, 2.0, 3.0);
        Vector3d v2 = new Vector3d(4.0, -2.0, 1.0);

        Vector3d sum = v1.add(v2);
        assertEquals(5.0, sum.x, EPSILON);
        assertEquals(0.0, sum.y, EPSILON);
        assertEquals(4.0, sum.z, EPSILON);

        Vector3d diff = v1.subtract(v2);
        assertEquals(-3.0, diff.x, EPSILON);
        assertEquals(4.0, diff.y, EPSILON);
        assertEquals(2.0, diff.z, EPSILON);

        Vector3d scaled = v1.multiply(2.5);
        assertEquals(2.5, scaled.x, EPSILON);
        assertEquals(5.0, scaled.y, EPSILON);
        assertEquals(7.5, scaled.z, EPSILON);

        // Dot product: 1*4 + 2*(-2) + 3*1 = 4 - 4 + 3 = 3
        assertEquals(3.0, v1.dot(v2), EPSILON);

        // Cross product: (2*1 - 3*(-2), 3*4 - 1*1, 1*(-2) - 2*4) = (8, 11, -10)
        Vector3d cross = v1.cross(v2);
        assertEquals(8.0, cross.x, EPSILON);
        assertEquals(11.0, cross.y, EPSILON);
        assertEquals(-10.0, cross.z, EPSILON);

        // Length
        Vector3d v3 = new Vector3d(3.0, 4.0, 0.0);
        assertEquals(25.0, v3.lengthSquared(), EPSILON);
        assertEquals(5.0, v3.length(), EPSILON);

        // Normalization
        Vector3d normalized = v3.normalize();
        assertEquals(0.6, normalized.x, EPSILON);
        assertEquals(0.8, normalized.y, EPSILON);
        assertEquals(0.0, normalized.z, EPSILON);
        assertEquals(1.0, normalized.length(), EPSILON);

        // Zero normalization
        assertEquals(Vector3d.ZERO, Vector3d.ZERO.normalize());

        // Distance
        assertEquals(5.0, new Vector3d(0, 0, 0).distance(new Vector3d(0, 5, 0)), EPSILON);
    }

    @Test
    void gravitySampleDerivesDirectionAndBasis() {
        // Downward gravity
        GravitySample downSample = GravitySample.standardDownward(0.08);
        assertEquals(0.08, downSample.magnitude(), EPSILON);
        assertEquals(new Vector3d(0, -1, 0), downSample.direction());
        assertEquals(new Vector3d(0, -1, 0), downSample.localDown());
        assertEquals(new Vector3d(0, 1, 0), downSample.localUp());
        assertFalse(downSample.isZero());

        // Zero gravity
        GravitySample zeroSample = GravitySample.zero();
        assertTrue(zeroSample.isZero());
        assertEquals(Vector3d.DOWN, zeroSample.direction());
        assertEquals(Vector3d.UP, zeroSample.localUp());

        // Arbitrary sideways gravity (pointing along +X)
        GravitySample sideways = GravitySample.of(new Vector3d(0.5, 0, 0));
        assertEquals(0.5, sideways.magnitude(), EPSILON);
        assertEquals(new Vector3d(1, 0, 0), sideways.direction());
        assertEquals(new Vector3d(-1, 0, 0), sideways.localUp());
    }

    @Test
    void uniformGravityFieldProvidesIdenticalAccelerationEverywhere() {
        UniformGravityField field = UniformGravityField.sideways(0.05, -0.02, 0.0);

        GravitySample s1 = field.sampleAt(0, 0, 0);
        GravitySample s2 = field.sampleAt(100, 250, -300);

        assertEquals(s1.acceleration(), s2.acceleration());
        assertEquals(0.05, s1.acceleration().x, EPSILON);
        assertEquals(-0.02, s1.acceleration().y, EPSILON);
    }

    @Test
    void radialGravitySourcePullsTowardCenterWithInverseSquareAndCore() {
        Vector3d center = new Vector3d(100.0, 50.0, 100.0);
        double surfaceG = 0.10;
        double innerR = 10.0;
        double maxR = 100.0;

        RadialGravitySource source = new RadialGravitySource(center, surfaceG, innerR, maxR);

        // Position directly to the east: (120, 50, 100) -> distance is 20
        GravitySample sampleEast = source.sampleAt(120.0, 50.0, 100.0);
        // Pull should point west (-X) towards center (100)
        assertEquals(-1.0, sampleEast.direction().x, EPSILON);
        assertEquals(0.0, sampleEast.direction().y, EPSILON);
        assertEquals(0.0, sampleEast.direction().z, EPSILON);

        // Distance = 20. Falloff is (10 / 20)^2 = 0.25 -> magnitude = 0.10 * 0.25 = 0.025
        assertEquals(0.025, sampleEast.magnitude(), EPSILON);

        // Inside inner radius: distance = 5. Linear core: surfaceG * (5 / 10) = 0.05
        GravitySample sampleInside = source.sampleAt(105.0, 50.0, 100.0);
        assertEquals(0.05, sampleInside.magnitude(), EPSILON);
        assertEquals(-1.0, sampleInside.direction().x, EPSILON);

        // Beyond max radius: distance = 150 > 100
        GravitySample sampleFar = source.sampleAt(250.0, 50.0, 100.0);
        assertTrue(sampleFar.isZero());

        // At exact center: singularity check
        GravitySample sampleCenter = source.sampleAt(100.0, 50.0, 100.0);
        assertTrue(sampleCenter.isZero());
    }

    @Test
    void compositeGravityFieldSuperimposesMultipleFields() {
        // Base uniform downward gravity: (0, -0.08, 0)
        UniformGravityField baseField = UniformGravityField.downward(0.08);

        // Anomaly pushing along +X: (0.05, 0, 0)
        UniformGravityField anomalyField = UniformGravityField.of(0.05, 0.0, 0.0);

        CompositeGravityField composite = CompositeGravityField.of(baseField, anomalyField);

        GravitySample sample = composite.sampleAt(10, 20, 30);
        assertEquals(0.05, sample.acceleration().x, EPSILON);
        assertEquals(-0.08, sample.acceleration().y, EPSILON);
        assertEquals(0.0, sample.acceleration().z, EPSILON);

        // Net magnitude: sqrt(0.05^2 + (-0.08)^2) = sqrt(0.0025 + 0.0064) = sqrt(0.0089)
        assertEquals(Math.sqrt(0.0089), sample.magnitude(), EPSILON);
    }

    @Test
    void validatesArgumentsGracefully() {
        assertThrows(IllegalArgumentException.class, () -> new RadialGravitySource(Vector3d.ZERO, -1.0, 10, 50));
        assertThrows(IllegalArgumentException.class, () -> new RadialGravitySource(Vector3d.ZERO, 1.0, 0, 50));
        assertThrows(IllegalArgumentException.class, () -> new RadialGravitySource(Vector3d.ZERO, 1.0, 50, 10));
    }
}
