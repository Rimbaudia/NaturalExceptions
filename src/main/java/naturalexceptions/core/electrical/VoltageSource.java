package naturalexceptions.core.electrical;

/** Ideal primitive DC source with a fixed open-circuit voltage. */
public final class VoltageSource {
    private final String stableId;
    private double voltageVolts;
    private Runnable changeListener;
    public VoltageSource(String stableId, double voltageVolts) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Source ID required");
        this.stableId = stableId;
        setVoltageVoltsInternal(voltageVolts);
    }
    public String stableId() { return stableId; }
    public double voltageVolts() { return voltageVolts; }
    public void setVoltageVolts(double value) {
        setVoltageVoltsInternal(value);
        if (changeListener != null) changeListener.run();
    }
    private void setVoltageVoltsInternal(double value) {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException("Voltage must be finite and nonnegative");
        voltageVolts = value;
    }
    void attach(Runnable listener) { changeListener = listener; }
}
