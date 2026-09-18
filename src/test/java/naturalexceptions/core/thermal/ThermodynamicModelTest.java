package naturalexceptions.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.property.ConstituentData;
import naturalexceptions.core.property.MaterialProperties;

final class ThermodynamicModelTest {

    private static MaterialProperties copperProps() {
        ConstituentData cu = ConstituentData.lookup(ConstituentData.ID_CU);
        return MaterialProperties.builder()
            .density(cu.density())
            .hardness(cu.hardness())
            .thermalConductivity(cu.thermalConductivity())
            .specificHeatSolid(cu.specificHeatSolid()) // 385.0 J/(kg·K)
            .specificHeatLiquid(cu.specificHeatLiquid()) // 410.0 J/(kg·K)
            .meltingPointKelvin(cu.meltingPointKelvin()) // 1357.77 K
            .latentHeatOfFusion(cu.latentHeatOfFusion()) // 205,000.0 J/kg
            .electricalResistivity(cu.electricalResistivity())
            .build();
    }

    private static MaterialProperties tinProps() {
        ConstituentData sn = ConstituentData.lookup(ConstituentData.ID_SN);
        return MaterialProperties.builder()
            .density(sn.density())
            .hardness(sn.hardness())
            .thermalConductivity(sn.thermalConductivity())
            .specificHeatSolid(sn.specificHeatSolid()) // 228.0 J/(kg·K)
            .specificHeatLiquid(sn.specificHeatLiquid()) // 250.0 J/(kg·K)
            .meltingPointKelvin(sn.meltingPointKelvin()) // 505.08 K
            .latentHeatOfFusion(sn.latentHeatOfFusion()) // 59,600.0 J/kg
            .build();
    }

    @Test
    void thermalEnergyDeterminesTemperature() {
        MaterialProperties props = copperProps();
        double massKg = 1.0;
        // 1.0 kg copper at 300 K: E = 1.0 * 385.0 * 300 = 115,500 J
        double energy = 115500.0;

        double derivedTemp = ThermodynamicModel.deriveTemperature(energy, massKg, props);
        assertEquals(300.0, derivedTemp, 0.001);
    }

    @Test
    void differentHeatCapacityChangesTemperatureResponse() {
        MaterialProperties cuProps = copperProps(); // c_p = 385 J/(kg·K)
        MaterialProperties snProps = tinProps(); // c_p = 228 J/(kg·K)
        double massKg = 1.0;
        double energyAdded = 10000.0; // 10 kJ

        ActiveThermalState cuState = ActiveThermalState.ambient(massKg, cuProps);
        ActiveThermalState snState = ActiveThermalState.ambient(massKg, snProps);

        double cuInitialTemp = cuState.temperatureKelvin();
        double snInitialTemp = snState.temperatureKelvin();

        cuState.addEnergyJoules(energyAdded);
        snState.addEnergyJoules(energyAdded);

        double cuDeltaT = cuState.temperatureKelvin() - cuInitialTemp;
        double snDeltaT = snState.temperatureKelvin() - snInitialTemp;

        // Tin has lower heat capacity, so its temperature rise must be higher
        assertTrue(snDeltaT > cuDeltaT);
        assertEquals(energyAdded / (massKg * 385.0), cuDeltaT, 0.01);
        assertEquals(energyAdded / (massKg * 228.0), snDeltaT, 0.01);
    }

    @Test
    void addingEnergyNeverLowersTemperatureOutsideTransitionModel() {
        MaterialProperties props = copperProps();
        double massKg = 1.0;
        ActiveThermalState state = ActiveThermalState.ambient(massKg, props);

        double prevTemp = state.temperatureKelvin();
        // Add energy in small increments across solid, melting, and superheating regimes
        for (int i = 0; i < 200; i++) {
            state.addEnergyJoules(5000.0);
            double currentTemp = state.temperatureKelvin();
            assertTrue(
                currentTemp >= prevTemp,
                "Temperature decreased from " + prevTemp + " to " + currentTemp + " after adding energy!");
            prevTemp = currentTemp;
        }
    }

    @Test
    void removingEnergyNeverRaisesTemperatureOutsideTransitionModel() {
        MaterialProperties props = copperProps();
        double massKg = 1.0;
        // Start at 1600 K (liquid copper)
        ActiveThermalState state = ActiveThermalState.atTemperature(1600.0, 1.0, massKg, props);

        double prevTemp = state.temperatureKelvin();
        // Remove energy progressively
        for (int i = 0; i < 200; i++) {
            state.removeEnergyJoules(5000.0);
            double currentTemp = state.temperatureKelvin();
            assertTrue(
                currentTemp <= prevTemp,
                "Temperature increased from " + prevTemp + " to " + currentTemp + " after removing energy!");
            prevTemp = currentTemp;
        }
    }

    @Test
    void phaseTransitionConsumesOrReleasesDeclaredEnergy() {
        MaterialProperties props = copperProps();
        double massKg = 1.0;
        double tm = props.meltingPointKelvin();
        double latentHeat = props.latentHeatOfFusion(); // 205,000 J

        // Initialize copper at solidus (T = Tm, meltFraction = 0.0)
        ActiveThermalState state = ActiveThermalState.atTemperature(tm, 0.0, massKg, props);
        assertEquals(tm, state.temperatureKelvin(), 0.001);
        assertEquals(0.0, state.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, state.phase());

        // Add 50% of latent heat
        state.addEnergyJoules(latentHeat * 0.5);
        assertEquals(tm, state.temperatureKelvin(), 0.001); // Temperature must remain pinned at Tm
        assertEquals(0.5, state.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, state.phase()); // In mushy zone, not yet fully liquid

        // Add remaining 50% of latent heat
        state.addEnergyJoules(latentHeat * 0.5);
        assertEquals(tm, state.temperatureKelvin(), 0.001);
        assertEquals(1.0, state.meltFraction(), 0.001);
        assertEquals(MaterialPhase.LIQUID, state.phase()); // Transition complete

        // Further energy superheats liquid
        state.addEnergyJoules(4100.0); // 10 K rise (c_p,liquid = 410 J/(kg·K))
        assertEquals(tm + 10.0, state.temperatureKelvin(), 0.01);

        // Reverse: remove energy
        state.removeEnergyJoules(4100.0);
        assertEquals(tm, state.temperatureKelvin(), 0.01);
        assertEquals(1.0, state.meltFraction(), 0.001);

        // Remove half latent heat (freezing plateau)
        state.removeEnergyJoules(latentHeat * 0.5);
        assertEquals(tm, state.temperatureKelvin(), 0.001);
        assertEquals(0.5, state.meltFraction(), 0.001);

        // Remove rest of latent heat
        state.removeEnergyJoules(latentHeat * 0.5);
        assertEquals(tm, state.temperatureKelvin(), 0.001);
        assertEquals(0.0, state.meltFraction(), 0.001);
        assertEquals(MaterialPhase.SOLID, state.phase());
    }

    @Test
    void activeThermalStateCanCollapseAtEquilibrium() {
        MaterialProperties props = copperProps();
        double massKg = 1.0;
        ActiveThermalState state = ActiveThermalState.ambient(massKg, props);

        assertTrue(state.isEquilibratedWith(293.15, 0.1));

        // Heat up to 400 K
        state.addEnergyJoules(38500.0);
        assertFalse(state.isEquilibratedWith(293.15, 0.1));

        // Cool back down
        state.removeEnergyJoules(38500.0);
        assertTrue(state.isEquilibratedWith(293.15, 0.1));
    }

    @Test
    void rejectsNegativeMassOrNegativeEnergy() {
        MaterialProperties props = copperProps();
        assertThrows(IllegalArgumentException.class, () -> new ActiveThermalState(-10.0, 1.0, props));
        assertThrows(IllegalArgumentException.class, () -> new ActiveThermalState(100.0, -1.0, props));
        assertThrows(IllegalArgumentException.class, () -> new ActiveThermalState(100.0, 0.0, props));
    }
}
