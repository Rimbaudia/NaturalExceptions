package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.knowledge.recipe.RecipeKnowledgeAdapter;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Skyfall is forged from a fallen star: crater crust and the star's heart, never Lumenite. */
final class RecipeSkyfallTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static InventoryCrafting grid(ItemStack crust, ItemStack core, ItemStack sword) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        grid.setInventorySlotContents(1, crust);
        grid.setInventorySlotContents(3, crust == null ? null : crust.copy());
        grid.setInventorySlotContents(5, crust == null ? null : crust.copy());
        grid.setInventorySlotContents(4, sword);
        grid.setInventorySlotContents(7, core);
        return grid;
    }

    @Test
    void crustAroundAnIronSwordWithTheStarsHeartBelow() {
        RecipeSkyfall recipe = new RecipeSkyfall();
        ItemStack crust = ItemMaterialSample.createStack(BuiltinMaterials.IMPACT_CRUST.ref());
        ItemStack core = ItemMaterialSample.createStack(BuiltinMaterials.STELLAR_MATTER.ref());
        ItemStack worn = new ItemStack(Items.iron_sword, 1, 120);
        assertTrue(recipe.matches(grid(crust, core, worn), null), "a worn iron sword is fine");
        assertFalse(recipe.matches(grid(core, core, worn), null), "the sides must be crater crust");
        assertFalse(recipe.matches(grid(crust, crust, worn), null), "the heart must be stellar matter");
        ItemStack copper = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());
        assertFalse(recipe.matches(grid(copper, core, worn), null), "not any old sample");
        assertFalse(recipe.matches(grid(crust, core, new ItemStack(Items.stone_sword)), null), "an iron sword");
        assertTrue(
            recipe.getRecipeOutput()
                .getItem() == NEItems.skyfall);
        assertNotNull(RecipeKnowledgeAdapter.adaptCraftingRecipe(recipe, 0), "the item browser can show it");
    }
}
