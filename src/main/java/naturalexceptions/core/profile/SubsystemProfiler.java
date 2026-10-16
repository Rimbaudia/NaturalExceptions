package naturalexceptions.core.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

public final class SubsystemProfiler {

    private final Map<ResourceId, State> subsystems = new LinkedHashMap<>();

    private static final class State {

        final Map<String, Long> counters = new HashMap<>();
        final Map<String, Long> accumulatedNanos = new HashMap<>();
        final Map<String, Long> invocations = new HashMap<>();
        final Map<String, Long> maxNanos = new HashMap<>();
        long activeCount;
        long sleepingCount;
        long queuedWorkCount;
    }

    private State getOrCreateState(ResourceId subsystem) {
        Objects.requireNonNull(subsystem, "subsystem");
        return subsystems.computeIfAbsent(subsystem, k -> new State());
    }

    public synchronized void incrementCounter(ResourceId subsystem, String counter, long delta) {
        if (counter == null || counter.trim()
            .isEmpty()) {
            return;
        }
        State s = getOrCreateState(subsystem);
        s.counters.put(counter, s.counters.getOrDefault(counter, 0L) + delta);
    }

    public synchronized void recordElapsedNanos(ResourceId subsystem, String operation, long elapsedNanos) {
        if (operation == null || operation.trim()
            .isEmpty() || elapsedNanos < 0) {
            return;
        }
        State s = getOrCreateState(subsystem);
        s.accumulatedNanos.put(operation, s.accumulatedNanos.getOrDefault(operation, 0L) + elapsedNanos);
        s.invocations.put(operation, s.invocations.getOrDefault(operation, 0L) + 1);
        s.maxNanos.put(operation, Math.max(s.maxNanos.getOrDefault(operation, 0L), elapsedNanos));
    }

    public synchronized void setActiveCount(ResourceId subsystem, long active) {
        getOrCreateState(subsystem).activeCount = Math.max(0, active);
    }

    public synchronized void setSleepingCount(ResourceId subsystem, long sleeping) {
        getOrCreateState(subsystem).sleepingCount = Math.max(0, sleeping);
    }

    public synchronized void setQueuedWork(ResourceId subsystem, long queued) {
        getOrCreateState(subsystem).queuedWorkCount = Math.max(0, queued);
    }

    public synchronized SubsystemMetrics snapshot(ResourceId subsystem) {
        Objects.requireNonNull(subsystem, "subsystem");
        State s = subsystems.get(subsystem);
        if (s == null) {
            return new SubsystemMetrics(
                subsystem,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                0L,
                0L,
                0L);
        }
        return new SubsystemMetrics(
            subsystem,
            new HashMap<>(s.counters),
            new HashMap<>(s.accumulatedNanos),
            new HashMap<>(s.invocations),
            new HashMap<>(s.maxNanos),
            s.activeCount,
            s.sleepingCount,
            s.queuedWorkCount);
    }

    public synchronized Map<ResourceId, SubsystemMetrics> snapshotAll() {
        Map<ResourceId, SubsystemMetrics> result = new LinkedHashMap<>();
        for (ResourceId id : subsystems.keySet()) {
            result.put(id, snapshot(id));
        }
        return Collections.unmodifiableMap(result);
    }

    public synchronized void reset() {
        subsystems.clear();
    }
}
