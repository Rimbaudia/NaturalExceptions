package naturalexceptions.minecraft.gear;

/** The Warden Relics' numbers, apart from the items so they can be checked. */
public final class RelicRules {

    /** Galeheart Belt: a dash of about six blocks along the gaze. */
    public static final double DASH_SPEED = 1.35;
    /** The dash keeps a little lift so it carries across a gap rather than skidding into it. */
    public static final double DASH_MIN_LIFT = 0.18;
    public static final double DASH_MAX_LIFT = 0.45;
    public static final long DASH_COOLDOWN_TICKS = 60L;

    /** Aegis Sigil: one gale per this many ticks, over this radius. */
    public static final long AEGIS_COOLDOWN_TICKS = 400L;
    public static final double AEGIS_RADIUS = 4.5;
    public static final double AEGIS_PUSH = 1.3;
    /** How long a thrown foe is kept from attacking again. */
    public static final int AEGIS_STAGGER_TICKS = 40;

    /** Gale Maul: a full charge takes this long; less than the minimum does not slam. */
    public static final int MAUL_FULL_CHARGE = 30;
    public static final int MAUL_MIN_CHARGE = 8;
    public static final double MAUL_RADIUS = 4.0;
    public static final float MAUL_MAX_DAMAGE = 10.0F;
    public static final int MAUL_DURABILITY = 900;

    private RelicRules() {}

    /** The dash velocity {x, y, z} for a look direction. */
    public static double[] dash(double lookX, double lookY, double lookZ) {
        double flat = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (flat < 1.0E-4) return new double[] { 0.0, DASH_MAX_LIFT, 0.0 }; // straight up or down: a hop
        double lift = Math.max(DASH_MIN_LIFT, Math.min(DASH_MAX_LIFT, lookY * DASH_SPEED));
        return new double[] { lookX / flat * DASH_SPEED, lift, lookZ / flat * DASH_SPEED };
    }

    public static boolean ready(long lastUse, long now, long cooldown) {
        return lastUse == Long.MIN_VALUE || now - lastUse >= cooldown;
    }

    /** Share of a full slam for a charge held this long: nothing below the minimum, then rising to one. */
    public static double maulCharge(int ticksHeld) {
        if (ticksHeld < MAUL_MIN_CHARGE) return 0.0;
        double t = Math.min(1.0, (double) ticksHeld / MAUL_FULL_CHARGE);
        return t * t * (3.0 - 2.0 * t); // eases in and out, so a near-full charge is nearly full
    }
}
