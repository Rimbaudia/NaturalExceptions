package naturalexceptions.core.thermal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.property.ConstituentData;
import naturalexceptions.core.property.MaterialProperties;

final class ThermalTransferTest {

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
    void thermalTransferConservesModeledLocalEnergy() {
        MaterialProperties props = copperProps();
        FiniteThermalBody hotBody = FiniteThermalBody.atTemperature(500.0, 0.0, 1.0, props);
        FiniteThermalBody coldBody = FiniteThermalBody.atTemperature(300.0, 0.0, 1.0, props);

        double initialTotalEnergy = hotBody.currentEnergyJoules() + coldBody.currentEnergyJoules();

        double transferred = ThermalTransfer.transferConservative(hotBody, coldBody, 10000.0);
        assertEquals(10000.0, transferred, 0.001);

        double finalTotalEnergy = hotBody.currentEnergyJoules() + coldBody.currentEnergyJoules();
        assertEquals(initialTotalEnergy, finalTotalEnergy, 1.0e-6);
    }

    @Test
    void exchangeElapsedFlowsFromHotterToColder() {
        MaterialProperties props = copperProps();
        FiniteThermalBody hotBody = FiniteThermalBody.atTemperature(600.0, 0.0, 1.0, props);
        FiniteThermalBody coldBody = FiniteThermalBody.atTemperature(300.0, 0.0, 1.0, props);

        double initialTotalEnergy = hotBody.currentEnergyJoules() + coldBody.currentEnergyJoules();

        // 10 W/K conductance over 5 seconds
        double transferred = ThermalTransfer.exchangeElapsed(hotBody, coldBody, 10.0, 5.0);
        assertTrue(transferred > 0.0);

        assertTrue(hotBody.temperatureKelvin() < 600.0);
        assertTrue(coldBody.temperatureKelvin() > 300.0);

        // Strict conservation
        double finalTotalEnergy = hotBody.currentEnergyJoules() + coldBody.currentEnergyJoules();
        assertEquals(initialTotalEnergy, finalTotalEnergy, 1.0e-6);
    }

    @Test
    void exchangeElapsedNeverOvershootsEquilibrium() {
        MaterialProperties props = copperProps();
        FiniteThermalBody hotBody = FiniteThermalBody.atTemperature(600.0, 0.0, 1.0, props);
        FiniteThermalBody coldBody = FiniteThermalBody.atTemperature(300.0, 0.0, 1.0, props);

        // Massive conductance and elapsed time (infinite theoretical heat transfer)
        double transferred = ThermalTransfer.exchangeElapsed(hotBody, coldBody, 100000.0, 1000.0);
        assertTrue(transferred > 0.0);

        // Neither body may cross the other
        assertTrue(hotBody.temperatureKelvin() >= coldBody.temperatureKelvin() - 1.0e-6);
        assertEquals(hotBody.temperatureKelvin(), coldBody.temperatureKelvin(), 0.01);
    }

    @Test
    void exchangeWithAmbientReservoirCoolsToEquilibrium() {
        MaterialProperties props = copperProps();
        FiniteThermalBody sample = FiniteThermalBody.atTemperature(500.0, 0.0, 1.0, props);
        AmbientReservoir ambient = new AmbientReservoir(293.15);

        // Cool in steps
        for (int i = 0; i < 50; i++) {
            ThermalTransfer.exchangeElapsed(sample, ambient, 5.0, 10.0);
        }

        assertTrue(
            sample.state()
                .isEquilibratedWith(293.15, 0.5));
    }

    @Test
    void exchangeWithFixedTemperatureReservoirHeatsUp() {
        MaterialProperties props = copperProps();
        FiniteThermalBody sample = FiniteThermalBody.ambient(1.0, props);
        FixedTemperatureReservoir heater = new FixedTemperatureReservoir(1500.0);

        double transferred = ThermalTransfer.exchangeElapsed(sample, heater, 20.0, 10.0);
        assertTrue(transferred > 0.0);
        assertTrue(sample.temperatureKelvin() > 293.15);
    }
}
