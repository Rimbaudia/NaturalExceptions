package naturalexceptions.core.gravity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GravityModelP21Test {

    private static final double EPSILON = 1e-6;

    @Test
    void gravityQueryReturnsVectorAndSourceIdentity() {
        GravitySourceId sourceA = GravitySourceId.of("naturalexceptions", "source_a");
        UniformGravityField field = UniformGravityField.of(0.0, -0.08, 0.0, sourceA);

        GravitySample sample = field.sampleAt(10, 20, 30);
        assertEquals(sourceA, sample.sourceId());
        assertEquals(0.08, sample.magnitude(), EPSILON);
        assertEquals(Vector3d.DOWN, sample.localDown());
        assertEquals(Vector3d.UP, sample.localUp());
    }

    @Test
    void localEntityFrameMapsInputOntoSurfaceTangentPlane() {
        // Inverted gravity: localDown is UP (+Y), localUp is DOWN (-Y)
        GravitySample ceilingSample = GravitySample.of(new Vector3d(0, 0.08, 0));
        LocalEntityFrame frame = LocalEntityFrame.deriveFrom(ceilingSample, 0.0);

        assertEquals(new Vector3d(0, 1, 0), frame.localDown());
        assertEquals(new Vector3d(0, -1, 0), frame.localUp());

        // Forward input (1.0, 0.0) should map onto tangent plane orthogonal to localUp
        Vector3d forwardMotion = frame.mapInput(1.0, 0.0);
        assertEquals(0.0, forwardMotion.dot(frame.localUp()), EPSILON);
        assertEquals(1.0, forwardMotion.length(), EPSILON);

        // Sideways gravity: localDown is +X, localUp is -X (standing on a wall at X)
        GravitySample wallSample = GravitySample.of(new Vector3d(0.08, 0, 0));
        LocalEntityFrame wallFrame = LocalEntityFrame.deriveFrom(wallSample, 0.0);

        assertEquals(new Vector3d(1, 0, 0), wallFrame.localDown());
        assertEquals(new Vector3d(-1, 0, 0), wallFrame.localUp());

        Vector3d wallMotion = wallFrame.mapInput(1.0, 0.5);
        // Wall motion must be completely orthogonal to localDown and localUp
        assertEquals(0.0, wallMotion.dot(wallFrame.localUp()), EPSILON);
        assertEquals(0.0, wallMotion.dot(wallFrame.localDown()), EPSILON);
    }

    @Test
    void surfaceContactResolvesGroundingAndVelocityOnFloorWallAndCeiling() {
        // Floor contact (-Y gravity, normal is +Y)
        GravitySurfaceContact floorContact = GravitySurfaceContact.grounded(Vector3d.UP, 0.05);
        assertTrue(floorContact.isGrounded());
        Vector3d fallingVel = new Vector3d(0.2, -0.8, 0.0);
        Vector3d resolvedFloor = floorContact.resolveVelocity(fallingVel, 0.6);
        // Downward penetration is canceled, tangential motion preserved and friction applied
        assertEquals(0.0, resolvedFloor.y, EPSILON);
        assertEquals(0.12, resolvedFloor.x, EPSILON);

        // Wall contact (+X gravity, normal is -X)
        Vector3d wallNormal = new Vector3d(-1, 0, 0);
        GravitySurfaceContact wallContact = GravitySurfaceContact.grounded(wallNormal, 0.02);
        assertTrue(wallContact.isGrounded());
        Vector3d wallImpactVel = new Vector3d(0.5, 0.0, 0.3); // Moving into wall (+X)
        Vector3d resolvedWall = wallContact.resolveVelocity(wallImpactVel, 0.8);
        assertEquals(0.0, resolvedWall.x, EPSILON);
        assertEquals(0.24, resolvedWall.z, EPSILON);

        // Ceiling contact (+Y gravity, normal is -Y)
        Vector3d ceilingNormal = new Vector3d(0, -1, 0);
        GravitySurfaceContact ceilingContact = GravitySurfaceContact.grounded(ceilingNormal, 0.01);
        assertTrue(ceilingContact.isGrounded());
        Vector3d ceilingImpactVel = new Vector3d(0.1, 0.6, 0.0); // Moving up into ceiling (+Y)
        Vector3d resolvedCeiling = ceilingContact.resolveVelocity(ceilingImpactVel, 0.5);
        assertEquals(0.0, resolvedCeiling.y, EPSILON);
        assertEquals(0.05, resolvedCeiling.x, EPSILON);
    }

    @Test
    void frameHysteresisPreventsThrashingBetweenCompetingSources() {
        GravitySourceId sourceA = GravitySourceId.of("test", "source_a");
        GravitySourceId sourceB = GravitySourceId.of("test", "source_b");

        // Source A provides 0.10 downward, Source B provides 0.11 sideways
        UniformGravityField fieldA = UniformGravityField.of(0.0, -0.10, 0.0, sourceA);
        UniformGravityField fieldB = UniformGravityField.of(0.11, 0.0, 0.0, sourceB);
        CompositeGravityField composite = CompositeGravityField.of(fieldA, fieldB);

        GravityFrameTracker tracker = new GravityFrameTracker(sourceA);

        // Without hysteresis (empty context), Source B wins because 0.11 > 0.10
        GravitySample naiveSample = composite.sampleAt(0, 0, 0, GravityQueryContext.empty());
        assertEquals(sourceB, naiveSample.sourceId());

        // With tracker bound to Source A (ungrounded, hysteresis factor 1.25):
        // 0.10 * 1.25 = 0.125 >= 0.11, so Source A remains active!
        tracker.setGrounded(false);
        GravitySample trackedSample = tracker.sampleAndUpdate(composite, 0, 0, 0, 1L);
        assertEquals(sourceA, trackedSample.sourceId());
        assertEquals(sourceA, tracker.currentSourceId());

        // Now test when grounded (hysteresis factor 1.5):
        // Even if Source B increases to 0.14: 0.10 * 1.5 = 0.15 >= 0.14 -> stays A
        UniformGravityField fieldBStronger = UniformGravityField.of(0.14, 0.0, 0.0, sourceB);
        CompositeGravityField composite2 = CompositeGravityField.of(fieldA, fieldBStronger);

        tracker.setGrounded(true);
        GravitySample groundedSample = tracker.sampleAndUpdate(composite2, 0, 0, 0, 2L);
        assertEquals(sourceA, groundedSample.sourceId());

        // But when Source B exceeds hysteresis threshold (0.16 > 0.10 * 1.5 = 0.15), it cleanly switches!
        UniformGravityField fieldBDecisive = UniformGravityField.of(0.16, 0.0, 0.0, sourceB);
        CompositeGravityField composite3 = CompositeGravityField.of(fieldA, fieldBDecisive);

        GravitySample switchedSample = tracker.sampleAndUpdate(composite3, 0, 0, 0, 3L);
        assertEquals(sourceB, switchedSample.sourceId());
        assertEquals(sourceB, tracker.currentSourceId());
        assertEquals(3L, tracker.lastSwitchTick());
    }

    @Test
    void cameraFrameSmoothsOrientationWithoutPitchGimbalLock() {
        GravityCameraFrame camera = new GravityCameraFrame(Vector3d.UP, 0.2);
        assertEquals(Vector3d.UP, camera.currentUp());

        // Target up suddenly shifts to +X (standing on wall)
        Vector3d targetUp = new Vector3d(1, 0, 0);
        camera.update(targetUp);

        // Camera up must interpolate smoothly rather than snapping instantly
        assertNotEquals(Vector3d.UP, camera.currentUp());
        assertNotEquals(targetUp, camera.currentUp());
        assertEquals(
            1.0,
            camera.currentUp()
                .length(),
            EPSILON);

        // Look direction at pitch = +90 degrees (zenith) and -90 degrees (nadir)
        // must compute cleanly without NaN or gimbal lock
        Vector3d zenithLook = camera.computeLookDirection(0.0, 90.0);
        assertEquals(0.0, zenithLook.distance(camera.currentUp()), EPSILON);
        assertFalse(Double.isNaN(zenithLook.x));
        assertFalse(Double.isNaN(zenithLook.y));
        assertFalse(Double.isNaN(zenithLook.z));

        Vector3d nadirLook = camera.computeLookDirection(0.0, -90.0);
        assertEquals(
            0.0,
            nadirLook.distance(
                camera.currentUp()
                    .multiply(-1.0)),
            EPSILON);
        assertFalse(Double.isNaN(nadirLook.x));

        // Horizontal look (pitch = 0) is orthogonal to currentUp
        Vector3d horizonLook = camera.computeLookDirection(45.0, 0.0);
        assertEquals(0.0, horizonLook.dot(camera.currentUp()), EPSILON);
    }

    @Test
    void impactCalculatorDetectsLateralAndCeilingImpactsIndependentlyOfGlobalY() {
        GravityImpactCalculator calculator = GravityImpactCalculator.standard(); // safe=0.5, scale=10.0

        // Downward vertical impact into floor (-Y)
        Vector3d downwardVel = new Vector3d(0, -1.2, 0);
        double floorSpeed = calculator.calculateNormalImpactSpeed(downwardVel, Vector3d.UP);
        assertEquals(1.2, floorSpeed, EPSILON);
        assertEquals(7.0f, calculator.calculateDamage(floorSpeed), 1e-4f);

        // Lateral impact into wall (+X motion into normal -X)
        Vector3d lateralVel = new Vector3d(1.2, 0, 0);
        double wallSpeed = calculator.calculateNormalImpactSpeed(lateralVel, new Vector3d(-1, 0, 0));
        assertEquals(1.2, wallSpeed, EPSILON);
        assertEquals(7.0f, calculator.calculateDamage(wallSpeed), 1e-4f);

        // Safe landing below threshold (< 0.5) deals 0 damage
        Vector3d gentleVel = new Vector3d(0, -0.3, 0);
        double gentleSpeed = calculator.calculateNormalImpactSpeed(gentleVel, Vector3d.UP);
        assertEquals(0.3, gentleSpeed, EPSILON);
        assertEquals(0.0f, calculator.calculateDamage(gentleSpeed), 1e-4f);

        // Tangential motion (glancing along wall) produces 0 normal impact
        Vector3d glancingVel = new Vector3d(0, 0, 1.5);
        double glancingSpeed = calculator.calculateNormalImpactSpeed(glancingVel, new Vector3d(-1, 0, 0));
        assertEquals(0.0, glancingSpeed, EPSILON);
        assertEquals(0.0f, calculator.calculateDamage(glancingSpeed), 1e-4f);
    }
}
