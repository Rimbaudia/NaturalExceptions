package naturalexceptions.core.material;

/**
 * High-level material classification for vanilla Minecraft and mod resources.
 */
public enum ResourceMaterialClass {

    /**
     * Metallic elements and alloys. Capable of smelting, crucible melting, liquid casting,
     * alloying, and tool fabrication.
     */
    METALLIC("Metallic", true),

    /**
     * Crystalline carbon diamond. High hardness, covalent crystal lattice.
     * Does not melt in an open hearth; requires high-pressure processing or abrasive grinding.
     */
    CARBON_CRYSTAL("Carbon Crystal", false),

    /**
     * Beryl gemstone crystal (Emerald).
     * Brittle cyclosilicate crystal; does not melt in an ordinary foundry hearth.
     */
    BERYL_CRYSTAL("Beryl Gemstone Crystal", false),

    /**
     * Silicate crystal (Nether Quartz).
     * Glass/ceramic precursor; does not melt into metallic melt.
     */
    SILICATE_CRYSTAL("Silicate Quartz Crystal", false),

    /**
     * Complex metamorphic/sedimentary mineral rock (Lapis Lazuli).
     * Mineral aggregate; not castable into metallic components.
     */
    MINERAL_ROCK("Sedimentary Mineral Rock", false),

    /**
     * Reactive/electrical particulate mineral (Redstone).
     * Destabilizes and combusts/volatilizes under thermal flux rather than melting cleanly.
     */
    REACTIVE_MINERAL("Reactive Mineral", false),

    /**
     * Combustible carbonaceous fuel (Coal, Charcoal).
     * Feeds combustion in the firebox; rejected from the metal charge crucible.
     */
    ORGANIC_FUEL("Combustible Carbon Fuel", false),

    /**
     * Nether magical combustion fuel (Blaze Rod, Blaze Powder).
     * Reaches extreme flame temperatures (>2400 K) when burned in the hearth.
     */
    MAGICAL_FUEL("Extreme Nether Fuel", false);

    private final String label;
    private final boolean meltable;

    ResourceMaterialClass(String label, boolean meltable) {
        this.label = label;
        this.meltable = meltable;
    }

    public String label() {
        return label;
    }

    public boolean isMeltable() {
        return meltable;
    }
}
