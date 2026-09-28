package naturalexceptions.minecraft.knowledge.recipe;

/** Semantic family of a game process, kept separate from how its inputs are drawn. */
public enum RecipeCategory {

    CRAFTING("Crafting"),
    SMELTING("Smelting"),
    FUEL("Fuel"),
    BREWING("Brewing"),
    MACHINE_PROCESSING("Machine / Processing"),
    WORLD_INTERACTION("World Interaction"),
    NATURAL_EXCEPTIONS_PROCESS("Natural Exceptions Process");

    private final String label;

    RecipeCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
