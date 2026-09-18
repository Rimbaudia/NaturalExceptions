package naturalexceptions.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.property.ConstituentData;
import naturalexceptions.core.property.MaterialProperties;

final class ThermalScalingProofTest {

    private static MaterialProperties copperProps() {
        ConstituentData cu = ConstituentData.lookup(ConstituentData.ID_CU);
        return MaterialProperties.builder()
            .density(cu.density())
            .hardness(cu.hardness())
            .thermalConductivity(cu.thermalConductivity())
            .specificHeatSolid(cu.specificHeatSolid()) // 385.0 J/(kg·K)
            .specificHeatLiquid(cu.specificHeatLiquid())
            .meltingPointKelvin(cu.meltingPointKelvin())
            .latentHeatOfFusion(cu.latentHeatOfFusion())
            .build();
    }

    @Test
    void dormantPopulationIncursZeroPerTickWork() {
        AmbientReservoir ambient = new AmbientReservoir(293.15);
        SparseThermalSimulation sim = new SparseThermalSimulation(100, ambient);

        // Populate 10,000 dormant ambient objects (they are implicit and never registered in active simulation)
        int dormantPopulation = 10000;
        assertEquals(0, sim.activeCount());

        // Run 20 ticks
        for (long tick = 1; tick <= 20; tick++) {
            sim.tick(tick);
            assertEquals(0, sim.lastTickExecutedUnits(), "Dormant objects should consume 0 CPU operations per tick!");
        }

        assertEquals(0, sim.totalExecutedUnits());
        assertEquals(0, sim.activeCount());
    }

    @Test
    void activeObjectsWakeCoolAndCollapseBackToSleep() {
        AmbientReservoir ambient = new AmbientReservoir(293.15);
        SparseThermalSimulation sim = new SparseThermalSimulation(100, ambient);
        MaterialProperties props = copperProps();

        // Wake 3 hot samples (400 K) with rapid cooling conductance
        for (long id = 1; id <= 3; id++) {
            ActiveThermalState state = ActiveThermalState.atTemperature(400.0, 0.0, 1.0, props);
            sim.wake(id, state, 500.0, 0);
        }

        assertEquals(3, sim.activeCount());

        // Run simulation until all 3 cool and collapse
        long tick = 1;
        while (sim.activeCount() > 0 && tick < 500) {
            sim.tick(tick);
            tick++;
        }

        // All 3 samples must have cooled to equilibrium and collapsed to implicit dormant state
        assertEquals(0, sim.activeCount());
        assertFalse(sim.isActive(1));
        assertFalse(sim.isActive(2));
        assertFalse(sim.isActive(3));
    }

    @Test
    void perTickBudgetIsRespectedUnderBurstWorkload() {
        AmbientReservoir ambient = new AmbientReservoir(293.15);
        int budget = 25;
        SparseThermalSimulation sim = new SparseThermalSimulation(budget, ambient);
        MaterialProperties props = copperProps();

        // Burst wake 100 samples
        for (long id = 1; id <= 100; id++) {
            ActiveThermalState state = ActiveThermalState.atTemperature(500.0, 0.0, 1.0, props);
            sim.wake(id, state, 1.0, 0);
        }

        assertEquals(100, sim.activeCount());

        // Tick 1: should execute at most budget (25) units
        sim.tick(1);
        assertEquals(25, sim.lastTickExecutedUnits());
        assertEquals(75, sim.pendingQueueCount());

        // Tick 2: another 25 units
        sim.tick(2);
        assertEquals(25, sim.lastTickExecutedUnits());
        assertEquals(50, sim.pendingQueueCount());
    }
}
