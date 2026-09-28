package naturalexceptions.core.electrical;

import java.util.Objects;

import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/** Finite conductor with explicit SI geometry and a physical damage/thermal state. */
public final class ResistiveConductor {
    private final String stableId;
    private final MaterialRef materialRef;
    private MaterialProperties materialProperties;
    private final double lengthMeters;
    private final double crossSectionSquareMeters;
    private final ActiveThermalState thermalState;
    private final ComponentDamage damage;
    private final double maximumServiceTemperatureKelvin;
    private Runnable topologyChange;

    public ResistiveConductor(String stableId, MaterialRef materialRef, MaterialProperties materialProperties,
        double lengthMeters, double crossSectionSquareMeters, ActiveThermalState thermalState, ComponentDamage damage,
        double maximumServiceTemperatureKelvin) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Conductor ID required");
        this.stableId = stableId;
        this.materialRef = Objects.requireNonNull(materialRef, "materialRef");
        this.materialProperties = Objects.requireNonNull(materialProperties, "materialProperties");
        if (!Double.isFinite(lengthMeters) || lengthMeters <= 0.0 || !Double.isFinite(crossSectionSquareMeters)
            || crossSectionSquareMeters <= 0.0) throw new IllegalArgumentException("Conductor geometry must be positive and finite");
        if (!Double.isFinite(maximumServiceTemperatureKelvin) || maximumServiceTemperatureKelvin <= 0.0)
            throw new IllegalArgumentException("Conductor limit must be positive and finite");
        this.lengthMeters = lengthMeters; this.crossSectionSquareMeters = crossSectionSquareMeters;
        this.thermalState = Objects.requireNonNull(thermalState, "thermalState");
        this.damage = Objects.requireNonNull(damage, "damage");
        this.maximumServiceTemperatureKelvin = maximumServiceTemperatureKelvin;
        validatedResistivity();
    }
    public String stableId() { return stableId; }
    public MaterialRef materialRef() { return materialRef; }
    public double lengthMeters() { return lengthMeters; }
    public double crossSectionSquareMeters() { return crossSectionSquareMeters; }
    public MaterialProperties materialProperties() { return materialProperties; }
    public ActiveThermalState thermalState() { return thermalState; }
    public ComponentDamage damage() { return damage; }
    public double maximumServiceTemperatureKelvin() { return maximumServiceTemperatureKelvin; }
    public boolean connected() { return !damage.isFractured(); }
    public double resistanceOhms() {
        double resistance = validatedResistivity() * lengthMeters / crossSectionSquareMeters;
        if (!Double.isFinite(resistance) || resistance <= 0.0) throw new IllegalStateException("Invalid conductor resistance");
        return resistance;
    }
    private double validatedResistivity() {
        double rho = materialProperties.electricalResistivity();
        if (!Double.isFinite(rho) || rho <= 0.0) throw new IllegalArgumentException("Resistivity must be positive and finite");
        return rho;
    }
    void attach(Runnable listener) { topologyChange = listener; }
    public void setMaterialProperties(MaterialProperties properties) {
        MaterialProperties validated = Objects.requireNonNull(properties, "properties");
        double resistivity = validated.electricalResistivity();
        if (!Double.isFinite(resistivity) || resistivity <= 0.0)
            throw new IllegalArgumentException("Resistivity must be positive and finite");
        this.materialProperties = validated;
        if (topologyChange != null) topologyChange.run();
    }
    boolean deposit(double joules) {
        thermalState.addEnergyJoules(joules);
        if (!damage.isFractured() && thermalState.temperatureKelvin() >= maximumServiceTemperatureKelvin) {
            damage.fracture();
            if (topologyChange != null) topologyChange.run();
            return true;
        }
        return false;
    }
    public void fracture() {
        if (!damage.isFractured()) { damage.fracture(); if (topologyChange != null) topologyChange.run(); }
    }
}
