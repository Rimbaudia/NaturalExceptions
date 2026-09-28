package naturalexceptions.minecraft.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinEvents;
import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.WorldEventSchedule;
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

    @Test
    void forcedDueImpactUsesOrdinaryBoundedResolverAndUnloadedSiteWaitsForChunkMaterialization() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(201L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        NEWorldSavedData data = new NEWorldSavedData();
        data.setMetadata(WorldMetadata.createNew("p11-forced-due", "N0.01.0-dev"));
        data.p11()
            .synchronize(schedule.snapshot(), 16, 16);
        StarRainImpact first = data.p11()
            .snapshot()
            .get(0);
        long now = first.dueTick() - 1L;
        long mass = first.stellarMass()
            .milligrams();

        assertTrue(
            data.p11()
                .expedite(first.id(), now));
        NEWorldTickHandler handler = new NEWorldTickHandler(new SubsystemProfiler(), 10);
        handler.tickWorld(data, now);

        assertTrue(first.isResolved());
        assertTrue(!first.isMaterialized());
        assertEquals(
            mass,
            first.stellarMass()
                .milligrams());
        assertEquals(
            0L,
            data.fluids()
                .totalMass()
                .milligrams());
        assertEquals(
            1,
            data.p11()
                .awaitingMaterializationInChunk(first.x() >> 4, first.z() >> 4)
                .size());
        handler.tickWorld(data, now + 1L);
        assertEquals(
            1L,
            handler.profiler()
                .snapshot(NEWorldTickHandler.SUBSYSTEM_STAR_RAIN)
                .counter("impacts_resolved"));
        assertEquals(
            0L,
            data.fluids()
                .totalMass()
                .milligrams());
    }

    @Test
    void severalForcedImpactsStillResolveAtMostTwoPerWorldTick() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(202L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        NEWorldSavedData data = new NEWorldSavedData();
        data.setMetadata(WorldMetadata.createNew("p11-forced-budget", "N0.01.0-dev"));
        data.p11()
            .synchronize(schedule.snapshot(), 16, 16);
        long now = data.p11()
            .nextDueTick() - 2L;
        for (int i = 0; i < 3; i++) {
            assertTrue(
                data.p11()
                    .expedite(
                        data.p11()
                            .snapshot()
                            .get(i)
                            .id(),
                        now));
        }
        NEWorldTickHandler handler = new NEWorldTickHandler(new SubsystemProfiler(), 10);

        handler.tickWorld(data, now);
        assertEquals(
            2L,
            handler.profiler()
                .snapshot(NEWorldTickHandler.SUBSYSTEM_STAR_RAIN)
                .counter("impacts_resolved"));
        assertEquals(
            2,
            data.p11()
                .awaitingMaterializationCount());

        handler.tickWorld(data, now + 1L);
        assertEquals(
            3L,
            handler.profiler()
                .snapshot(NEWorldTickHandler.SUBSYSTEM_STAR_RAIN)
                .counter("impacts_resolved"));
        assertEquals(
            3,
            data.p11()
                .awaitingMaterializationCount());
    }
}
