package naturalexceptions.core.fabrication;

import java.util.Objects;

/**
 * Metalworking and shaping operations that transform component geometries while conserving matter.
 */
public enum WorkingOperation {

    RESHAPE,
    FLATTEN,
    DRAW,
    BEND,
    FINISH;

    /**
     * Applies this working operation to transform an existing component instance into a target geometry.
     * Conserves 100% of component mass.
     */
    public void apply(ComponentInstance component, GeometryId targetGeometry) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(targetGeometry, "targetGeometry");

        switch (this) {
            case RESHAPE:
            case FLATTEN:
            case DRAW:
            case BEND:
                component.work(targetGeometry, ManufacturingState.WORKED);
                break;
            case FINISH:
                component.work(targetGeometry, ManufacturingState.FINISHED);
                break;
        }
    }
}
