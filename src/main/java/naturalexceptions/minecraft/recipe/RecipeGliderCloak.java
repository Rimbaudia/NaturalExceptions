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
 * The Glider Cloak: its sail of Cloudsilk, its shoulders of wool (whose colour it takes, most of it), leather and
 * Windbloom.
 *
 * <pre>
 *   W L W     W = wool of any colour, L = leather
 *   F S F     F = Windbloom, S = Cloudsilk
 *   S . S
 * </pre>
 */
public final class RecipeGliderCloak extends ShapedRecipes {

    public RecipeGliderCloak(Item cloak, Item windbloom, Item cloudsilk) {
        super(3, 3, pattern(windbloom, cloudsilk), new ItemStack(cloak));
    }

    private static ItemStack[] pattern(Item windbloom, Item cloudsilk) {
        ItemStack wool = new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE);
        ItemStack leather = new ItemStack(Items.leather), flower = new ItemStack(windbloom);
        ItemStack silk = new ItemStack(cloudsilk);
        return new ItemStack[] { wool, leather, wool, flower, silk, flower, silk, null, silk };
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return GliderCloaks.withColour(getRecipeOutput().copy(), GliderCloaks.woolColour(grid));
    }

    /** The same cloak from a Warden's Gale Core, leather, a wool and two Cloudsilk, in the wool's colour. */
    public static final class FromCore extends ShapelessRecipes {

        public FromCore(Item cloak, Item core, Item cloudsilk) {
            super(
                new ItemStack(cloak),
                Arrays.asList(
                    new ItemStack(core),
                    new ItemStack(Items.leather),
                    new ItemStack(Blocks.wool, 1, OreDictionary.WILDCARD_VALUE),
                    new ItemStack(cloudsilk),
                    new ItemStack(cloudsilk)));
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting grid) {
            return GliderCloaks.withColour(getRecipeOutput().copy(), GliderCloaks.woolColour(grid));
        }
    }
}
