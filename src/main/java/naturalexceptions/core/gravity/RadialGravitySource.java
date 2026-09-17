package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * A localized point or spherical gravity source attracting objects toward a center coordinate.
 * Uses an interior linear core (avoiding singularities) and inverse-square falloff up to a maximum radius.
 */
public final class RadialGravitySource implements GravityField {

    private final Vector3d center;
    private final double surfaceGravity;
    private final double innerRadius;
    private final double maxRadius;

    public RadialGravitySource(Vector3d center, double surfaceGravity, double innerRadius, double maxRadius) {
        this.center = Objects.requireNonNull(center, "center");
        if (surfaceGravity < 0.0) {
            throw new IllegalArgumentException("surfaceGravity cannot be negative: " + surfaceGravity);
        }
        if (innerRadius <= 0.0) {
            throw new IllegalArgumentException("innerRadius must be positive: " + innerRadius);
        }
        if (maxRadius < innerRadius) {
            throw new IllegalArgumentException(
                "maxRadius (" + maxRadius + ") cannot be less than innerRadius (" + innerRadius + ")");
        }
        this.surfaceGravity = surfaceGravity;
        this.innerRadius = innerRadius;
        this.maxRadius = maxRadius;
    }

    public Vector3d center() {
        return center;
    }

    public double surfaceGravity() {
        return surfaceGravity;
    }

    public double innerRadius() {
        return innerRadius;
    }

    public double maxRadius() {
        return maxRadius;
    }

    @Override
    public GravitySample sampleAt(double x, double y, double z) {
        Vector3d toCenter = center.subtract(x, y, z);
        double distSq = toCenter.lengthSquared();

        // Beyond max radius: zero effect
        if (distSq > maxRadius * maxRadius) {
            return GravitySample.zero();
        }

        double dist = Math.sqrt(distSq);

        // Singularity safeguard at exact center
        if (dist < 1e-9) {
            return GravitySample.zero();
        }

        Vector3d direction = toCenter.multiply(1.0 / dist);
        double magnitude;

        if (dist <= innerRadius) {
            // Linear core interior: smooth transition to center
            magnitude = surfaceGravity * (dist / innerRadius);
        } else {
            // Inverse-square falloff
            double ratio = innerRadius / dist;
            magnitude = surfaceGravity * (ratio * ratio);
        }

        return new GravitySample(direction.multiply(magnitude));
    }

    @Override
    public String toString() {
        return String.format(
            "RadialGravitySource{center=%s, g=%.3f, r0=%.1f, rMax=%.1f}",
            center,
            surfaceGravity,
            innerRadius,
            maxRadius);
    }
}
