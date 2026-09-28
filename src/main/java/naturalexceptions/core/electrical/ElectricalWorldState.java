package naturalexceptions.core.electrical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** World-owned dirty circuit queue plus a set containing only genuinely powered, time-dependent networks. */
public final class ElectricalWorldState {
    private final Map<String, CircuitNetwork> networks = new LinkedHashMap<>();
    private final LinkedHashSet<String> dirty = new LinkedHashSet<>();
    private final LinkedHashSet<String> active = new LinkedHashSet<>();
    private final LinkedHashSet<String> sleeping = new LinkedHashSet<>();
    private long solveWork;
    private long thermalWork;

    public synchronized void register(CircuitNetwork network) {
        if (network == null) throw new IllegalArgumentException("Circuit network required");
        if (networks.containsKey(network.stableId())) throw new IllegalArgumentException("Duplicate circuit ID");
        networks.put(network.stableId(), network);
        network.attachDirtyListener(() -> markDirty(network.stableId()));
        markDirty(network.stableId());
    }

    public synchronized CircuitNetwork network(String stableId) { return networks.get(stableId); }
    public synchronized List<CircuitNetwork> networks() {
        return Collections.unmodifiableList(new ArrayList<>(networks.values()));
    }
    public synchronized void markDirty(String stableId) {
        CircuitNetwork network = networks.get(stableId);
        if (network != null) {
            dirty.add(stableId);
            sleeping.remove(stableId);
            if (dirty.size() > networks.size()) throw new IllegalStateException("Dirty network queue is inconsistent");
        }
    }

    /** Solves only changed topology and advances only circuits that currently carry current. */
    public synchronized int processWork(double elapsedSeconds, int budget) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < 0.0) throw new IllegalArgumentException("Invalid elapsed time");
        if (budget <= 0) return 0;
        int work = 0;
        while (work < budget && !dirty.isEmpty()) {
            String id = dirty.iterator().next(); dirty.remove(id);
            CircuitNetwork network = networks.get(id);
            if (network == null) continue;
            CircuitSolution solution = network.solve(); solveWork++; work++;
            if (solution.currentAmps() > 0.0) { active.add(id); sleeping.remove(id); }
            else { active.remove(id); sleeping.add(id); }
        }
        if (elapsedSeconds > 0.0 && work < budget && !active.isEmpty()) {
            List<String> snapshot = new ArrayList<>(active);
            for (String id : snapshot) {
                if (work >= budget) break;
                CircuitNetwork network = networks.get(id);
                if (network == null) { active.remove(id); continue; }
                network.advanceElapsed(elapsedSeconds); thermalWork++; work++;
                if (network.isDirty()) dirty.add(id);
            }
        }
        return work;
    }

    public synchronized int networkCount() { return networks.size(); }
    public synchronized int activeNetworkCount() { return active.size(); }
    public synchronized int sleepingNetworkCount() { return sleeping.size(); }
    public synchronized int dirtyNetworkCount() { return dirty.size(); }
    public synchronized int queuedWorkCount() { return dirty.size() + active.size(); }
    public synchronized long solveWork() { return solveWork; }
    public synchronized long thermalWork() { return thermalWork; }
}
