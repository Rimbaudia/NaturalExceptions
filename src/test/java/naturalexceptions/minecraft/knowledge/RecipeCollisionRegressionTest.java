package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.knowledge.recipe.RecipeCollisionAudit;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

class RecipeCollisionRegressionTest {

    @BeforeAll
    static void setup() {
        BuiltinContent.register();
        NEItems.ensureBootstrapped();
        NEItems.register();
        NEBlocks.register();
    }

    private static InventoryCrafting create2x2Grid() {
        return new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) {
                return true;
            }
        }, 2, 2);
    }

    @Test
    void testVanillaClayBlockRecipeRemainsIntactWith4ClayBalls() {
        CraftingManager cm = CraftingManager.getInstance();
        InventoryCrafting grid = create2x2Grid();

        // Place 4 vanilla Clay Balls in 2x2 grid
        grid.setInventorySlotContents(0, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(1, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(2, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(3, new ItemStack(Items.clay_ball));

        ItemStack result = cm.findMatchingRecipe(grid, null);
        assertNotNull(result, "4 Clay Balls MUST produce a crafting result");
        assertEquals(
            new ItemStack(Blocks.clay).getItem(),
            result.getItem(),
            "4 Clay Balls MUST craft vanilla Clay Block, NEVER Refractory Brick Block!");
        assertEquals(1, result.stackSize);
    }

    @Test
    void testRefractoryBrickMixMustBeFiredBeforeMakingBlocks() {
        CraftingManager cm = CraftingManager.getInstance();
        InventoryCrafting grid = create2x2Grid();

        // Place 2 Clay Balls + 2 Sand in 2x2 grid
        grid.setInventorySlotContents(0, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(1, new ItemStack(Blocks.sand));
        grid.setInventorySlotContents(2, new ItemStack(Blocks.sand));
        grid.setInventorySlotContents(3, new ItemStack(Items.clay_ball));

        ItemStack result = cm.findMatchingRecipe(grid, null);
        assertNotNull(result, "2 Clay Balls + 2 Sand MUST produce a refractory mix");
        assertEquals(
            NEItems.refractoryMix,
            result.getItem(),
            "Clay and sand must produce the precursor, not a finished refractory block");

        ItemStack fired = net.minecraft.item.crafting.FurnaceRecipes.smelting()
            .getSmeltingResult(new ItemStack(NEItems.refractoryMix));
        assertNotNull(fired, "Furnace must fire Refractory Mix");
        assertEquals(NEItems.refractoryBrick, fired.getItem());

        InventoryCrafting bricks = create2x2Grid();
        for (int i = 0; i < 4; i++) bricks.setInventorySlotContents(i, new ItemStack(NEItems.refractoryBrick));
        ItemStack block = cm.findMatchingRecipe(bricks, null);
        assertNotNull(block, "Four fired refractory bricks must craft the structural block");
        assertEquals(new ItemStack(NEBlocks.refractoryBrick).getItem(), block.getItem());
    }

    @Test
    void blankMoldHasAReachableRecipeWithoutTakingTheVanillaClayBlockRecipe() {
        CraftingManager cm = CraftingManager.getInstance();
        InventoryCrafting grid = create2x2Grid();
        grid.setInventorySlotContents(0, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(1, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(2, new ItemStack(Items.clay_ball));
        grid.setInventorySlotContents(3, new ItemStack(Blocks.sand));

        ItemStack result = cm.findMatchingRecipe(grid, null);
        assertNotNull(result);
        assertEquals(NEItems.castingMold, result.getItem());
        assertNull(ItemCastingMold.getGeometry(result), "The crafted result must be an unshaped Blank Mold");
    }

    @Test
    void testRecipeCollisionAuditReportsZeroFatalCollisions() {
        RecipeCollisionAudit.AuditReport report = RecipeCollisionAudit.runAudit();
        assertTrue(report.isClayBlockIntact(), "Vanilla Clay Block recipe must be intact");
        assertTrue(report.isRefractoryBrickDistinct(), "Refractory Brick recipe must be distinct");
        assertFalse(
            report.hasCollisions(),
            "Audit should detect zero illegal recipe collisions: " + report.getCollisions());
    }
}
