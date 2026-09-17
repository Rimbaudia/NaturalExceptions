package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Gravity-relative surface contact resolution.
 *
 * <p>
 * Decouples grounding from vanilla Minecraft's Y-axis checks (onGround, isCollidedVertically).
 * Evaluates support along the localDown vector, allowing walls (under sideways gravity)
 * and ceilings (under inverted gravity) to act as valid supporting ground surfaces without
 * continuous drift.
 */
public final class GravitySurfaceContact {

    public static final double DEFAULT_SURFACE_THRESHOLD = 0.15;

    private final boolean isGrounded;
    private final Vector3d surfaceNormal;
    private final double distanceToSurface;

    public GravitySurfaceContact(boolean isGrounded, Vector3d surfaceNormal, double distanceToSurface) {
        this.isGrounded = isGrounded;
        this.surfaceNormal = surfaceNormal != null ? surfaceNormal : Vector3d.UP;
        this.distanceToSurface = distanceToSurface;
    }

    public static GravitySurfaceContact airborne() {
        return new GravitySurfaceContact(false, Vector3d.UP, Double.POSITIVE_INFINITY);
    }

    public static GravitySurfaceContact grounded(Vector3d surfaceNormal, double distanceToSurface) {
        return new GravitySurfaceContact(true, surfaceNormal, distanceToSurface);
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public Vector3d surfaceNormal() {
        return surfaceNormal;
    }

    public double distanceToSurface() {
        return distanceToSurface;
    }

    /**
     * Resolves velocity against the supporting surface.
     * Cancels penetration velocity into the surface while preserving/damping tangential motion.
     */
    public Vector3d resolveVelocity(Vector3d velocity, double frictionFactor) {
        Objects.requireNonNull(velocity, "velocity");
        if (!isGrounded) {
            return velocity;
        }

        double normalVel = velocity.dot(surfaceNormal);
        // If velocity is directed into the surface (negative relative to normal), cancel it
        if (normalVel < 0.0) {
            Vector3d tangentVel = velocity.subtract(surfaceNormal.multiply(normalVel));
            return tangentVel.multiply(frictionFactor);
        }

        return velocity;
    }

    @Override
    public String toString() {
        return "GravitySurfaceContact{grounded=" + isGrounded
            + ", normal="
            + surfaceNormal
            + ", dist="
            + distanceToSurface
            + "}";
    }
}
