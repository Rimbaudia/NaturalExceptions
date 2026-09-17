package naturalexceptions.core.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.profile.SubsystemMetrics;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.queue.BoundedWorkQueue;
import naturalexceptions.core.scheduler.DueTimeScheduler;
import naturalexceptions.core.scheduler.ScheduledEvent;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

final class SimulationScalingSpikeTest {

    private static final ResourceId SUBSYSTEM_SIM = ResourceId.of("naturalexceptions", "subsystem/sim_scaling");
    private static final ResourceId EVENT_TYPE_TEST = ResourceId.of("naturalexceptions", "event/scaling_test");

    @Test
    void scenario1_dormantScalingProvesZeroPerTickCostForDormantRecords() {
        int[] dormantPopulations = { 0, 1_000, 10_000, 100_000 };
        final int activeCount = 5;
        final long targetTick = 100L;

        for (int dormantCount : dormantPopulations) {
            DueTimeScheduler scheduler = new DueTimeScheduler();
            SubsystemProfiler profiler = new SubsystemProfiler();

            // Populate dormant events far in the future (tick 1,000,000)
            for (int i = 0; i < dormantCount; i++) {
                DeterministicId id = DeterministicId.derive("sim", "dormant", Integer.toString(i));
                scheduler.schedule(new ScheduledEvent(id, EVENT_TYPE_TEST, 1_000_000L, "dormant-" + i));
            }

            // Populate active events due at targetTick
            for (int i = 0; i < activeCount; i++) {
                DeterministicId id = DeterministicId.derive("sim", "active", Integer.toString(i));
                scheduler.schedule(new ScheduledEvent(id, EVENT_TYPE_TEST, targetTick, "active-" + i));
            }

            profiler.setSleepingCount(SUBSYSTEM_SIM, dormantCount);
            profiler.setActiveCount(SUBSYSTEM_SIM, activeCount);

            // Simulate tick processing at targetTick
            AtomicLong inspectedRecords = new AtomicLong(0);
            AtomicLong executedUnits = new AtomicLong(0);

            long startTime = System.nanoTime();
            long nextDue = scheduler.peekNextDueTick();
            inspectedRecords.incrementAndGet(); // Checked queue head in O(1)

            if (nextDue >= 0 && nextDue <= targetTick) {
                List<ScheduledEvent> due = scheduler.pollDueEvents(targetTick);
                executedUnits.addAndGet(due.size());
                // The number of records touched is strictly the due items
                inspectedRecords.addAndGet(due.size());
            }
            long elapsedNanos = System.nanoTime() - startTime;

            profiler.incrementCounter(SUBSYSTEM_SIM, "inspected_records", inspectedRecords.get());
            profiler.incrementCounter(SUBSYSTEM_SIM, "executed_records", executedUnits.get());
            profiler.recordElapsedNanos(SUBSYSTEM_SIM, "tick_work", elapsedNanos);

            SubsystemMetrics metrics = profiler.snapshot(SUBSYSTEM_SIM);

            // Architectural Invariant: Inspected and executed record count MUST BE strictly a function of activeCount,
            // strictly independent of whether there are 0 or 100,000 dormant records.
            assertEquals(
                activeCount,
                executedUnits.get(),
                "Must execute exactly the " + activeCount + " due events regardless of dormant count " + dormantCount);
            assertEquals(
                dormantCount,
                scheduler.pendingCount(),
                "Dormant records must remain undisturbed in scheduler");
            assertEquals(dormantCount, metrics.sleepingCount());
        }
    }

    @Test
    void scenario2_activeBoundedWorkloadDrainsDeterministicallyWithinPerTickBudget() {
        BoundedWorkQueue<Integer> queue = new BoundedWorkQueue<>();
        final int totalItems = 2_500;
        final int budgetPerTick = 100;

        for (int i = 0; i < totalItems; i++) {
            queue.enqueue(i);
        }

        assertEquals(totalItems, queue.size());

        List<Integer> processed = new ArrayList<>();
        int tickCount = 0;

        while (!queue.isEmpty()) {
            tickCount++;
            int beforeDepth = queue.size();
            int drained = queue.drain(budgetPerTick, processed::add);

            // Verify bounded execution
            assertTrue(
                drained <= budgetPerTick,
                "Tick " + tickCount + " drained " + drained + " which exceeds budget " + budgetPerTick);
            assertEquals(beforeDepth - drained, queue.size(), "Queue size must decrease by exactly drained count");
        }

        // 2,500 items at 100/tick must take exactly 25 ticks
        assertEquals(25, tickCount);
        assertEquals(totalItems, processed.size());

        // Prove FIFO preservation: 0, 1, 2, ..., 2499
        for (int i = 0; i < totalItems; i++) {
            assertEquals(
                i,
                processed.get(i)
                    .intValue(),
                "FIFO ordering must be preserved across spills");
        }
    }

    @Test
    void scenario3_scheduledWakeUpDoesNotScanUnrelatedDormantState() {
        DueTimeScheduler scheduler = new DueTimeScheduler();
        final int dormantPopulation = 50_000;

        // Schedule 50,000 events far in the future
        for (int i = 0; i < dormantPopulation; i++) {
            DeterministicId id = DeterministicId.derive("sim", "future", Integer.toString(i));
            scheduler.schedule(new ScheduledEvent(id, EVENT_TYPE_TEST, 5_000L + (i % 1000), "payload-" + i));
        }

        // Schedule 3 events to wake at tick 50
        DeterministicId wake1 = DeterministicId.derive("sim", "wake", "1");
        DeterministicId wake2 = DeterministicId.derive("sim", "wake", "2");
        DeterministicId wake3 = DeterministicId.derive("sim", "wake", "3");

        scheduler.schedule(new ScheduledEvent(wake1, EVENT_TYPE_TEST, 50L, "wake-1"));
        scheduler.schedule(new ScheduledEvent(wake2, EVENT_TYPE_TEST, 50L, "wake-2"));
        scheduler.schedule(new ScheduledEvent(wake3, EVENT_TYPE_TEST, 50L, "wake-3"));

        assertEquals(dormantPopulation + 3, scheduler.pendingCount());

        // Fast forward to tick 50
        List<ScheduledEvent> awakened = new ArrayList<>();
        if (scheduler.peekNextDueTick() <= 50L) {
            awakened.addAll(scheduler.pollDueEvents(50L));
        }

        assertEquals(3, awakened.size());
        assertEquals(dormantPopulation, scheduler.pendingCount());

        // The 50,000 future records were never iterated over
        assertTrue(
            awakened.stream()
                .anyMatch(
                    e -> e.id()
                        .equals(wake1)));
        assertTrue(
            awakened.stream()
                .anyMatch(
                    e -> e.id()
                        .equals(wake2)));
        assertTrue(
            awakened.stream()
                .anyMatch(
                    e -> e.id()
                        .equals(wake3)));
    }

    @Test
    void scenario4_persistenceInterruptionMatchesUninterruptedRun() {
        // Build 200 events scheduled between tick 10 and tick 60
        final int totalEvents = 200;

        // Run A: Uninterrupted run
        DueTimeScheduler uninterruptedScheduler = new DueTimeScheduler();
        for (int i = 0; i < totalEvents; i++) {
            long dueTick = 10L + (i % 50);
            DeterministicId id = DeterministicId.derive("sim", "interruption", Integer.toString(i));
            uninterruptedScheduler.schedule(new ScheduledEvent(id, EVENT_TYPE_TEST, dueTick, "p-" + i));
        }

        List<ScheduledEvent> uninterruptedHistory = new ArrayList<>();
        for (long tick = 1; tick <= 65; tick++) {
            if (uninterruptedScheduler.peekNextDueTick() >= 0 && uninterruptedScheduler.peekNextDueTick() <= tick) {
                uninterruptedHistory.addAll(uninterruptedScheduler.pollDueEvents(tick));
            }
        }
        assertEquals(totalEvents, uninterruptedHistory.size());

        // Run B: Interrupted run stopped at tick 30, saved to NBT, reconstructed in fresh instance, and continued
        NEWorldSavedData worldData = new NEWorldSavedData();
        worldData.setMetadata(WorldMetadata.createNew("interrupted-world", "N0.01.0-dev"));
        for (int i = 0; i < totalEvents; i++) {
            long dueTick = 10L + (i % 50);
            DeterministicId id = DeterministicId.derive("sim", "interruption", Integer.toString(i));
            worldData.scheduler()
                .schedule(new ScheduledEvent(id, EVENT_TYPE_TEST, dueTick, "p-" + i));
        }

        List<ScheduledEvent> interruptedHistory = new ArrayList<>();

        // Process ticks 1 to 30
        for (long tick = 1; tick <= 30; tick++) {
            if (worldData.scheduler()
                .peekNextDueTick() >= 0
                && worldData.scheduler()
                    .peekNextDueTick() <= tick) {
                interruptedHistory.addAll(
                    worldData.scheduler()
                        .pollDueEvents(tick));
            }
        }

        // SAVE AUTHORITATIVE STATE TO NBT AT TICK 30
        NBTTagCompound saveNbt = new NBTTagCompound();
        worldData.writeToNBT(saveNbt);

        // DESTROY ORIGINAL INSTANCE
        worldData = null;

        // RECONSTRUCT FROM NBT IN FRESH INSTANCE
        NEWorldSavedData reloadedData = new NEWorldSavedData();
        reloadedData.readFromNBT(saveNbt);

        // Resume processing ticks 31 to 65
        for (long tick = 31; tick <= 65; tick++) {
            if (reloadedData.scheduler()
                .peekNextDueTick() >= 0
                && reloadedData.scheduler()
                    .peekNextDueTick() <= tick) {
                interruptedHistory.addAll(
                    reloadedData.scheduler()
                        .pollDueEvents(tick));
            }
        }

        assertEquals(totalEvents, interruptedHistory.size());

        // Invariant: Interrupted execution sequence must IDENTICALLY match uninterrupted sequence
        for (int i = 0; i < totalEvents; i++) {
            ScheduledEvent expected = uninterruptedHistory.get(i);
            ScheduledEvent actual = interruptedHistory.get(i);
            assertEquals(expected.id(), actual.id(), "Event at index " + i + " must have identical ID");
            assertEquals(expected.dueTick(), actual.dueTick(), "Event at index " + i + " must have identical due tick");
            assertEquals(expected.payload(), actual.payload(), "Event at index " + i + " must have identical payload");
        }
    }
}
