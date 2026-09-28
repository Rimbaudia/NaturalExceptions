package naturalexceptions.minecraft.knowledge.recipe;

/**
 * Visual presentation style for a recipe in the Knowledge Browser.
 */
public enum RecipeDisplayType {

    CRAFTING_GRID("Crafting Table", true, RecipeCategory.CRAFTING),
    FURNACE("Furnace Smelting", false, RecipeCategory.SMELTING),
    FURNACE_FUEL("Furnace Fuel", false, RecipeCategory.FUEL),
    BREWING("Brewing Stand", false, RecipeCategory.BREWING),
    MACHINE_PROCESS("Machine Processing", false, RecipeCategory.MACHINE_PROCESSING),
    WORLD_ACTION("World Interaction", false, RecipeCategory.WORLD_INTERACTION),
    PROCESS("Metallurgical Process", false, RecipeCategory.NATURAL_EXCEPTIONS_PROCESS);

    private final String defaultTitle;
    private final boolean isGrid;
    private final RecipeCategory category;

    RecipeDisplayType(String defaultTitle, boolean isGrid, RecipeCategory category) {
        this.defaultTitle = defaultTitle;
        this.isGrid = isGrid;
        this.category = category;
    }

    public String defaultTitle() {
        return defaultTitle;
    }

    public boolean isGrid() {
        return isGrid;
    }

    public RecipeCategory category() {
        return category;
    }
}
