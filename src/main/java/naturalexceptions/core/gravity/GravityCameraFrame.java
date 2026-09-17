package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Camera orientation frame decoupled from physical gravity.
 *
 * <p>
 * Maintains a smoothed local-up vector using spherical linear interpolation
 * to prevent abrupt visual snaps when transitioning between gravity sources or circumnavigating
 * radial bodies.
 *
 * <p>
 * Constructs a full 3D orthonormal camera basis (forward, up, right) from yaw and pitch
 * defined relative to the local reference frame, eliminating vanilla global-Y pitch gimbal lock.
 */
public final class GravityCameraFrame {

    public static final double DEFAULT_SMOOTHING_FACTOR = 0.15;

    private Vector3d currentUp;
    private final double smoothingFactor;

    public GravityCameraFrame(Vector3d initialUp, double smoothingFactor) {
        Objects.requireNonNull(initialUp, "initialUp");
        Vector3d norm = initialUp.normalize();
        this.currentUp = norm.lengthSquared() > 1e-6 ? norm : Vector3d.UP;
        this.smoothingFactor = Math.max(0.01, Math.min(1.0, smoothingFactor));
    }

    public GravityCameraFrame(Vector3d initialUp) {
        this(initialUp, DEFAULT_SMOOTHING_FACTOR);
    }

    public static GravityCameraFrame standard() {
        return new GravityCameraFrame(Vector3d.UP, DEFAULT_SMOOTHING_FACTOR);
    }

    public Vector3d currentUp() {
        return currentUp;
    }

    public double smoothingFactor() {
        return smoothingFactor;
    }

    /**
     * Steps the camera frame toward the target local-up direction.
     * Uses spherical linear interpolation (SLERP) so that camera roll and orientation
     * change smoothly without snapping.
     */
    public void update(Vector3d targetUp) {
        if (targetUp == null || targetUp.lengthSquared() < 1e-6) {
            return;
        }
        Vector3d target = targetUp.normalize();
        this.currentUp = slerp(this.currentUp, target, smoothingFactor);
    }

    /**
     * Calculates the 3D camera look direction given local yaw and pitch (in degrees).
     * Local yaw rotates around currentUp.
     * Local pitch rotates up/down relative to the local horizon plane.
     * Pitch can safely approach or pass +/- 90 degrees without global gimbal lock
     * because the reference frame is aligned with currentUp.
     */
    public Vector3d computeLookDirection(double localYawDegrees, double localPitchDegrees) {
        double yawRad = Math.toRadians(localYawDegrees);
        double pitchRad = Math.toRadians(localPitchDegrees);

        // Find an arbitrary reference vector orthogonal to currentUp
        Vector3d refAxis = Math.abs(currentUp.y) < 0.9 ? Vector3d.UP : new Vector3d(1, 0, 0);
        Vector3d localRight = currentUp.cross(refAxis)
            .normalize();
        Vector3d localForward = localRight.cross(currentUp)
            .normalize();

        // Rotate localForward around currentUp by yaw
        Vector3d horizontalLook = localForward.multiply(Math.cos(yawRad))
            .add(localRight.multiply(Math.sin(yawRad)))
            .normalize();

        // Elevate by pitch relative to horizon (cos(pitch)*horiz + sin(pitch)*currentUp)
        return horizontalLook.multiply(Math.cos(pitchRad))
            .add(currentUp.multiply(Math.sin(pitchRad)))
            .normalize();
    }

    /**
     * Spherical linear interpolation between two unit vectors.
     */
    public static Vector3d slerp(Vector3d start, Vector3d end, double t) {
        double dot = start.dot(end);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        // If almost identical, direct linear interpolation to avoid numerical instability
        if (dot > 0.9995) {
            return start.multiply(1.0 - t)
                .add(end.multiply(t))
                .normalize();
        }

        // If opposing vectors, pick an arbitrary perpendicular axis
        if (dot < -0.9995) {
            Vector3d ortho = Math.abs(start.y) < 0.9 ? start.cross(Vector3d.UP) : start.cross(new Vector3d(1, 0, 0));
            ortho = ortho.normalize();
            double theta = Math.PI * t;
            return start.multiply(Math.cos(theta))
                .add(ortho.multiply(Math.sin(theta)))
                .normalize();
        }

        double theta0 = Math.acos(dot);
        double theta = theta0 * t;
        Vector3d relativeVec = end.subtract(start.multiply(dot))
            .normalize();
        return start.multiply(Math.cos(theta))
            .add(relativeVec.multiply(Math.sin(theta)))
            .normalize();
    }
}
