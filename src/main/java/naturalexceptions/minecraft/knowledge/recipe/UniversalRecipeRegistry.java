package naturalexceptions.minecraft.knowledge.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.PotionHelper;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.oredict.OreDictionary;

import naturalexceptions.minecraft.knowledge.KnowledgeEntry;
import naturalexceptions.minecraft.knowledge.KnowledgeRegistry;
import naturalexceptions.minecraft.knowledge.ProcessStep;

/**
 * Universal runtime recipe registry indexing all vanilla recipes, Forge OreDict recipes,
 * furnace smelting, and authored Natural Exceptions metallurgical processes.
 */
public final class UniversalRecipeRegistry {

    private static final UniversalRecipeRegistry INSTANCE = new UniversalRecipeRegistry();

    private final List<UniversalRecipe> allRecipes = new ArrayList<>();
    private final Map<String, UniversalRecipe> recipesById = new LinkedHashMap<>();
    private final Map<Item, List<UniversalRecipe>> byOutputItem = new LinkedHashMap<>();
    private final Map<Item, List<UniversalRecipe>> byInputItem = new LinkedHashMap<>();
    private final Map<Item, List<UniversalRecipe>> byMachineItem = new LinkedHashMap<>();
    private final Map<Integer, List<UniversalRecipe>> byInputOreId = new LinkedHashMap<>();
    private final Set<String> unsupportedRecipeClasses = new LinkedHashSet<>();
    private boolean initialized = false;

    private UniversalRecipeRegistry() {}

    public static UniversalRecipeRegistry instance() {
        return INSTANCE;
    }

    public synchronized void init() {
        if (initialized) return;
        initialized = true;

        allRecipes.clear();
        recipesById.clear();
        byOutputItem.clear();
        byInputItem.clear();
        byMachineItem.clear();
        byInputOreId.clear();
        unsupportedRecipeClasses.clear();

        // 1. Index CraftingManager runtime recipes (Vanilla + Forge + Mods)
        try {
            List recipeList = CraftingManager.getInstance()
                .getRecipeList();
            if (recipeList != null) {
                int index = 0;
                for (Object obj : recipeList) {
                    if (obj instanceof IRecipe) {
                        IRecipe gameRecipe = (IRecipe) obj;
                        try {
                            UniversalRecipe recipe = RecipeKnowledgeAdapter.adaptCraftingRecipe(gameRecipe, index++);
                            if (recipe != null) {
                                registerRecipe(recipe);
                            } else if (gameRecipe.getRecipeOutput() != null && gameRecipe.getRecipeOutput()
                                .getItem() != null) {
                                    unsupportedRecipeClasses.add(
                                        gameRecipe.getClass()
                                            .getName());
                                }
                        } catch (Throwable ignored) {
                            unsupportedRecipeClasses.add(
                                gameRecipe.getClass()
                                    .getName());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Index FurnaceRecipes runtime smelting
        try {
            Map smeltingList = FurnaceRecipes.smelting()
                .getSmeltingList();
            if (smeltingList != null) {
                int index = 0;
                for (Object entryObj : smeltingList.entrySet()) {
                    if (entryObj instanceof Map.Entry) {
                        try {
                            UniversalRecipe recipe = RecipeKnowledgeAdapter
                                .adaptFurnaceRecipe((Map.Entry<?, ItemStack>) entryObj, index++);
                            if (recipe != null) {
                                registerRecipe(recipe);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Index runtime fuel handlers and brewing ingredient transformations.
        indexFurnaceFuels();
        indexBrewingRecipes();
        indexBucketPlacement();

        // 4. Index Authored Natural Exceptions processes
        try {
            KnowledgeRegistry.instance()
                .init();
            for (KnowledgeEntry entry : KnowledgeRegistry.instance()
                .getAllEntries()) {
                int stepIdx = 0;
                for (ProcessStep step : entry.acquisitions()) {
                    if (isOrdinaryCraftingStep(step) && isCoveredByAutomaticRecipe(step)) {
                        stepIdx++;
                        continue;
                    }
                    String id = "ne_acq_" + entry.id() + "_" + (stepIdx++);
                    registerRecipe(UniversalRecipe.process(id, step));
                }
                stepIdx = 0;
                for (ProcessStep step : entry.usages()) {
                    if (isOrdinaryCraftingStep(step) && isCoveredByAutomaticRecipe(step)) {
                        stepIdx++;
                        continue;
                    }
                    String id = "ne_usage_" + entry.id() + "_" + (stepIdx++);
                    registerRecipe(UniversalRecipe.process(id, step));
                }
            }
        } catch (Throwable ignored) {}
    }

    public synchronized void reset() {
        allRecipes.clear();
        recipesById.clear();
        byOutputItem.clear();
        byInputItem.clear();
        byMachineItem.clear();
        byInputOreId.clear();
        unsupportedRecipeClasses.clear();
        initialized = false;
    }

    private void registerRecipe(UniversalRecipe recipe) {
        if (recipe == null) return;
        recipesById.put(recipe.id(), recipe);
        allRecipes.add(recipe);
        ItemStack output = recipe.output();
        if (output != null && output.getItem() != null) {
            byOutputItem.computeIfAbsent(output.getItem(), k -> new ArrayList<>())
                .add(recipe);
        }
        for (ItemStack input : recipe.allInputs()) {
            if (input == null || input.getItem() == null) continue;
            byInputItem.computeIfAbsent(input.getItem(), k -> new ArrayList<>())
                .add(recipe);
            for (int oreId : OreDictionary.getOreIDs(input)) {
                byInputOreId.computeIfAbsent(oreId, k -> new ArrayList<>())
                    .add(recipe);
            }
        }
        ItemStack machine = recipe.machine();
        if (machine != null && machine.getItem() != null) {
            byMachineItem.computeIfAbsent(machine.getItem(), k -> new ArrayList<>())
                .add(recipe);
        }
    }

    private static boolean isOrdinaryCraftingStep(ProcessStep step) {
        String operation = step.operation()
            .toLowerCase(java.util.Locale.ROOT);
        String station = step.machineName()
            .toLowerCase(java.util.Locale.ROOT);
        return operation.contains("crafting") || station.equals("crafting grid") || station.equals("crafting table");
    }

    private boolean isCoveredByAutomaticRecipe(ProcessStep step) {
        for (UniversalRecipe recipe : allRecipes) {
            if (recipe.type() != RecipeDisplayType.CRAFTING_GRID || !step.matchesOutput(recipe.output())) continue;
            boolean allInputsAreRepresented = true;
            for (ItemStack input : step.inputs()) {
                if (!recipe.matchesInput(input)) {
                    allInputsAreRepresented = false;
                    break;
                }
            }
            if (allInputsAreRepresented) return true;
        }
        return false;
    }

    public List<UniversalRecipe> getAllRecipes() {
        return Collections.unmodifiableList(allRecipes);
    }

    public List<String> getUnsupportedRecipeClasses() {
        init();
        return Collections.unmodifiableList(new ArrayList<>(unsupportedRecipeClasses));
    }

    /**
     * Finds all recipes that produce the queried ItemStack ("R" key).
     */
    public List<UniversalRecipe> findAcquisitions(ItemStack query) {
        if (query == null) return Collections.emptyList();
        init();

        init();

        Set<UniversalRecipe> matches = new LinkedHashSet<>();
        for (UniversalRecipe recipe : byOutputItem.getOrDefault(query.getItem(), Collections.emptyList())) {
            if (recipe.matchesOutput(query)) {
                matches.add(recipe);
            }
        }

        return new ArrayList<>(matches);
    }

    /**
     * Finds all recipes that consume the queried ItemStack as an input or ingredient ("U" key).
     */
    public List<UniversalRecipe> findUsages(ItemStack query) {
        if (query == null) return Collections.emptyList();
        init();

        init();

        Set<UniversalRecipe> candidates = new LinkedHashSet<>(
            byInputItem.getOrDefault(query.getItem(), Collections.emptyList()));
        candidates.addAll(byMachineItem.getOrDefault(query.getItem(), Collections.emptyList()));
        for (int oreId : OreDictionary.getOreIDs(query)) {
            candidates.addAll(byInputOreId.getOrDefault(oreId, Collections.emptyList()));
        }
        Set<UniversalRecipe> matches = new LinkedHashSet<>();
        for (UniversalRecipe recipe : candidates) {
            if (recipe.matchesInput(query) || recipe.matchesMachine(query)) {
                matches.add(recipe);
            }
        }

        return new ArrayList<>(matches);
    }

    /** Returns only semantic usage categories which actually contain a matching entry. */
    public Map<RecipeCategory, List<UniversalRecipe>> findUsageGroups(ItemStack query) {
        return groupByCategory(findUsages(query));
    }

    /** Groups recipe results by their authored display type without inspecting labels or operation strings. */
    public static Map<RecipeCategory, List<UniversalRecipe>> groupByCategory(List<UniversalRecipe> recipes) {
        Map<RecipeCategory, List<UniversalRecipe>> groups = new EnumMap<>(RecipeCategory.class);
        if (recipes == null) return groups;
        for (UniversalRecipe recipe : recipes) {
            if (recipe == null || recipe.type() == null) continue;
            groups.computeIfAbsent(
                recipe.type()
                    .category(),
                ignored -> new ArrayList<>())
                .add(recipe);
        }
        for (Map.Entry<RecipeCategory, List<UniversalRecipe>> entry : groups.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(groups);
    }

    private void indexFurnaceFuels() {
        int index = 0;
        Set<String> seen = new HashSet<>();
        for (GameItemRegistry.IndexedItem indexed : GameItemRegistry.instance()
            .getAllIndexedItems()) {
            ItemStack stack = indexed.stack();
            if (stack.getItem() == null) continue;
            int burnTicks;
            try {
                burnTicks = TileEntityFurnace.getItemBurnTime(stack);
            } catch (Throwable ignored) {
                continue;
            }
            if (burnTicks <= 0) continue;
            String key = stack.getItem()
                .getUnlocalizedName(stack) + ":"
                + stack.getItemDamage();
            if (!seen.add(key)) continue;
            registerRecipe(UniversalRecipe.furnaceFuel("furnace_fuel_" + index++, stack, burnTicks));
        }
    }

    private void indexBrewingRecipes() {
        List<ItemStack> ingredients = new ArrayList<>();
        Set<String> ingredientKeys = new HashSet<>();
        Set<Integer> potionStates = new LinkedHashSet<>();
        for (GameItemRegistry.IndexedItem indexed : GameItemRegistry.instance()
            .getAllIndexedItems()) {
            ItemStack stack = indexed.stack();
            Item item = stack.getItem();
            if (item == null) continue;
            if (item instanceof ItemPotion && stack.getItemDamage() != 32767) {
                potionStates.add(stack.getItemDamage());
            }
            try {
                if (item.isPotionIngredient(stack)) {
                    String effect = item.getPotionEffect(stack);
                    String key = item.getUnlocalizedName(stack) + ":" + stack.getItemDamage() + ":" + effect;
                    if (effect != null && ingredientKeys.add(key)) ingredients.add(stack);
                }
            } catch (Throwable ignored) {}
        }

        potionStates.add(0);
        Queue<Integer> pending = new ArrayDeque<>(potionStates);
        Set<Integer> visited = new LinkedHashSet<>(potionStates);
        int recipeIndex = 0;
        while (!pending.isEmpty() && visited.size() <= 32768) {
            int inputDamage = pending.remove();
            ItemStack potionInput = new ItemStack(Items.potionitem, 1, inputDamage);
            for (ItemStack ingredient : ingredients) {
                String effect;
                try {
                    effect = ingredient.getItem()
                        .getPotionEffect(ingredient);
                } catch (Throwable ignored) {
                    continue;
                }
                if (effect == null) continue;
                int outputDamage;
                try {
                    outputDamage = PotionHelper.applyIngredient(inputDamage, effect);
                } catch (Throwable ignored) {
                    continue;
                }
                if (outputDamage == inputDamage || !isValidBrewingTransition(inputDamage, outputDamage)) continue;

                ItemStack potionOutput = new ItemStack(Items.potionitem, 1, outputDamage);
                UniversalRecipe recipe = RecipeKnowledgeAdapter
                    .adaptBrewingRecipe("brewing_" + recipeIndex++, potionInput, ingredient, potionOutput);
                registerRecipe(recipe);
                if (outputDamage >= 0 && outputDamage < 32768 && visited.add(outputDamage)) pending.add(outputDamage);
            }
        }
    }

    private static boolean isValidBrewingTransition(int inputDamage, int outputDamage) {
        if (!ItemPotion.isSplash(inputDamage) && ItemPotion.isSplash(outputDamage)) return true;
        List<?> inputEffects = Items.potionitem.getEffects(inputDamage);
        List<?> outputEffects = Items.potionitem.getEffects(outputDamage);
        return (inputDamage <= 0 || inputEffects != outputEffects)
            && (inputEffects == null || !inputEffects.equals(outputEffects) && outputEffects != null);
    }

    private void indexBucketPlacement() {
        registerRecipe(
            UniversalRecipe.bucketPlacement(
                "bucket_place_water",
                new ItemStack(Items.water_bucket),
                new ItemStack(Items.bucket),
                "Water"));
        registerRecipe(
            UniversalRecipe.bucketPlacement(
                "bucket_place_lava",
                new ItemStack(Items.lava_bucket),
                new ItemStack(Items.bucket),
                "Lava"));
    }
}
