package naturalexceptions.core.electrical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Stable series DC graph owner. Mechanical assemblies do not imply electrical edges. */
public final class CircuitNetwork {
    private final String stableId;
    private final VoltageSource source;
    private final SwitchElement switchElement;
    private final ResistiveLoad load;
    private final List<ResistiveConductor> conductors = new ArrayList<>();
    private boolean dirty = true;
    private CircuitSolution cached;
    private long solveCount;
    private Runnable dirtyListener;

    public CircuitNetwork(String stableId, VoltageSource source, SwitchElement switchElement, ResistiveLoad load) {
        if (stableId == null || stableId.trim().isEmpty()) throw new IllegalArgumentException("Network ID required");
        this.stableId = stableId; this.source = Objects.requireNonNull(source, "source");
        this.switchElement = Objects.requireNonNull(switchElement, "switchElement");
        this.load = Objects.requireNonNull(load, "load");
        source.attach(this::markDirty); switchElement.attach(this::markDirty); load.attach(this::markDirty);
    }
    public String stableId() { return stableId; }
    public VoltageSource source() { return source; }
    public SwitchElement switchElement() { return switchElement; }
    public ResistiveLoad load() { return load; }
    public List<ResistiveConductor> conductors() { return Collections.unmodifiableList(conductors); }
    public synchronized boolean isDirty() { return dirty; }
    public synchronized long solveCount() { return solveCount; }
    public synchronized void addConductor(ResistiveConductor conductor) {
        Objects.requireNonNull(conductor, "conductor");
        for (ResistiveConductor existing : conductors) if (existing.stableId().equals(conductor.stableId()))
            throw new IllegalArgumentException("Duplicate conductor ID: " + conductor.stableId());
        conductors.add(conductor); conductor.attach(this::markDirty); markDirty();
    }
    public synchronized void markDirty() {
        dirty = true;
        if (dirtyListener != null) dirtyListener.run();
    }
    synchronized void attachDirtyListener(Runnable listener) {
        dirtyListener = listener;
        if (dirty) dirtyListener.run();
    }

    public synchronized CircuitSolution solve() {
        if (!dirty && cached != null) return cached;
        double conductorResistance = 0.0;
        boolean connected = switchElement.closed() && load.connected();
        for (ResistiveConductor conductor : conductors) {
            conductorResistance += conductor.resistanceOhms();
            if (!conductor.connected()) connected = false;
        }
        double pathResistance = conductorResistance + load.resistanceOhms();
        if (!Double.isFinite(pathResistance) || pathResistance <= 0.0) {
            throw new IllegalStateException("Circuit path resistance must be positive and finite");
        }
        double current = connected ? source.voltageVolts() / pathResistance : 0.0;
        double conductorPower = current * current * conductorResistance;
        double loadPower = current * current * load.resistanceOhms();
        if (!Double.isFinite(current) || !Double.isFinite(conductorPower) || !Double.isFinite(loadPower)) {
            throw new IllegalStateException("Circuit solve produced a non-finite result");
        }
        cached = new CircuitSolution(source.voltageVolts(), current, pathResistance, loadPower, conductorPower,
            !connected);
        dirty = false; solveCount++;
        return cached;
    }

    /** Advances a real powered process by seconds; stable open circuits require no recurring work. */
    public synchronized double advanceElapsed(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) throw new IllegalArgumentException("Elapsed seconds must be finite and nonnegative");
        if (seconds == 0.0) return 0.0;
        CircuitSolution solution = solve();
        if (solution.currentAmps() == 0.0) return 0.0;
        double current = solution.currentAmps();
        double[] conductorEnergy = new double[conductors.size()];
        double totalEnergy = 0.0;
        for (int i = 0; i < conductors.size(); i++) {
            ResistiveConductor conductor = conductors.get(i);
            if (!conductor.connected()) continue;
            double energy = current * current * conductor.resistanceOhms() * seconds;
            if (!Double.isFinite(energy) || !Double.isFinite(conductor.thermalState().energyJoules() + energy))
                throw new IllegalArgumentException("Elapsed interval exceeds representable conductor energy");
            conductorEnergy[i] = energy;
            totalEnergy += energy;
            if (!Double.isFinite(totalEnergy)) throw new IllegalArgumentException("Circuit energy exceeds finite range");
        }
        double loadEnergy = solution.loadPowerWatts() * seconds;
        if (!Double.isFinite(loadEnergy) || !Double.isFinite(load.thermalState().energyJoules() + loadEnergy))
            throw new IllegalArgumentException("Elapsed interval exceeds representable load energy");
        totalEnergy += loadEnergy;
        if (!Double.isFinite(totalEnergy)) throw new IllegalArgumentException("Circuit energy exceeds finite range");
        for (int i = 0; i < conductors.size(); i++) {
            if (conductorEnergy[i] > 0.0) conductors.get(i).deposit(conductorEnergy[i]);
        }
        if (load.deposit(loadEnergy)) markDirty();
        return totalEnergy;
    }
}
