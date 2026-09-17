package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Gravity-relative collision impact calculator.
 *
 * <p>
 * Decouples impact severity from vanilla Minecraft's hardcoded Y-axis fall distance
 * (which only counts downward vertical fall and ignores lateral or ceiling impacts).
 *
 * <p>
 * Computes normal impact velocity relative to the contacted surface or local gravity direction:
 * v_impact = max(0, -v_pre · n_surface) or v_impact = max(0, (v_pre - v_post) · n_surface).
 */
public final class GravityImpactCalculator {

    public static final double DEFAULT_SAFE_IMPACT_SPEED = 0.5; // blocks/tick (~10 m/s)
    public static final double DEFAULT_DAMAGE_SCALE = 10.0;

    private final double safeImpactSpeed;
    private final double damageScale;

    public GravityImpactCalculator(double safeImpactSpeed, double damageScale) {
        if (safeImpactSpeed < 0.0) {
            throw new IllegalArgumentException("safeImpactSpeed cannot be negative: " + safeImpactSpeed);
        }
        if (damageScale < 0.0) {
            throw new IllegalArgumentException("damageScale cannot be negative: " + damageScale);
        }
        this.safeImpactSpeed = safeImpactSpeed;
        this.damageScale = damageScale;
    }

    public static GravityImpactCalculator standard() {
        return new GravityImpactCalculator(DEFAULT_SAFE_IMPACT_SPEED, DEFAULT_DAMAGE_SCALE);
    }

    public double safeImpactSpeed() {
        return safeImpactSpeed;
    }

    public double damageScale() {
        return damageScale;
    }

    /**
     * Calculates the normal impact speed into a surface given velocity before impact
     * and the surface outward normal.
     *
     * <p>
     * If velocity is directed into the surface (v · n < 0), the impact speed is |v · n|.
     * If moving away or tangential, impact speed is 0.0.
     */
    public double calculateNormalImpactSpeed(Vector3d velocityBeforeImpact, Vector3d surfaceNormal) {
        Objects.requireNonNull(velocityBeforeImpact, "velocityBeforeImpact");
        Objects.requireNonNull(surfaceNormal, "surfaceNormal");

        Vector3d normal = surfaceNormal.normalize();
        if (normal.lengthSquared() < 1e-6) {
            return 0.0;
        }

        double projection = velocityBeforeImpact.dot(normal);
        // Normal points outward from surface; motion into surface has projection < 0
        if (projection < 0.0) {
            return -projection;
        }
        return 0.0;
    }

    /**
     * Calculates impact speed from velocity loss during a collision tick along the local down direction.
     */
    public double calculateLocalImpactSpeed(Vector3d velBefore, Vector3d velAfter, Vector3d localDown) {
        Objects.requireNonNull(velBefore, "velBefore");
        Objects.requireNonNull(velAfter, "velAfter");
        Objects.requireNonNull(localDown, "localDown");

        Vector3d down = localDown.normalize();
        if (down.lengthSquared() < 1e-6) {
            return 0.0;
        }

        double downBefore = velBefore.dot(down);
        double downAfter = velAfter.dot(down);
        double deltaDown = downBefore - downAfter;
        return Math.max(0.0, deltaDown);
    }

    /**
     * Calculates impact damage from normal impact speed.
     * Returns 0.0 if impact speed is at or below safeImpactSpeed.
     */
    public float calculateDamage(double impactSpeed) {
        if (impactSpeed <= safeImpactSpeed) {
            return 0.0f;
        }
        double excess = impactSpeed - safeImpactSpeed;
        return (float) (excess * damageScale);
    }
}
