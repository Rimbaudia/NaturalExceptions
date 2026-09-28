package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemGenericAssembly;

final class RecipeToolAssemblyDisassemblySafetyTest {
    @BeforeAll static void registerMaterials() { BuiltinContent.register(); }

    @Test void craftingGridDoesNotOfferLossyOneItemDisassembly() {
        ComponentInstance head = new ComponentInstance(GeometryId.PICK_HEAD, BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION), MaterialQuantity.ofGrams(900),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(GeometryId.HANDLE, BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION), MaterialQuantity.ofGrams(300),
            ManufacturingState.FINISHED);
        ItemStack assembly = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(head, handle));
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        }, 3, 3);
        grid.setInventorySlotContents(0, assembly);

        RecipeToolAssembly recipe = new RecipeToolAssembly();
        assertFalse(recipe.matches(grid, (World) null));
        assertNull(recipe.getCraftingResult(grid));
    }
}
