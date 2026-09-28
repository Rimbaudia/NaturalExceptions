package naturalexceptions.core.electrical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class ElectricalCoreTest {
    private static MaterialProperties copper(double resistivity) {
        return MaterialProperties.builder().density(8.96).specificHeatSolid(385.0).specificHeatLiquid(410.0)
            .meltingPointKelvin(1357.77).latentHeatOfFusion(205000.0).electricalResistivity(resistivity).build();
    }

    @Test
    void seriesCircuitObeysOhmsLawAndOpenSwitchInterruptsCurrent() {
        MaterialProperties copper = copper(2.0);
        ResistiveConductor wire = new ResistiveConductor("wire", MaterialRef.authored("minecraft", "copper"),
            copper, 2.0, 1.0, ActiveThermalState.ambient(0.001, copper), new ComponentDamage(), 10000.0);
        SwitchElement toggle = new SwitchElement("switch", false);
        ResistiveLoad load = new ResistiveLoad("heater", 2.0,
            ActiveThermalState.ambient(0.01, copper), new ComponentDamage(), 10000.0);
        CircuitNetwork network = new CircuitNetwork("proof", new VoltageSource("battery", 12.0), toggle, load);
        network.addConductor(wire);

        CircuitSolution open = network.solve();
        assertEquals(0.0, open.currentAmps(), 0.0);
        toggle.setClosed(true);
        CircuitSolution closed = network.solve();
        assertEquals(6.0, closed.totalResistanceOhms(), 1.0e-9);
        assertEquals(2.0, closed.currentAmps(), 1.0e-9);
        assertEquals(8.0, closed.loadPowerWatts(), 1.0e-9);
        assertEquals(16.0, closed.conductorPowerWatts(), 1.0e-9);
        assertEquals(closed.voltageVolts() * closed.currentAmps(),
            closed.loadPowerWatts() + closed.conductorPowerWatts(), 1.0e-9);
        assertFalse(network.isDirty());
    }

    @Test
    void materialAndGeometrySetResistanceAndStableSolutionsSleep() {
        MaterialProperties material = copper(1.0);
        ResistiveConductor shortWide = new ResistiveConductor("a", MaterialRef.authored("minecraft", "copper"),
            material, 1.0, 1.0, ActiveThermalState.ambient(0.001, material), new ComponentDamage(), 5000.0);
        ResistiveConductor longThin = new ResistiveConductor("b", MaterialRef.authored("minecraft", "copper"),
            material, 2.0, 0.5, ActiveThermalState.ambient(0.001, material), new ComponentDamage(), 5000.0);
        assertEquals(1.0, shortWide.resistanceOhms(), 1.0e-12);
        assertEquals(4.0, longThin.resistanceOhms(), 1.0e-12);
        CircuitNetwork network = new CircuitNetwork("sleep", new VoltageSource("source", 12.0),
            new SwitchElement("switch", false), new ResistiveLoad("load", 4.0,
                ActiveThermalState.ambient(0.01, material), new ComponentDamage(), 5000.0));
        network.addConductor(shortWide);
        network.solve();
        long solved = network.solveCount();
        network.solve();
        network.solve();
        assertEquals(solved, network.solveCount());
        assertEquals(0.0, network.solve().currentAmps(), 0.0);
    }

    @Test
    void electricalPowerBecomesThermalEnergyAndFracturedWireOpensNetwork() {
        MaterialProperties conductorProperties = copper(1.68e-8);
        ComponentDamage damage = new ComponentDamage();
        // A 1 m copper wire with 1e-8 m² area has 8.96e-5 kg from its material density.
        ActiveThermalState wireHeat = ActiveThermalState.ambient(8.96e-5, conductorProperties);
        ResistiveConductor wire = new ResistiveConductor("fuse", MaterialRef.authored("minecraft", "copper"),
            conductorProperties, 1.0, 1.0e-8, wireHeat, damage, conductorProperties.meltingPointKelvin());
        MaterialProperties loadProperties = copper(1.0);
        ResistiveLoad load = new ResistiveLoad("load", 1.0,
            ActiveThermalState.ambient(0.001, loadProperties), new ComponentDamage(), 5000.0);
        SwitchElement toggle = new SwitchElement("switch", true);
        CircuitNetwork network = new CircuitNetwork("failure", new VoltageSource("source", 12.0), toggle, load);
        network.addConductor(wire);
        CircuitSolution initial = network.solve();
        double energyBefore = wireHeat.energyJoules();

        network.advanceElapsed(2.0);

        assertEquals(initial.conductorPowerWatts() * 2.0, wireHeat.energyJoules() - energyBefore, 1.0e-6);
        assertEquals(initial.voltageVolts() * initial.currentAmps() * 2.0,
            wireHeat.energyJoules() - energyBefore + load.thermalState().energyJoules()
                - ActiveThermalState.ambient(0.001, loadProperties).energyJoules(), 1.0e-6);
        assertEquals(naturalexceptions.core.material.MaterialPhase.LIQUID, wireHeat.phase());
        assertTrue(damage.isFractured());
        assertTrue(network.isDirty());
        assertEquals(0.0, network.solve().currentAmps(), 0.0);
        assertFalse(network.isDirty());
    }

    @Test
    void unrepresentableElapsedHeatingIsRejectedBeforeAnyThermalMutation() {
        MaterialProperties props = copper(1.0);
        ActiveThermalState wireHeat = ActiveThermalState.ambient(0.001, props);
        ActiveThermalState loadHeat = ActiveThermalState.ambient(0.001, props);
        CircuitNetwork network = new CircuitNetwork("overflow", new VoltageSource("source", 12),
            new SwitchElement("switch", true), new ResistiveLoad("load", 1, loadHeat, new ComponentDamage(), 5000));
        network.addConductor(new ResistiveConductor("wire", MaterialRef.authored("minecraft", "copper"), props,
            1, 1, wireHeat, new ComponentDamage(), 5000));
        double wireBefore = wireHeat.energyJoules();
        double loadBefore = loadHeat.energyJoules();

        assertThrows(IllegalArgumentException.class, () -> network.advanceElapsed(Double.MAX_VALUE));
        assertEquals(wireBefore, wireHeat.energyJoules(), 0.0);
        assertEquals(loadBefore, loadHeat.energyJoules(), 0.0);
    }
}
