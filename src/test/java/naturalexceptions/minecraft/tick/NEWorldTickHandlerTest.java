package naturalexceptions.minecraft.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinEvents;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.profile.SubsystemMetrics;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.scheduler.ScheduledEvent;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

final class NEWorldTickHandlerTest {

    @Test
    void dormantWorldDoesNearlyZeroWork() {
        SubsystemProfiler profiler = new SubsystemProfiler();
        NEWorldTickHandler handler = new NEWorldTickHandler(profiler, 10);
        NEWorldSavedData data = new NEWorldSavedData();
        data.setMetadata(WorldMetadata.createNew("test-world", "N0.01.0-dev"));

        handler.tickWorld(data, 1000L);

        assertEquals(
            0,
            handler.workQueue()
                .size());
        SubsystemMetrics metrics = profiler.snapshot(NEWorldTickHandler.SUBSYSTEM_SCHEDULER);
        assertEquals(0L, metrics.counter("events_processed"));
        assertEquals(0L, metrics.activeCount());
        assertEquals(0L, metrics.sleepingCount());
        assertEquals(0L, metrics.queuedWorkCount());
    }

    @Test
    void pollsDueEventsAndProcessesThem() {
        SubsystemProfiler profiler = new SubsystemProfiler();
        AtomicInteger firedCount = new AtomicInteger();

        NEWorldTickHandler handler = new NEWorldTickHandler(profiler, 10, event -> firedCount.incrementAndGet());
        NEWorldSavedData data = new NEWorldSavedData();
        data.setMetadata(WorldMetadata.createNew("test-world", "N0.01.0-dev"));

        DeterministicId id = DeterministicId.derive("tick-test-1");
        data.scheduler()
            .schedule(new ScheduledEvent(id, BuiltinEvents.DELAYED_TEST_EVENT, 100L, "payload"));

        // Tick at 50L (not due)
        handler.tickWorld(data, 50L);
        assertEquals(0, firedCount.get());
        assertEquals(
            1,
            data.scheduler()
                .pendingCount());

        // Tick at 100L (due)
        handler.tickWorld(data, 100L);
        assertEquals(1, firedCount.get());
        assertEquals(
            0,
            data.scheduler()
                .pendingCount());
        assertTrue(
            handler.workQueue()
                .isEmpty());

        // Tick at 150L (already fired, does not fire again)
        handler.tickWorld(data, 150L);
        assertEquals(1, firedCount.get());
    }

    @Test
    void respectsBudgetAndSpillsAcrossTicks() {
        SubsystemProfiler profiler = new SubsystemProfiler();
        AtomicInteger firedCount = new AtomicInteger();

        // Budget is 5 per tick
        NEWorldTickHandler handler = new NEWorldTickHandler(profiler, 5, event -> firedCount.incrementAndGet());
        NEWorldSavedData data = new NEWorldSavedData();
        data.setMetadata(WorldMetadata.createNew("test-world", "N0.01.0-dev"));

        // Schedule 12 events all due at tick 100
        for (int i = 0; i < 12; i++) {
            DeterministicId id = DeterministicId.derive("budget-test", String.valueOf(i));
            data.scheduler()
                .schedule(new ScheduledEvent(id, BuiltinEvents.DELAYED_TEST_EVENT, 100L, ""));
        }

        // Tick 1 at 100L: polls all 12 into queue, drains budget of 5 -> 7 remain queued
        handler.tickWorld(data, 100L);
        assertEquals(5, firedCount.get());
        assertEquals(
            7,
            handler.workQueue()
                .size());

        // Tick 2 at 101L: drains next 5 -> 2 remain queued
        handler.tickWorld(data, 101L);
        assertEquals(10, firedCount.get());
        assertEquals(
            2,
            handler.workQueue()
                .size());

        // Tick 3 at 102L: drains remaining 2 -> 0 remain queued
        handler.tickWorld(data, 102L);
        assertEquals(12, firedCount.get());
        assertEquals(
            0,
            handler.workQueue()
                .size());

        SubsystemMetrics metrics = profiler.snapshot(NEWorldTickHandler.SUBSYSTEM_SCHEDULER);
        assertEquals(12L, metrics.counter("events_processed"));
    }
}
