package naturalexceptions.minecraft.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.item.ItemStack;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.assembly.TargetMaterialProfile;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;

final class AssemblyCombatWearTest {
    @BeforeAll static void registerMaterials() { BuiltinContent.register(); }

    @Test void eachCombatHitAppliesOneWearInteractionForEveryToolHead() {
        for (GeometryId geometry : new GeometryId[] {
            GeometryId.BLADE, GeometryId.PICK_HEAD, GeometryId.AXE_HEAD, GeometryId.SHOVEL_HEAD
        }) {
            AssemblyGraph graph = AssemblyGraph.ofTool(component(geometry, BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)), component(GeometryId.HANDLE,
                    BuiltinMaterials.WOOD.ref(), CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION)));
            double expectedWear = ItemGenericAssembly.getCapability(graph).calculateWearRate(TargetMaterialProfile.stone());
            ItemStack stack = ItemGenericAssembly.createStack(graph);

            new ItemGenericAssembly().hitEntity(stack, null, null);

            assertEquals(expectedWear, ItemGenericAssembly.getAssembly(stack).head().damage().wear(), 1.0e-12,
                geometry + " should receive one combat wear application");
        }
    }

    private static ComponentInstance component(GeometryId geometry, naturalexceptions.core.material.MaterialRef ref,
        CanonicalMaterialKey key) {
        return new ComponentInstance(geometry, ref, key, MaterialQuantity.ofGrams(500), ManufacturingState.FINISHED);
    }
}
