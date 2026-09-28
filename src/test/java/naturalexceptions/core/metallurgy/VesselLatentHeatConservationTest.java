package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.core.thermal.ThermodynamicModel;

/** Melting and reduction must never manufacture the latent heat they need. */
final class VesselLatentHeatConservationTest {

    /** Ambient hotter than anything in the vessel: no loss path is active, so energy must be exactly conserved. */
    private static final double NO_LOSS_AMBIENT_K = 10_000.0;

    @BeforeAll
    static void init() {
        BuiltinContent.register();
    }

    private static double vesselEnergy(VesselState vessel) {
        double total = 0.0;
        for (AggregateLayer layer : vessel.layers()) total += layer.thermalState()
            .energyJoules();
        return total;
    }

    @Test
    void refinedMetalAtMeltOnsetDoesNotGainLatentHeat() {
        VesselState vessel = new VesselState();
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        double kg = 0.5;
        double melt = BuiltinMaterials.COPPER_PROPERTIES.meltingPointKelvin();
        double onset = ThermodynamicModel.energyForTemperature(melt, 0.0, kg, BuiltinMaterials.COPPER_PROPERTIES);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(cu),
                cu,
                MaterialQuantity.ofKilograms(kg),
                new ActiveThermalState(onset + 1.0, kg, BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));
        double before = vesselEnergy(vessel);

        vessel.step(1, NO_LOSS_AMBIENT_K);

        assertEquals(
            AggregatePhaseType.MOLTEN_METAL,
            vessel.layers()
                .get(0)
                .phaseType());
        assertEquals(before, vesselEnergy(vessel), 1.0e-6);
        assertTrue(
            vessel.layers()
                .get(0)
                .thermalState()
                .meltFraction() < 0.01,
            "melting must still require latent heat from the fire");
    }

    @Test
    void oreReductionPartitionsChargeEnergyAndVentsGasShare() {
        VesselState vessel = new VesselState();
        OreFeedstockDefinition ore = OreFeedstockDefinition.copperOre();
        vessel.addRawOre(ore);
        AggregateLayer charge = vessel.layers()
            .get(0);
        double kg = charge.mass()
            .toKilograms();
        double onset = ThermodynamicModel.energyForTemperature(
            ore.metallicProperties()
                .meltingPointKelvin(),
            0.0,
            kg,
            ore.metallicProperties());
        charge.thermalState()
            .setEnergyJoules(onset + 1.0);
        double before = vesselEnergy(vessel);
        double ventedBefore = vessel.exhaustSink()
            .ventedEnergyJoules();

        vessel.step(1, NO_LOSS_AMBIENT_K);

        double vented = vessel.exhaustSink()
            .ventedEnergyJoules() - ventedBefore;
        assertEquals(before, vesselEnergy(vessel) + vented, 1.0e-6);
    }

    @Test
    void remeltCycleNeverRaisesEnergyWithoutFuel() {
        VesselState vessel = new VesselState();
        CanonicalComposition cu = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        double kg = 0.25;
        double hot = ThermodynamicModel.energyForTemperature(1500.0, 1.0, kg, BuiltinMaterials.COPPER_PROPERTIES);
        vessel.remelt(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(cu),
                cu,
                MaterialQuantity.ofKilograms(kg),
                new ActiveThermalState(hot, kg, BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.MOLTEN_METAL));
        double previous = vesselEnergy(vessel);
        for (int i = 0; i < 200; i++) {
            vessel.step(20, 293.15);
            double now = vesselEnergy(vessel);
            assertTrue(now <= previous + 1.0e-6, "cooling vessel gained energy at step " + i);
            previous = now;
        }
        assertEquals(hot, previous + vessel.surroundingsHeatJoules(), 1.0e-3);
    }
}
