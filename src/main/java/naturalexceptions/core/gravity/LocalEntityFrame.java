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
        Vector3d up = sample.localUp();
        Vector3d down = sample.localDown();

        double yawRad = Math.toRadians(headingYawDegrees);
        // Vanilla Minecraft standard ground heading (yaw 0 = +Z, yaw 90 = -X)
        Vector3d standardForward = new Vector3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));

        // Rotate standard forward vector from UP (0, 1, 0) to localUp using minimal geodesic rotation
        Vector3d worldUp = Vector3d.UP;
        Vector3d fwd;
        double dot = worldUp.dot(up);
        if (dot > 0.99999) {
            fwd = standardForward;
        } else if (dot < -0.99999) {
            // Inverted ceiling: 180-degree geodesic continuation around X
            fwd = new Vector3d(standardForward.x, 0.0, -standardForward.z);
        } else {
            Vector3d rotAxis = worldUp.cross(up)
                .normalize();
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
            fwd = rotateVector(standardForward, rotAxis, angle);
        }

        // Project onto tangent plane and normalize
        Vector3d tangentProj = fwd.subtract(up.multiply(fwd.dot(up)));
        if (tangentProj.lengthSquared() > 1e-4) {
            fwd = tangentProj.normalize();
        } else {
            fwd = arbitraryTangent(up);
        }

        // Canonical right-handed convention:
        // right = up x forward
        // check: right x up = forward, forward x right = up, det = +1
        Vector3d right = up.cross(fwd)
            .normalize();

        return new LocalEntityFrame(down, up, fwd, right, sample.sourceId());
    }

    public static LocalEntityFrame deriveFromView(GravitySample sample, Vector3d viewForward,
        Vector3d fallbackForward) {
        Vector3d up = sample.localUp();
        Vector3d down = sample.localDown();

        Vector3d proj = viewForward != null ? viewForward.subtract(up.multiply(viewForward.dot(up))) : null;
        Vector3d fwd;
        if (proj != null && proj.lengthSquared() > 1e-4) {
            fwd = proj.normalize();
        } else if (fallbackForward != null) {
            Vector3d fbProj = fallbackForward.subtract(up.multiply(fallbackForward.dot(up)));
            fwd = fbProj.lengthSquared() > 1e-4 ? fbProj.normalize() : arbitraryTangent(up);
        } else {
            fwd = arbitraryTangent(up);
        }

        Vector3d right = up.cross(fwd)
            .normalize();
        return new LocalEntityFrame(down, up, fwd, right, sample.sourceId());
    }

    private static Vector3d arbitraryTangent(Vector3d up) {
        Vector3d ref = Math.abs(up.y) < 0.9 ? Vector3d.UP : new Vector3d(1.0, 0.0, 0.0);
        return ref.cross(up)
            .normalize();
    }

    public static Vector3d rotateVector(Vector3d v, Vector3d unitAxis, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        Vector3d cross = unitAxis.cross(v);
        double dot = unitAxis.dot(v);
        return v.multiply(cos)
            .add(cross.multiply(sin))
            .add(unitAxis.multiply(dot * (1.0 - cos)));
    }

    public double basisDeterminant() {
        return tangentRight.dot(localUp.cross(tangentForward));
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
