package naturalexceptions.core.worldgen.dungeon;

/**
 * Which trap devices a Trigger Stone sets off: every device within a small box around it. There is no wiring, so a
 * trap works however the dungeon was cut into chunks, and the layout only has to keep devices within reach.
 */
public final class TrapLinks {

    /** How far a trigger reaches sideways (x and z) and up or down. */
    public static final int REACH_H = 5;
    public static final int REACH_V = 3;

    private TrapLinks() {}

    public static boolean reaches(int tx, int ty, int tz, int dx, int dy, int dz) {
        return Math.abs(dx - tx) <= REACH_H && Math.abs(dz - tz) <= REACH_H && Math.abs(dy - ty) <= REACH_V;
    }
}
