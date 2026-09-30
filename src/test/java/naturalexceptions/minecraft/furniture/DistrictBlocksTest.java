package naturalexceptions.minecraft.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.item.ItemWaterJug;
import naturalexceptions.minecraft.recipe.SkystoneRecipes;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The town's workshops and tourney ground do their work. */
final class DistrictBlocksTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
        SkystoneRecipes.register();
    }

    private static ItemStack made(CuttingRecipes.Kind kind, ItemStack input, Item output) {
        for (CuttingRecipes.Recipe r : CuttingRecipes.forInput(kind, input)) if (r.output()
            .getItem() == output) return r.output();
        return null;
    }

    @Test
    void aLoomWeavesWoolIntoCarpetDressingsOrString() {
        ItemStack lime = new ItemStack(Blocks.wool, 1, 5);
        ItemStack carpet = made(CuttingRecipes.Kind.LOOM, lime, Item.getItemFromBlock(Blocks.carpet));
        assertTrue(carpet != null && carpet.stackSize == 2 && carpet.getItemDamage() == 5, "two lime carpets");
        assertEquals(3, made(CuttingRecipes.Kind.LOOM, lime, NEItems.bandage).stackSize, "three dressings");
        assertEquals(4, made(CuttingRecipes.Kind.LOOM, lime, Items.string).stackSize, "unravelled");
    }

    @Test
    void aPottersWheelThrowsAJugFromABlockOrFourBallsOfClay() {
        ItemStack jug = made(CuttingRecipes.Kind.POTTER, new ItemStack(Blocks.clay), NEItems.waterJug);
        assertTrue(jug != null && ItemWaterJug.drinksLeft(jug) == 0, "a jug, empty from the wheel");
        assertTrue(
            CuttingRecipes.forInput(CuttingRecipes.Kind.POTTER, new ItemStack(Items.clay_ball, 3))
                .isEmpty(),
            "three balls are not enough");
        assertFalse(
            CuttingRecipes.forInput(CuttingRecipes.Kind.POTTER, new ItemStack(Items.clay_ball, 4))
                .isEmpty(),
            "four are");
    }

    @Test
    void aWaterJugHoldsFourDrinks() {
        ItemStack jug = new ItemStack(NEItems.waterJug, 1, 0);
        assertEquals(ItemWaterJug.DRINKS, ItemWaterJug.drinksLeft(jug));
        jug.setItemDamage(ItemWaterJug.DRINKS);
        assertEquals(0, ItemWaterJug.drinksLeft(jug));
    }

    @Test
    void aGlassblowersFurnaceBlowsSandWhileItHasFire() {
        ItemStack panes = made(CuttingRecipes.Kind.GLASS, new ItemStack(Blocks.sand), Item.getItemFromBlock(Blocks.glass_pane));
        assertEquals(4, panes.stackSize, "four panes a block of sand");
        TileEntityCuttingBench furnace = new TileEntityCuttingBench(CuttingRecipes.Kind.GLASS);
        assertFalse(furnace.hot(), "cold without fuel");
        furnace.setInventorySlotContents(TileEntityCuttingBench.FUEL, new ItemStack(Items.coal));
        assertTrue(furnace.hot(), "fuel lights it");
        furnace.burn();
        assertEquals(7, furnace.heat(), "a coal fires eight pieces, one used");
        assertEquals(null, furnace.getStackInSlot(TileEntityCuttingBench.FUEL), "the coal went on the fire");
        for (int i = 0; i < 7; i++) furnace.burn();
        assertFalse(furnace.hot(), "out again");
        assertTrue(new TileEntityCuttingBench(CuttingRecipes.Kind.LOOM).hot(), "a loom needs no fire");
    }

    @Test
    void aTanningRackCuresThreeHidesIntoLeather() {
        TileEntityTanningRack rack = new TileEntityTanningRack();
        for (int i = 0; i < 3; i++) assertTrue(rack.hang(new ItemStack(Items.rotten_flesh)));
        assertFalse(rack.hang(new ItemStack(Items.rotten_flesh)), "three at most");
        rack.cure(1);
        ItemStack taken = rack.takeDown();
        assertEquals(Items.leather, taken.getItem(), "the cured hide comes down first");
        assertEquals(Items.rotten_flesh, rack.takeDown()
            .getItem());
        assertTrue(TileEntityTanningRack.CURE >= 1200 && TileEntityTanningRack.CURE <= 3600, "a couple of minutes");
    }

    @Test
    void anArcheryButtScoresByTheRing() {
        assertEquals(10, BlockArcheryButt.score(0.0, 0.0));
        assertEquals(10, BlockArcheryButt.score(0.05, 0.05));
        assertEquals(5, BlockArcheryButt.score(0.2, 0.0));
        assertEquals(3, BlockArcheryButt.score(0.0, -0.3));
        assertEquals(1, BlockArcheryButt.score(0.45, 0.2));
    }

    @Test
    void theDistrictsPiecesCanBeMade() {
        Item[] pieces = { Item.getItemFromBlock(NEBlocks.loom), Item.getItemFromBlock(NEBlocks.pottersWheel),
            Item.getItemFromBlock(NEBlocks.glassFurnace), Item.getItemFromBlock(NEBlocks.tanningRack),
            Item.getItemFromBlock(NEBlocks.archeryButt), Item.getItemFromBlock(NEBlocks.trainingDummy) };
        for (Item piece : pieces) {
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
