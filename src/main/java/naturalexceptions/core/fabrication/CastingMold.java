package naturalexceptions.core.fabrication;

import java.util.Objects;

import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/**
 * Material-agnostic mold for casting physical components from molten metallurgical layers.
 *
 * <p>
 * Core invariants:
 * <ul>
 * <li>Does NOT whitelist materials: any physically compatible liquid material can fill the mold.</li>
 * <li>Capacity is determined dynamically: required mass = cavity volume × material density.</li>
 * <li>Partial fills remain partial; incomplete castings cannot be extracted.</li>
 * <li>Solidification is driven by P3 thermodynamics (cooling to T &lt; T_melt without arbitrary crafting timers).</li>
 * </ul>
 */
public final class CastingMold {

    private final ComponentGeometry geometry;
    private MaterialRef materialRef;
    private CanonicalMaterialKey stateKey;
    private MaterialProperties properties;
    private MaterialQuantity filledMass = MaterialQuantity.ZERO;
    private ActiveThermalState thermalState;

    public CastingMold(ComponentGeometry geometry) {
        this.geometry = Objects.requireNonNull(geometry, "geometry");
    }

    public ComponentGeometry geometry() {
        return geometry;
    }

    public MaterialRef materialRef() {
        return materialRef;
    }

    public CanonicalMaterialKey stateKey() {
        return stateKey;
    }

    public MaterialQuantity filledMass() {
        return filledMass;
    }

    public ActiveThermalState thermalState() {
        return thermalState;
    }

    public MaterialQuantity requiredMass() {
        if (properties == null) {
            // Default 5.0 g/cm³ if empty
            return geometry.requiredMass(5.0);
        }
        return geometry.requiredMass(properties);
    }

    public boolean isFilled() {
        return properties != null && filledMass.compareTo(requiredMass()) >= 0;
    }

    public boolean isSolid() {
        return isFilled() && thermalState != null && thermalState.phase() == MaterialPhase.SOLID;
    }

    /**
     * Pours molten material from a vessel layer into this mold.
     */
    public MaterialQuantity pour(AggregateLayer moltenLayer, MaterialProperties props) {
        Objects.requireNonNull(moltenLayer, "moltenLayer");
        Objects.requireNonNull(props, "props");

        if (this.properties == null) {
            this.materialRef = moltenLayer.materialRef();
            this.stateKey = moltenLayer.stateKey();
            this.properties = props;
        } else if (!this.materialRef.equals(moltenLayer.materialRef())) {
            throw new IllegalArgumentException(
                "Cannot mix different materials inside the same mold: " + this.materialRef
                    + " vs "
                    + moltenLayer.materialRef());
        }

        MaterialQuantity needed = requiredMass().subtract(filledMass);
        if (needed.isZero()) {
            return MaterialQuantity.ZERO;
        }

        MaterialQuantity transfer = moltenLayer.mass()
            .compareTo(needed) <= 0 ? moltenLayer.mass() : needed;
        ActiveThermalState extractedThermal = moltenLayer.extractMass(transfer);

        if (this.thermalState == null) {
            this.thermalState = extractedThermal;
        } else {
            this.thermalState.setMassKg(
                this.filledMass.add(transfer)
                    .toKilograms());
            this.thermalState.addEnergyJoules(extractedThermal.energyJoules());
        }

        this.filledMass = this.filledMass.add(transfer);
        return transfer;
    }

    /**
     * Cools mold contents toward ambient temperature over time.
     */
    public void cool(double deltaSeconds, double ambientTempK) {
        if (thermalState == null || deltaSeconds <= 0.0) return;

        double temp = thermalState.temperatureKelvin();
        if (temp > ambientTempK) {
            double h = 25.0; // W / (m² · K) cooling coefficient in air
            double area = 0.05; // ~0.05 m² surface area
            double qJoules = h * area * (temp - ambientTempK) * deltaSeconds;
            thermalState.removeEnergyJoules(qJoules);
        }
    }

    /**
     * Extracts the solidified component from the mold.
     */
    public ComponentInstance extractComponent() {
        if (!isFilled()) {
            throw new IllegalStateException(
                "Cannot extract component from an incompletely filled mold: " + filledMass + " / " + requiredMass());
        }
        if (!isSolid()) {
            throw new IllegalStateException(
                "Cannot extract component while still molten or mushy (temperature=" + thermalState.temperatureKelvin()
                    + " K)");
        }

        ComponentInstance instance = new ComponentInstance(
            geometry.id(),
            materialRef,
            stateKey.asSolidified(),
            filledMass,
            ManufacturingState.AS_CAST);

        // Reset mold
        this.materialRef = null;
        this.stateKey = null;
        this.properties = null;
        this.filledMass = MaterialQuantity.ZERO;
        this.thermalState = null;

        return instance;
    }
}
