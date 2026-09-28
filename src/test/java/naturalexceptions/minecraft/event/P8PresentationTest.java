package naturalexceptions.minecraft.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemAlmanac;

final class P8PresentationTest {

    @Test
    void compassUsesMinecraftNorth() {
        assertEquals("north", P8Presentation.direction(0, -100));
        assertEquals("east", P8Presentation.direction(100, 0));
        assertEquals("south-west", P8Presentation.direction(-70, 70));
    }

    @Test
    void almanacShowsDateSeasonAndActiveBloodNightWithoutInternalIds() {
        P8WorldState state = new P8WorldState(3L, 1, 0L);
        long now = 30L * NaturalCalendar.TICKS_PER_DAY + 100L;
        assertTrue(state.scheduleBloodNight(EventRegion.of(0, 0), 9L, now - 50L, 24_000L, 0L));
        state.advance(now);
        List<String> lines = ItemAlmanac.lines(state, Collections.<StarRainImpact>emptyList(),
            Collections.<DeterministicId, Long>emptyMap(), now, 10, 10);
        String all = String.join("\n", lines);
        assertTrue(all.contains("Day 7 of Summer, year 1"), all);
        assertTrue(all.contains("Blood Night"), all);
        assertFalse(all.contains("naturalexceptions:"), "no raw ids: " + all);
    }

    @Test
    void almanacPointsAtAnUnrecoveredImpact() {
        DeterministicId parent = DeterministicId.derive("test", "parent");
        DeterministicId id = DeterministicId.derive("test", "impact");
        StarRainImpact impact = new StarRainImpact(parent, id, 0, 10L, 300, 0, 1, MaterialQuantity.ZERO,
            true, true);
        Map<DeterministicId, Long> remaining = Collections.singletonMap(id, 400_000L);
        List<String> lines = ItemAlmanac.lines(null, Collections.singletonList(impact), remaining, 1000L, 0, 0);
        String all = String.join("\n", lines);
        assertTrue(all.contains("to the east"), all);
        assertFalse(all.contains("300"), "no exact coordinates: " + all);
    }
}
