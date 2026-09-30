package naturalexceptions.minecraft.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.block.BlockStormcrystal;
import naturalexceptions.minecraft.recipe.SkystoneRecipes;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The island's new pieces do their work: condensers fill with cloud, Stormcrystal gives shards, all can be made. */
final class SkyLandmarkBlocksTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
        SkystoneRecipes.register();
    }

    @Test
    void aCondenserShowsTheCloudItHolds() {
        BlockCloudCondenser condenser = (BlockCloudCondenser) NEBlocks.cloudCondenser;
        double[][] empty = condenser.parts(0), full = condenser.parts(BlockCloudCondenser.MAX);
        assertEquals(empty.length + 1, full.length, "a cloud inside once it holds any");
        double[] cloud = full[full.length - 1];
        assertTrue(cloud[4] > 0.8 && cloud[4] <= 1.0, "full to the top: " + cloud[4]);
        assertTrue(BlockCloudCondenser.MIN_Y >= 128, "only high in the sky");
        double minutes = 4096.0 / 3.0 / 20.0 * BlockCloudCondenser.GATHER / 60.0;
        assertTrue(minutes > 3 && minutes < 7, "a cloud about every five minutes: " + minutes);
    }

    @Test
    void stormcrystalBreaksIntoShards() {
        BlockStormcrystal crystal = (BlockStormcrystal) NEBlocks.stormcrystal;
        for (int i = 0; i < 20; i++) {
            List<ItemStack> drops = crystal.getDrops(null, 0, 0, 0, BlockStormcrystal.BODY, 0);
            assertEquals(NEItems.stormglassShard, drops.get(0).getItem());
            int n = drops.get(0).stackSize;
            assertTrue(n >= 2 && n <= 4, "two to four shards: " + n);
        }
        assertTrue(crystal.isSideSolid(null, 0, 0, 0, null), "Stormglass can hang from it");
    }

    @Test
    void theIslandsPiecesCanBeMade() {
        for (Item piece : new Item[] { Item.getItemFromBlock(NEBlocks.cloudCondenser),
            Item.getItemFromBlock(NEBlocks.lighthouseLamp) }) {
            boolean found = false;
            for (Object entry : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack out = ((IRecipe) entry).getRecipeOutput();
                found |= out != null && out.getItem() == piece;
            }
            assertTrue(found, piece.getUnlocalizedName() + " has a recipe");
        }
    }
}
