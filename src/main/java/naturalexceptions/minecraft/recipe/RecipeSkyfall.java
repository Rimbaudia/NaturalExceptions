package naturalexceptions.minecraft.recipe;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Skyfall is forged from a fallen star: an iron sword set in three pieces of the crater's Fused Impact Crust, with
 * a piece of the star's own heart (stellar matter) at its hilt.
 *
 * <pre>
 *   . C .
 *   C S C     C = Fused Impact Crust sample, S = iron sword, X = stellar matter sample
 *   . X .
 * </pre>
 *
 * A plain shaped recipe as far as the item browser can see (it shows example samples); matching also checks
 * that each sample really is the right material.
 */
public final class RecipeSkyfall extends ShapedRecipes {

    private static final int[][] CRUST_CELLS = { { 1, 0 }, { 0, 1 }, { 2, 1 } };
    private static final int[] CORE_CELL = { 1, 2 };

    public RecipeSkyfall() {
        super(3, 3, pattern(), new ItemStack(NEItems.skyfall));
    }

    private static ItemStack[] pattern() {
        ItemStack crust = sample(BuiltinMaterials.IMPACT_CRUST.ref());
        ItemStack core = sample(BuiltinMaterials.STELLAR_MATTER.ref());
        ItemStack sword = new ItemStack(Items.iron_sword, 1, OreDictionary.WILDCARD_VALUE);
        return new ItemStack[] { null, crust, null, crust, sword, crust, null, core, null };
    }

    private static ItemStack sample(MaterialRef ref) {
        ItemStack stack = ItemMaterialSample.createStack(ref);
        stack.setItemDamage(OreDictionary.WILDCARD_VALUE);
        return stack;
    }

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        if (!super.matches(grid, world) || grid.getSizeInventory() < 9) return false;
        for (int[] cell : CRUST_CELLS) {
            if (!isSampleOf(grid.getStackInRowAndColumn(cell[0], cell[1]), BuiltinMaterials.IMPACT_CRUST.ref()))
                return false;
        }
        return isSampleOf(
            grid.getStackInRowAndColumn(CORE_CELL[0], CORE_CELL[1]),
            BuiltinMaterials.STELLAR_MATTER.ref());
    }

    static boolean isSampleOf(ItemStack stack, MaterialRef ref) {
        return stack != null && stack.getItem() == NEItems.materialSample
            && ref.equals(ItemMaterialSample.getMaterialRef(stack));
    }
}
