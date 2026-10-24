package naturalexceptions.minecraft.entity;

/**
 * The Gathered's numbers. Blows mostly pass through the dead; with every candle in its hall alight it kneels, and is
 * as flesh for a few seconds, before it rises and puts them all out again.
 */
public final class GatheredRules {

    private GatheredRules() {}

    public static final float MAX_HEALTH = 70.0F;
    public static final int XP = 80;
    public static final double SPEED = 0.05;
    /** It hovers this high over the floor, drifting. */
    public static final double HOVER = 0.6;
    /** How far round its hall it finds the candles. */
    public static final int CANDLE_REACH = 14;

    /** What gets through to it standing, and kneeling before the lit candles. */
    public static final float SHROUDED_SHARE = 0.15F;
    /** How long it kneels once every candle is lit. */
    public static final int KNEEL_TICKS = 100;

    /** Its grasp: reach, damage, windup and the wait between. */
    public static final double GRASP_REACH = 4.5;
    public static final float GRASP_DAMAGE = 6.0F;
    public static final int GRASP_WINDUP = 20, GRASP_COOLDOWN = 45;
    /** Its wail: reach, damage, windup, and how often (faster once it has torn). */
    public static final double WAIL_REACH = 10.0;
    public static final float WAIL_DAMAGE = 4.0F;
    public static final int WAIL_WINDUP = 30, WAIL_INTERVAL = 240, WAIL_INTERVAL_TORN = 160;
    /** At half health its faces tear loose: shades at once, more now and then, never too many. */
    public static final int SHADES_AT_TEAR = 3, SHADES_LATER = 2, SHADE_INTERVAL = 300, MAX_SHADES = 5;

    public static float through(boolean kneeling) {
        return kneeling ? 1.0F : SHROUDED_SHARE;
    }

    public static boolean torn(float health) {
        return health <= MAX_HEALTH / 2.0F;
    }

    /** How many lit candles a wail puts out. */
    public static int snuffs(boolean torn) {
        return torn ? 2 : 1;
    }

    /** Whether it kneels: there are candles, and every one is lit. */
    public static boolean kneels(int candles, int lit) {
        return candles > 0 && lit >= candles;
    }
}
