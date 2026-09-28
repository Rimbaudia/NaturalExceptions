package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class StarRainHistoryTest {

    @Test
    void p8ScheduleFeedsOneBoundedDueImpactQueueAndCanBeReconciledAgain() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(91L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        schedule.advance(rain.scheduledAtTick());
        StarRainHistory history = new StarRainHistory();

        assertTrue(history.synchronize(schedule.snapshot(), 32, -16));
        assertEquals(4, history.impactCount());
        java.util.List<StarRainImpact> firstBatch = history.dueImpacts(rain.endsAtTick(), 2);
        assertEquals(2, firstBatch.size());
        for (StarRainImpact impact : firstBatch) history.resolve(impact.id());
        assertEquals(4, history.impactCount());
        assertTrue(!history.synchronize(schedule.snapshot(), 32, -16));
        assertEquals(4, history.impactCount());
        java.util.List<StarRainImpact> finalBatch = history.dueImpacts(rain.endsAtTick(), 8);
        assertEquals(2, finalBatch.size());
        for (StarRainImpact impact : finalBatch) history.resolve(impact.id());
        assertEquals(-1L, history.nextDueTick());

        schedule.advance(rain.endsAtTick());
        EventRecord laterRain = EventInstanceFactory
            .regular(91L, 2, EventRecord.PROTECTED_STAR_RAIN, EventRegion.global(), 1L, 200L, 100L, 1000L, null, 0);
        assertTrue(schedule.schedule(laterRain));
        schedule.advance(laterRain.scheduledAtTick());
        assertTrue(history.synchronize(schedule.snapshot(), 32, -16));
        assertEquals(2, history.parentCount());
        assertEquals(8, history.impactCount());
    }

    @Test
    void awaitingMaterializationIsIndexedByChunkAndClearedWhenMatterChangesOwner() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(9L, 1, 0L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        schedule.advance(rain.scheduledAtTick());
        StarRainHistory original = new StarRainHistory();
        original.synchronize(schedule.snapshot(), -17, 31);
        StarRainImpact impact = original.snapshot()
            .get(0);
        assertTrue(original.resolve(impact.id()));

        StarRainHistory restored = new StarRainHistory(original.snapshot());
        assertEquals(
            1,
            restored.awaitingMaterializationInChunk(impact.x() >> 4, impact.z() >> 4)
                .size());
        assertTrue(
            restored.awaitingMaterializationInChunk((impact.x() >> 4) + 100, impact.z() >> 4)
                .isEmpty());
        assertTrue(
            restored.transferToSpatialSite(impact.id())
                .isPositive());
        assertTrue(
            restored.awaitingMaterializationInChunk(impact.x() >> 4, impact.z() >> 4)
                .isEmpty());
        assertEquals(0, restored.awaitingMaterializationCount());
    }

    @Test
    void forcingExistingImpactDuePreservesGeneratedIdentityParametersAndMass() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(71L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        StarRainHistory history = new StarRainHistory();
        history.synchronize(schedule.snapshot(), 32, -16);
        StarRainImpact first = history.dueImpacts(Long.MAX_VALUE, 1)
            .get(0);
        StarRainImpact original = StarRainImpactFactory.create(rain, 32, -16)
            .stream()
            .filter(
                candidate -> candidate.id()
                    .equals(first.id()))
            .findFirst()
            .get();
        long now = first.dueTick() - 1L;

        assertTrue(history.expedite(first.id(), now));
        assertEquals(original.id(), first.id());
        assertEquals(original.parentEventId(), first.parentEventId());
        assertEquals(original.ordinal(), first.ordinal());
        assertEquals(original.dueTick(), first.dueTick());
        assertEquals(original.x(), first.x());
        assertEquals(original.z(), first.z());
        assertEquals(original.severity(), first.severity());
        assertEquals(original.stellarMass(), first.stellarMass());
        assertEquals(now, first.effectiveDueTick());
        assertTrue(
            history.dueImpacts(now, 2)
                .contains(first));
        assertFalse(history.expedite(first.id(), now));
        assertFalse(history.synchronize(schedule.snapshot(), 999, 999));
        assertEquals(4, history.impactCount());
        assertEquals(
            first.id(),
            history.get(first.id())
                .id());
    }

    @Test
    void resolvedImpactCannotBeForcedAgainAndDueQueueRemainsBounded() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(72L, 2, 10L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        StarRainHistory history = new StarRainHistory();
        history.synchronize(schedule.snapshot(), 0, 0);
        StarRainImpact first = history.dueImpacts(Long.MAX_VALUE, 1)
            .get(0);
        long now = first.dueTick() - 1L;

        assertTrue(history.expedite(first.id(), now));
        assertEquals(
            1,
            history.dueImpacts(now, 2)
                .size());
        assertTrue(history.resolve(first.id()));
        assertFalse(history.expedite(first.id(), now));
        assertFalse(history.resolve(first.id()));
        assertEquals(
            1,
            history.awaitingMaterializationInChunk(first.x() >> 4, first.z() >> 4)
                .size());
        assertTrue(
            history.transferToSpatialSite(first.id())
                .isPositive());
        assertTrue(
            history.transferToSpatialSite(first.id())
                .isZero());
        assertFalse(history.expedite(first.id(), now));
    }
}
