package naturalexceptions.core.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.worldgen.OverworldGeneration;
import naturalexceptions.core.worldgen.OverworldSample;

final class SeasonalContextTest {

    @Test
    void seasonalModifiersPreserveLocalTemperatureTendencyAndRemainBounded() {
        OverworldGeneration world = new OverworldGeneration(1234L, WorldgenEpoch.INITIAL);
        OverworldSample warm = null;
        OverworldSample cool = null;
        for (int x = -1200; x <= 1200; x += 173) {
            for (int z = -1200; z <= 1200; z += 181) {
                OverworldSample candidate = world.sample(x, z);
                if (warm == null || candidate.temperature() > warm.temperature()) warm = candidate;
                if (cool == null || candidate.temperature() < cool.temperature()) cool = candidate;
            }
        }

        SeasonalContext warmSummer = SeasonalContext.query(warm,
            NaturalCalendar.at(24L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.NONE);
        SeasonalContext coolSummer = SeasonalContext.query(cool,
            NaturalCalendar.at(24L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.NONE);
        assertTrue(warmSummer.effectiveTemperature() > coolSummer.effectiveTemperature(),
            "summer must modify, not replace, local temperature tendency");
        assertBounded(warmSummer.effectiveTemperature());
        assertBounded(warmSummer.effectiveMoisture());
    }

    @Test
    void seasonalBlendsDoNotCollapseDistinctExtremeLocalValuesAtClamps() {
        OverworldGeneration world = new OverworldGeneration(0L, WorldgenEpoch.INITIAL);
        OverworldSample cold = world.sample(-4000, -4000);
        OverworldSample warm = world.sample(4000, 4000);
        if (cold.temperature() > warm.temperature()) {
            OverworldSample swap = cold; cold = warm; warm = swap;
        }
        assertTrue(cold.temperature() < warm.temperature());
        SeasonalContext coldWinter = SeasonalContext.query(cold,
            NaturalCalendar.at(72L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.NONE);
        SeasonalContext warmWinter = SeasonalContext.query(warm,
            NaturalCalendar.at(72L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.NONE);
        assertTrue(coldWinter.effectiveTemperature() < warmWinter.effectiveTemperature());
    }

    @Test
    void ordinarySeasonAndProofStrangeSeasonChangeSharedEnvironmentalRules() {
        OverworldSample local = new OverworldGeneration(71L, WorldgenEpoch.INITIAL).sample(15, 42);
        SeasonalContext spring = SeasonalContext.query(local, NaturalCalendar.at(0L), StrangeSeason.NONE);
        SeasonalContext winter = SeasonalContext.query(local,
            NaturalCalendar.at(72L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.NONE);
        SeasonalContext ashen = SeasonalContext.query(local,
            NaturalCalendar.at(72L * NaturalCalendar.TICKS_PER_DAY), StrangeSeason.ASHEN_AUTUMN);

        assertEquals(NaturalCalendar.Season.WINTER, winter.ordinarySeason());
        assertEquals(StrangeSeason.ASHEN_AUTUMN, ashen.strangeSeason());
        assertNotEquals(spring.moldCoolingMultiplier(), winter.moldCoolingMultiplier());
        assertNotEquals(winter.effectiveMoisture(), ashen.effectiveMoisture());
        assertTrue(ashen.moldCoolingMultiplier() > 0.0);
        assertTrue(ashen.moldCoolingMultiplier() <= 2.0);
    }

    @Test
    void seasonalContextRetainsAlreadyComposedSpatialAndTemporalState() {
        OverworldSample sample = new OverworldGeneration(42L, WorldgenEpoch.of(2)).sample(0, 0);
        SeasonalContext context = SeasonalContext.query(sample, NaturalCalendar.at(0), StrangeSeason.NONE,
            0.8, 0.75, 0.9);
        assertTrue(context.effectiveMoisture() > 0.75);
        assertEquals(0.75, context.waterAvailability(), 0.0);
        assertEquals(0.9, context.nocturnalReactivity(), 0.0);
        assertEquals(0.675, context.waterReactionIntensity(), 1.0e-12);
    }

    private static void assertBounded(double value) {
        assertTrue(value >= 0.0 && value <= 1.0, "expected [0,1], got " + value);
    }
}
