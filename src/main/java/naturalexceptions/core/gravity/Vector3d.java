package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Immutable 3D vector for pure core spatial and physical calculations.
 * Completely decoupled from any Minecraft or rendering libraries.
 */
public final class Vector3d {

    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d UP = new Vector3d(0, 1, 0);
    public static final Vector3d DOWN = new Vector3d(0, -1, 0);

    public final double x;
    public final double y;
    public final double z;

    public Vector3d(double x, double y, double z) {
        this.x = (x == 0.0) ? 0.0 : x;
        this.y = (y == 0.0) ? 0.0 : y;
        this.z = (z == 0.0) ? 0.0 : z;
    }

    public static Vector3d of(double x, double y, double z) {
        if (x == 0.0 && y == 0.0 && z == 0.0) {
            return ZERO;
        }
        return new Vector3d(x, y, z);
    }

    public Vector3d add(Vector3d other) {
        return new Vector3d(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vector3d add(double dx, double dy, double dz) {
        return new Vector3d(this.x + dx, this.y + dy, this.z + dz);
    }

    public Vector3d subtract(Vector3d other) {
        return new Vector3d(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3d subtract(double dx, double dy, double dz) {
        return new Vector3d(this.x - dx, this.y - dy, this.z - dz);
    }

    public Vector3d multiply(double scalar) {
        return new Vector3d(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public double dot(Vector3d other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vector3d cross(Vector3d other) {
        return new Vector3d(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vector3d normalize() {
        double len = length();
        if (len < 1e-12) {
            return ZERO;
        }
        return new Vector3d(x / len, y / len, z / len);
    }

    public Vector3d negate() {
        return new Vector3d(-x, -y, -z);
    }

    public double distanceSquared(Vector3d other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(Vector3d other) {
        return Math.sqrt(distanceSquared(other));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector3d)) return false;
        Vector3d other = (Vector3d) o;
        return Math.abs(this.x - other.x) < 1e-11 && Math.abs(this.y - other.y) < 1e-11
            && Math.abs(this.z - other.z) < 1e-11;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Math.round(x * 1e8), Math.round(y * 1e8), Math.round(z * 1e8));
    }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f, %.4f)", x, y, z);
    }
}
