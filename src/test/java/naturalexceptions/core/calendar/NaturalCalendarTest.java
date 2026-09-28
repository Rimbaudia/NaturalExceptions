package naturalexceptions.core.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NaturalCalendarTest {

    @Test
    void mapsOrdinarySeasonAndYearBoundariesToDurableCalendarPositions() {
        assertEquals(NaturalCalendar.Season.SPRING, NaturalCalendar.at(0L).season());
        assertEquals(NaturalCalendar.Season.SUMMER, NaturalCalendar.at(24L * 24_000L).season());
        NaturalCalendar.Position autumn = NaturalCalendar.at(48L * 24_000L);
        assertEquals(NaturalCalendar.Season.AUTUMN, autumn.season());
        assertEquals(48L, autumn.dayOfYear());
        assertEquals(NaturalCalendar.Season.WINTER, NaturalCalendar.at(72L * 24_000L).season());

        NaturalCalendar.Position nextYear = NaturalCalendar.at(96L * 24_000L);
        assertEquals(1L, nextYear.year());
        assertEquals(0L, nextYear.dayOfYear());
        assertEquals(NaturalCalendar.Season.SPRING, nextYear.season());
    }

    @Test
    void preservesSubseasonProgressAndHandlesLargeElapsedWorldTime() {
        long seasonTicks = 24L * NaturalCalendar.TICKS_PER_DAY;
        NaturalCalendar.Position halfway = NaturalCalendar.at(seasonTicks / 2L);
        assertEquals(12L, halfway.absoluteDay());
        assertEquals(0.5, halfway.seasonProgress(), 1.0e-12);

        long largeElapsedTicks = Long.MAX_VALUE - 17L;
        NaturalCalendar.Position position = NaturalCalendar.at(largeElapsedTicks);
        assertEquals(largeElapsedTicks / NaturalCalendar.TICKS_PER_DAY, position.absoluteDay());
        assertTrue(position.seasonProgress() >= 0.0 && position.seasonProgress() < 1.0);
        assertEquals(position.absoluteDay() / 96L, position.year());
    }

    @Test
    void rejectsNegativeWorldTimeInsteadOfCreatingAnAmbiguousPrehistory() {
        assertThrows(IllegalArgumentException.class, () -> NaturalCalendar.at(-1L));
    }
}
