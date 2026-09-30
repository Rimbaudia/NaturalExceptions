package naturalexceptions.minecraft.recipe;

import java.util.Arrays;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;

import naturalexceptions.minecraft.item.GliderCloaks;

/**
 * The Glider Cloak, sewn from any wool, leather and Windbloom, takes the colour of its wool (most of it).
 *
 * <pre>
 *   W L W
 *   F W F     W = wool of any colour, L = leather, F = Windbloom
 *   W . W
 * </pre>
 */
public final class RecipeGliderCloak extends ShapedRecipes {

    public RecipeGliderCloak(Item cloak, Item windbloom) {
        super(3, 3, pattern(windbloom), new ItemStack(cloak));
    }

    private static ItemStack[] pattern(Item windbloom) {
        ItemStack wool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);
        ItemStack leather = new ItemStack(Items.leather), flower = new ItemStack(windbloom);
        return new ItemStack[] { wool, leather, wool, flower, wool, flower, wool, null, wool };
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return GliderCloaks.withColour(getRecipeOutput().copy(), GliderCloaks.woolColour(grid));
    }

    /** The same cloak from a Warden's Gale Core, leather and two wool, in the wool's colour. */
    public static final class FromCore extends ShapelessRecipes {

        public FromCore(Item cloak, Item core) {
            super(
                new ItemStack(cloak),
                Arrays.asList(
                    new ItemStack(core),
                    new ItemStack(Items.leather),
                    new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE),
                    new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE)));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            return GliderCloaks.withColour(getRecipeOutput().copy(), GliderCloaks.woolColour(grid));
        }
    }
}
