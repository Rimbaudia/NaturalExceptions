package naturalexceptions.minecraft.furniture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.block.BlockSkystoneDecor;
import naturalexceptions.minecraft.block.BlockSkystoneSeam;
import naturalexceptions.minecraft.item.ItemMasonsChisel;
import naturalexceptions.minecraft.recipe.SkystoneRecipes;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The quarry's tools work: the Mason's Bench and the Sawmill cut without waste, seams give stone, chisels dress. */
final class QuarryToolsTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
        SkystoneRecipes.register();
    }

    @Test
    void aMasonsBenchDressesOneSkystoneIntoAnyPiece() {
        List<CuttingRecipes.Recipe> fromRock = CuttingRecipes
            .forInput(CuttingRecipes.Kind.MASON, new ItemStack(NEBlocks.skystoneRock, 5));
        assertTrue(fromRock.size() >= 9, "every Skystone piece: " + fromRock.size());
        for (CuttingRecipes.Recipe r : fromRock) {
            assertEquals(1, r.count(), "one rock a piece");
            assertTrue(r.output().stackSize >= 1);
        }
        List<CuttingRecipes.Recipe> fromBricks = CuttingRecipes
            .forInput(CuttingRecipes.Kind.MASON, new ItemStack(NEBlocks.skystoneBricks, 1, 0));
        assertEquals(fromRock.size() - 1, fromBricks.size(), "bricks re-dressed into all but themselves");
        assertTrue(
            CuttingRecipes.accepts(CuttingRecipes.Kind.MASON, new ItemStack(NEBlocks.skystoneRock)),
            "the bench takes the rock");
        assertFalse(CuttingRecipes.accepts(CuttingRecipes.Kind.MASON, new ItemStack(Blocks.dirt)), "not dirt");
    }

    @Test
    void aSawmillCutsALogIntoMoreThanTheGridDoes() {
        List<CuttingRecipes.Recipe> spruce = CuttingRecipes
            .forInput(CuttingRecipes.Kind.SAW, new ItemStack(Blocks.log, 1, 1));
        ItemStack planks = null;
        for (CuttingRecipes.Recipe r : spruce)
            if (r.output()
                .getItem() == Item.getItemFromBlock(Blocks.planks)) planks = r.output();
        assertTrue(planks != null && planks.stackSize == 6 && planks.getItemDamage() == 1, "six spruce planks");
        assertTrue(
            !CuttingRecipes.forInput(CuttingRecipes.Kind.SAW, new ItemStack(Blocks.log2, 1, 1))
                .isEmpty(),
            "dark oak too");
    }

    @Test
    void aSeamGivesTwoSkystoneAndAWorkedFaceNothingUntilItOpens() {
        BlockSkystoneSeam seam = (BlockSkystoneSeam) NEBlocks.skystoneSeam;
        List<ItemStack> drops = seam.getDrops(null, 0, 0, 0, BlockSkystoneSeam.SEAM, 0);
        assertEquals(1, drops.size());
        assertEquals(Item.getItemFromBlock(NEBlocks.skystoneRock), drops.get(0).getItem());
        assertEquals(BlockSkystoneSeam.YIELD, drops.get(0).stackSize);
        assertTrue(seam.getDrops(null, 0, 0, 0, BlockSkystoneSeam.WORKED, 0).isEmpty(), "the worked face is spent");
        assertFalse(seam.canSilkHarvest(null, null, 0, 0, 0, 0), "a seam cannot be carried off whole");
        double minutes = 4096.0 / 3.0 / 20.0 * BlockSkystoneSeam.REGROWTH / 60.0; // a random tick a block every 68 s
        assertTrue(minutes > 15 && minutes < 25, "a worked face opens in about twenty minutes: " + minutes);
    }

    @Test
    void aChiselDressesSkystoneRoundItsLooks() {
        Block bricks = NEBlocks.skystoneBricks, decor = NEBlocks.skystoneDecor;
        assertEquals(1, ItemMasonsChisel.dressed(bricks, 0));
        assertEquals(2, ItemMasonsChisel.dressed(bricks, 1));
        assertEquals(0, ItemMasonsChisel.dressed(bricks, 2));
        assertEquals(-1, ItemMasonsChisel.dressed(bricks, 3), "a loose secret wall is left alone");
        assertEquals(BlockSkystoneDecor.TILES_CRACKED, ItemMasonsChisel.dressed(decor, BlockSkystoneDecor.TILES));
        assertEquals(BlockSkystoneDecor.CHISELED, ItemMasonsChisel.dressed(decor, BlockSkystoneDecor.POLISHED));
        assertEquals(-1, ItemMasonsChisel.dressed(NEBlocks.skystoneRock, 0), "not the raw rock");
    }

    @Test
    void theQuarrysToolsCanBeMade() {
        Item[] tools = { Item.getItemFromBlock(NEBlocks.masonsBench), Item.getItemFromBlock(NEBlocks.sawmill),
            Item.getItemFromBlock(NEBlocks.hoistWindlass), Item.getItemFromBlock(NEBlocks.hoistBasket),
            NEItems.masonsChisel };
        for (Item tool : tools) {
            boolean found = false;
            for (Object entry : CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack out = ((IRecipe) entry).getRecipeOutput();
                found |= out != null && out.getItem() == tool;
            }
            assertTrue(found, tool.getUnlocalizedName() + " has a recipe");
        }
    }

    @Test
    void aHoistKeepsNineThingsForBothItsEnds() {
        TileEntityHoist hoist = new TileEntityHoist();
        assertEquals(9, hoist.getSizeInventory());
        hoist.setInventorySlotContents(0, new ItemStack(NEBlocks.skystoneRock, 64));
        assertEquals(64, hoist.getStackInSlot(0).stackSize);
    }
}
