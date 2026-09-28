package naturalexceptions.core.fluid;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class StellarThermalBehaviorTest {
    @Test
    void stellarMatterTransfersHeatFasterThanCopperThroughTheSharedFluidModel() {
        BuiltinContent.register();
        MaterialQuantity mass = MaterialQuantity.ofGrams(100.0);
        FluidParcel stellar = parcel(BuiltinMaterials.STELLAR_MATTER.ref(),
            BuiltinMaterials.STELLAR_MATTER_COMPOSITION, BuiltinMaterials.STELLAR_MATTER_PROPERTIES, mass);
        CanonicalComposition copperComposition = CanonicalComposition.of(BuiltinMaterials.COPPER_RESOURCE_ID);
        FluidParcel copper = parcel(BuiltinMaterials.COPPER.ref(), copperComposition,
            BuiltinMaterials.COPPER_PROPERTIES, mass);
        FluidWorldState starState = new FluidWorldState();
        FluidWorldState copperState = new FluidWorldState();
        starState.put(new FluidCellKey(0, 1, 64, 1), stellar);
        copperState.put(new FluidCellKey(0, 2, 64, 1), copper);
        double stellarEnergy = stellar.thermalState().energyJoules();
        double copperEnergy = copper.thermalState().energyJoules();

        starState.processThermalWork(60.0, 1);
        copperState.processThermalWork(60.0, 1);

        double stellarLoss = stellarEnergy - stellar.thermalState().energyJoules();
        double copperLoss = copperEnergy - copper.thermalState().energyJoules();
        assertTrue(stellarLoss > copperLoss);
    }

    private static FluidParcel parcel(MaterialRef ref, CanonicalComposition composition, MaterialProperties properties,
        MaterialQuantity mass) {
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        ActiveThermalState thermal = ActiveThermalState.atTemperature(1600.0, 1.0, mass.toKilograms(), properties);
        return new FluidParcel(ref, key, composition, mass, thermal);
    }
}
