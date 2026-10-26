package naturalexceptions.core.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialNameResolver;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;

/** Molten iron takes up charcoal's carbon: a little makes steel, harder than iron; much more, cast iron. */
final class SteelTest {

    private static CanonicalComposition ironWith(double carbon) {
        return CanonicalComposition.builder()
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 1.0 - carbon)
            .add(BuiltinMaterials.CARBON_RESOURCE_ID, carbon)
            .build();
    }

    private static MaterialProperties solid(CanonicalComposition comp) {
        return new MaterialPropertyResolver().resolve(MaterialRef.generatedFrom(CanonicalMaterialKey.of(comp)), comp)
            .properties();
    }

    @Test
    void aLittleCarbonMakesSteelMuchMakesCastIron() {
        assertTrue(BuiltinMaterials.isSteel(ironWith(0.01)));
        assertTrue(!BuiltinMaterials.isSteel(ironWith(0.001)), "too little is still iron");
        assertTrue(BuiltinMaterials.isCastIron(ironWith(0.04)));
        assertTrue(solid(ironWith(0.01)).hardness() >= 6.0, "steel is a step past iron");
        assertTrue(
            solid(ironWith(0.04)).meltingPointKelvin() < BuiltinMaterials.IRON_PROPERTIES.meltingPointKelvin() - 200,
            "cast iron melts far lower than iron");
        assertTrue(BuiltinMaterials.STEEL_PROPERTIES.hardness() >= 6.0);
        BuiltinContent.register();
        assertEquals("Steel", MaterialNameResolver.resolveMaterialName(MaterialRef.generatedFrom(CanonicalMaterialKey.of(ironWith(0.01))), ironWith(0.01)));
    }

    @Test
    void moltenIronTakesUpTheCharcoalAndBecomesSteel() {
        BuiltinContent.register();
        VesselState vessel = new VesselState();
        MaterialQuantity mass = MaterialQuantity.ofGrams(2000.0);
        CanonicalComposition iron = CanonicalComposition.of(BuiltinMaterials.IRON_RESOURCE_ID);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.IRON.ref(),
                CanonicalMaterialKey.of(iron),
                iron,
                mass,
                ActiveThermalState.ambient(mass.toKilograms(), BuiltinMaterials.IRON_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));
        vessel.addRawOre(OreFeedstockDefinition.charcoalCarbon());
        for (int i = 0; i < 4000; i++) {
            vessel.addElectricHeat(15000.0);
            vessel.step(1, 293.15);
        }
        AggregateLayer steel = null;
        for (AggregateLayer layer : vessel.layers()) {
            assertTrue(
                !layer.composition()
                    .equals(BuiltinMaterials.CARBON_COMPOSITION),
                "the charcoal's carbon has gone into the iron");
            if (layer.phaseType() == AggregatePhaseType.MOLTEN_METAL) steel = layer;
        }
        assertTrue(steel != null, "the iron melted");
        assertEquals(BuiltinMaterials.STEEL.ref(), steel.materialRef(), "and it is steel");
        assertEquals(2020.0, steel.mass().toKilograms() * 1000.0, 1.0, "no mass made or lost");
        assertTrue(steel.physicalPhase() == MaterialPhase.LIQUID);
    }
}
