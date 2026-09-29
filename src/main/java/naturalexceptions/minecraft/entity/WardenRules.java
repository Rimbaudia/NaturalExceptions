package naturalexceptions.minecraft.entity;

import java.util.Random;

import naturalexceptions.minecraft.gear.Treasure;

/**
 * The Sky Warden's numbers and dice, apart from the entity so they can be checked: health, phases, attack timing and
 * what it drops.
 */
public final class WardenRules {

    public static final float MAX_HEALTH = 70.0F;
    public static final int ARMOUR = 6;
    public static final double KNOCKBACK_RESISTANCE = 0.6;
    public static final double SPEED_BOUND = 0.22;
    public static final double SPEED_UNBOUND = 0.3;
    public static final float MELEE_DAMAGE = 7.0F;

    /** Ticks between Gale Slams, and how long each is telegraphed (arms raised) before it lands. */
    public static final int SLAM_INTERVAL = 160;
    public static final int SLAM_WINDUP = 20;
    public static final double SLAM_RADIUS = 5.0;
    public static final float SLAM_DAMAGE = 4.0F;
    /** Horizontal and upward shove of a Gale Slam at its centre (it fades to half at the edge). */
    public static final double SLAM_PUSH = 1.6;
    public static final double SLAM_LIFT = 0.55;

    /** In phase two, a fan of Lumenite bolts at players further than this, every so often. */
    public static final double FAN_MIN_RANGE = 6.0;
    public static final int FAN_INTERVAL = 70;
    public static final int FAN_WINDUP = 15;
    public static final int FAN_BOLTS = 3;
    public static final float FAN_SPREAD_DEGREES = 12.0F;

    public static final int XP = 40;

    public enum Phase {
        /** Full to half health: slow swings and Gale Slams. */
        BOUND,
        /** Below half: faster, bolts at range, the alcoves open. */
        UNBOUND
    }

    private WardenRules() {}

    public static Phase phase(float health, float maxHealth) {
        return health > maxHealth * 0.5F ? Phase.BOUND : Phase.UNBOUND;
    }

    /** Shove from a Gale Slam at a distance from its centre: full at the middle, half at the edge, none beyond. */
    public static double slamStrength(double distance) {
        if (distance > SLAM_RADIUS) return 0.0;
        return 1.0 - 0.5 * (distance / SLAM_RADIUS);
    }

    // --- Drops ------------------------------------------------------------------------------------------------

    /** Relic weights: the visor and the belt are the common ones, the Gale Maul the rarest. */
    public static final int VISOR_WEIGHT = 30;
    public static final int BELT_WEIGHT = 30;
    public static final int AEGIS_WEIGHT = 25;
    public static final int MAUL_WEIGHT = 15;
    public static final double ENCHANTED_CHANCE = 0.3;
    public static final double TREASURE_CHANCE = 0.1;

    /** The relic a Warden drops: one of the three wearable relics, or null for the Gale Maul. */
    public static Treasure relic(Random random) {
        int roll = random.nextInt(VISOR_WEIGHT + BELT_WEIGHT + AEGIS_WEIGHT + MAUL_WEIGHT);
        if (roll < VISOR_WEIGHT) return Treasure.WARDENS_VISOR;
        roll -= VISOR_WEIGHT;
        if (roll < BELT_WEIGHT) return Treasure.GALEHEART_BELT;
        roll -= BELT_WEIGHT;
        if (roll < AEGIS_WEIGHT) return Treasure.AEGIS_SIGIL;
        return null;
    }

    /** Counts of the guaranteed drops: {lumenite, skystone, gold, diamonds, gale cores}. */
    public static int[] guaranteed(Random random) {
        return new int[] { 12 + random.nextInt(9), 8 + random.nextInt(9), 3 + random.nextInt(4), 1 + random.nextInt(3),
            1 + random.nextInt(2) };
    }
}
