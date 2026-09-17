package naturalexceptions.core.gravity;

/**
 * Interface representing a spatial field capable of computing local gravitational acceleration
 * and source identity at arbitrary coordinates.
 */
public interface GravityField {

    /**
     * Evaluates the gravitational acceleration and source identity at point (x, y, z)
     * with query context for hysteresis/anchoring.
     */
    GravitySample sampleAt(double x, double y, double z, GravityQueryContext context);

    /**
     * Default overload without query context.
     */
    default GravitySample sampleAt(double x, double y, double z) {
        return sampleAt(x, y, z, GravityQueryContext.empty());
    }

    /**
     * Convenience method to evaluate gravity at a Vector3d position.
     */
    default GravitySample sampleAt(Vector3d position, GravityQueryContext context) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        return sampleAt(position.x, position.y, position.z, context);
    }

    default GravitySample sampleAt(Vector3d position) {
        return sampleAt(position, GravityQueryContext.empty());
    }
}
