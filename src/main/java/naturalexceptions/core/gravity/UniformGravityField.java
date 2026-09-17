package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * A gravitational field with uniform direction and magnitude across all space.
 */
public final class UniformGravityField implements GravityField {

    public static final UniformGravityField STANDARD_EARTH = new UniformGravityField(new Vector3d(0, -9.81, 0));
    public static final UniformGravityField STANDARD_MINECRAFT = new UniformGravityField(new Vector3d(0, -0.08, 0));
    public static final UniformGravityField ZERO = new UniformGravityField(Vector3d.ZERO);

    private final Vector3d acceleration;
    private final GravitySample sample;

    public UniformGravityField(Vector3d acceleration) {
        this.acceleration = Objects.requireNonNull(acceleration, "acceleration");
        this.sample = new GravitySample(acceleration);
    }

    public static UniformGravityField of(double gx, double gy, double gz) {
        return new UniformGravityField(new Vector3d(gx, gy, gz));
    }

    public static UniformGravityField downward(double magnitude) {
        return new UniformGravityField(new Vector3d(0, -Math.abs(magnitude), 0));
    }

    public static UniformGravityField sideways(double gx, double gy, double gz) {
        return new UniformGravityField(new Vector3d(gx, gy, gz));
    }

    public Vector3d acceleration() {
        return acceleration;
    }

    @Override
    public GravitySample sampleAt(double x, double y, double z) {
        return sample;
    }

    @Override
    public String toString() {
        return "UniformGravityField{" + acceleration + "}";
    }
}
