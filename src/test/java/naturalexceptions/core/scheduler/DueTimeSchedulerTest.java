package naturalexceptions.core.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class DueTimeSchedulerTest {

    private static final ResourceId TEST_EVENT_TYPE = ResourceId.of("naturalexceptions", "test/delayed_event");

    @Test
    void pollsEventsInStrictDueOrder() {
        DueTimeScheduler scheduler = new DueTimeScheduler();

        ScheduledEvent ev3 = new ScheduledEvent(DeterministicId.derive("event-3"), TEST_EVENT_TYPE, 300L, "data-3");
        ScheduledEvent ev1 = new ScheduledEvent(DeterministicId.derive("event-1"), TEST_EVENT_TYPE, 100L, "data-1");
        ScheduledEvent ev2 = new ScheduledEvent(DeterministicId.derive("event-2"), TEST_EVENT_TYPE, 200L, "data-2");

        // Insert in scrambled order
        scheduler.schedule(ev3);
        scheduler.schedule(ev1);
        scheduler.schedule(ev2);

        assertEquals(100L, scheduler.peekNextDueTick());
        assertEquals(3, scheduler.pendingCount());

        // Poll at tick 50 (none due)
        List<ScheduledEvent> dueAt50 = scheduler.pollDueEvents(50L);
        assertTrue(dueAt50.isEmpty());
        assertEquals(3, scheduler.pendingCount());

        // Poll at tick 150 (only ev1 due)
        List<ScheduledEvent> dueAt150 = scheduler.pollDueEvents(150L);
        assertEquals(1, dueAt150.size());
        assertEquals(
            ev1.id(),
            dueAt150.get(0)
                .id());
        assertEquals(2, scheduler.pendingCount());
        assertEquals(200L, scheduler.peekNextDueTick());

        // Poll at tick 350 (ev2 and ev3 due)
        List<ScheduledEvent> dueAt350 = scheduler.pollDueEvents(350L);
        assertEquals(2, dueAt350.size());
        assertEquals(
            ev2.id(),
            dueAt350.get(0)
                .id());
        assertEquals(
            ev3.id(),
            dueAt350.get(1)
                .id());
        assertEquals(0, scheduler.pendingCount());
        assertEquals(-1L, scheduler.peekNextDueTick());
    }

    @Test
    void deterministicTieBreakingOnEqualDueTicks() {
        DueTimeScheduler scheduler = new DueTimeScheduler();

        DeterministicId idA = DeterministicId.derive("alpha");
        DeterministicId idB = DeterministicId.derive("beta");
        DeterministicId idC = DeterministicId.derive("gamma");

        // Schedule all three at tick 500
        scheduler.schedule(new ScheduledEvent(idC, TEST_EVENT_TYPE, 500L, ""));
        scheduler.schedule(new ScheduledEvent(idA, TEST_EVENT_TYPE, 500L, ""));
        scheduler.schedule(new ScheduledEvent(idB, TEST_EVENT_TYPE, 500L, ""));

        List<ScheduledEvent> polled = scheduler.pollDueEvents(500L);
        assertEquals(3, polled.size());

        // Expected order matches natural comparison of DeterministicId
        assertTrue(
            polled.get(0)
                .id()
                .compareTo(
                    polled.get(1)
                        .id())
                < 0);
        assertTrue(
            polled.get(1)
                .id()
                .compareTo(
                    polled.get(2)
                        .id())
                < 0);
    }

    @Test
    void rejectsDuplicateEventId() {
        DueTimeScheduler scheduler = new DueTimeScheduler();
        DeterministicId id = DeterministicId.derive("duplicate-check");

        scheduler.schedule(new ScheduledEvent(id, TEST_EVENT_TYPE, 100L, ""));
        assertThrows(
            IllegalArgumentException.class,
            () -> scheduler.schedule(new ScheduledEvent(id, TEST_EVENT_TYPE, 200L, "")));
    }

    @Test
    void firesOnceAndDoesNotReoccur() {
        DueTimeScheduler scheduler = new DueTimeScheduler();
        DeterministicId id = DeterministicId.derive("once-check");
        scheduler.schedule(new ScheduledEvent(id, TEST_EVENT_TYPE, 100L, ""));

        List<ScheduledEvent> first = scheduler.pollDueEvents(100L);
        assertEquals(1, first.size());
        assertFalse(scheduler.contains(id));

        List<ScheduledEvent> second = scheduler.pollDueEvents(100L);
        assertTrue(second.isEmpty());

        List<ScheduledEvent> third = scheduler.pollDueEvents(200L);
        assertTrue(third.isEmpty());
    }

    @Test
    void snapshotAndRestorePreservesStateAndOrdering() {
        DueTimeScheduler scheduler1 = new DueTimeScheduler();
        ScheduledEvent ev1 = new ScheduledEvent(DeterministicId.derive("item-1"), TEST_EVENT_TYPE, 100L, "d1");
        ScheduledEvent ev2 = new ScheduledEvent(DeterministicId.derive("item-2"), TEST_EVENT_TYPE, 200L, "d2");

        scheduler1.schedule(ev2);
        scheduler1.schedule(ev1);

        List<ScheduledEvent> snapshot = scheduler1.snapshot();
        assertEquals(2, snapshot.size());

        DueTimeScheduler scheduler2 = new DueTimeScheduler();
        scheduler2.restore(snapshot);

        assertEquals(2, scheduler2.pendingCount());
        assertEquals(100L, scheduler2.peekNextDueTick());

        List<ScheduledEvent> polled = scheduler2.pollDueEvents(250L);
        assertEquals(2, polled.size());
        assertEquals(
            ev1.id(),
            polled.get(0)
                .id());
        assertEquals(
            ev2.id(),
            polled.get(1)
                .id());
    }
}
