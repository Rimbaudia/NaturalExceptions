package naturalexceptions.core.fabrication;

import java.util.Objects;

import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Geometric definition of an individual physical component class.
 *
 * <p>
 * Defines shape dimensions, cavity volume in cm³, and required mass calculations based on material density.
 * Does not whitelist materials: any material with a known density can fill any geometry.
 */
public final class ComponentGeometry {

    private final GeometryId id;
    private final double volumeCm3;

    public ComponentGeometry(GeometryId id, double volumeCm3) {
        this.id = Objects.requireNonNull(id, "id");
        if (volumeCm3 <= 0.0 || Double.isNaN(volumeCm3) || Double.isInfinite(volumeCm3)) {
            throw new IllegalArgumentException("Volume must be strictly positive: " + volumeCm3);
        }
        this.volumeCm3 = volumeCm3;
    }

    public static ComponentGeometry of(GeometryId id, double volumeCm3) {
        return new ComponentGeometry(id, volumeCm3);
    }

    public static ComponentGeometry blank() {
        return of(GeometryId.BLANK, 25.0); // 25 cm³ standard blank
    }

    public static ComponentGeometry ingot() {
        return of(GeometryId.INGOT, 25.0);
    }

    public static ComponentGeometry pickHead() {
        return of(GeometryId.PICK_HEAD, 30.0); // 30 cm³ pick head
    }

    public static ComponentGeometry handle() {
        return of(GeometryId.HANDLE, 150.0); // 150 cm³ handle
    }

    public static ComponentGeometry plate() {
        return of(GeometryId.PLATE, 20.0); // 20 cm³ plate
    }

    public static ComponentGeometry rod() {
        return of(GeometryId.ROD, 15.0); // 15 cm³ rod
    }

    public static ComponentGeometry blade() {
        return of(GeometryId.BLADE, 25.0); // 25 cm³ blade
    }

    public static ComponentGeometry axeHead() {
        return of(GeometryId.AXE_HEAD, 30.0); // 30 cm³ axe head
    }

    public static ComponentGeometry shovelHead() {
        return of(GeometryId.SHOVEL_HEAD, 15.0); // 15 cm³ shovel head
    }

    public static ComponentGeometry hoeHead() {
        return of(GeometryId.HOE_HEAD, 20.0); // 20 cm³ hoe head
    }

    public static ComponentGeometry forId(GeometryId id) {
        if (id == null) return blank();
        switch (id) {
            case PICK_HEAD:
                return pickHead();
            case AXE_HEAD:
                return axeHead();
            case SHOVEL_HEAD:
                return shovelHead();
            case HOE_HEAD:
                return hoeHead();
            case HANDLE:
                return handle();
            case PLATE:
                return plate();
            case ROD:
                return rod();
            case BLADE:
                return blade();
            case INGOT:
                return ingot();
            case BLANK:
            default:
                return blank();
        }
    }

    public GeometryId id() {
        return id;
    }

    public double volumeCm3() {
        return volumeCm3;
    }

    /**
     * Calculates the exact mass of material required to fill this geometry given its physical density.
     */
    public MaterialQuantity requiredMass(double densityGPerCm3) {
        if (densityGPerCm3 <= 0.0) {
            throw new IllegalArgumentException("Density must be positive: " + densityGPerCm3);
        }
        double grams = volumeCm3 * densityGPerCm3;
        return MaterialQuantity.ofGrams(grams);
    }

    public MaterialQuantity requiredMass(MaterialProperties properties) {
        Objects.requireNonNull(properties, "properties");
        return requiredMass(properties.density());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentGeometry)) return false;
        ComponentGeometry that = (ComponentGeometry) o;
        return Double.compare(that.volumeCm3, volumeCm3) == 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, volumeCm3);
    }

    @Override
    public String toString() {
        return id.id() + "(" + volumeCm3 + " cm³)";
    }
}
