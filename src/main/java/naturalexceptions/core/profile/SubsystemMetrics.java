package naturalexceptions.core.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

public final class SubsystemMetrics {

    private final ResourceId subsystem;
    private final Map<String, Long> counters;
    private final Map<String, Long> accumulatedNanos;
    private final Map<String, Long> invocations;
    private final long activeCount;
    private final long sleepingCount;
    private final long queuedWorkCount;

    public SubsystemMetrics(ResourceId subsystem, Map<String, Long> counters, Map<String, Long> accumulatedNanos,
        Map<String, Long> invocations, long activeCount, long sleepingCount, long queuedWorkCount) {
        this.subsystem = Objects.requireNonNull(subsystem, "subsystem");
        this.counters = Collections.unmodifiableMap(counters);
        this.accumulatedNanos = Collections.unmodifiableMap(accumulatedNanos);
        this.invocations = Collections.unmodifiableMap(invocations);
        this.activeCount = activeCount;
        this.sleepingCount = sleepingCount;
        this.queuedWorkCount = queuedWorkCount;
    }

    public ResourceId subsystem() {
        return subsystem;
    }

    public long counter(String name) {
        Long val = counters.get(name);
        return val != null ? val : 0L;
    }

    public long accumulatedNanos(String operation) {
        Long val = accumulatedNanos.get(operation);
        return val != null ? val : 0L;
    }

    public long invocations(String operation) {
        Long val = invocations.get(operation);
        return val != null ? val : 0L;
    }

    public long activeCount() {
        return activeCount;
    }

    public long sleepingCount() {
        return sleepingCount;
    }

    public long queuedWorkCount() {
        return queuedWorkCount;
    }

    public Map<String, Long> counters() {
        return counters;
    }

    public Map<String, Long> accumulatedNanos() {
        return accumulatedNanos;
    }

    public Map<String, Long> invocations() {
        return invocations;
    }
}
