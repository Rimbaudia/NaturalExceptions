package naturalexceptions.minecraft.gear;

/**
 * The wearable treasures: what each is called, where it is worn, what it does and where it comes from (its
 * {@link Source}). Most turn up in sky dungeon chests and can be crafted; some are found only in a dungeon's vault, and
 * the relics are dropped only by a Sky Warden (never in a chest, never crafted).
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
        "When struck, a gale throws nearby foes back (once every 20 s).", true),
    LAMPLIGHTERS_CAP("lamplightersCap", "lamplighters_cap", GearKind.HAT,
        "A Lumenite wick lights the ground around you as you walk."),
    WEATHERWISE_HOOD("weatherwiseHood", "weatherwise_hood", GearKind.HAT,
        "Feels the sky: warns you of a Star Rain or a Blood Night the day before. Lightning never strikes you."),
    DIVERS_BELL_HELM("diversBellHelm", "divers_bell_helm", GearKind.HAT,
        "Four times the air underwater, and clear sight below the surface."),
    GALE_STRIDERS("galeStriders", "gale_striders", GearKind.SHOE,
        "Run faster. With a pair, step up a block without jumping."),
    SKYSTONE_SABATONS("skystoneSabatons", "skystone_sabatons", GearKind.SHOE,
        "Crumbling floors hold under you. Half the knockback; with a pair, none.", Source.VAULT),
    FEATHERSTEP_SLIPPERS("featherstepSlippers", "featherstep_slippers", GearKind.SHOE,
        "Trigger Stones and pressure plates don't feel you, and monsters notice you later. A pair, later still."),
    CLOUD_TREADS("cloudTreads", "cloud_treads", GearKind.SHOE,
        "You fall gently and land softly. With a pair, walk on water (sneak to sink).");

    /** Where a treasure comes from. */
    public enum Source {
        /** Sky dungeon chests (and crafted). */
        CHEST,
        /** A sky dungeon's vault only, never crafted. */
        VAULT,
        /** A Sky Warden's relic: dropped only by one, never in a chest, never crafted. */
        WARDEN
    }

    private final String unlocalizedName;
    private final String registryName;
    private final GearKind kind;
    private final String effect;
    private final Source source;

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect) {
        this(unlocalizedName, registryName, kind, effect, Source.CHEST);
    }

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect, boolean relic) {
        this(unlocalizedName, registryName, kind, effect, relic ? Source.WARDEN : Source.CHEST);
    }

    Treasure(String unlocalizedName, String registryName, GearKind kind, String effect, Source source) {
        this.unlocalizedName = unlocalizedName;
        this.registryName = registryName;
        this.kind = kind;
        this.effect = effect;
        this.source = source;
    }

    public Source source() {
        return source;
    }

    /** A Warden Relic: dropped only by a Sky Warden. */
    public boolean relic() {
        return source == Source.WARDEN;
    }

    /** Found in sky dungeon chests, and craftable. */
    public boolean crafted() {
        return source == Source.CHEST;
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
