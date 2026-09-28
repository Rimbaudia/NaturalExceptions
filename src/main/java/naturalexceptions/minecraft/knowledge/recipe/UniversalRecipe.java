package naturalexceptions.minecraft.knowledge.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import naturalexceptions.minecraft.knowledge.ProcessStep;

/**
 * Universal recipe model unifying vanilla crafting, Forge OreDict recipes,
 * furnace smelting, and authored Natural Exceptions metallurgical processes.
 */
public final class UniversalRecipe {

    private final String id;
    private final RecipeDisplayType type;
    private final ItemStack output;
    private final List<ItemStack> allInputs;
    private final CraftingGridSlot[][] grid; // 3x3 [row][col]
    private final CraftingGridSlot furnaceInput;
    private final ItemStack machine;
    private final String machineName;
    private final String operationName;
    private final List<String> notes;
    private final float experience;

    public UniversalRecipe(String id, RecipeDisplayType type, ItemStack output, List<ItemStack> allInputs,
        CraftingGridSlot[][] grid, CraftingGridSlot furnaceInput, ItemStack machine, String machineName,
        String operationName, List<String> notes, float experience) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.output = output != null ? output.copy() : null;
        this.allInputs = allInputs != null ? new ArrayList<>(allInputs) : new ArrayList<>();
        this.grid = grid;
        this.furnaceInput = furnaceInput;
        this.machine = machine != null ? machine.copy() : null;
        this.machineName = machineName != null ? machineName : "Crafting";
        this.operationName = operationName != null ? operationName : type.defaultTitle();
        this.notes = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        this.experience = experience;
    }

    public String id() {
        return id;
    }

    public RecipeDisplayType type() {
        return type;
    }

    public ItemStack output() {
        return output != null ? output.copy() : null;
    }

    public List<ItemStack> allInputs() {
        return Collections.unmodifiableList(allInputs);
    }

    public CraftingGridSlot[][] grid() {
        return grid;
    }

    public CraftingGridSlot furnaceInput() {
        return furnaceInput;
    }

    public ItemStack machine() {
        return machine != null ? machine.copy() : null;
    }

    public String machineName() {
        return machineName;
    }

    public String operationName() {
        return operationName;
    }

    public List<String> notes() {
        return Collections.unmodifiableList(notes);
    }

    public float experience() {
        return experience;
    }

    /**
     * Checks if this recipe produces the queried item.
     */
    public boolean matchesOutput(ItemStack query) {
        if (query == null || output == null) return false;
        return ProcessStep.isMatching(output, query);
    }

    /**
     * Checks if this recipe consumes the queried item as an ingredient.
     */
    public boolean matchesInput(ItemStack query) {
        if (query == null) return false;

        for (ItemStack in : allInputs) {
            if (ProcessStep.isMatching(in, query)) {
                return true;
            }
        }

        // OreDictionary check
        int[] queryOreIds = OreDictionary.getOreIDs(query);
        if (queryOreIds != null && queryOreIds.length > 0) {
            for (ItemStack in : allInputs) {
                int[] inOreIds = OreDictionary.getOreIDs(in);
                if (inOreIds != null && inOreIds.length > 0) {
                    for (int qId : queryOreIds) {
                        for (int iId : inOreIds) {
                            if (qId == iId) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /** Checks whether the queried item is the station used by this recipe. */
    public boolean matchesMachine(ItemStack query) {
        return query != null && machine != null && ProcessStep.isMatching(machine, query);
    }

    /**
     * Factory for 3x3 crafting grid recipes.
     */
    public static UniversalRecipe crafting(String id, ItemStack output, CraftingGridSlot[][] grid,
        List<ItemStack> flatInputs, String operation, List<String> notes) {
        return new UniversalRecipe(
            id,
            RecipeDisplayType.CRAFTING_GRID,
            output,
            flatInputs,
            grid,
            null,
            new ItemStack(Blocks.crafting_table),
            "Crafting Table",
            operation != null ? operation : "Crafting Table",
            notes,
            0.0F);
    }

    /**
     * Factory for furnace smelting recipes.
     */
    public static UniversalRecipe furnace(String id, ItemStack input, ItemStack output, float xp) {
        List<ItemStack> inputs = Collections.singletonList(input.copy());
        List<String> notes = new ArrayList<>();
        if (xp > 0.0F) {
            notes.add(String.format("Experience: %.2f XP", xp));
        }

        return new UniversalRecipe(
            id,
            RecipeDisplayType.FURNACE,
            output,
            inputs,
            null,
            CraftingGridSlot.of(input),
            new ItemStack(Blocks.furnace),
            "Furnace",
            "Furnace Smelting",
            notes,
            xp);
    }

    public static UniversalRecipe furnaceFuel(String id, ItemStack input, int burnTicks) {
        List<ItemStack> inputs = Collections.singletonList(input.copy());
        ItemStack remainder = input.getItem() != null && input.getItem()
            .hasContainerItem(input) ? input.getItem()
                .getContainerItem(input) : null;
        int burnSeconds = Math.max(1, burnTicks / 20);
        return new UniversalRecipe(
            id,
            RecipeDisplayType.FURNACE_FUEL,
            remainder,
            inputs,
            null,
            null,
            new ItemStack(Blocks.furnace),
            "Furnace",
            "Furnace Fuel (" + burnSeconds + " s)",
            Collections.singletonList("Burn time: " + burnSeconds + " seconds."),
            0.0F);
    }

    public static UniversalRecipe brewing(String id, ItemStack potionInput, ItemStack ingredient,
        ItemStack potionOutput) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(potionInput.copy());
        inputs.add(ingredient.copy());
        return new UniversalRecipe(
            id,
            RecipeDisplayType.BREWING,
            potionOutput,
            inputs,
            null,
            null,
            new ItemStack(Items.brewing_stand),
            "Brewing Stand",
            "Brewing",
            Collections.singletonList("Place a potion and ingredient in a Brewing Stand."),
            0.0F);
    }

    public static UniversalRecipe bucketPlacement(String id, ItemStack input, ItemStack remainder, String fluidName) {
        return new UniversalRecipe(
            id,
            RecipeDisplayType.WORLD_ACTION,
            remainder,
            Collections.singletonList(input.copy()),
            null,
            null,
            null,
            "World",
            "Place " + fluidName,
            Collections.singletonList(
                "Places a " + fluidName.toLowerCase(java.util.Locale.ROOT)
                    + " source block and returns an empty bucket."),
            0.0F);
    }

    /**
     * Factory for custom authored metallurgical process steps.
     */
    public static UniversalRecipe process(String id, ProcessStep step) {
        ItemStack firstOut = !step.outputs()
            .isEmpty() ? step.outputs()
                .get(0) : null;
        return new UniversalRecipe(
            id,
            RecipeDisplayType.PROCESS,
            firstOut,
            step.inputs(),
            null,
            null,
            step.machineStack(),
            step.machineName(),
            step.operation(),
            step.notes(),
            0.0F);
    }
}
