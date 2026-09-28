package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.knowledge.recipe.CraftingGridSlot;
import naturalexceptions.minecraft.knowledge.recipe.GameItemRegistry;
import naturalexceptions.minecraft.knowledge.recipe.RecipeCategory;
import naturalexceptions.minecraft.knowledge.recipe.RecipeDisplayType;
import naturalexceptions.minecraft.knowledge.recipe.RecipeKnowledgeAdapter;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipe;
import naturalexceptions.minecraft.knowledge.recipe.UniversalRecipeRegistry;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

class UniversalRecipeRegistryTest {

    @BeforeAll
    static void setup() {
        BuiltinContent.register();
        NEItems.ensureBootstrapped();
        NEBlocks.register();
        NEItems.register();
        GameItemRegistry.instance()
            .reset();
        UniversalRecipeRegistry.instance()
            .reset();
        UniversalRecipeRegistry.instance()
            .init();
        GameItemRegistry.instance()
            .init();
    }

    @Test
    void testAutomaticDiscoveryOfVanillaBreadRecipe() {
        assertNull(
            naturalexceptions.minecraft.knowledge.KnowledgeRegistry.instance()
                .getEntry("bread"));
        ItemStack bread = new ItemStack(Items.bread);
        List<UniversalRecipe> recipes = UniversalRecipeRegistry.instance()
            .findAcquisitions(bread);

        assertFalse(recipes.isEmpty(), "Bread recipe MUST be automatically discovered from CraftingManager");
        UniversalRecipe breadRecipe = recipes.get(0);

        assertEquals(RecipeDisplayType.CRAFTING_GRID, breadRecipe.type());
        assertEquals(
            Items.bread,
            breadRecipe.output()
                .getItem());

        // Check that wheat is in the grid
        CraftingGridSlot[][] grid = breadRecipe.grid();
        assertNotNull(grid);

        boolean foundWheat = false;
        int wheatCount = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!grid[r][c].isEmpty()) {
                    ItemStack st = grid[r][c].getDisplayStack(0);
                    if (st != null && st.getItem() == Items.wheat) {
                        foundWheat = true;
                        wheatCount++;
                    }
                }
            }
        }
        assertTrue(foundWheat, "Bread recipe must contain Wheat");
        assertEquals(3, wheatCount, "Bread recipe must require exactly 3 Wheat");
    }

    @Test
    void testAutomaticDiscoveryOfVanillaClayBlockRecipe() {
        ItemStack clayBlock = new ItemStack(Blocks.clay);
        List<UniversalRecipe> recipes = UniversalRecipeRegistry.instance()
            .findAcquisitions(clayBlock);

        assertFalse(recipes.isEmpty(), "Clay Block recipe MUST be discovered");
        UniversalRecipe recipe = recipes.get(0);

        assertEquals(RecipeDisplayType.CRAFTING_GRID, recipe.type());
        assertEquals(
            new ItemStack(Blocks.clay).getItem(),
            recipe.output()
                .getItem());
    }

    @Test
    void testAutomaticDiscoveryOfVanillaFurnaceSmelting() {
        ItemStack ironIngot = new ItemStack(Items.iron_ingot);
        List<UniversalRecipe> recipes = UniversalRecipeRegistry.instance()
            .findAcquisitions(ironIngot);

        assertFalse(recipes.isEmpty(), "Iron Ingot acquisitions must be found");
        boolean foundFurnace = false;
        for (UniversalRecipe r : recipes) {
            if (r.type() == RecipeDisplayType.FURNACE) {
                foundFurnace = true;
                assertNotNull(r.furnaceInput());
                assertEquals(
                    new ItemStack(Blocks.iron_ore).getItem(),
                    r.furnaceInput()
                        .getDisplayStack(0)
                        .getItem());
            }
        }
        assertTrue(foundFurnace, "Iron Ingot must have a furnace smelting recipe discovered automatically");
    }

    @Test
    void furnaceUsagesIncludeRecipesThatUseItAsTheMachine() {
        assertTrue(
            UniversalRecipeRegistry.instance()
                .findUsages(new ItemStack(Blocks.furnace))
                .stream()
                .anyMatch(recipe -> recipe.type() == RecipeDisplayType.FURNACE));
    }

    @Test
    void lavaBucketUsagesIncludeItsRuntimeFurnaceFuelAction() {
        assertTrue(
            UniversalRecipeRegistry.instance()
                .findUsages(new ItemStack(Items.lava_bucket))
                .stream()
                .anyMatch(
                    recipe -> recipe.operationName()
                        .contains("Furnace Fuel")));
    }

    @Test
    void waterBucketUsagesExplainItsWorldPlacementAction() {
        assertTrue(
            UniversalRecipeRegistry.instance()
                .findUsages(new ItemStack(Items.water_bucket))
                .stream()
                .anyMatch(
                    recipe -> recipe.operationName()
                        .equals("Place Water")));
    }

    @Test
    void waterBottleUsagesIncludeRuntimeBrewingTransitions() {
        ItemStack waterBottle = new ItemStack(Items.potionitem, 1, 0);
        assertTrue(
            UniversalRecipeRegistry.instance()
                .findUsages(waterBottle)
                .stream()
                .anyMatch(
                    recipe -> "Brewing".equals(recipe.operationName()) && recipe.output() != null
                        && recipe.output()
                            .getItem() == Items.potionitem
                        && recipe.allInputs()
                            .stream()
                            .anyMatch(input -> input.getItem() == Items.nether_wart)));
    }

    @Test
    void brewingStandUsagesIncludeBrewingTransitions() {
        assertTrue(
            UniversalRecipeRegistry.instance()
                .getAllRecipes()
                .stream()
                .filter(recipe -> recipe.type() == RecipeDisplayType.BREWING)
                .anyMatch(
                    recipe -> recipe.machine() != null && recipe.machine()
                        .getItem() == Items.brewing_stand));
        assertTrue(
            UniversalRecipeRegistry.instance()
                .findUsages(new ItemStack(Items.brewing_stand))
                .stream()
                .anyMatch(recipe -> recipe.type() == RecipeDisplayType.BREWING));
    }

    @Test
    void coalUsagesKeepCraftingAndFuelInSeparateSemanticGroups() {
        java.util.Map<RecipeCategory, List<UniversalRecipe>> groups = UniversalRecipeRegistry.instance()
            .findUsageGroups(new ItemStack(Items.coal));

        assertTrue(groups.containsKey(RecipeCategory.CRAFTING));
        assertTrue(groups.containsKey(RecipeCategory.FUEL));
        assertTrue(
            groups.get(RecipeCategory.FUEL)
                .stream()
                .allMatch(recipe -> recipe.type() == RecipeDisplayType.FURNACE_FUEL));
        assertFalse(
            groups.get(RecipeCategory.CRAFTING)
                .stream()
                .anyMatch(recipe -> recipe.type() == RecipeDisplayType.FURNACE_FUEL));
    }

    @Test
    void testUsageLookupUniversalReversal() {
        // 1. Wheat -> must find Bread
        ItemStack wheat = new ItemStack(Items.wheat);
        List<UniversalRecipe> wheatUsages = UniversalRecipeRegistry.instance()
            .findUsages(wheat);
        assertFalse(wheatUsages.isEmpty(), "Wheat usages must be found");
        assertTrue(
            wheatUsages.stream()
                .anyMatch(
                    r -> r.output() != null && r.output()
                        .getItem() == Items.bread),
            "Wheat usages must include Bread");

        // 2. Clay Ball -> must find Clay Block and Furnace Brick
        ItemStack clayBall = new ItemStack(Items.clay_ball);
        List<UniversalRecipe> clayUsages = UniversalRecipeRegistry.instance()
            .findUsages(clayBall);
        assertFalse(clayUsages.isEmpty(), "Clay Ball usages must be found");
        assertTrue(
            clayUsages.stream()
                .anyMatch(
                    r -> r.output() != null && r.output()
                        .getItem() == new ItemStack(Blocks.clay).getItem()),
            "Clay Ball usages must include Clay Block");
        assertTrue(
            clayUsages.stream()
                .anyMatch(
                    r -> r.type() == RecipeDisplayType.FURNACE && r.output() != null
                        && r.output()
                            .getItem() == Items.brick),
            "Clay Ball usages must include Furnace Brick");

        // 3. Copper Ingot -> must find Smelter Tuyere
        ItemStack copperIngot = new ItemStack(NEItems.copperIngot);
        List<UniversalRecipe> copperUsages = UniversalRecipeRegistry.instance()
            .findUsages(copperIngot);
        assertFalse(copperUsages.isEmpty(), "Copper Ingot usages must be found");
        assertTrue(
            copperUsages.stream()
                .anyMatch(
                    r -> r.output() != null && r.output()
                        .getItem() == new ItemStack(NEBlocks.smelterTuyere).getItem()),
            "Copper Ingot usages must include Smelter Tuyere");
    }

    @Test
    void testForgeOreDictionaryIngredientSupport() {
        ItemStack bellows = new ItemStack(NEItems.bellows);
        List<UniversalRecipe> recipes = UniversalRecipeRegistry.instance()
            .findAcquisitions(bellows);

        assertFalse(recipes.isEmpty(), "Bellows recipe must be discovered");
        UniversalRecipe bellowsRecipe = recipes.get(0);

        assertEquals(RecipeDisplayType.CRAFTING_GRID, bellowsRecipe.type());
        assertNotNull(bellowsRecipe.grid());

        // Planks slot in bellows recipe should support alternatives
        CraftingGridSlot plankSlot = bellowsRecipe.grid()[0][1]; // Top center 'P'
        assertFalse(plankSlot.isEmpty());
    }

    @Test
    void testShapelessForgeOreRecipePreservesAlternativesWithoutInventingPlacement() {
        net.minecraftforge.oredict.OreDictionary.registerOre("testBrowserAlternatives", new ItemStack(Items.stick));
        net.minecraftforge.oredict.OreDictionary.registerOre("testBrowserAlternatives", new ItemStack(Items.wheat));
        ShapelessOreRecipe oreRecipe = new ShapelessOreRecipe(
            new ItemStack(NEItems.refractoryMix),
            "testBrowserAlternatives",
            net.minecraft.init.Items.stick);
        UniversalRecipe adapted = RecipeKnowledgeAdapter.adaptCraftingRecipe(oreRecipe, 100_000);

        assertNotNull(adapted);
        assertTrue(
            adapted.operationName()
                .contains("Shapeless"));
        assertEquals(RecipeDisplayType.CRAFTING_GRID, adapted.type());
        List<CraftingGridSlot> occupied = new java.util.ArrayList<>();
        for (CraftingGridSlot[] row : adapted.grid()) {
            for (CraftingGridSlot slot : row) if (!slot.isEmpty()) occupied.add(slot);
        }
        assertEquals(2, occupied.size());
        assertTrue(
            occupied.stream()
                .anyMatch(
                    slot -> slot.alternatives()
                        .size() > 1),
            "plankWood alternatives retained");
    }

    @Test
    void testUnknownCustomRecipesAreReportedRatherThanShownAsEmptyCrafting() {
        assertTrue(
            UniversalRecipeRegistry.instance()
                .getUnsupportedRecipeClasses()
                .contains(naturalexceptions.minecraft.recipe.RecipeToolAssembly.class.getName()),
            "Procedural tool assembly is represented by authored process knowledge and reported unsupported as IRecipe");
    }

    @Test
    void testGameItemRegistryDiscoveryAndModFiltering() {
        List<GameItemRegistry.IndexedItem> allItems = GameItemRegistry.instance()
            .getAllIndexedItems();
        assertFalse(allItems.isEmpty(), "GameItemRegistry should index items");
        assertEquals(
            1,
            allItems.stream()
                .filter(
                    item -> item.displayName()
                        .equals("Furnace"))
                .count(),
            "Lit and unlit furnace item forms should not duplicate the browser entry");
        assertFalse(
            allItems.stream()
                .anyMatch(
                    item -> item.displayName()
                        .equals("tile.null.name")));

        // Search Bread
        List<GameItemRegistry.IndexedItem> breadItems = GameItemRegistry.instance()
            .search("Bread");
        assertFalse(breadItems.isEmpty());
        assertTrue(
            breadItems.stream()
                .anyMatch(
                    i -> i.stack()
                        .getItem() == Items.bread));

        // Filter @minecraft
        List<GameItemRegistry.IndexedItem> mcIron = GameItemRegistry.instance()
            .search("@minecraft iron");
        assertFalse(mcIron.isEmpty());
        assertTrue(
            mcIron.stream()
                .allMatch(
                    i -> i.modId()
                        .equals("minecraft")));

        // Filter @naturalexceptions
        List<GameItemRegistry.IndexedItem> neItems = GameItemRegistry.instance()
            .search("@naturalexceptions");
        assertFalse(neItems.isEmpty());
        assertTrue(
            neItems.stream()
                .allMatch(
                    i -> i.modId()
                        .equals("naturalexceptions")));
    }
}
