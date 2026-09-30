package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

final class BiologicalStateTest {

    @Test
    void sharedStateStoresBoundedRegionsAndResolvesMetabolismByElapsedTime() {
        BiologicalState state = new BiologicalState(BuiltinBodyPlans.humanLike(), 100L);
        double initialEnergy = state.energyReserveJoules();
        double initialHydration = state.hydrationReserveMillilitres();

        assertEquals(
            6,
            state.regions()
                .size());
        assertTrue(
            state.physiology()
                .canBleed());
        assertFalse(state.hasActiveProcesses());
        state.advanceTo(1100L);

        // 1000 ticks is 50 s: at rest, 50 s of energy and water use.
        assertEquals(
            initialEnergy - 50.0 * state.physiology()
                .energyUseJoulesPerSecond(),
            state.energyReserveJoules(),
            1.0e-6);
        assertEquals(
            initialHydration - 50.0 * state.physiology()
                .hydrationUseMillilitresPerSecond(),
            state.hydrationReserveMillilitres(),
            1.0e-9);
        assertFalse(state.hasActiveProcesses());
        state.advanceTo(1100L);
        assertEquals(
            initialEnergy - 50.0 * state.physiology()
                .energyUseJoulesPerSecond(),
            state.energyReserveJoules(),
            1.0e-6);
    }

    @Test
    void elapsedResolutionRejectsBackwardTimeAndRegionStateIsLocal() {
        BiologicalState state = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        ResourceId leg = ResourceId.of("naturalexceptions", "left_leg");
        ResourceId arm = ResourceId.of("naturalexceptions", "left_arm");
        state.region(leg)
            .damageStructure(0.4);

        assertEquals(
            0.6,
            state.region(leg)
                .structuralIntegrity(),
            1.0e-12);
        assertEquals(
            1.0,
            state.region(arm)
                .structuralIntegrity(),
            1.0e-12);
        assertThrows(IllegalArgumentException.class, () -> state.advanceTo(-1L));
    }
}
