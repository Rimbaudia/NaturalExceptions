package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Every treasure and weapon can be crafted, not only found; and so can the Lumenite Lamp. */
final class TreasureRecipesTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static boolean craftable(Item item) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            if (recipe.getRecipeOutput() != null && recipe.getRecipeOutput()
                .getItem() == item) return true;
        }
        return false;
    }

    @Test
    void everyNewItemHasARecipe() {
        List<Item> items = new ArrayList<>();
        for (Treasure treasure : Treasure.values()) items.add(NEItems.TREASURES.get(treasure));
        items.add(NEItems.skyfall);
        items.add(NEItems.shardcaster);
        items.add(Item.getItemFromBlock(NEBlocks.lumeniteLamp));
        items.add(NEItems.windbloomTea);
        items.add(Item.getItemFromBlock(NEBlocks.skystoneBricks));
        for (Item item : items) assertTrue(craftable(item), item.getUnlocalizedName() + " has a crafting recipe");
    }

    @Test
    void skystoneSurvivesAStarImpactAndCloudsAndWindbloomFeedTheSkyGear() {
        assertTrue(
            !naturalexceptions.minecraft.event.StarRainWorldMaterializer.carvable(NEBlocks.skystoneBricks, false, 3.0F),
            "a Fallen Star's crater does not carve Skystone");
        assertTrue(
            naturalexceptions.minecraft.event.StarRainWorldMaterializer
                .carvable(net.minecraft.init.Blocks.stonebrick, false, 1.5F),
            "ordinary stone brick still breaks");
        assertTrue(usedIn(NEItems.TREASURES.get(Treasure.CLOUD_FLASK), Item.getItemFromBlock(NEBlocks.cloud)));
        assertTrue(usedIn(NEItems.TREASURES.get(Treasure.GLIDER_CLOAK), Item.getItemFromBlock(NEBlocks.windbloom)));
    }

    private static boolean usedIn(Item output, Item ingredient) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(entry instanceof net.minecraft.item.crafting.ShapedRecipes)) continue;
            net.minecraft.item.crafting.ShapedRecipes recipe = (net.minecraft.item.crafting.ShapedRecipes) entry;
            if (recipe.getRecipeOutput() == null || recipe.getRecipeOutput()
                .getItem() != output) continue;
            for (net.minecraft.item.ItemStack stack : recipe.recipeItems)
                if (stack != null && stack.getItem() == ingredient) return true;
        }
        return false;
    }
}
