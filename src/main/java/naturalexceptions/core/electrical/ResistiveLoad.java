package naturalexceptions.core.electrical;

import java.util.Objects;

import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.thermal.ActiveThermalState;

/** Single resistive load with a finite thermal body. */
public final class ResistiveLoad {
    private final String stableId;
    private double resistanceOhms;
    private final ActiveThermalState thermalState;
    private final ComponentDamage damage;
    private final double maximumServiceTemperatureKelvin;
    private Runnable changeListener;
    public ResistiveLoad(String stableId, double resistanceOhms, ActiveThermalState thermalState,
        ComponentDamage damage, double maximumServiceTemperatureKelvin) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Load ID required");
        this.stableId = stableId; this.thermalState = Objects.requireNonNull(thermalState, "thermalState");
        this.damage = Objects.requireNonNull(damage, "damage");
        if (!Double.isFinite(maximumServiceTemperatureKelvin) || maximumServiceTemperatureKelvin <= 0.0)
            throw new IllegalArgumentException("Load limit must be positive and finite");
        this.maximumServiceTemperatureKelvin = maximumServiceTemperatureKelvin;
        setResistanceInternal(resistanceOhms);
    }
    public String stableId() { return stableId; }
    public double resistanceOhms() { return resistanceOhms; }
    public ActiveThermalState thermalState() { return thermalState; }
    public ComponentDamage damage() { return damage; }
    public double maximumServiceTemperatureKelvin() { return maximumServiceTemperatureKelvin; }
    public boolean connected() { return !damage.isFractured(); }
    private void setResistanceInternal(double value) {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException("Load resistance must be positive and finite");
        resistanceOhms = value;
    }
    void attach(Runnable listener) { changeListener = listener; }
    public void setResistanceOhms(double value) {
        if (Double.compare(resistanceOhms, value) == 0) return;
        setResistanceInternal(value);
        if (changeListener != null) changeListener.run();
    }
    boolean deposit(double joules) {
        thermalState.addEnergyJoules(joules);
        if (!damage.isFractured() && thermalState.temperatureKelvin() >= maximumServiceTemperatureKelvin) {
            damage.fracture(); return true;
        }
        return false;
    }
}
