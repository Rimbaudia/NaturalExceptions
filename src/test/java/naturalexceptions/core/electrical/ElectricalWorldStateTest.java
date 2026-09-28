package naturalexceptions.core.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class ElectricalWorldStateTest {
    @Test
    void unchangedSolvedCircuitSleepsAndOnlyItsSwitchQueuesItAgain() {
        MaterialProperties props = MaterialProperties.builder().density(8.96).specificHeatSolid(385)
            .specificHeatLiquid(410).meltingPointKelvin(1357).latentHeatOfFusion(205000)
            .electricalResistivity(1.68e-8).build();
        ResistiveLoad load = new ResistiveLoad("load", 20, ActiveThermalState.ambient(.01, props),
            new ComponentDamage(), 5000);
        SwitchElement toggle = new SwitchElement("switch", false);
        CircuitNetwork circuit = new CircuitNetwork("one", new VoltageSource("source", 12), toggle, load);
        circuit.addConductor(new ResistiveConductor("wire", MaterialRef.authored("minecraft", "copper"), props,
            1, 1e-6, ActiveThermalState.ambient(.001, props), new ComponentDamage(), 5000));
        ElectricalWorldState world = new ElectricalWorldState();
        world.register(circuit);

        assertEquals(1, world.processWork(.05, 4));
        assertEquals(0, world.activeNetworkCount());
        assertEquals(1, world.sleepingNetworkCount());
        assertEquals(0, world.processWork(.05, 4));
        toggle.setClosed(true);
        assertEquals(1, world.dirtyNetworkCount());
        assertEquals(2, world.processWork(.05, 4));
        assertTrue(world.activeNetworkCount() > 0);
    }

    @Test
    void switchChangeDoesNotRecomputeAnUnrelatedNetwork() {
        MaterialProperties props = MaterialProperties.builder().density(8.96).specificHeatSolid(385)
            .specificHeatLiquid(410).meltingPointKelvin(1357).latentHeatOfFusion(205000)
            .electricalResistivity(1.68e-8).build();
        SwitchElement firstSwitch = new SwitchElement("first_switch", false);
        CircuitNetwork first = new CircuitNetwork("first", new VoltageSource("first_source", 12), firstSwitch,
            new ResistiveLoad("first_load", 10, ActiveThermalState.ambient(.01, props), new ComponentDamage(), 900));
        CircuitNetwork second = new CircuitNetwork("second", new VoltageSource("second_source", 9),
            new SwitchElement("second_switch", false), new ResistiveLoad("second_load", 5,
                ActiveThermalState.ambient(.01, props), new ComponentDamage(), 900));
        ElectricalWorldState world = new ElectricalWorldState();
        world.register(first);
        world.register(second);
        world.processWork(0.0, 8);
        long secondSolves = second.solveCount();

        firstSwitch.setClosed(true);
        assertEquals(1, world.dirtyNetworkCount());
        world.processWork(0.0, 8);

        assertEquals(secondSolves, second.solveCount());
        assertEquals(2, first.solveCount());
    }
}
