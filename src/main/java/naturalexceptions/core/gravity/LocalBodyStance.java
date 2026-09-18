package naturalexceptions.core.gravity;

import java.util.Objects;

/**
 * Geometric model of an entity's local stance in arbitrary gravity.
 *
 * <p>
 * Defines conceptual body geometry for arbitrary stance:
 * <ul>
 * <li>{@code bodyCenter}: Geometric center of the standing body in the local frame</li>
 * <li>{@code localDown} / {@code localUp}: Directional gravity vectors</li>
 * <li>{@code halfHeight}: Half-height of the body along localUp (0.9 for standard player)</li>
 * <li>{@code eyeOffsetFromCenter}: Eye offset from bodyCenter along localUp (0.72 for player)</li>
 * <li>{@code supportPoint}: {@code bodyCenter + localDown * halfHeight} (feet contacting support)</li>
 * <li>{@code headPoint}: {@code bodyCenter + localUp * halfHeight} (top of head)</li>
 * <li>{@code eyePoint}: {@code bodyCenter + localUp * eyeOffsetFromCenter} (local eye origin)</li>
 * </ul>
 *
 * <p>
 * This is a pure core domain model with zero Minecraft imports.
 */
public final class LocalBodyStance {

    public static final double DEFAULT_PLAYER_HALF_HEIGHT = 0.9;
    public static final double DEFAULT_PLAYER_HALF_WIDTH = 0.3;
    public static final double DEFAULT_PLAYER_EYE_OFFSET = 0.72; // eyeHeight (1.62) - halfHeight (0.9)

    private final Vector3d center;
    private final Vector3d localDown;
    private final Vector3d localUp;
    private final double halfWidth;
    private final double halfHeight;
    private final double eyeOffsetFromCenter;
    private final Vector3d supportPoint;
    private final Vector3d headPoint;
    private final Vector3d eyePoint;

    public LocalBodyStance(Vector3d center, Vector3d localDown, double halfWidth, double halfHeight,
        double eyeOffsetFromCenter) {
        this.center = center != null ? center : Vector3d.ZERO;
        Vector3d d = localDown != null && localDown.lengthSquared() > 1e-6 ? localDown.normalize() : Vector3d.DOWN;
        this.localDown = d;
        this.localUp = d.negate();
        this.halfWidth = Math.max(1e-4, halfWidth);
        this.halfHeight = Math.max(1e-4, halfHeight);
        this.eyeOffsetFromCenter = eyeOffsetFromCenter;

        // Support point on the boundary of the AABB along localDown
        this.supportPoint = new Vector3d(
            this.center.x + this.localDown.x * this.halfWidth,
            this.center.y + this.localDown.y * this.halfHeight,
            this.center.z + this.localDown.z * this.halfWidth);

        // Head point on the boundary of the AABB along localUp
        this.headPoint = new Vector3d(
            this.center.x + this.localUp.x * this.halfWidth,
            this.center.y + this.localUp.y * this.halfHeight,
            this.center.z + this.localUp.z * this.halfWidth);

        // Eye point is located eyeHeight (halfHeight + eyeOffsetFromCenter) along localUp from supportPoint
        double eyeHeight = this.halfHeight + this.eyeOffsetFromCenter;
        this.eyePoint = this.supportPoint.add(this.localUp.multiply(eyeHeight));
    }

    public LocalBodyStance(Vector3d center, Vector3d localDown, double halfWidth, double halfHeight) {
        this(center, localDown, halfWidth, halfHeight, DEFAULT_PLAYER_EYE_OFFSET);
    }

    /**
     * Constructs a stance from an existing collision bounding box center, anchoring
     * the feet on the contact surface along localDown.
     */
    public static LocalBodyStance fromAabb(Vector3d aabbCenter, Vector3d localDown, double halfWidth, double halfHeight,
        double eyeHeight) {
        double eyeOffset = eyeHeight - halfHeight;
        return new LocalBodyStance(aabbCenter, localDown, halfWidth, halfHeight, eyeOffset);
    }

    public Vector3d bodyCenter() {
        return center;
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

    public double eyeOffsetFromCenter() {
        return eyeOffsetFromCenter;
    }

    /**
     * Center of the body's support footprint (feet) contacting the ground/wall.
     */
    public Vector3d supportPoint() {
        return supportPoint;
    }

    /**
     * Center of the body's head, farthest from the support surface along localUp.
     */
    public Vector3d headPoint() {
        return headPoint;
    }

    /**
     * Gravity-relative eye position: supportPoint + localUp * (halfHeight + eyeOffsetFromCenter).
     */
    public Vector3d eyePoint() {
        return eyePoint;
    }

    /**
     * Camera eye translation delta needed to convert from entity render origin (posX, posY, posZ)
     * to the gravity-relative eyePoint: deltaEye = entityPos - eyePoint.
     */
    public Vector3d cameraEyeDelta(Vector3d entityPos) {
        return entityPos.subtract(eyePoint);
    }

    /**
     * Translation offset from entity base position (posX, posY, posZ) to supportPoint.
     * In vanilla coordinates: base = (C.x, C.y - halfHeight, C.z).
     * supportSurfaceOffset = supportPoint - base = (d_x * halfWidth, halfHeight + d_y * halfHeight, d_z * halfWidth).
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
            && Double.compare(that.eyeOffsetFromCenter, eyeOffsetFromCenter) == 0
            && Objects.equals(center, that.center)
            && Objects.equals(localDown, that.localDown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, localDown, halfWidth, halfHeight, eyeOffsetFromCenter);
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
            + ", eye="
            + eyePoint()
            + ", head="
            + headPoint()
            + '}';
    }
}
