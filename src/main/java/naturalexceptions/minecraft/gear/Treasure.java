package naturalexceptions.minecraft.gear;

/**
 * The wearable treasures: what each is called, where it is worn and what it does. Most turn up in sky dungeon chests
 * and can be crafted; the relics are dropped only by a Sky Warden (never in a chest, never crafted).
 */
public enum Treasure {

    CLOUD_FLASK("cloudFlask", "cloud_flask", GearKind.BELT, "A bottled cloud. Jump again in mid-air."),
    GLIDER_CLOAK("gliderCloak", "glider_cloak", GearKind.BACK, "Hold jump while falling to glide."),
    HORSESHOE_PENDANT("horseshoePendant", "horseshoe_pendant", GearKind.NECK, "You never take a hard landing."),
    CLIMBING_CLAWS("climbingClaws", "climbing_claws", GearKind.GLOVE,
        "Cling to walls; jump to climb. A pair climbs faster."),
    BAND_OF_MENDING("bandOfMending", "band_of_mending", GearKind.RING,
        "Wounds heal half again as fast. Up to two bands help."),
    WARDENS_VISOR("wardensVisor", "wardens_visor", GearKind.FACE,
        "Shows hidden traps, alcoves and cracked walls nearby; sees in the dark.", true),
    GALEHEART_BELT("galeheartBelt", "galeheart_belt", GearKind.BELT,
        "Press the Gear Ability key to dash along your gaze, even in mid-air.", true),
    AEGIS_SIGIL("aegisSigil", "aegis_sigil", GearKind.NECK,
        "When struck, a gale throws nearby foes back (once every 20 s).", true);

    private final String unlocalizedName;
    private final String registryName;
    private final GearKind kind;
    private final String effect;
    private final boolean relic;

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect) {
        this(unlocalizedName, registryName, kind, effect, false);
    }

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect, boolean relic) {
        this.unlocalizedName = unlocalizedName;
        this.registryName = registryName;
        this.kind = kind;
        this.effect = effect;
        this.relic = relic;
    }

    /** A Warden Relic: dropped only by a Sky Warden. */
    public boolean relic() {
        return relic;
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
