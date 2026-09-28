package naturalexceptions.core.metallurgy;

import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/**
 * Coherent aggregate physical layer inside a metallurgical vessel.
 *
 * <p>
 * Retains exact mass, thermal energy, composition, and phase classification.
 * Mass merges and extractions are strictly conservation-grade.
 */
public final class AggregateLayer {

    private final MaterialRef materialRef;
    private final CanonicalMaterialKey stateKey;
    private final CanonicalComposition composition;
    private MaterialQuantity mass;
    private final ActiveThermalState thermalState;
    private AggregatePhaseType phaseType;

    public AggregateLayer(MaterialRef materialRef, CanonicalMaterialKey stateKey, CanonicalComposition composition,
        MaterialQuantity mass, ActiveThermalState thermalState, AggregatePhaseType phaseType) {
        this.materialRef = Objects.requireNonNull(materialRef, "materialRef");
        this.composition = Objects.requireNonNull(composition, "composition");
        this.stateKey = stateKey != null ? stateKey : CanonicalMaterialKey.of(composition);
        this.mass = Objects.requireNonNull(mass, "mass");
        this.thermalState = Objects.requireNonNull(thermalState, "thermalState");
        this.phaseType = Objects.requireNonNull(phaseType, "phaseType");
    }

    public static AggregateLayer of(MaterialRef ref, CanonicalMaterialKey stateKey, MaterialQuantity mass,
        ActiveThermalState thermal, AggregatePhaseType phaseType) {
        return new AggregateLayer(ref, stateKey, stateKey.composition(), mass, thermal, phaseType);
    }

    public MaterialRef materialRef() {
        return materialRef;
    }

    public CanonicalMaterialKey stateKey() {
        return stateKey;
    }

    public CanonicalComposition composition() {
        return composition;
    }

    public MaterialQuantity mass() {
        return mass;
    }

    public ActiveThermalState thermalState() {
        return thermalState;
    }

    public AggregatePhaseType phaseType() {
        return phaseType;
    }

    public void setPhaseType(AggregatePhaseType phaseType) {
        this.phaseType = Objects.requireNonNull(phaseType, "phaseType");
    }

    public MaterialPhase physicalPhase() {
        return thermalState.phase();
    }

    public double temperatureKelvin() {
        return thermalState.temperatureKelvin();
    }

    public double density() {
        MaterialProperties props = thermalState.properties();
        return props != null ? props.density(physicalPhase()) : 5.0;
    }

    /** Occupied vessel volume in litres (mass kg / density kg per litre). */
    public double occupiedVolumeLitres() {
        double resolvedDensity = density();
        if (!Double.isFinite(resolvedDensity) || resolvedDensity <= 0.0) resolvedDensity = 5.0;
        double litres = mass.toKilograms() / resolvedDensity;
        return Double.isFinite(litres) && litres >= 0.0 ? litres : 0.0;
    }

    /**
     * Conserves energy and mass while merging another layer of identical or compatible material.
     */
    public void mergeFrom(MaterialQuantity additionalMass, ActiveThermalState additionalThermal) {
        Objects.requireNonNull(additionalMass, "additionalMass");
        Objects.requireNonNull(additionalThermal, "additionalThermal");

        this.mass = this.mass.add(additionalMass);
        this.thermalState.setMassKg(this.mass.toKilograms());
        this.thermalState.addEnergyJoules(additionalThermal.energyJoules());
    }

    /**
     * Extracts an exact quantity of mass from this layer, scaling thermal energy proportionally to conserve specific
     * enthalpy.
     */
    public ActiveThermalState extractMass(MaterialQuantity extractedMass) {
        Objects.requireNonNull(extractedMass, "extractedMass");
        if (extractedMass.isZero()) {
            return ActiveThermalState.ambient(0.001, thermalState.properties());
        }
        if (extractedMass.compareTo(mass) > 0) {
            throw new IllegalArgumentException(
                "Cannot extract more mass than layer contains: " + extractedMass + " > " + mass);
        }

        double fraction = (double) extractedMass.milligrams() / (double) mass.milligrams();
        double extractedJoules = thermalState.energyJoules() * fraction;

        this.thermalState.removeEnergyJoules(extractedJoules);
        this.mass = this.mass.subtract(extractedMass);
        if (this.mass.isPositive()) {
            this.thermalState.setMassKg(this.mass.toKilograms());
        }

        return new ActiveThermalState(extractedJoules, extractedMass.toKilograms(), thermalState.properties());
    }

    /** Metal that can flow out of a tap: a molten-metal layer that is at least half liquid by melt fraction. */
    public boolean isPourable() {
        return phaseType() == AggregatePhaseType.MOLTEN_METAL
            && (physicalPhase() == naturalexceptions.core.material.MaterialPhase.LIQUID
                || thermalState().meltFraction() >= 0.50);
    }
}
