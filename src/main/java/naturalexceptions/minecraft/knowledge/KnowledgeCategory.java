package naturalexceptions.minecraft.knowledge;

/**
 * High-level categories for grouping entries in the Knowledge Browser.
 */
public enum KnowledgeCategory {

    CRAFTING("Crafting", "Grid crafting and tool assembly recipes"),
    METALLURGY("Metallurgy", "Thermal smelting, crucibles, fuels, and slag management"),
    CASTING("Casting", "Casting molds, molten pouring, and component cooling"),
    ASSEMBLY("Assembly", "Tool construction, component fitting, and maintenance"),
    MULTIBLOCKS("Multiblocks", "Furnace layouts, structural chambers, and machinery"),
    MATERIALS("Materials", "Elemental properties, thermal requirements, and behaviors"),
    POWER("Power", "Electricity: sources, cable, machines, and high tension"),
    SKY("The Sky", "The sky islands, their dungeons, and the land's wind"),
    GEAR("Gear", "Worn things with effects, and where they are found"),
    SURVIVAL("Survival", "Thirst, wounds, and what the sky brings down");

    private final String displayName;
    private final String description;

    KnowledgeCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
