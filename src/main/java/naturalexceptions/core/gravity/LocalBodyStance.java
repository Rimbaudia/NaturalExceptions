package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Geometric model of an entity's local stance in arbitrary gravity.
 *
 * <p>
 * Defines support footprint (feet) and head extent relative to local down/up vectors,
 * calculates the exact translation offset needed to plant feet against support surfaces,
 * and derives the orientation angles from world UP (0, 1, 0) to localUp.
 *
 * <p>
 * This is a pure core domain model with zero Minecraft imports.
 */
public final class LocalBodyStance {

    private final Vector3d center;
    private final Vector3d localDown;
    private final Vector3d localUp;
    private final double halfWidth;
    private final double halfHeight;

    public LocalBodyStance(Vector3d center, Vector3d localDown, double halfWidth, double halfHeight) {
        this.center = center != null ? center : Vector3d.ZERO;
        Vector3d d = localDown != null && localDown.lengthSquared() > 1e-6 ? localDown.normalize() : Vector3d.DOWN;
        this.localDown = d;
        this.localUp = d.negate();
        this.halfWidth = Math.max(1e-4, halfWidth);
        this.halfHeight = Math.max(1e-4, halfHeight);
    }

    public Vector3d center() {
        return center;
    }

    public Vector3d localDown() {
        return localDown;
    }

    public Vector3d localUp() {
        return localUp;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double halfHeight() {
        return halfHeight;
    }

    /**
     * Center of the body's support footprint (feet) contacting the ground/wall.
     */
    public Vector3d supportPoint() {
        return new Vector3d(
            center.x + localDown.x * halfWidth,
            center.y + localDown.y * halfHeight,
            center.z + localDown.z * halfWidth);
    }

    /**
     * Center of the body's head, farthest from the support surface along localUp.
     */
    public Vector3d headPoint() {
        return new Vector3d(
            center.x + localUp.x * halfWidth,
            center.y + localUp.y * halfHeight,
            center.z + localUp.z * halfWidth);
    }

    /**
     * Translation offset from entity base position (posX, posY, posZ) to supportPoint.
     * In vanilla coordinates: base = (C.x, C.y - halfHeight, C.z).
     * supportSurfaceOffset = supportPoint - base = (d_x * halfWidth, halfHeight + d_y * halfHeight, d_z * halfWidth).
     *
     * <p>
     * For vanilla floor (d = (0, -1, 0)): offset is (0, 0, 0).
     * For +X wall (d = (1, 0, 0)): offset is (halfWidth, halfHeight, 0).
     * For -X wall (d = (-1, 0, 0)): offset is (-halfWidth, halfHeight, 0).
     * For ceiling (d = (0, 1, 0)): offset is (0, 2 * halfHeight, 0).
     */
    public Vector3d supportSurfaceOffset() {
        return new Vector3d(localDown.x * halfWidth, halfHeight + localDown.y * halfHeight, localDown.z * halfWidth);
    }

    /**
     * Rotation angle in degrees from world UP (0, 1, 0) to localUp.
     */
    public double rotationFromWorldUpDegrees() {
        double dot = Vector3d.UP.dot(localUp);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    /**
     * Unit rotation axis from world UP (0, 1, 0) to localUp.
     * If localUp is aligned with world UP, returns (0, 1, 0).
     * If localUp is exactly inverted (-world UP), returns (1, 0, 0) as canonical 180-degree flip axis.
     */
    public Vector3d rotationAxis() {
        double dot = Vector3d.UP.dot(localUp);
        if (dot > 0.99999) {
            return Vector3d.UP;
        }
        if (dot < -0.99999) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        return Vector3d.UP.cross(localUp)
            .normalize();
    }

    /**
     * Axis-aligned bounds [minX, minY, minZ, maxX, maxY, maxZ] for probing feet-side contact.
     * The probe box is anchored strictly at the feet side along localDown, extending outward
     * by probeDistance and inward by inwardMargin, while covering the lateral extent of the body
     * across orthogonal axes.
     *
     * <p>
     * Blocks contacting the head or upper body are strictly outside these bounds.
     */
    public double[] feetProbeBounds(double probeDistance, double inwardMargin) {
        Vector3d sup = supportPoint();
        Vector3d probeTip = sup.add(localDown.multiply(probeDistance));
        Vector3d probeBase = sup.subtract(localDown.multiply(inwardMargin));

        double minX = Math.min(probeTip.x, probeBase.x);
        double maxX = Math.max(probeTip.x, probeBase.x);
        double minY = Math.min(probeTip.y, probeBase.y);
        double maxY = Math.max(probeTip.y, probeBase.y);
        double minZ = Math.min(probeTip.z, probeBase.z);
        double maxZ = Math.max(probeTip.z, probeBase.z);

        // Lateral expansion in orthogonal directions
        double latX = (1.0 - Math.abs(localDown.x)) * halfWidth;
        double latY = (1.0 - Math.abs(localDown.y)) * halfHeight;
        double latZ = (1.0 - Math.abs(localDown.z)) * halfWidth;

        return new double[] { minX - latX, minY - latY, minZ - latZ, maxX + latX, maxY + latY, maxZ + latZ };
    }

    /**
     * Derives a {@link LocalEntityFrame} for this stance and heading yaw.
     */
    public LocalEntityFrame toEntityFrame(double headingYawDegrees, GravitySourceId sourceId) {
        GravitySample sample = new GravitySample(
            localDown.multiply(0.08),
            sourceId != null ? sourceId : GravitySourceId.DEFAULT_MINECRAFT);
        return LocalEntityFrame.deriveFrom(sample, headingYawDegrees);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalBodyStance that = (LocalBodyStance) o;
        return Double.compare(that.halfWidth, halfWidth) == 0 && Double.compare(that.halfHeight, halfHeight) == 0
            && Objects.equals(center, that.center)
            && Objects.equals(localDown, that.localDown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, localDown, halfWidth, halfHeight);
    }

    @Override
    public String toString() {
        return "LocalBodyStance{" + "center="
            + center
            + ", localDown="
            + localDown
            + ", localUp="
            + localUp
            + ", support="
            + supportPoint()
            + ", head="
            + headPoint()
            + '}';
    }
}
