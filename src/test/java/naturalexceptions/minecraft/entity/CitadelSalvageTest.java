package naturalexceptions.minecraft.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.block.BlockBrokenAutomaton;
import naturalexceptions.minecraft.block.PartModel;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * What the dead city's machines leave: salvage from a fallen Automaton or a run-down one (iron, Stormglass, a heart
 * now and then, rarely a Sentry's Crest), never a man's bread; and a heart cracks open for Stormglass.
 */
final class CitadelSalvageTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static Set<Item> seen(List<List<ItemStack>> rolls) {
        Set<Item> seen = new HashSet<>();
        for (List<ItemStack> roll : rolls) for (ItemStack stack : roll) seen.add(stack.getItem());
        return seen;
    }

    @Test
    void anAutomatonIsSalvageNotAMan() {
        Random random = new Random(7);
        List<List<ItemStack>> rolls = new java.util.ArrayList<>();
        for (int i = 0; i < 2000; i++) rolls.add(EntityCitadelAutomaton.drops(random, 0));
        Set<Item> seen = seen(rolls);
        assertFalse(seen.contains(Items.bread), "no bread in a machine");
        assertTrue(seen.contains(NEItems.stormglassHeart), "its heart, often");
        assertTrue(seen.contains(NEItems.TREASURES.get(Treasure.SENTRYS_CREST)), "now and then a sentry's crest");
        assertTrue(seen.contains(NEItems.citadelHalberd));
    }

    @Test
    void aRunDownAutomatonBreaksUpForSalvage() {
        Random random = new Random(3);
        List<List<ItemStack>> rolls = new java.util.ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            List<ItemStack> roll = BlockBrokenAutomaton.salvage(random, 0);
            assertTrue(
                roll.get(0)
                    .getItem() == Items.iron_ingot,
                "always some iron from its frame");
            rolls.add(roll);
        }
        Set<Item> seen = seen(rolls);
        assertTrue(seen.contains(NEItems.stormglassShard));
        assertTrue(seen.contains(NEItems.stormglassHeart));
        assertTrue(seen.contains(NEItems.TREASURES.get(Treasure.SENTRYS_CREST)));
    }

    @Test
    void itSitsOrKneelsInItsOwnBlockFacingAnyWay() {
        BlockBrokenAutomaton block = (BlockBrokenAutomaton) NEBlocks.brokenAutomaton;
        Set<String> shapes = new HashSet<>();
        for (int meta = 0; meta < 8; meta++) {
            double[] bounds = PartModel.bounds(block.parts(meta));
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(bounds[axis] >= 0.0, "inside its block, meta " + meta);
                assertTrue(bounds[axis + 3] <= (axis == 1 ? 1.05 : 1.0), "inside its block, meta " + meta);
            }
            shapes.add(java.util.Arrays.deepToString(block.parts(meta)));
        }
        assertEquals(8, shapes.size(), "two poses, four ways each");
    }

    @Test
    void aHeartCracksOpenForStormglassAndTheCrestIsSalvageOnly() {
        boolean cracks = false, crestCrafted = false;
        Item crest = NEItems.TREASURES.get(Treasure.SENTRYS_CREST);
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            IRecipe recipe = (IRecipe) entry;
            ItemStack out = recipe.getRecipeOutput();
            if (out == null) continue;
            if (out.getItem() == crest) crestCrafted = true;
            if (out.getItem() == NEItems.stormglassShard && out.stackSize == 4
                && recipe instanceof net.minecraft.item.crafting.ShapelessRecipes
                && ((ItemStack) ((net.minecraft.item.crafting.ShapelessRecipes) recipe).recipeItems.get(0)).getItem()
                    == NEItems.stormglassHeart)
                cracks = true;
        }
        assertTrue(cracks, "a heart cracks open for four shards");
        assertFalse(crestCrafted, "a crest is salvaged, never made");
        assertEquals(Treasure.Source.SALVAGE, Treasure.SENTRYS_CREST.source());
        assertEquals(net.minecraft.item.EnumRarity.epic, crest.getRarity(new ItemStack(crest)));
    }
}
