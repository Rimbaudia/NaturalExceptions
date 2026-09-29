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
        for (Item item : items) assertTrue(craftable(item), item.getUnlocalizedName() + " has a crafting recipe");
    }
}
