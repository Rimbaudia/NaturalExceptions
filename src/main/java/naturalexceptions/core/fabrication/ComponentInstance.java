package naturalexceptions.core.fabrication;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/**
 * Concrete physical instance of a manufactured component.
 *
 * <p>
 * Retains exact material identity, geometry shape, conserved mass, manufacturing history, and accumulated physical
 * damage.
 */
public final class ComponentInstance {

    private final DeterministicId instanceId;
    private GeometryId geometryId;
    private final MaterialRef materialRef;
    private CanonicalMaterialKey stateKey;
    private MaterialQuantity mass;
    private ManufacturingState manufacturingState;
    private final ComponentDamage damage;

    public ComponentInstance(GeometryId geometryId, MaterialRef materialRef, CanonicalMaterialKey stateKey,
        MaterialQuantity mass, ManufacturingState manufacturingState) {
        this(
            DeterministicId.derive("comp", geometryId.id(), materialRef.asString(), Long.toString(mass.milligrams())),
            geometryId,
            materialRef,
            stateKey,
            mass,
            manufacturingState,
            new ComponentDamage());
    }

    public ComponentInstance(DeterministicId instanceId, GeometryId geometryId, MaterialRef materialRef,
        CanonicalMaterialKey stateKey, MaterialQuantity mass, ManufacturingState manufacturingState,
        ComponentDamage damage) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.geometryId = Objects.requireNonNull(geometryId, "geometryId");
        this.materialRef = Objects.requireNonNull(materialRef, "materialRef");
        this.stateKey = Objects.requireNonNull(stateKey, "stateKey");
        this.mass = Objects.requireNonNull(mass, "mass");
        this.manufacturingState = Objects.requireNonNull(manufacturingState, "manufacturingState");
        this.damage = Objects.requireNonNull(damage, "damage");
    }

    public DeterministicId instanceId() {
        return instanceId;
    }

    public GeometryId geometryId() {
        return geometryId;
    }

    public MaterialRef materialRef() {
        return materialRef;
    }

    public CanonicalMaterialKey stateKey() {
        return stateKey;
    }

    public MaterialQuantity mass() {
        return mass;
    }

    public ManufacturingState manufacturingState() {
        return manufacturingState;
    }

    public ComponentDamage damage() {
        return damage;
    }

    /**
     * Performs a working operation that transforms the geometry and manufacturing history while strictly conserving
     * mass.
     */
    public void work(GeometryId targetGeometry, ManufacturingState nextState) {
        Objects.requireNonNull(targetGeometry, "targetGeometry");
        Objects.requireNonNull(nextState, "nextState");
        this.geometryId = targetGeometry;
        this.manufacturingState = nextState;
    }

    public void setManufacturingState(ManufacturingState state) {
        this.manufacturingState = Objects.requireNonNull(state, "state");
    }

    public void setStateKey(CanonicalMaterialKey stateKey) {
        this.stateKey = Objects.requireNonNull(stateKey, "stateKey");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentInstance)) return false;
        ComponentInstance that = (ComponentInstance) o;
        return instanceId.equals(that.instanceId);
    }

    @Override
    public int hashCode() {
        return instanceId.hashCode();
    }

    @Override
    public String toString() {
        return geometryId.id() + "[" + materialRef + ", " + mass + ", " + manufacturingState.id() + "]";
    }
}
