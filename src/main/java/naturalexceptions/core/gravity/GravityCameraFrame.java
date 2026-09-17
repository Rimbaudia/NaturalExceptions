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
    private Vector3d currentForward;
    private Vector3d currentRight;
    private final double smoothingFactor;

    public GravityCameraFrame(Vector3d initialUp, double smoothingFactor) {
        Objects.requireNonNull(initialUp, "initialUp");
        Vector3d norm = initialUp.normalize();
        this.currentUp = norm.lengthSquared() > 1e-6 ? norm : Vector3d.UP;
        this.smoothingFactor = Math.max(0.01, Math.min(1.0, smoothingFactor));

        // Initialize basis: align standard (0, 0, 1) and (1, 0, 0) with currentUp
        Vector3d worldUp = Vector3d.UP;
        double dot = worldUp.dot(this.currentUp);
        if (dot > 0.99999) {
            this.currentForward = new Vector3d(0.0, 0.0, 1.0);
            this.currentRight = new Vector3d(1.0, 0.0, 0.0);
        } else if (dot < -0.99999) {
            this.currentForward = new Vector3d(0.0, 0.0, 1.0);
            this.currentRight = new Vector3d(-1.0, 0.0, 0.0);
        } else {
            Vector3d axis = worldUp.cross(this.currentUp)
                .normalize();
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            this.currentForward = LocalEntityFrame.rotateVector(new Vector3d(0.0, 0.0, 1.0), axis, angle)
                .normalize();
            this.currentRight = this.currentUp.cross(this.currentForward)
                .normalize();
            this.currentForward = this.currentRight.cross(this.currentUp)
                .normalize();
        }
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

    public Vector3d currentForward() {
        return currentForward;
    }

    public Vector3d currentRight() {
        return currentRight;
    }

    public double smoothingFactor() {
        return smoothingFactor;
    }

    /**
     * Steps the camera frame toward the target local-up direction.
     * Uses parallel transport along the geodesic rotation so that orientation and roll
     * evolve smoothly without accumulating twist or snapping 180 degrees.
     */
    public void update(Vector3d targetUp) {
        if (targetUp == null || targetUp.lengthSquared() < 1e-6) {
            return;
        }
        Vector3d target = targetUp.normalize();
        double dot = currentUp.dot(target);
        if (dot > 0.999999) {
            return;
        }

        Vector3d rotAxis;
        double angle;
        if (dot < -0.999999) {
            rotAxis = currentRight;
            angle = Math.PI * smoothingFactor;
        } else {
            rotAxis = currentUp.cross(target)
                .normalize();
            angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot))) * smoothingFactor;
        }

        this.currentUp = LocalEntityFrame.rotateVector(this.currentUp, rotAxis, angle)
            .normalize();
        this.currentForward = LocalEntityFrame.rotateVector(this.currentForward, rotAxis, angle)
            .normalize();
        this.currentRight = this.currentUp.cross(this.currentForward)
            .normalize();
        this.currentForward = this.currentRight.cross(this.currentUp)
            .normalize();
    }

    /**
     * Calculates the 3D camera look direction given local yaw and pitch (in degrees).
     * Local yaw rotates around currentUp (yaw 0 = forward, 90 = right).
     * Local pitch tilts up/down relative to the local horizon plane (-90 = up, +90 = down).
     */
    public Vector3d computeLookDirection(double localYawDegrees, double localPitchDegrees) {
        double yawRad = Math.toRadians(localYawDegrees);
        double pitchRad = Math.toRadians(localPitchDegrees);

        // Vanilla Minecraft heading: yaw 0 = forward, yaw 90 = -X (right/west in vanilla)
        Vector3d fwdYaw = currentForward.multiply(Math.cos(yawRad))
            .subtract(currentRight.multiply(Math.sin(yawRad)))
            .normalize();

        // Elevate by pitch: positive pitch looks up toward zenith, negative looks down toward nadir
        return fwdYaw.multiply(Math.cos(pitchRad))
            .add(currentUp.multiply(Math.sin(pitchRad)))
            .normalize();
    }

    /**
     * Computes the complete 3D camera orthonormal basis (right, up, look) from yaw and pitch.
     * Guaranteed right-handed with determinant +1.
     */
    public CameraBasis computeCameraBasis(double localYawDegrees, double localPitchDegrees) {
        double yawRad = Math.toRadians(localYawDegrees);
        double pitchRad = Math.toRadians(localPitchDegrees);

        Vector3d fwdYaw = currentForward.multiply(Math.cos(yawRad))
            .subtract(currentRight.multiply(Math.sin(yawRad)))
            .normalize();

        Vector3d look = fwdYaw.multiply(Math.cos(pitchRad))
            .add(currentUp.multiply(Math.sin(pitchRad)))
            .normalize();

        // Screen right is the viewer's right ear on the local tangent plane:
        // fwdYaw x currentUp is always strictly orthogonal to both and non-zero for any pitch
        Vector3d right = fwdYaw.cross(currentUp)
            .normalize();

        // Screen up: right x look
        Vector3d screenUp = right.cross(look)
            .normalize();

        return new CameraBasis(right, screenUp, look);
    }

    public static final class CameraBasis {

        public final Vector3d right;
        public final Vector3d up;
        public final Vector3d look;

        public CameraBasis(Vector3d right, Vector3d up, Vector3d look) {
            this.right = right;
            this.up = up;
            this.look = look;
        }

        public double determinant() {
            Vector3d back = look.negate();
            return right.dot(up.cross(back));
        }
    }

    /**
     * Spherical linear interpolation between two unit vectors.
     */
    public static Vector3d slerp(Vector3d start, Vector3d end, double t) {
        double dot = start.dot(end);
        dot = Math.max(-1.0, Math.min(1.0, dot));

        if (dot > 0.9995) {
            return start.multiply(1.0 - t)
                .add(end.multiply(t))
                .normalize();
        }

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
