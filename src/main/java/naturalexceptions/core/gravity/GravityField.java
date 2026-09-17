package naturalexceptions.core.gravity;

/**
 * Interface representing a spatial field capable of computing local gravitational acceleration
 * at arbitrary coordinates.
 */
public interface GravityField {

    /**
     * Evaluates the gravitational acceleration at point (x, y, z).
     */
    GravitySample sampleAt(double x, double y, double z);

    /**
     * Convenience method to evaluate gravity at a Vector3d position.
     */
    default GravitySample sampleAt(Vector3d position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        return sampleAt(position.x, position.y, position.z);
    }
}
