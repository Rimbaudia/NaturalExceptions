package naturalexceptions.core.worldgen;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;

/** A deterministic compact copper seam authored by one coarse deposit cell. */
public final class CopperDeposit {

    private final DeterministicId id;
    private final GeologicalProvince hostProvince;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final double directionX;
    private final double directionZ;
    private final double halfLength;
    private final double radius;

    CopperDeposit(DeterministicId id, GeologicalProvince hostProvince, int centerX, int centerY, int centerZ,
        double directionX, double directionZ, double halfLength, double radius) {
        this.id = Objects.requireNonNull(id, "id");
        this.hostProvince = Objects.requireNonNull(hostProvince, "hostProvince");
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.directionX = directionX;
        this.directionZ = directionZ;
        this.halfLength = halfLength;
        this.radius = radius;
    }

    public DeterministicId id() {
        return id;
    }

    public GeologicalProvince hostProvince() {
        return hostProvince;
    }

    public int centerX() {
        return centerX;
    }

    public int centerY() {
        return centerY;
    }

    public int centerZ() {
        return centerZ;
    }

    public int horizontalExtent() {
        return (int) Math.ceil(halfLength + radius);
    }

    public int verticalExtent() {
        return (int) Math.ceil(radius / 1.65);
    }

    public boolean contains(int x, int y, int z) {
        double along = (x - centerX) * directionX + (z - centerZ) * directionZ;
        along = Math.max(-halfLength, Math.min(halfLength, along));
        double nearestX = centerX + directionX * along;
        double nearestZ = centerZ + directionZ * along;
        double dx = x - nearestX;
        double dz = z - nearestZ;
        double dy = (y - centerY) * 1.65;
        return dx * dx + dz * dz + dy * dy <= radius * radius;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CopperDeposit)) return false;
        CopperDeposit that = (CopperDeposit) other;
        return id.equals(that.id) && hostProvince == that.hostProvince
            && centerX == that.centerX
            && centerY == that.centerY
            && centerZ == that.centerZ
            && Double.compare(directionX, that.directionX) == 0
            && Double.compare(directionZ, that.directionZ) == 0
            && Double.compare(halfLength, that.halfLength) == 0
            && Double.compare(radius, that.radius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostProvince, centerX, centerY, centerZ, directionX, directionZ, halfLength, radius);
    }
}
