package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.anomaly.DestabilizationField;
import naturalexceptions.core.anomaly.EnvironmentalInfluence;
import naturalexceptions.core.anomaly.P8EnvironmentQuery;
import naturalexceptions.core.anomaly.ResolvedEnvironmentalContext;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.anomaly.SpatialAnomaly;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.calendar.SeasonalContext;
import naturalexceptions.core.calendar.StrangeSeason;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.worldgen.OverworldGeneration;
import naturalexceptions.core.worldgen.OverworldSample;
import naturalexceptions.core.persistence.WorldgenEpoch;

final class P8WorldStateTest {
    @Test
    void freshWorldPersistsProtectedStarRainAndSchedulesConditionedSeasonalEvents() {
        P8WorldState a = new P8WorldState(111L, 2, 0L);
        P8WorldState b = new P8WorldState(111L, 2, 0L);
        EventRecord rain = a.events().snapshot().events().get(0);
        assertTrue(rain.protectedOpportunity());
        assertFalse(rain.hasTemporalInfluence()); // P11 physical effects are not present in P8.
        assertEquals(rain.id(), b.events().snapshot().events().get(0).id());
        assertEquals(rain.scheduledAtTick(), b.events().snapshot().events().get(0).scheduledAtTick());
        assertNotEquals(rain.scheduledAtTick(), new P8WorldState(112L, 2, 0L).events().snapshot().events().get(0).scheduledAtTick());

        assertTrue(a.advance(0));
        EventRecord blood = a.events().snapshot().events().stream()
            .filter(e -> P8WorldState.BLOOD_NIGHT.equals(e.type())).findFirst().orElse(null);
        assertNotNull(blood);
        assertEquals(60L * NaturalCalendar.TICKS_PER_DAY, blood.scheduledAtTick());
    }

    @Test
    void strangeSeasonUsesOrdinaryCalendarContextAndIsNotRerolledAfterRestore() {
        P8WorldState state = new P8WorldState(3L, 2, 0L);
        state.destabilization().increase(new RegionKey(0, 0), ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("strange-season-test"), 0.9, 0);
        assertTrue(state.advance(DestabilizationField.COARSE_STEP_TICKS));
        assertEquals(StrangeSeason.ASHEN_AUTUMN, state.strangeSeasonAt(DestabilizationField.COARSE_STEP_TICKS));
        OverworldSample sample = new OverworldGeneration(3L, WorldgenEpoch.of(2)).sample(0, 0);
        SeasonalContext ordinary = SeasonalContext.query(sample, NaturalCalendar.at(DestabilizationField.COARSE_STEP_TICKS),
            StrangeSeason.NONE);
        SeasonalContext selected = SeasonalContext.query(sample,
            NaturalCalendar.at(DestabilizationField.COARSE_STEP_TICKS),
            state.strangeSeasonAt(DestabilizationField.COARSE_STEP_TICKS));
        assertNotEquals(ordinary.effectiveMoisture(), selected.effectiveMoisture());
        EventScheduleSnapshot snapshot = state.events().snapshot();
        P8WorldState restored = new P8WorldState(3L, 2, 0L, WorldEventSchedule.restore(snapshot),
            state.anomalies(), DestabilizationField.restore(state.destabilization().snapshot()));
        assertEquals(StrangeSeason.ASHEN_AUTUMN, restored.strangeSeasonAt(DestabilizationField.COARSE_STEP_TICKS));
    }

    @Test
    void completedBloodNightLeavesOnePersistentRegionalConsequence() {
        P8WorldState state = new P8WorldState(99L, 2, 0L);
        state.advance(0L);
        long completion = 63L * NaturalCalendar.TICKS_PER_DAY;
        assertTrue(state.advance(completion));
        assertEquals(1, state.destabilization().regionCount());
        assertTrue(state.destabilization().derivedGlobal() > 0.0);
        state.advance(completion);
        assertEquals(1, state.destabilization().regionCount()); // event completion is idempotent
    }

    @Test
    void operatorStyleLocalBloodNightTriggerBecomesAnInspectableActiveEvent() {
        P8WorldState state = new P8WorldState(91L, 2, 0L);
        EventRegion local = EventRegion.of(4, -3);
        long now = 321_000L;

        assertTrue(state.scheduleBloodNight(local, now, now, 24_000L, 864_000L));
        state.advance(now);

        EventRecord triggered = state.events().snapshot().events().stream()
            .filter(event -> P8WorldState.BLOOD_NIGHT.equals(event.type()) && event.targetRegion().equals(local))
            .findFirst().orElse(null);
        assertNotNull(triggered);
        assertEquals(EventPhase.ACTIVE, triggered.phase());
        assertEquals(100, state.activeInfluencesAt(4 * 512, -3 * 512, now).get(0).strength());
        assertTrue(state.activeInfluencesAt(5 * 512, -3 * 512, now).isEmpty());
    }

    @Test
    void localSpatialAndTemporalInfluencesComposeWithoutPairwisePhenomenonRules() {
        OverworldGeneration generation = new OverworldGeneration(9L, WorldgenEpoch.of(2));
        int[] wet = findWetLocation(generation);
        P8WorldState state = new P8WorldState(9L, 2, 0L);
        DeterministicId lakeId = DeterministicId.derive("cow-lake-test");
        state.anomalies().add(new SpatialAnomaly(lakeId, ResourceId.of("naturalexceptions", "cow_lake"),
            wet[0], wet[1], 100, EnvironmentalInfluence.of(lakeId, 0.6, 0, 0.4)));
        EventRecord blood = EventInstanceFactory.regular(9L, 2, P8WorldState.BLOOD_NIGHT,
            EventRegion.global(), 0, 0, 200, 0, P8WorldState.NIGHT_REACTIVITY, 100);
        state.events().schedule(blood);
        state.events().advance(1);
        double together = P8EnvironmentQuery.at(generation.sample(wet[0], wet[1]), wet[0], wet[1],
            state.anomalies(), state.activeInfluencesAt(wet[0], wet[1], 1)).waterReactionIntensity();
        double lakeOnly = P8EnvironmentQuery.at(generation.sample(wet[0], wet[1]), wet[0], wet[1],
            state.anomalies(), null).waterReactionIntensity();
        assertTrue(together > lakeOnly);
        double bloodOnly = P8EnvironmentQuery.at(generation.sample(wet[0], wet[1]), wet[0], wet[1],
            null, state.activeInfluencesAt(wet[0], wet[1], 1)).waterReactionIntensity();
        assertTrue(lakeOnly > 0.0);
        assertTrue(bloodOnly > 0.0);
        assertTrue(together > bloodOnly);
    }

    @Test
    void bloodNightAndCowLakeWaterReactionRequiresCanonicalLiquidWater() {
        OverworldGeneration generation = new OverworldGeneration(9L, WorldgenEpoch.of(2));
        int[] wet = findWetLocation(generation);
        P8WorldState state = new P8WorldState(9L, 2, 0L);
        DeterministicId lakeId = DeterministicId.derive("cow-lake-fluid-test");
        state.anomalies().add(new SpatialAnomaly(lakeId, ResourceId.of("naturalexceptions", "cow_lake"),
            wet[0], wet[1], 100, EnvironmentalInfluence.of(lakeId, 0.6, 0, 0.4)));
        EventRecord blood = EventInstanceFactory.regular(9L, 2, P8WorldState.BLOOD_NIGHT,
            EventRegion.global(), 0, 0, 200, 0, P8WorldState.NIGHT_REACTIVITY, 100);
        state.events().schedule(blood);
        state.events().advance(1);
        ResolvedEnvironmentalContext combined = P8EnvironmentQuery.at(generation.sample(wet[0], wet[1]),
            wet[0], wet[1], state.anomalies(), state.activeInfluencesAt(wet[0], wet[1], 1));
        naturalexceptions.core.fluid.FluidParcel water = fluidWater();
        assertTrue(P8EnvironmentQuery.waterReactionIntensityFor(water, combined) > 0.0);
        assertEquals(0.0, P8EnvironmentQuery.waterReactionIntensityFor(fluidCopper(), combined), 0.0);
    }

    private static naturalexceptions.core.fluid.FluidParcel fluidWater() {
        naturalexceptions.core.composition.CanonicalComposition c = naturalexceptions.core.composition.CanonicalComposition
            .of(ResourceId.of("minecraft", "water"));
        naturalexceptions.core.material.CanonicalMaterialKey k = naturalexceptions.core.material.CanonicalMaterialKey
            .of(c, naturalexceptions.core.material.MaterialPhase.LIQUID, null, null);
        naturalexceptions.core.metallurgy.MaterialQuantity q = naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(1);
        naturalexceptions.core.property.MaterialProperties p = naturalexceptions.core.property.MaterialProperties.builder()
            .density(1).specificHeatSolid(4180).specificHeatLiquid(4180).meltingPointKelvin(273.15)
            .latentHeatOfFusion(334000).build();
        return new naturalexceptions.core.fluid.FluidParcel(ResourceIdMaterial.water(), k, c, q,
            naturalexceptions.core.thermal.ActiveThermalState.ambient(q.toKilograms(), p));
    }

    private static naturalexceptions.core.fluid.FluidParcel fluidCopper() {
        naturalexceptions.core.composition.CanonicalComposition c = naturalexceptions.core.composition.CanonicalComposition
            .of(ResourceId.of("naturalexceptions", "copper"));
        naturalexceptions.core.material.CanonicalMaterialKey k = naturalexceptions.core.material.CanonicalMaterialKey
            .of(c, naturalexceptions.core.material.MaterialPhase.LIQUID, null, null);
        naturalexceptions.core.metallurgy.MaterialQuantity q = naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(1);
        naturalexceptions.core.property.MaterialProperties p = naturalexceptions.content.material.BuiltinMaterials.COPPER_PROPERTIES;
        naturalexceptions.core.thermal.ActiveThermalState t = naturalexceptions.core.thermal.ActiveThermalState.atTemperature(
            1400, 1, q.toKilograms(), p);
        return new naturalexceptions.core.fluid.FluidParcel(naturalexceptions.content.material.BuiltinMaterials.COPPER.ref(),
            k, c, q, t);
    }

    private static final class ResourceIdMaterial {
        static naturalexceptions.core.material.MaterialRef water() {
            return naturalexceptions.core.material.MaterialRef.authored("minecraft", "water");
        }
    }

    @Test
    void lazyAnomalyRefinementUsesTheSameSeededSuitableSitesAndDurableIdentities() {
        P8WorldState first = new P8WorldState(901L, 2, 0L);
        P8WorldState second = new P8WorldState(901L, 2, 0L);
        for (int x = -20_480; x <= 20_480 && first.anomalies().size() == 0; x += 1024) {
            for (int z = -20_480; z <= 20_480 && first.anomalies().size() == 0; z += 1024) {
                first.ensureAnomaliesNear(x, z);
                second.ensureAnomaliesNear(x, z);
            }
        }
        assertTrue(first.anomalies().size() > 0);
        assertEquals(first.anomalies().snapshot(), second.anomalies().snapshot());
    }

    private static int[] findWetLocation(OverworldGeneration generation) {
        for (int x = -4096; x <= 4096; x += 64) for (int z = -4096; z <= 4096; z += 64) {
            if (generation.sample(x, z).hydrology().name().equals("WET_BASIN")) return new int[] { x, z };
        }
        throw new AssertionError("Expected at least one deterministic wet-basin sample");
    }
}
