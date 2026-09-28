package naturalexceptions.core.electrical;

/** Finite derived values from a DC solve; resistance is finite path resistance even when the switch is open. */
public final class CircuitSolution {
    private final double voltageVolts, currentAmps, totalResistanceOhms, loadPowerWatts, conductorPowerWatts;
    private final boolean openCircuit;
    CircuitSolution(double voltage, double current, double resistance, double loadPower, double conductorPower,
        boolean openCircuit) {
        this.voltageVolts = voltage; this.currentAmps = current; this.totalResistanceOhms = resistance;
        this.loadPowerWatts = loadPower; this.conductorPowerWatts = conductorPower; this.openCircuit = openCircuit;
    }
    public double voltageVolts() { return voltageVolts; }
    public double currentAmps() { return currentAmps; }
    public double totalResistanceOhms() { return totalResistanceOhms; }
    public double loadPowerWatts() { return loadPowerWatts; }
    public double conductorPowerWatts() { return conductorPowerWatts; }
    public boolean openCircuit() { return openCircuit; }
}
