package naturalexceptions.core.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

final class SubsystemProfilerTest {

    private static final ResourceId SCHEDULER_SUB = ResourceId.of("naturalexceptions", "subsystem/scheduler");
    private static final ResourceId QUEUE_SUB = ResourceId.of("naturalexceptions", "subsystem/work_queue");

    @Test
    void accumulatesCountersAndTimings() {
        SubsystemProfiler profiler = new SubsystemProfiler();

        profiler.incrementCounter(SCHEDULER_SUB, "events_fired", 1);
        profiler.incrementCounter(SCHEDULER_SUB, "events_fired", 2);
        profiler.recordElapsedNanos(SCHEDULER_SUB, "tick", 1500L);
        profiler.recordElapsedNanos(SCHEDULER_SUB, "tick", 2500L);

        SubsystemMetrics snapshot = profiler.snapshot(SCHEDULER_SUB);
        assertNotNull(snapshot);
        assertEquals(SCHEDULER_SUB, snapshot.subsystem());
        assertEquals(3L, snapshot.counter("events_fired"));
        assertEquals(4000L, snapshot.accumulatedNanos("tick"));
        assertEquals(2L, snapshot.invocations("tick"));
    }

    @Test
    void tracksActiveSleepingAndQueuedCounts() {
        SubsystemProfiler profiler = new SubsystemProfiler();

        profiler.setActiveCount(QUEUE_SUB, 5);
        profiler.setSleepingCount(QUEUE_SUB, 100);
        profiler.setQueuedWork(QUEUE_SUB, 42);

        SubsystemMetrics snapshot = profiler.snapshot(QUEUE_SUB);
        assertEquals(5L, snapshot.activeCount());
        assertEquals(100L, snapshot.sleepingCount());
        assertEquals(42L, snapshot.queuedWorkCount());
    }

    @Test
    void safeWhenSubsystemHasNoData() {
        SubsystemProfiler profiler = new SubsystemProfiler();
        ResourceId unrecorded = ResourceId.of("naturalexceptions", "unrecorded");

        SubsystemMetrics snapshot = profiler.snapshot(unrecorded);
        assertNotNull(snapshot);
        assertEquals(0L, snapshot.counter("any"));
        assertEquals(0L, snapshot.accumulatedNanos("any"));
        assertEquals(0L, snapshot.invocations("any"));
        assertEquals(0L, snapshot.activeCount());
        assertEquals(0L, snapshot.sleepingCount());
        assertEquals(0L, snapshot.queuedWorkCount());
    }

    @Test
    void snapshotAllReturnsAllRecordedSubsystems() {
        SubsystemProfiler profiler = new SubsystemProfiler();
        profiler.incrementCounter(SCHEDULER_SUB, "tick", 1);
        profiler.incrementCounter(QUEUE_SUB, "tick", 1);

        Map<ResourceId, SubsystemMetrics> all = profiler.snapshotAll();
        assertEquals(2, all.size());
        assertTrue(all.containsKey(SCHEDULER_SUB));
        assertTrue(all.containsKey(QUEUE_SUB));
    }
}
