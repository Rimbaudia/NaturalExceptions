package naturalexceptions.minecraft.knowledge.recipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Adapter converting runtime Minecraft/Forge recipe objects into unified UniversalRecipe models.
 */
public final class RecipeKnowledgeAdapter {

    private static final Field SHAPED_ORE_WIDTH = shapedOreField("width");
    private static final Field SHAPED_ORE_HEIGHT = shapedOreField("height");

    private RecipeKnowledgeAdapter() {}

    /**
     * Adapts an arbitrary crafting IRecipe into a UniversalRecipe.
     * Returns null if unparseable or output is null.
     */
    public static UniversalRecipe adaptCraftingRecipe(IRecipe recipe, int recipeIndex) {
        if (recipe == null || recipe.getRecipeOutput() == null
            || recipe.getRecipeOutput()
                .getItem() == null)
            return null;

        String id = "crafting_" + recipeIndex
            + "_"
            + recipe.getRecipeOutput()
                .getItem()
                .getUnlocalizedName();

        if (recipe instanceof ShapedRecipes) {
            return adaptShaped((ShapedRecipes) recipe, id);
        }
        if (recipe instanceof ShapelessRecipes) {
            return adaptShapeless((ShapelessRecipes) recipe, id);
        }
        if (recipe instanceof ShapedOreRecipe) {
            return adaptShapedOre((ShapedOreRecipe) recipe, id);
        }
        if (recipe instanceof ShapelessOreRecipe) {
            return adaptShapelessOre((ShapelessOreRecipe) recipe, id);
        }

        // Unknown IRecipe types can have procedural inputs which are not exposed safely.
        return null;
    }

    private static UniversalRecipe adaptShaped(ShapedRecipes recipe, String id) {
        int width = recipe.recipeWidth;
        int height = recipe.recipeHeight;
        ItemStack[] items = recipe.recipeItems;

        CraftingGridSlot[][] grid = new CraftingGridSlot[3][3];
        List<ItemStack> flatInputs = new ArrayList<>();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r < height && c < width && items != null) {
                    int idx = r * width + c;
                    if (idx < items.length && items[idx] != null) {
                        grid[r][c] = CraftingGridSlot.of(items[idx]);
                        flatInputs.add(items[idx].copy());
                    } else {
                        grid[r][c] = CraftingGridSlot.empty();
                    }
                } else {
                    grid[r][c] = CraftingGridSlot.empty();
                }
            }
        }

        return UniversalRecipe.crafting(
            id,
            recipe.getRecipeOutput(),
            grid,
            flatInputs,
            "Shaped Crafting",
            Collections.singletonList("Shaped 3x3 crafting grid recipe."));
    }

    private static UniversalRecipe adaptShapeless(ShapelessRecipes recipe, String id) {
        List items = recipe.recipeItems;
        CraftingGridSlot[][] grid = new CraftingGridSlot[3][3];
        List<ItemStack> flatInputs = new ArrayList<>();

        int slotIndex = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (items != null && slotIndex < items.size()) {
                    Object obj = items.get(slotIndex++);
                    if (obj instanceof ItemStack) {
                        ItemStack st = (ItemStack) obj;
                        grid[r][c] = CraftingGridSlot.of(st);
                        flatInputs.add(st.copy());
                    } else {
                        grid[r][c] = CraftingGridSlot.empty();
                    }
                } else {
                    grid[r][c] = CraftingGridSlot.empty();
                }
            }
        }

        return UniversalRecipe.crafting(
            id,
            recipe.getRecipeOutput(),
            grid,
            flatInputs,
            "Shapeless Crafting",
            Collections.singletonList("Shapeless recipe (ingredients can be placed in any arrangement)."));
    }

    private static UniversalRecipe adaptShapedOre(ShapedOreRecipe recipe, String id) {
        Object[] input = recipe.getInput();
        int[] dimensions = shapedOreDimensions(recipe, input == null ? 0 : input.length);
        int width = dimensions[0];
        int height = dimensions[1];
        CraftingGridSlot[][] grid = new CraftingGridSlot[3][3];
        List<ItemStack> flatInputs = new ArrayList<>();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (r < height && c < width && input != null) {
                    int idx = r * width + c;
                    if (idx < input.length && input[idx] != null) {
                        CraftingGridSlot slot = adaptOreInput(input[idx]);
                        grid[r][c] = slot;
                        flatInputs.addAll(slot.alternatives());
                    } else {
                        grid[r][c] = CraftingGridSlot.empty();
                    }
                } else {
                    grid[r][c] = CraftingGridSlot.empty();
                }
            }
        }

        return UniversalRecipe.crafting(
            id,
            recipe.getRecipeOutput(),
            grid,
            flatInputs,
            "Shaped Ore Recipe",
            Collections.singletonList("Accepts OreDictionary alternatives."));
    }

    private static UniversalRecipe adaptShapelessOre(ShapelessOreRecipe recipe, String id) {
        ArrayList<Object> input = recipe.getInput();
        CraftingGridSlot[][] grid = new CraftingGridSlot[3][3];
        List<ItemStack> flatInputs = new ArrayList<>();

        int slotIndex = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (input != null && slotIndex < input.size()) {
                    Object obj = input.get(slotIndex++);
                    if (obj != null) {
                        CraftingGridSlot slot = adaptOreInput(obj);
                        grid[r][c] = slot;
                        flatInputs.addAll(slot.alternatives());
                    } else {
                        grid[r][c] = CraftingGridSlot.empty();
                    }
                } else {
                    grid[r][c] = CraftingGridSlot.empty();
                }
            }
        }

        return UniversalRecipe.crafting(
            id,
            recipe.getRecipeOutput(),
            grid,
            flatInputs,
            "Shapeless Ore Recipe",
            Collections.singletonList("Shapeless recipe accepting OreDictionary alternatives."));
    }

    @SuppressWarnings("unchecked")
    private static CraftingGridSlot adaptOreInput(Object obj) {
        if (obj == null) return CraftingGridSlot.empty();

        if (obj instanceof ItemStack) {
            return CraftingGridSlot.of((ItemStack) obj);
        }
        if (obj instanceof String) {
            String oreName = (String) obj;
            ArrayList<ItemStack> ores = OreDictionary.getOres(oreName);
            return CraftingGridSlot.ofList(ores, oreName);
        }
        if (obj instanceof List) {
            List<ItemStack> list = (List<ItemStack>) obj;
            return CraftingGridSlot.ofList(list, null);
        }

        return CraftingGridSlot.empty();
    }

    private static int[] shapedOreDimensions(ShapedOreRecipe recipe, int inputLength) {
        // 1.7.10 ShapedOreRecipe supports 1x1 through 3x3 rectangles; the
        // recipe stores only width/height privately, while getInput exposes a
        // row-major array. Prefer the narrowest valid rectangle for known sizes.
        try {
            if (SHAPED_ORE_WIDTH != null && SHAPED_ORE_HEIGHT != null) {
                return new int[] { SHAPED_ORE_WIDTH.getInt(recipe), SHAPED_ORE_HEIGHT.getInt(recipe) };
            }
        } catch (IllegalAccessException ignored) {}
        // Only used if the supported Forge 1.7.10 implementation changes its
        // private fields; preserve all ingredient slots even if placement is unknown.
        if (inputLength <= 0) return new int[] { 0, 0 };
        return new int[] { Math.min(3, inputLength), (inputLength + 2) / 3 };
    }

    private static Field shapedOreField(String name) {
        try {
            Field field = ShapedOreRecipe.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    /**
     * Adapts a furnace smelting entry into a UniversalRecipe.
     */
    public static UniversalRecipe adaptFurnaceRecipe(Map.Entry<?, ItemStack> entry, int index) {
        if (entry == null || entry.getValue() == null) return null;

        ItemStack output = entry.getValue();
        Object key = entry.getKey();
        ItemStack input = null;

        if (key instanceof ItemStack) {
            input = (ItemStack) key;
        } else if (key instanceof Integer) {
            input = new ItemStack(net.minecraft.item.Item.getItemById((Integer) key), 1, 32767);
        }

        if (input == null || input.getItem() == null) return null;

        String id = "furnace_" + index
            + "_"
            + output.getItem()
                .getUnlocalizedName();
        float xp = net.minecraft.item.crafting.FurnaceRecipes.smelting()
            .func_151398_b(output);

        return UniversalRecipe.furnace(id, input, output, xp);
    }

    /** Adapts one runtime PotionHelper transformation to the shared recipe model. */
    public static UniversalRecipe adaptBrewingRecipe(String id, ItemStack potionInput, ItemStack ingredient,
        ItemStack potionOutput) {
        if (potionInput == null || ingredient == null || potionOutput == null) return null;
        return UniversalRecipe.brewing(id, potionInput, ingredient, potionOutput);
    }
}
