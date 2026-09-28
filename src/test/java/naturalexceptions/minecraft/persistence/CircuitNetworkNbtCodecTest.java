package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.core.electrical.CircuitNetwork;
import naturalexceptions.core.electrical.ResistiveConductor;
import naturalexceptions.core.electrical.ResistiveLoad;
import naturalexceptions.core.electrical.SwitchElement;
import naturalexceptions.core.electrical.VoltageSource;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class CircuitNetworkNbtCodecTest {
    @Test
    void physicalSwitchAndConductorFailureSurviveReloadWhileSolutionIsRebuilt() {
        MaterialProperties props = MaterialProperties.builder().density(8.96).specificHeatSolid(385)
            .specificHeatLiquid(410).meltingPointKelvin(1357).latentHeatOfFusion(205000)
            .electricalResistivity(1.68e-8).build();
        ComponentDamage damage = new ComponentDamage();
        ResistiveConductor wire = new ResistiveConductor("wire", MaterialRef.authored("minecraft", "copper"),
            props, 1, 1e-6, ActiveThermalState.ambient(.001, props), damage, 1200);
        SwitchElement toggle = new SwitchElement("switch", true);
        ResistiveLoad load = new ResistiveLoad("load", 20, ActiveThermalState.ambient(.01, props),
            new ComponentDamage(), 1200);
        CircuitNetwork network = new CircuitNetwork("persisted-network", new VoltageSource("source", 12), toggle, load);
        network.addConductor(wire);
        wire.fracture();

        NBTTagCompound nbt = CircuitNetworkNbtCodec.write(network);
        CircuitNetwork restored = CircuitNetworkNbtCodec.read(nbt);
        assertTrue(restored.switchElement().closed());
        assertTrue(restored.conductors().get(0).damage().isFractured());
        assertEquals(0.0, restored.solve().currentAmps(), 0.0);
        assertEquals(12.0, restored.source().voltageVolts(), 0.0);
    }
}
