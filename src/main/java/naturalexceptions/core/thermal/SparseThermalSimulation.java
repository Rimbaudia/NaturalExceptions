package naturalexceptions.core.thermal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.queue.BoundedWorkQueue;
import naturalexceptions.core.scheduler.DueTimeScheduler;
import naturalexceptions.core.scheduler.ScheduledEvent;

/**
 * Event-driven, sparse thermal simulation manager.
 *
 * <p>
 * Enforces strict P3 architectural invariants:
 * <ul>
 * <li><b>Sparse state:</b> Objects near ambient equilibrium remain in an implicit dormant state and
 * are not stored or iterated in the active simulation loop.</li>
 * <li><b>Zero universal ticking:</b> Dormant objects cost 0 CPU operations per tick.</li>
 * <li><b>Event-driven scheduling:</b> Non-equilibrium objects are scheduled via {@link DueTimeScheduler}
 * and drained via {@link BoundedWorkQueue} under a hard per-tick budget.</li>
 * <li><b>Equilibrium collapse:</b> When a sample cools to ambient within tolerance and is in solid phase,
 * it collapses back into implicit dormant state.</li>
 * </ul>
 */
public final class SparseThermalSimulation {

    public static final double DEFAULT_SLEEP_TOLERANCE_K = 0.5;
    public static final double SECONDS_PER_TICK = 0.05; // 20 TPS
    public static final ResourceId EVENT_TYPE_THERMAL = ResourceId.of("naturalexceptions", "thermal_update");

    private final DueTimeScheduler scheduler = new DueTimeScheduler();
    private final BoundedWorkQueue<Long> workQueue = new BoundedWorkQueue<>();
    private final int perTickBudget;
    private final Map<Long, ActiveRecord> activeRecords = new HashMap<>();
    private final AmbientReservoir ambientReservoir;

    private int lastTickExecutedUnits = 0;
    private long totalExecutedUnits = 0;

    public SparseThermalSimulation(int perTickBudget, AmbientReservoir ambientReservoir) {
        if (perTickBudget <= 0) {
            throw new IllegalArgumentException("Per-tick budget must be strictly positive: " + perTickBudget);
        }
        this.perTickBudget = perTickBudget;
        this.ambientReservoir = Objects.requireNonNull(ambientReservoir, "AmbientReservoir cannot be null");
    }

    public static final class ActiveRecord {

        private final long id;
        private final ActiveThermalState state;
        private final double conductance;
        private long lastUpdateTick;

        public ActiveRecord(long id, ActiveThermalState state, double conductance, long startTick) {
            this.id = id;
            this.state = state;
            this.conductance = conductance;
            this.lastUpdateTick = startTick;
        }

        public long id() {
            return id;
        }

        public ActiveThermalState state() {
            return state;
        }

        public double conductance() {
            return conductance;
        }

        public long lastUpdateTick() {
            return lastUpdateTick;
        }

        public void setLastUpdateTick(long tick) {
            this.lastUpdateTick = tick;
        }
    }

    /**
     * Wakes a dormant sample into explicit active simulation.
     */
    public void wake(long sampleId, ActiveThermalState state, double conductance, long currentTick) {
        Objects.requireNonNull(state, "ActiveThermalState cannot be null");
        if (state.isEquilibratedWith(ambientReservoir.temperatureKelvin(), DEFAULT_SLEEP_TOLERANCE_K)) {
            // Already at equilibrium, does not require active tracking
            return;
        }

        ActiveRecord record = new ActiveRecord(sampleId, state, conductance, currentTick);
        activeRecords.put(sampleId, record);

        DeterministicId eventId = DeterministicId.derive("thermal", String.valueOf(sampleId));
        if (!scheduler.contains(eventId)) {
            scheduler
                .schedule(new ScheduledEvent(eventId, EVENT_TYPE_THERMAL, currentTick + 1, String.valueOf(sampleId)));
        }
    }

    /**
     * Advances simulation for a world tick.
     *
     * @param currentTick current world tick
     */
    public void tick(long currentTick) {
        lastTickExecutedUnits = 0;

        // Poll events due at or before current tick
        List<ScheduledEvent> dueEvents = scheduler.pollDueEvents(currentTick);
        for (ScheduledEvent event : dueEvents) {
            try {
                long sampleId = Long.parseLong(event.payload());
                workQueue.enqueue(sampleId);
            } catch (NumberFormatException ignored) {}
        }

        // Drain work up to per-tick budget
        lastTickExecutedUnits = workQueue.drain(perTickBudget, sampleId -> {
            ActiveRecord record = activeRecords.get(sampleId);
            if (record == null) {
                return;
            }

            totalExecutedUnits++;

            long elapsedTicks = Math.max(1, currentTick - record.lastUpdateTick());
            double elapsedSeconds = elapsedTicks * SECONDS_PER_TICK;
            record.setLastUpdateTick(currentTick);

            // Execute heat exchange with ambient
            FiniteThermalBody body = new FiniteThermalBody(record.state());
            ThermalTransfer.exchangeElapsed(body, ambientReservoir, record.conductance(), elapsedSeconds);

            // Check if equilibrated to collapse
            if (record.state()
                .isEquilibratedWith(ambientReservoir.temperatureKelvin(), DEFAULT_SLEEP_TOLERANCE_K)) {
                // Collapse to implicit ambient state!
                activeRecords.remove(sampleId);
            } else {
                // Schedule next evaluation
                DeterministicId nextEventId = DeterministicId.derive("thermal", String.valueOf(sampleId));
                scheduler.schedule(
                    new ScheduledEvent(nextEventId, EVENT_TYPE_THERMAL, currentTick + 5, String.valueOf(sampleId)));
            }
        });
    }

    public boolean isActive(long sampleId) {
        return activeRecords.containsKey(sampleId);
    }

    public int activeCount() {
        return activeRecords.size();
    }

    public int scheduledCount() {
        return scheduler.pendingCount();
    }

    public int pendingQueueCount() {
        return workQueue.size();
    }

    public int lastTickExecutedUnits() {
        return lastTickExecutedUnits;
    }

    public long totalExecutedUnits() {
        return totalExecutedUnits;
    }
}
