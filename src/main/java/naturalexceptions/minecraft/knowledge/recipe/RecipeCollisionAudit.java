package naturalexceptions.minecraft.knowledge.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;

import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Audit utility verifying recipe integrity and ensuring no recipe collisions
 * between Natural Exceptions and vanilla Minecraft (especially 4 Clay Balls -> Clay Block).
 */
public final class RecipeCollisionAudit {

    private RecipeCollisionAudit() {}

    public static final class AuditReport {

        private final boolean clayBlockIntact;
        private final boolean refractoryBrickDistinct;
        private final List<String> collisions = new ArrayList<>();

        public AuditReport(boolean clayBlockIntact, boolean refractoryBrickDistinct, List<String> collisions) {
            this.clayBlockIntact = clayBlockIntact;
            this.refractoryBrickDistinct = refractoryBrickDistinct;
            if (collisions != null) this.collisions.addAll(collisions);
        }

        public boolean isClayBlockIntact() {
            return clayBlockIntact;
        }

        public boolean isRefractoryBrickDistinct() {
            return refractoryBrickDistinct;
        }

        public boolean hasCollisions() {
            return !collisions.isEmpty();
        }

        public List<String> getCollisions() {
            return collisions;
        }
    }

    /**
     * Executes the audit against the active CraftingManager runtime recipe list.
     */
    public static AuditReport runAudit() {
        CraftingManager cm = CraftingManager.getInstance();
        List recipeList = cm.getRecipeList();

        boolean clayBlockFound = false;
        boolean refractoryBrickFound = false;
        List<String> detectedCollisions = new ArrayList<>();

        // Test 1: Simulate crafting grid with 4 clay balls
        InventoryCrafting grid4Clay = createDummyGrid(2, 2);
        grid4Clay.setInventorySlotContents(0, new ItemStack(Items.clay_ball));
        grid4Clay.setInventorySlotContents(1, new ItemStack(Items.clay_ball));
        grid4Clay.setInventorySlotContents(2, new ItemStack(Items.clay_ball));
        grid4Clay.setInventorySlotContents(3, new ItemStack(Items.clay_ball));

        ItemStack result4Clay = cm.findMatchingRecipe(grid4Clay, null);
        if (result4Clay != null && result4Clay.getItem() == new ItemStack(Blocks.clay).getItem()) {
            clayBlockFound = true;
        }

        // Test 2: Clay and sand produce an unfired mix, then furnace firing
        // produces refractory bricks, which are compressed into a block.
        InventoryCrafting gridClaySand = createDummyGrid(2, 2);
        gridClaySand.setInventorySlotContents(0, new ItemStack(Items.clay_ball));
        gridClaySand.setInventorySlotContents(1, new ItemStack(Blocks.sand));
        gridClaySand.setInventorySlotContents(2, new ItemStack(Blocks.sand));
        gridClaySand.setInventorySlotContents(3, new ItemStack(Items.clay_ball));

        ItemStack resultClaySand = cm.findMatchingRecipe(gridClaySand, null);
        ItemStack firedBrick = resultClaySand != null && NEItems.refractoryMix != null ? FurnaceRecipes.smelting()
            .getSmeltingResult(new ItemStack(NEItems.refractoryMix)) : null;
        if (resultClaySand != null && NEItems.refractoryMix != null
            && resultClaySand.getItem() == NEItems.refractoryMix
            && firedBrick != null
            && firedBrick.getItem() == NEItems.refractoryBrick) {
            InventoryCrafting brickGrid = createDummyGrid(2, 2);
            for (int i = 0; i < 4; i++) brickGrid.setInventorySlotContents(i, new ItemStack(NEItems.refractoryBrick));
            ItemStack resultBrickBlock = cm.findMatchingRecipe(brickGrid, null);
            refractoryBrickFound = resultBrickBlock != null
                && resultBrickBlock.getItem() == new ItemStack(NEBlocks.refractoryBrick).getItem();
        }

        Map<String, String> outputsByPattern = new LinkedHashMap<>();
        Map<String, Map<Boolean, String>> outputsByIngredientSet = new LinkedHashMap<>();
        int recipeIndex = 0;
        for (Object object : recipeList) {
            if (!(object instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) object;
            UniversalRecipe display = RecipeKnowledgeAdapter.adaptCraftingRecipe(recipe, recipeIndex++);
            if (display == null || display.grid() == null || display.output() == null) continue;
            String pattern = craftingPattern(display);
            String output = stackKey(display.output()) + "x" + display.output().stackSize;
            String existingOutput = outputsByPattern.putIfAbsent(pattern, output);
            if (existingOutput != null && !existingOutput.equals(output)) {
                detectedCollisions.add(pattern + " produces both " + existingOutput + " and " + output);
            }
            boolean shapeless = display.operationName()
                .toLowerCase(java.util.Locale.ROOT)
                .contains("shapeless");
            String ingredientSet = ingredientMultiset(display);
            Map<Boolean, String> outputsByShape = outputsByIngredientSet
                .computeIfAbsent(ingredientSet, ignored -> new LinkedHashMap<>());
            String oppositeOutput = outputsByShape.get(!shapeless);
            if (oppositeOutput != null && !oppositeOutput.equals(output)) {
                detectedCollisions
                    .add("shaped/shapeless " + ingredientSet + " produces both " + oppositeOutput + " and " + output);
            }
            outputsByShape.putIfAbsent(shapeless, output);
        }

        return new AuditReport(clayBlockFound, refractoryBrickFound, detectedCollisions);
    }

    private static String craftingPattern(UniversalRecipe recipe) {
        boolean shapeless = recipe.operationName()
            .toLowerCase(java.util.Locale.ROOT)
            .contains("shapeless");
        StringBuilder signature = new StringBuilder(shapeless ? "shapeless:" : "shaped:");
        if (shapeless) {
            List<String> slots = new ArrayList<>();
            for (CraftingGridSlot[] row : recipe.grid()) {
                for (CraftingGridSlot slot : row) if (slot != null && !slot.isEmpty()) slots.add(slotKey(slot));
            }
            Collections.sort(slots);
            return signature.append(slots)
                .toString();
        }
        for (CraftingGridSlot[] row : recipe.grid()) {
            for (CraftingGridSlot slot : row) signature.append(slot == null ? "_" : slotKey(slot))
                .append('|');
        }
        return signature.toString();
    }

    private static String slotKey(CraftingGridSlot slot) {
        List<String> alternatives = new ArrayList<>();
        for (ItemStack stack : slot.alternatives()) alternatives.add(stackKey(stack));
        Collections.sort(alternatives);
        return alternatives.toString();
    }

    private static String ingredientMultiset(UniversalRecipe recipe) {
        List<String> ingredients = new ArrayList<>();
        for (CraftingGridSlot[] row : recipe.grid()) {
            for (CraftingGridSlot slot : row) {
                if (slot != null && !slot.isEmpty()) ingredients.add(slotKey(slot));
            }
        }
        Collections.sort(ingredients);
        return ingredients.toString();
    }

    private static String stackKey(ItemStack stack) {
        return net.minecraft.item.Item.getIdFromItem(stack.getItem()) + ":"
            + stack.getItemDamage()
            + ":"
            + (stack.hasTagCompound() ? stack.getTagCompound()
                .toString() : "");
    }

    private static InventoryCrafting createDummyGrid(int width, int height) {
        return new InventoryCrafting(new net.minecraft.inventory.Container() {

            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return true;
            }
        }, width, height);
    }
}
