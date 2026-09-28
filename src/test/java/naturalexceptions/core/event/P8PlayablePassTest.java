package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.calendar.NaturalCalendar;

final class P8PlayablePassTest {

    private static List<EventRecord> starRains(P8WorldState state) {
        List<EventRecord> result = new ArrayList<>();
        for (EventRecord event : state.events()
            .snapshot()
            .events()) {
            if (EventRecord.PROTECTED_STAR_RAIN.equals(event.type())) result.add(event);
        }
        return result;
    }

    @Test
    void regionalBloodNightDestabilizesItsOwnTargetRegion() {
        P8WorldState state = new P8WorldState(42L, 1, 0L);
        EventRegion target = EventRegion.of(3, -2);
        assertTrue(state.scheduleBloodNight(target, 1L, 100L, 1000L, 0L));
        state.advance(200L);
        state.advance(2000L);
        assertTrue(
            state.destabilization()
                .value(new RegionKey(3, -2)) > 0.0,
            "the event's own region is destabilized");
        assertEquals(
            1,
            state.destabilization()
                .regionCount(),
            "no unrelated region is touched");
    }

    @Test
    void worldWideBloodNightDestabilizesOnlyWitnessedRegions() {
        P8WorldState state = new P8WorldState(42L, 1, 0L);
        assertTrue(state.scheduleBloodNight(EventRegion.global(), 7L, 100L, 1000L, 0L));
        RegionKey home = RegionKey.fromBlock(40, 90);
        state.advance(200L, Arrays.asList(home));
        state.advance(2000L, Arrays.asList(home));
        assertTrue(
            state.destabilization()
                .value(home) > 0.0);
        assertEquals(
            1,
            state.destabilization()
                .regionCount());
    }

    @Test
    void starRainRecursAfterTheProtectedFirstButNotConstantly() {
        P8WorldState state = new P8WorldState(99L, 1, 0L);
        EventRecord first = starRains(state).get(0);
        assertTrue(first.protectedOpportunity());

        // Before the protected first one settles, nothing ordinary is scheduled.
        state.advance(first.scheduledAtTick() - NaturalCalendar.TICKS_PER_DAY);
        assertEquals(1, starRains(state).size());

        // Walk a whole year day by day.
        long end = first.endsAtTick() + NaturalCalendar.DAYS_PER_YEAR * NaturalCalendar.TICKS_PER_DAY;
        for (long tick = first.scheduledAtTick(); tick <= end; tick += NaturalCalendar.TICKS_PER_DAY) {
            state.advance(tick);
        }
        List<EventRecord> rains = starRains(state);
        int ordinary = 0;
        for (EventRecord rain : rains) {
            if (rain.protectedOpportunity()) continue;
            ordinary++;
            assertTrue(rain.scheduledAtTick() > first.endsAtTick(), "ordinary rain never precedes the first");
            long timeOfDay = rain.scheduledAtTick() % NaturalCalendar.TICKS_PER_DAY;
            assertTrue(timeOfDay >= 12_000L && timeOfDay <= 14_000L, "showers begin at nightfall");
        }
        // Once per 24-day season over a 96-day year: a handful, not a constant stream.
        assertTrue(ordinary >= 3 && ordinary <= 5, "ordinary Star Rain count " + ordinary);
    }

    @Test
    void starRainSchedulingIsIdempotentAcrossReload() {
        P8WorldState a = new P8WorldState(5L, 1, 0L);
        long start = a.regularStarRainStart(3L);
        assertEquals(start, new P8WorldState(5L, 1, 0L).regularStarRainStart(3L));
        assertFalse(start % NaturalCalendar.TICKS_PER_DAY == 0L);
    }
}
