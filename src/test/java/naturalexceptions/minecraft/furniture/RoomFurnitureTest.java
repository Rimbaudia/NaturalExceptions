package naturalexceptions.minecraft.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The furniture of the new rooms does something: a fountain fills, a brazier cooks, an orrery tells the sky. */
final class RoomFurnitureTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void everyPieceCanBeMade() {
        for (Block block : new Block[] { NEBlocks.candelabra, NEBlocks.brazier, NEBlocks.stoneBench, NEBlocks.pew,
            NEBlocks.fountain, NEBlocks.orrery }) {
            boolean found = false;
            for (Object entry : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack out = ((IRecipe) entry).getRecipeOutput();
                found |= out != null && out.getItem() == Item.getItemFromBlock(block);
            }
            assertTrue(found, block.getUnlocalizedName() + " has a recipe");
        }
    }

    @Test
    void aFountainFillsBottlesAndBucketsForever() {
        ItemStack bottle = BlockFountain.fill(new ItemStack(Items.glass_bottle));
        assertEquals(Items.potionitem, bottle.getItem());
        assertEquals(0, bottle.getItemDamage(), "a plain water bottle");
        assertEquals(
            Items.water_bucket,
            BlockFountain.fill(new ItemStack(Items.bucket))
                .getItem());
        assertNull(BlockFountain.fill(new ItemStack(Items.stick)), "nothing else");
    }

    @Test
    void aBrazierCooksRawMeatAndNothingElse() {
        assertEquals(
            Items.cooked_beef,
            BlockBrazier.cooked(new ItemStack(Items.beef))
                .getItem());
        assertEquals(
            Items.cooked_fished,
            BlockBrazier.cooked(new ItemStack(Items.fish))
                .getItem());
        assertNull(BlockBrazier.cooked(new ItemStack(Items.iron_ingot)), "not a smelter");
        assertNull(BlockBrazier.cooked(new ItemStack(Items.cooked_beef)), "already cooked");
    }

    @Test
    void anOrreryReadsTheSky() {
        assertTrue(
            BlockOrrery.reading(0L, 0)
                .startsWith("It is 06:00, dawn."),
            BlockOrrery.reading(0L, 0));
        assertTrue(
            BlockOrrery.reading(0L, 0)
                .endsWith("full tonight."));
        assertTrue(
            BlockOrrery.reading(10000L, 7)
                .contains("16:00"));
        assertTrue(
            BlockOrrery.reading(10000L, 7)
                .endsWith("full in 1 night."));
        assertTrue(
            BlockOrrery.reading(18000L, 4)
                .contains("new: full in 4 nights."));
    }
}
