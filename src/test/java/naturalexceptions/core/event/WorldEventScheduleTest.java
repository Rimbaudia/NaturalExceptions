package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

final class WorldEventScheduleTest {

    private static final ResourceId BLOOD_NIGHT = ResourceId.of("naturalexceptions", "blood_night");
    private static final ResourceId STAR_RAIN = ResourceId.of("naturalexceptions", "star_rain");
    private static final ResourceId RED_TIDE = ResourceId.of("naturalexceptions", "red_tide");
    private static final ResourceId OTHER_EVENT = ResourceId.of("naturalexceptions", "other_event");
    private static final EventRegion REGION = EventRegion.of(-2, 7);

    @Test
    void regularEventIdentityIsStableAndSeparatesOrdinalsAndWorldEpochs() {
        EventRecord first = EventInstanceFactory.regular(
            991L,
            2,
            BLOOD_NIGHT,
            REGION,
            0L,
            100L,
            200L,
            600L,
            RED_TIDE,
            3);
        EventRecord restoredDerivation = EventInstanceFactory.regular(
            991L,
            2,
            BLOOD_NIGHT,
            REGION,
            0L,
            100L,
            200L,
            600L,
            RED_TIDE,
            3);
        EventRecord next = EventInstanceFactory.regular(
            991L,
            2,
            BLOOD_NIGHT,
            REGION,
            1L,
            400L,
            200L,
            600L,
            RED_TIDE,
            3);
        EventRecord newEpoch = EventInstanceFactory.regular(
            991L,
            3,
            BLOOD_NIGHT,
            REGION,
            0L,
            100L,
            200L,
            600L,
            RED_TIDE,
            3);

        assertEquals(first.id(), restoredDerivation.id());
        assertNotEquals(first.id(), next.id());
        assertNotEquals(first.id(), newEpoch.id());
        assertEquals(100L, first.scheduledAtTick());
        assertEquals(300L, first.endsAtTick());
    }

    @Test
    void protectedStarRainOpportunityIsDeterministicWithinEarlyWindowAndPersistable() {
        EventRecord first = EventInstanceFactory.protectedFirstStarRain(991L, 2, 1000L, 24000L, 96000L, 12000L);
        EventRecord retry = EventInstanceFactory.protectedFirstStarRain(991L, 2, 1000L, 24000L, 96000L, 12000L);
        EventRecord anotherWorld = EventInstanceFactory.protectedFirstStarRain(992L, 2, 1000L, 24000L, 96000L, 12000L);

        assertTrue(first.protectedOpportunity());
        assertEquals(STAR_RAIN, first.type());
        assertEquals(first.id(), retry.id());
        assertEquals(first.scheduledAtTick(), retry.scheduledAtTick());
        assertTrue(first.scheduledAtTick() >= 25000L);
        assertTrue(first.scheduledAtTick() <= 97000L);
        assertNotEquals(first.id(), anotherWorld.id());
        assertNotEquals(first.scheduledAtTick(), anotherWorld.scheduledAtTick());
        assertFalse(first.hasTemporalInfluence());
    }

    @Test
    void lifecycleTransitionsAreIdempotentAndExposeOnlyActiveTemporalInfluences() {
        WorldEventSchedule schedule = new WorldEventSchedule();
        EventRecord event = EventInstanceFactory.regular(
            99L,
            2,
            BLOOD_NIGHT,
            REGION,
            0L,
            100L,
            50L,
            500L,
            RED_TIDE,
            2);
        assertTrue(schedule.schedule(event));

        assertEquals(100L, schedule.nextTransitionTick());
        assertEquals(EventPhase.SCHEDULED, schedule.get(event.id()).phase());
        assertTrue(schedule.activeInfluences(REGION, 99L).isEmpty());
        assertEquals(1, schedule.advance(100L).size());
        assertEquals(EventPhase.ACTIVE, schedule.get(event.id()).phase());
        assertEquals(150L, schedule.nextTransitionTick());
        assertEquals(2, schedule.activeInfluences(REGION, 100L).get(0).strength());
        assertTrue(schedule.advance(100L).isEmpty());
        assertEquals(1, schedule.advance(150L).size());
        assertEquals(EventPhase.COMPLETED, schedule.get(event.id()).phase());
        assertEquals(-1L, schedule.nextTransitionTick());
        assertTrue(schedule.activeInfluences(REGION, 150L).isEmpty());
        assertTrue(schedule.advance(500L).isEmpty());
    }

    @Test
    void snapshotRestorePreservesPendingAndActiveHistoryWithoutRerolling() {
        WorldEventSchedule schedule = new WorldEventSchedule();
        EventRecord pending = EventInstanceFactory.regular(
            99L, 2, BLOOD_NIGHT, REGION, 0L, 100L, 50L, 500L, RED_TIDE, 2);
        EventRecord active = EventInstanceFactory.regular(
            99L, 2, OTHER_EVENT, REGION, 1L, 200L, 50L, 500L, null, 0);
        schedule.schedule(pending);
        schedule.schedule(active);
        schedule.advance(220L);
        WorldEventSchedule reloaded = WorldEventSchedule.restore(schedule.snapshot());

        assertEquals(pending.id(), reloaded.get(pending.id()).id());
        assertEquals(EventPhase.COMPLETED, reloaded.get(pending.id()).phase());
        assertEquals(EventPhase.ACTIVE, reloaded.get(active.id()).phase());
        assertTrue(reloaded.advance(220L).isEmpty());
    }

    @Test
    void newlyScheduledInstancesMustStartInScheduledPhase() {
        EventRecord scheduled = EventInstanceFactory.regular(8L, 2, BLOOD_NIGHT, REGION,
            4L, 100L, 20L, 0L, null, 0);
        EventRecord active = new EventRecord(scheduled.id(), scheduled.type(), scheduled.targetRegion(), false,
            scheduled.scheduledAtTick(), scheduled.endsAtTick(), EventPhase.ACTIVE, 101L,
            scheduled.parameterSeed(), scheduled.cooldownUntilTick(), null, 0);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> new WorldEventSchedule().schedule(active));
    }

    @Test
    void completedEventCooldownBlocksSameTypeAndRegionButNotOtherRegion() {
        WorldEventSchedule schedule = new WorldEventSchedule();
        EventRecord event = EventInstanceFactory.regular(
            99L, 2, BLOOD_NIGHT, REGION, 0L, 100L, 50L, 500L, null, 0);
        assertTrue(schedule.schedule(event));
        schedule.advance(150L);
        assertEquals(650L, schedule.cooldownUntil(BLOOD_NIGHT, REGION));

        EventRecord blocked = EventInstanceFactory.regular(
            99L, 2, BLOOD_NIGHT, REGION, 1L, 649L, 10L, 20L, null, 0);
        EventRecord allowedAfter = EventInstanceFactory.regular(
            99L, 2, BLOOD_NIGHT, REGION, 2L, 650L, 10L, 20L, null, 0);
        EventRecord elsewhere = EventInstanceFactory.regular(
            99L, 2, BLOOD_NIGHT, EventRegion.of(1, 7), 0L, 200L, 10L, 20L, null, 0);

        assertFalse(schedule.schedule(blocked));
        assertTrue(schedule.schedule(allowedAfter));
        assertTrue(schedule.schedule(elsewhere));
    }

    @Test
    void protectedOpportunityIsUniqueWhenRepeatedlyEnsured() {
        WorldEventSchedule schedule = new WorldEventSchedule();
        EventRecord protectedEvent = EventInstanceFactory.protectedFirstStarRain(991L, 2, 1000L, 24000L, 96000L, 12000L);
        assertTrue(schedule.ensureProtectedFirstStarRain(protectedEvent));
        assertFalse(schedule.ensureProtectedFirstStarRain(
            EventInstanceFactory.protectedFirstStarRain(991L, 2, 1000L, 24000L, 96000L, 12000L)));
        assertEquals(1, schedule.protectedStarRainCount());
        assertTrue(schedule.advance(protectedEvent.scheduledAtTick()).size() == 1);
        assertTrue(schedule.activeInfluences(protectedEvent.targetRegion(), protectedEvent.scheduledAtTick()).isEmpty());
    }

}
