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
        for (Treasure treasure : Treasure.values()) if (!treasure.relic()) items.add(NEItems.TREASURES.get(treasure));
        items.add(NEItems.skyfall);
        items.add(NEItems.shardcaster);
        items.add(Item.getItemFromBlock(NEBlocks.lumeniteLamp));
        items.add(NEItems.windbloomTea);
        items.add(Item.getItemFromBlock(NEBlocks.skystoneBricks));
        items.add(Item.getItemFromBlock(NEBlocks.galeVent));
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

    @Test
    void wardenRelicsCannotBeCraftedButTheGaleCoreCraftsSkyGear() {
        for (Treasure treasure : Treasure.values())
            if (treasure.relic()) assertTrue(!craftable(NEItems.TREASURES.get(treasure)), treasure + " is Warden-only");
        assertTrue(!craftable(NEItems.galeMaul), "the Gale Maul is Warden-only");
        assertTrue(usedIn(Item.getItemFromBlock(NEBlocks.galeVent), NEItems.galeCore));
        assertTrue(usedInShapeless(NEItems.TREASURES.get(Treasure.CLOUD_FLASK), NEItems.galeCore));
        assertTrue(usedInShapeless(NEItems.TREASURES.get(Treasure.GLIDER_CLOAK), NEItems.galeCore));
    }

    /** A crafting grid of nine, filled row by row (null for an empty place). */
    private static net.minecraft.inventory.InventoryCrafting grid(net.minecraft.item.ItemStack... cells) {
        net.minecraft.inventory.Container none = new net.minecraft.inventory.Container() {

            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return false;
            }
        };
        net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(none, 3, 3);
        for (int i = 0; i < cells.length; i++) grid.setInventorySlotContents(i, cells[i]);
        return grid;
    }

    private static net.minecraft.item.ItemStack craft(net.minecraft.inventory.InventoryCrafting grid) {
        return CraftingManager.getInstance()
            .findMatchingRecipe(grid, null);
    }

    @Test
    void theGliderCloakTakesAnyWoolAndItsColour() {
        net.minecraft.item.ItemStack leather = new net.minecraft.item.ItemStack(net.minecraft.init.Items.leather);
        net.minecraft.item.ItemStack windbloom = new net.minecraft.item.ItemStack(NEBlocks.windbloom);
        Item cloak = NEItems.TREASURES.get(Treasure.GLIDER_CLOAK);
        for (int[] wools : new int[][] { { 0, 0, 0, 0, 0 }, { 14, 14, 14, 14, 14 }, { 15, 15, 15, 15, 15 },
            { 11, 0, 11, 11, 0 } }) {
            net.minecraft.item.ItemStack[] w = new net.minecraft.item.ItemStack[5];
            for (int k = 0; k < 5; k++)
                w[k] = new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.wool, 1, wools[k]);
            net.minecraft.item.ItemStack made = craft(
                grid(w[0], leather, w[1], windbloom, w[2], windbloom, w[3], null, w[4]));
            org.junit.jupiter.api.Assertions.assertNotNull(made, "a cloak from wool " + wools[0]);
            org.junit.jupiter.api.Assertions.assertEquals(cloak, made.getItem());
            int most = wools[0] == 11 ? 11 : wools[0];
            org.junit.jupiter.api.Assertions.assertEquals(
                most,
                naturalexceptions.minecraft.item.GliderCloaks.colour(made),
                "the colour most of its wool is");
        }
        net.minecraft.item.ItemStack core = new net.minecraft.item.ItemStack(NEItems.galeCore);
        net.minecraft.item.ItemStack red = new net.minecraft.item.ItemStack(net.minecraft.init.Blocks.wool, 1, 14);
        net.minecraft.item.ItemStack made = craft(grid(core, leather, red, red, null, null, null, null, null));
        org.junit.jupiter.api.Assertions.assertNotNull(made, "the Gale Core cloak");
        org.junit.jupiter.api.Assertions.assertEquals(14, naturalexceptions.minecraft.item.GliderCloaks.colour(made));
    }

    private static boolean usedInShapeless(Item output, Item ingredient) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(entry instanceof net.minecraft.item.crafting.ShapelessRecipes)) continue;
            net.minecraft.item.crafting.ShapelessRecipes recipe = (net.minecraft.item.crafting.ShapelessRecipes) entry;
            if (recipe.getRecipeOutput() == null || recipe.getRecipeOutput()
                .getItem() != output) continue;
            for (Object stack : recipe.recipeItems) if (stack instanceof net.minecraft.item.ItemStack
                && ((net.minecraft.item.ItemStack) stack).getItem() == ingredient) return true;
        }
        return false;
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
