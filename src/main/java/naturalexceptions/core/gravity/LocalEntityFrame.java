package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Gravity-aware local reference frame for an entity or player.
 *
 * <p>
 * Provides local up/down axes, orthogonal tangent axes spanning the local
 * walking surface, and maps 2D player inputs onto the local tangent plane
 * rather than hardcoded global X/Z coordinates.
 */
public final class LocalEntityFrame {

    private final Vector3d localDown;
    private final Vector3d localUp;
    private final Vector3d tangentForward;
    private final Vector3d tangentRight;
    private final GravitySourceId activeSourceId;

    public LocalEntityFrame(Vector3d localDown, Vector3d localUp, Vector3d tangentForward, Vector3d tangentRight,
        GravitySourceId activeSourceId) {
        this.localDown = Objects.requireNonNull(localDown, "localDown");
        this.localUp = Objects.requireNonNull(localUp, "localUp");
        this.tangentForward = Objects.requireNonNull(tangentForward, "tangentForward");
        this.tangentRight = Objects.requireNonNull(tangentRight, "tangentRight");
        this.activeSourceId = activeSourceId != null ? activeSourceId : GravitySourceId.DEFAULT_MINECRAFT;
    }

    public static LocalEntityFrame standardMinecraft() {
        return new LocalEntityFrame(
            Vector3d.DOWN,
            Vector3d.UP,
            new Vector3d(0, 0, 1),
            new Vector3d(1, 0, 0),
            GravitySourceId.DEFAULT_MINECRAFT);
    }

    public static LocalEntityFrame deriveFrom(GravitySample sample, double headingYawDegrees) {
        Vector3d down = sample.localDown();
        Vector3d up = sample.localUp();

        // Convert yaw to reference direction vector in horizontal XZ plane
        double yawRad = Math.toRadians(-headingYawDegrees);
        Vector3d headingRef = new Vector3d(Math.sin(yawRad), 0.0, Math.cos(yawRad));

        // Project heading onto plane orthogonal to up
        Vector3d projected = headingRef.subtract(up.multiply(headingRef.dot(up)));
        if (projected.lengthSquared() < 1e-6) {
            // Degenerate case: heading is nearly collinear with up; choose fallback reference
            Vector3d altRef = Math.abs(up.y) < 0.9 ? Vector3d.UP : new Vector3d(1, 0, 0);
            projected = altRef.subtract(up.multiply(altRef.dot(up)));
        }
        Vector3d forward = projected.normalize();
        Vector3d right = forward.cross(up)
            .normalize();

        return new LocalEntityFrame(down, up, forward, right, sample.sourceId());
    }

    public Vector3d localDown() {
        return localDown;
    }

    public Vector3d localUp() {
        return localUp;
    }

    public Vector3d tangentForward() {
        return tangentForward;
    }

    public Vector3d tangentRight() {
        return tangentRight;
    }

    public GravitySourceId activeSourceId() {
        return activeSourceId;
    }

    /**
     * Maps forward (W/S) and strafe (A/D) inputs onto the local surface tangent plane.
     */
    public Vector3d mapInput(double forwardInput, double strafeInput) {
        return tangentForward.multiply(forwardInput)
            .add(tangentRight.multiply(strafeInput));
    }

    @Override
    public String toString() {
        return "LocalEntityFrame{up=" + localUp + ", fwd=" + tangentForward + ", source=" + activeSourceId + "}";
    }
}
