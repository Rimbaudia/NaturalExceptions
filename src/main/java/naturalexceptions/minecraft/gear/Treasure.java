package naturalexceptions.minecraft.gear;

/** The wearable treasures found in sky caches: what each is called, where it is worn and what it does. */
public enum Treasure {

    CLOUD_FLASK("cloudFlask", "cloud_flask", GearKind.BELT, "A bottled cloud. Jump again in mid-air."),
    GLIDER_CLOAK("gliderCloak", "glider_cloak", GearKind.BACK, "Hold jump while falling to glide."),
    HORSESHOE_PENDANT("horseshoePendant", "horseshoe_pendant", GearKind.NECK, "You never take a hard landing."),
    CLIMBING_CLAWS("climbingClaws", "climbing_claws", GearKind.GLOVE,
        "Cling to walls; jump to climb. A pair climbs faster."),
    BAND_OF_MENDING("bandOfMending", "band_of_mending", GearKind.RING,
        "Wounds heal half again as fast. Up to two bands help.");

    private final String unlocalizedName;
    private final String registryName;
    private final GearKind kind;
    private final String effect;

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect) {
        this.unlocalizedName = unlocalizedName;
        this.registryName = registryName;
        this.kind = kind;
        this.effect = effect;
    }

    public String unlocalizedName() {
        return unlocalizedName;
    }

    public String registryName() {
        return registryName;
    }

    public GearKind kind() {
        return kind;
    }

    public String effect() {
        return effect;
    }

    /** Texture name of the item sprite. */
    public String texture() {
        return "naturalexceptions:" + registryName;
    }
}
