package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.thermal.ActiveThermalState;

/** An electric element heats the charge with all it draws, and no hotter than itself. */
final class ElectricHeatTest {

    private static VesselState copper(double grams) {
        VesselState vessel = new VesselState();
        MaterialQuantity mass = MaterialQuantity.ofGrams(grams);
        CanonicalComposition comp = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(comp),
                comp,
                mass,
                ActiveThermalState.ambient(mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));
        return vessel;
    }

    @Test
    void everyJouleGoesIntoTheCharge() {
        VesselState vessel = copper(1000.0);
        double before = vessel.layers()
            .get(0)
            .temperatureKelvin();
        double taken = vessel.addElectricHeat(38500.0); // 1 kg of copper, 385 J/K: 100 K
        assertEquals(38500.0, taken, 1e-6);
        assertEquals(
            before + 100.0,
            vessel.layers()
                .get(0)
                .temperatureKelvin(),
            0.5);
    }

    @Test
    void noHotterThanTheElementAndAnEmptyVesselTakesNothing() {
        VesselState vessel = copper(100.0);
        for (int i = 0; i < 400; i++) vessel.addElectricHeat(10000.0);
        assertTrue(
            vessel.layers()
                .get(0)
                .temperatureKelvin() < VesselState.ELEMENT_KELVIN + 300.0,
            "it stops heating past its element");
        assertEquals(0.0, new VesselState().addElectricHeat(5000.0), 0.0, "nothing to heat");
    }
}
