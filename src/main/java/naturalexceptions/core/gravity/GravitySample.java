package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Immutable evaluation of the local gravitational field at a specific coordinate in space.
 * Provides the acceleration vector, directional bases, and owning source identity for local-frame selection.
 */
public final class GravitySample {

    private final Vector3d acceleration;
    private final GravitySourceId sourceId;

    public GravitySample(Vector3d acceleration, GravitySourceId sourceId) {
        this.acceleration = Objects.requireNonNull(acceleration, "acceleration");
        this.sourceId = sourceId != null ? sourceId : GravitySourceId.DEFAULT_MINECRAFT;
    }

    public GravitySample(Vector3d acceleration) {
        this(
            acceleration,
            acceleration.lengthSquared() < 1e-12 ? GravitySourceId.ZERO_G : GravitySourceId.DEFAULT_MINECRAFT);
    }

    public static GravitySample of(Vector3d acceleration) {
        return new GravitySample(acceleration);
    }

    public static GravitySample of(Vector3d acceleration, GravitySourceId sourceId) {
        return new GravitySample(acceleration, sourceId);
    }

    public static GravitySample zero() {
        return new GravitySample(Vector3d.ZERO, GravitySourceId.ZERO_G);
    }

    public static GravitySample standardDownward(double g) {
        return standardDownward(g, GravitySourceId.DEFAULT_MINECRAFT);
    }

    public static GravitySample standardDownward(double g, GravitySourceId sourceId) {
        return new GravitySample(new Vector3d(0, -Math.abs(g), 0), sourceId);
    }

    public Vector3d acceleration() {
        return acceleration;
    }

    public GravitySourceId sourceId() {
        return sourceId;
    }

    public double magnitude() {
        return acceleration.length();
    }

    /**
     * The direction of local gravity ("down"). If magnitude is zero, defaults to global down (0, -1, 0).
     */
    public Vector3d direction() {
        double len = acceleration.length();
        if (len < 1e-12) {
            return Vector3d.DOWN;
        }
        return acceleration.normalize();
    }

    public Vector3d localDown() {
        return direction();
    }

    public Vector3d localUp() {
        return direction().negate();
    }

    public boolean isZero() {
        return acceleration.lengthSquared() < 1e-12;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GravitySample)) return false;
        GravitySample that = (GravitySample) o;
        return acceleration.equals(that.acceleration) && sourceId.equals(that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acceleration, sourceId);
    }

    @Override
    public String toString() {
        return "GravitySample{acceleration=" + acceleration
            + ", magnitude="
            + magnitude()
            + ", sourceId="
            + sourceId
            + "}";
    }
}
