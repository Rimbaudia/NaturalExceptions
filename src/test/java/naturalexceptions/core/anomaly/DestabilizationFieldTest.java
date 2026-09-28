package naturalexceptions.core.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class DestabilizationFieldTest {

    @Test
    void partitionsNegativeCoordinatesWithFloorDivision() {
        assertEquals(new RegionKey(-1, -1), RegionKey.fromBlock(-1, -1));
        assertEquals(new RegionKey(-2, -1), RegionKey.fromBlock(-513, -512));
        assertEquals(new RegionKey(1, 0), RegionKey.fromBlock(512, 511));
    }

    @Test
    void explicitCauseRaisesLocalScalarAndNeighborReceivesCoarseSpread() {
        DestabilizationField field = new DestabilizationField();
        RegionKey source = new RegionKey(0, 0);
        RegionKey neighbor = new RegionKey(1, 0);
        field.ensureRegion(source, 0);
        field.increase(source, ResourceId.of("naturalexceptions", "blood_night"),
            DeterministicId.derive("blood-night-instance"), 0.8, 0);
        field.advanceCoarse(DestabilizationField.COARSE_STEP_TICKS);
        assertTrue(field.value(source) < 0.8); // causes no longer continuously hold the scalar up
        assertTrue(field.value(neighbor) > 0.0);
        assertEquals(1, field.regionCount()); // spread fronts are derived on demand, not materialized
        assertEquals(1, field.snapshot().regions().size());
    }

    @Test
    void spreadAdvancesOneRegionRingPerCoarseTransitionRatherThanSynchronizingGlobally() {
        DestabilizationField field = new DestabilizationField();
        RegionKey source = new RegionKey(0, 0);
        field.increase(source, ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("spread-source"), 1.0, 0);
        field.advanceCoarse(DestabilizationField.COARSE_STEP_TICKS);
        assertEquals(0.0, field.value(new RegionKey(2, 0)), 0);
        field.advanceCoarse(2 * DestabilizationField.COARSE_STEP_TICKS);
        assertTrue(field.value(new RegionKey(2, 0)) > 0.0);
    }

    @Test
    void largeCatchupUsesBoundedCoarseWorkAndClosedFormTail() {
        DestabilizationField field = new DestabilizationField();
        RegionKey source = new RegionKey(0, 0);
        field.increase(source, ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("cause"), 1.0, 0);
        long farFuture = 80L * 365L * DestabilizationField.COARSE_STEP_TICKS;
        field.advanceCoarse(farFuture);
        assertTrue(field.lastAdvanceCoarseSteps() <= DestabilizationField.MAX_COARSE_STEPS_PER_ADVANCE + 1);
        assertTrue(field.value(source) >= 0.0 && field.value(source) < 1e-8);
        assertTrue(field.derivedGlobal() < 1e-8);
    }

    @Test
    void loadedAndDormantResolutionAgreeAcrossLargeElapsedInterval() {
        long target = 80L * DestabilizationField.COARSE_STEP_TICKS;
        RegionKey source = new RegionKey(0, 0);
        DestabilizationField loaded = new DestabilizationField();
        DestabilizationField dormant = new DestabilizationField();
        ResourceId cause = ResourceId.of("naturalexceptions", "test_cause");
        DeterministicId id = DeterministicId.derive("loaded-dormant-equivalence");
        loaded.increase(source, cause, id, 0.8, 0);
        dormant.increase(source, cause, id, 0.8, 0);
        for (long tick = DestabilizationField.COARSE_STEP_TICKS; tick <= target;
            tick += DestabilizationField.COARSE_STEP_TICKS) loaded.advanceCoarse(tick);
        dormant.advanceCoarse(target);
        assertEquals(loaded.value(source), dormant.value(source), 1.0e-10);
        assertEquals(loaded.value(new RegionKey(1, 0)), dormant.value(new RegionKey(1, 0)), 1.0e-10);
        assertEquals(loaded.derivedGlobal(), dormant.derivedGlobal(), 1.0e-10);
    }

    @Test
    void globalMeasureIsDerivedFromSparseRegionalStateAndRoundTrips() {
        DestabilizationField field = new DestabilizationField();
        field.increase(new RegionKey(2, -1), ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("cause-1"), 0.5, 0);
        field.increase(new RegionKey(-3, 4), ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("cause-2"), 0.25, 0);
        assertEquals(0.375, field.derivedGlobal(), 1e-12);
        DestabilizationField restored = DestabilizationField.restore(field.snapshot());
        assertEquals(field.snapshot(), restored.snapshot());
        assertEquals(field.derivedGlobal(), restored.derivedGlobal(), 0);
    }

    @Test
    void ordinaryCallsHaveNoTickReplayCounter() {
        DestabilizationField field = new DestabilizationField();
        field.increase(new RegionKey(0, 0), ResourceId.of("naturalexceptions", "experiment"),
            DeterministicId.derive("ordinary-test"), 0.2, 0);
        field.advanceCoarse(1_000_000_000L);
        assertEquals(0, field.lastAdvanceTickReplayCount());
    }

    @Test
    void advancementBeforeWakeBoundaryDoesNotResolveSparseRegions() {
        DestabilizationField field = new DestabilizationField();
        RegionKey source = new RegionKey(0, 0);
        field.increase(source, ResourceId.of("naturalexceptions", "test_cause"),
            DeterministicId.derive("early-cause"), 0.5, 0);
        assertEquals(DestabilizationField.COARSE_STEP_TICKS, field.nextCoarseUpdateTick());
        assertTrue(!field.isCoarseUpdateDue(DestabilizationField.COARSE_STEP_TICKS - 1));
        field.advanceCoarse(DestabilizationField.COARSE_STEP_TICKS - 1);
        assertEquals(0.5, field.value(source), 0.0);
        assertEquals(0, field.lastAdvanceCoarseSteps());
        assertTrue(field.isCoarseUpdateDue(DestabilizationField.COARSE_STEP_TICKS));
    }
}
