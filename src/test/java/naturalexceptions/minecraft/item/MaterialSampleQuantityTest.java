package naturalexceptions.minecraft.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.metallurgy.MaterialQuantity;

final class MaterialSampleQuantityTest {
    @Test
    void ambientAndHotSamplesRetainExplicitMass() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(735.125);
        net.minecraft.item.ItemStack ambient = ItemMaterialSample.createStack(
            BuiltinMaterials.STELLAR_MATTER.ref(), BuiltinMaterials.STELLAR_MATTER_COMPOSITION, mass, null);
        assertEquals(mass, ItemMaterialSample.getMass(ambient));

        naturalexceptions.core.thermal.ActiveThermalState hot = naturalexceptions.core.thermal.ActiveThermalState
            .atTemperature(1180.0, 0.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        net.minecraft.item.ItemStack heated = ItemMaterialSample.createStack(
            BuiltinMaterials.STELLAR_MATTER.ref(), BuiltinMaterials.STELLAR_MATTER_COMPOSITION, mass, hot);
        assertEquals(mass, ItemMaterialSample.getMass(heated));
        assertEquals(hot.energyJoules(), ItemMaterialSample.getThermalState(heated,
            BuiltinMaterials.STELLAR_MATTER_PROPERTIES).energyJoules(), 1.0e-6);
    }

    @Test
    void solidSampleCanRetainImpactProvenance() {
        naturalexceptions.core.id.DeterministicId impact = naturalexceptions.core.id.DeterministicId.derive(
            "impact", "sample-provenance");
        net.minecraft.item.ItemStack sample = ItemMaterialSample.createStack(
            BuiltinMaterials.STELLAR_MATTER.ref(), BuiltinMaterials.STELLAR_MATTER_COMPOSITION,
            MaterialQuantity.ofGrams(735.125), null);

        ItemMaterialSample.setProvenance(sample, impact);

        assertEquals(impact, ItemMaterialSample.getProvenance(sample));
    }
}
