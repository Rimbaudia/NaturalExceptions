package naturalexceptions.minecraft.entity;

/**
 * The Foundry Colossus's numbers: the Citadel's miniboss, a little lighter than the Castellan. Hot, its iron shell
 * turns most of a blow; doused, its heart is open and it takes blows in full, but only for a few seconds.
 */
public final class ColossusRules {

    private ColossusRules() {}

    public static final float MAX_HEALTH = 60.0F;
    public static final int ARMOUR = 8;
    public static final double SPEED = 0.2;
    public static final int XP = 60;
    /** How near someone must come to wake it, and how far it looks for someone once awake. */
    public static final double WAKE_RANGE = 10.0, HUNT_RANGE = 16.0;

    /** How long it stays awake with no one in sight before it goes back to its place and sleeps. */
    public static final int SETTLE_TICKS = 200;

    /** What gets through its shell while it is hot; doused, all of it. */
    public static final float HOT_SHARE = 0.2F;
    /** How long its heart stays open after a dousing. */
    public static final int QUENCH_TICKS = 100;

    /** The hammer: how far it reaches, how hard it falls, how long it is raised first, and between blows. */
    public static final double SLAM_REACH = 3.5;
    public static final float SLAM_DAMAGE = 8.0F;
    /** How hard the hammer throws back whoever it strikes (a shove of a few blocks, not across the town). */
    public static final double SLAM_THROW = 0.25;
    public static final int SLAM_WINDUP = 20, SLAM_COOLDOWN = 50;
    /** The pour: how far the molten line runs, how long the crucible tilts first, and between pours. */
    public static final int POUR_LENGTH = 8, POUR_WINDUP = 30, POUR_COOLDOWN = 200;
    /** The slag it lobs at anyone keeping away: from how far, its windup, and between throws. */
    public static final double THROW_FROM = 6.0;
    public static final int THROW_WINDUP = 25, THROW_COOLDOWN = 70;

    /** The share of a blow that gets through, hot or doused. */
    public static float through(boolean quenched) {
        return quenched ? 1.0F : HOT_SHARE;
    }

    /**
     * The blocks a pour from (x, z) towards (tx, tz) runs over: {@link #POUR_LENGTH} of them along the straight line,
     * one step at a time, starting a block out from its feet.
     */
    public static int[][] pourLine(double x, double z, double tx, double tz) {
        double dx = tx - x, dz = tz - z, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-3) {
            dx = 0.0;
            dz = 1.0;
            d = 1.0;
        }
        int[][] line = new int[POUR_LENGTH][];
        for (int i = 0; i < POUR_LENGTH; i++) {
            double t = 1.5 + i;
            line[i] = new int[] { (int) Math.floor(x + dx / d * t), (int) Math.floor(z + dz / d * t) };
        }
        return line;
    }
}
