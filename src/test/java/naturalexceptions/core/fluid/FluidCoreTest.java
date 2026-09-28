package naturalexceptions.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class FluidCoreTest {

    private static FluidParcel water(long mg) {
        CanonicalComposition composition = CanonicalComposition.of(ResourceId.of("minecraft", "water"));
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        MaterialProperties props = MaterialProperties.builder().density(1.0).specificHeatSolid(4180.0)
            .specificHeatLiquid(4180.0).meltingPointKelvin(273.15).latentHeatOfFusion(334000.0).build();
        MaterialQuantity quantity = MaterialQuantity.ofMilligrams(mg);
        ActiveThermalState thermal = ActiveThermalState.ambient(quantity.toKilograms(), props);
        return new FluidParcel(MaterialRef.authored("minecraft", "water"), key, composition, quantity, thermal);
    }

    @Test
    void parcelRetainsCanonicalIdentityAndQuantizedVolume() {
        FluidParcel water = water(1000000L);
        assertEquals(MaterialRef.authored("minecraft", "water"), water.materialRef());
        assertEquals(MaterialPhase.LIQUID, water.phase());
        assertEquals(MaterialQuantity.ofMilligrams(1000000L), water.mass());
        assertEquals(1_000_000L, water.volumeMicrolitres());
    }

    @Test
    void finiteVesselFillDrainAndCapacityConserveMassAndEnergy() {
        FluidParcel incoming = water(1000000L);
        long inputMass = incoming.mass().milligrams();
        double inputEnergy = incoming.thermalState().energyJoules();
        FluidVessel vessel = new FluidVessel(750_000L, FluidInteractionRules.sameMaterialOnly());

        assertEquals(MaterialQuantity.ofMilligrams(750000L), vessel.fill(incoming));
        assertEquals(inputMass, vessel.totalMass().milligrams() + incoming.mass().milligrams());
        assertEquals(MaterialQuantity.ofMilligrams(750000L), vessel.contents().get(0).mass());
        FluidParcel drained = vessel.drain(MaterialQuantity.ofMilligrams(500000L));
        assertEquals(MaterialQuantity.ofMilligrams(500000L), drained.mass());
        assertEquals(inputEnergy, vessel.totalThermalEnergyJoules() + incoming.thermalState().energyJoules()
            + drained.thermalState().energyJoules(), 1.0e-6);
    }

    @Test
    void incompatibleLiquidsRemainSeparateAndDensityOrdered() {
        FluidParcel water = water(1000000L);
        CanonicalComposition oilComp = CanonicalComposition.of(ResourceId.of("naturalexceptions", "oil"));
        CanonicalMaterialKey oilKey = CanonicalMaterialKey.of(oilComp, MaterialPhase.LIQUID, null, null);
        MaterialProperties oilProps = MaterialProperties.builder().density(0.8).specificHeatSolid(2000.0)
            .specificHeatLiquid(2000.0).meltingPointKelvin(250.0).build();
        MaterialQuantity oilMass = MaterialQuantity.ofMilligrams(800000L);
        FluidParcel oil = new FluidParcel(MaterialRef.authored("naturalexceptions", "oil"), oilKey, oilComp,
            oilMass, ActiveThermalState.ambient(oilMass.toKilograms(), oilProps));
        FluidVessel vessel = new FluidVessel(3000000L, FluidInteractionRules.immiscibleByDefault());
        vessel.fill(oil);
        vessel.fill(water);

        assertEquals(2, vessel.contents().size());
        assertNotEquals(vessel.contents().get(0).materialRef(), vessel.contents().get(1).materialRef());
        assertEquals(water.materialRef(), vessel.contents().get(0).materialRef());
        assertNull(vessel.lastTransformation());
    }

    @Test
    void registeredMisciblePairProducesCanonicalMixedMaterial() {
        FluidParcel water = water(1000000L);
        CanonicalComposition saltComposition = CanonicalComposition.of(ResourceId.of("naturalexceptions", "salt"));
        CanonicalMaterialKey saltKey = CanonicalMaterialKey.of(saltComposition, MaterialPhase.LIQUID, null, null);
        MaterialProperties saltProperties = MaterialProperties.builder().density(2.0).specificHeatSolid(800.0)
            .specificHeatLiquid(800.0).meltingPointKelvin(800.0).build();
        MaterialQuantity saltMass = MaterialQuantity.ofMilligrams(1000000L);
        FluidParcel salt = new FluidParcel(MaterialRef.authored("naturalexceptions", "salt"), saltKey,
            saltComposition, saltMass, ActiveThermalState.ambient(saltMass.toKilograms(), saltProperties));
        double energy = water.thermalState().energyJoules() + salt.thermalState().energyJoules();
        FluidInteractionRules miscible = (a, b) -> FluidInteractionRules.Result.MISCIBLE;
        FluidVessel vessel = new FluidVessel(3000000L, miscible);
        vessel.fill(water);
        assertEquals(0L, water.mass().milligrams());
        vessel.fill(salt);

        assertEquals(1, vessel.contents().size());
        assertTrue(vessel.contents().get(0).materialRef().isGenerated());
        assertEquals(2000000L, vessel.totalMass().milligrams());
        assertEquals(energy, vessel.totalThermalEnergyJoules(), 1.0e-6);
    }

    @Test
    void declaredLiquidTransformationConsumesAndProducesQuantifiedMatterAndHeat() {
        FluidParcel water = water(1000000L);
        CanonicalComposition saltComposition = CanonicalComposition.of(ResourceId.of("naturalexceptions", "salt"));
        CanonicalMaterialKey saltKey = CanonicalMaterialKey.of(saltComposition, MaterialPhase.LIQUID, null, null);
        MaterialProperties props = MaterialProperties.builder().density(2).specificHeatSolid(800)
            .specificHeatLiquid(800).meltingPointKelvin(800).build();
        MaterialQuantity saltMass = MaterialQuantity.ofMilligrams(1000000L);
        FluidParcel salt = new FluidParcel(MaterialRef.authored("naturalexceptions", "salt"), saltKey,
            saltComposition, saltMass, ActiveThermalState.ambient(saltMass.toKilograms(), props));
        CanonicalComposition brineComposition = CanonicalComposition.builder()
            .add(ResourceId.of("minecraft", "water"), 1).add(ResourceId.of("naturalexceptions", "salt"), 1).build();
        CanonicalMaterialKey brineKey = CanonicalMaterialKey.of(brineComposition, MaterialPhase.LIQUID, null, null);
        MaterialProperties brineProps = MaterialProperties.builder().density(1.2).specificHeatSolid(3000)
            .specificHeatLiquid(3000).meltingPointKelvin(250).build();
        FluidParcel brine = new FluidParcel(MaterialRef.generatedFrom(brineKey), brineKey, brineComposition,
            MaterialQuantity.ofMilligrams(2000000L), ActiveThermalState.ambient(2.0, brineProps));
        double inputEnergy = water.thermalState().energyJoules() + salt.thermalState().energyJoules();
        FluidInteractionRules reacting = new FluidInteractionRules() {
            @Override public Result classify(FluidParcel a, FluidParcel b) { return Result.TRANSFORM; }
            @Override public FluidTransformation transform(FluidParcel a, FluidParcel b) {
                return FluidTransformation.of(java.util.Collections.singletonList(brine), 100.0);
            }
        };
        FluidVessel vessel = new FluidVessel(3000000L, reacting);
        vessel.fill(water); vessel.fill(salt);

        assertEquals(1, vessel.contents().size());
        assertEquals(2000000L, vessel.totalMass().milligrams());
        assertEquals(100.0, vessel.lastTransformationEnergyDeltaJoules(), 0.0);
        assertEquals(inputEnergy + 100.0, vessel.totalThermalEnergyJoules(), 1.0e-6);
        assertEquals(MaterialRef.generatedFrom(brineKey), vessel.contents().get(0).materialRef());
    }

    @Test
    void explicitReservoirBoundaryIsNotConfusedWithFiniteParcel() {
        FluidSourceBoundary source = new FluidSourceBoundary("spring", water(1000000L).materialRef(), true);
        assertTrue(source.infinite());
        assertEquals(0L, water(1000000L).mass().milligrams() - 1000000L);
    }

    @Test
    void moltenSmelterMaterialMovesThroughFluidAndCoolsThroughSharedThermalPhase() {
        CanonicalComposition copper = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey moltenKey = CanonicalMaterialKey.of(copper, MaterialPhase.LIQUID, null, null);
        MaterialQuantity mass = MaterialQuantity.ofGrams(100.0);
        MaterialProperties props = MaterialProperties.builder().density(8.96).specificHeatSolid(385.0)
            .specificHeatLiquid(410.0).meltingPointKelvin(1357.77).latentHeatOfFusion(205000.0).build();
        ActiveThermalState thermal = ActiveThermalState.atTemperature(1400.0, 1.0, mass.toKilograms(), props);
        AggregateLayer smelterMelt = new AggregateLayer(MaterialRef.authored("naturalexceptions", "copper"),
            moltenKey, copper, mass, thermal, AggregatePhaseType.MOLTEN_METAL);

        FluidParcel liquid = FluidParcel.fromAggregateLayer(smelterMelt);
        FluidVessel receivingVessel = new FluidVessel(20000L, FluidInteractionRules.sameMaterialOnly());
        assertEquals(mass, receivingVessel.fill(liquid));
        FluidParcel moved = receivingVessel.drain(mass);
        assertEquals(smelterMelt.materialRef(), moved.materialRef());
        assertEquals(MaterialPhase.LIQUID, moved.phase());
        moved.thermalState().removeEnergyJoules(moved.thermalState().energyJoules() - 1000.0);
        assertTrue(moved.reconcilePhase());
        assertEquals(MaterialPhase.SOLID, moved.phase());
        assertEquals(MaterialPhase.SOLID, moved.stateKey().phase());
        assertEquals(mass, moved.mass());
    }
}
