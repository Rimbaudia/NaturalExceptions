package naturalexceptions.core.worldgen;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Style;
import naturalexceptions.core.worldgen.SkyDungeon.Trap;

/**
 * Writes the corridors between rooms: a style for the look (arched, cramped, a bridge between lava trenches, flooded,
 * collapsed) and, now and then, a trap. Corridors run along x or z; "t" counts along them, "a" across (-1..1 inside,
 * the walls at -2 and 2).
 */
final class CorridorWriter {

    private final SkyDungeon d;

    CorridorWriter(SkyDungeon dungeon) {
        this.d = dungeon;
    }

    private boolean alongX;
    private int across;

    /** World position of (t, a, y). */
    private void put(int t, int a, int y, Block block, int meta) {
        if (alongX) d.put(t, y, across + a, block, meta);
        else d.put(across + a, y, t, block, meta);
    }

    private void put(int t, int a, int y, Block block) {
        put(t, a, y, block, 0);
    }

    private void stone(int t, int a, int y) {
        if (alongX) d.stone(t, y, across + a);
        else d.stone(across + a, y, t);
    }

    /**
     * A corridor from t0 to t1 (inside the rooms' walls, which are at t0 - 1 and t1 + 1), centred on {@code centre}
     * across, with its floor at {@code floor}.
     */
    void write(boolean alongX, int centre, int t0, int t1, int floor, Style style, Trap trap) {
        this.alongX = alongX;
        this.across = centre;
        if (trap != Trap.NONE) style = Style.PLAIN;
        int high = style == Style.ARCHED ? 4 : 3;
        for (int t = t0; t <= t1; t++) {
            for (int a = -2; a <= 2; a++) for (int y = floor; y <= floor + high + 1; y++) {
                boolean edge = Math.abs(a) == 2 || y == floor || y == floor + high + 1;
                if (edge) stone(t, a, y);
                else put(t, a, y, Block.AIR);
            }
        }
        int mid = (t0 + t1) / 2;
        boolean lit = true;
        switch (style) {
            case ARCHED:
                for (int t = t0; t <= t1; t++) if ((t - t0) % 3 == 1) {
                    put(t, -1, floor + high, Block.QUARTZ);
                    put(t, 1, floor + high, Block.QUARTZ);
                }
                break;
            case CRAMPED:
            case SECRET:
                for (int t = t0 + 1; t <= t1 - 1; t++) {
                    for (int y = floor + 1; y <= floor + 3; y++) {
                        stone(t, -1, y);
                        stone(t, 1, y);
                    }
                    stone(t, 0, floor + 3);
                    if (style == Style.CRAMPED && d.noise.unit(t, across, 31) < 0.2) put(t, 0, floor + 2, Block.COBWEB);
                }
                lit = false;
                break;
            case BRIDGE:
                for (int t = t0 + 2; t <= t1 - 2; t++) for (int a : new int[] { -1, 1 }) pit(t, a, floor, true);
                for (int a : new int[] { -1, 1 }) for (int y = floor - RoomWriter.PIT_DEPTH - 1; y < floor; y++) {
                    stone(t0 + 1, a, y);
                    stone(t1 - 1, a, y);
                }
                break;
            case FLOODED:
                for (int t = t0 + 1; t <= t1 - 1; t++) for (int a = -1; a <= 1; a++) {
                    put(t, a, floor, Block.WATER);
                    stone(t, a, floor - 1);
                }
                break;
            case COLLAPSED:
                for (int t = t0 + 1; t <= t1 - 1; t++) for (int a : new int[] { -1, 1 }) {
                    double roll = d.noise.unit(t * 3 + a, across, 32);
                    if (roll < 0.45) put(t, a, floor + 1, Block.RUBBLE, (int) (roll * 6.6) % 3);
                    if (roll < 0.15) put(t, a, floor + 2, Block.RUBBLE, 0);
                    if (roll > 0.85) put(t, a, floor + 3, Block.COBWEB);
                }
                lit = d.noise.unit(mid, across, 33) < 0.5;
                break;
            default:
                break;
        }
        if (lit) put(mid, 0, floor + high + 1, Block.LAMP);
        if (trap != Trap.NONE) trap(trap, mid, floor);
    }

    /** Open floor over a trench three deep at (t, a); lava at the bottom if {@code lava}. */
    private void pit(int t, int a, int floor, boolean lava) {
        put(t, a, floor, Block.AIR);
        for (int y = floor - RoomWriter.PIT_DEPTH; y < floor; y++) put(t, a, y, Block.AIR);
        if (lava) put(t, a, floor - RoomWriter.PIT_DEPTH, Block.LAVA);
        stone(t, a, floor - RoomWriter.PIT_DEPTH - 1);
        for (int y = floor - RoomWriter.PIT_DEPTH - 1; y < floor; y++) stone(t, a < 0 ? -2 : 2, y);
    }

    /** Facing (vanilla side) that points across the corridor towards +a, and towards -a. */
    private int facingPlusA() {
        return alongX ? 3 : 5;
    }

    private int facingMinusA() {
        return alongX ? 2 : 4;
    }

    /** Facing towards +t (for a ladder hanging on a wall further along). */
    private int ladderOnPlusT() {
        return alongX ? 4 : 2;
    }

    private int ladderOnMinusT() {
        return alongX ? 5 : 3;
    }

    private void trap(Trap trap, int mid, int floor) {
        switch (trap) {
            case VOLLEY:
            case FIRE_VOLLEY: {
                for (int a = -1; a <= 1; a++) put(mid, a, floor, Block.TRIGGER);
                int fire = trap == Trap.FIRE_VOLLEY ? SkyDungeon.FIRE : 0;
                for (int t = mid - 1; t <= mid + 1; t++) {
                    put(t, -2, floor + 2, Block.DISPENSER, facingPlusA() | fire);
                    put(t, 2, floor + 2, Block.DISPENSER, facingMinusA() | fire);
                }
                break;
            }
            case CRUMBLE: {
                for (int t = mid - 1; t <= mid + 2; t++) for (int a = -1; a <= 1; a++) {
                    for (int y = floor - RoomWriter.PIT_DEPTH; y < floor; y++) put(t, a, y, Block.AIR);
                    stone(t, a, floor - RoomWriter.PIT_DEPTH - 1);
                    put(t, a, floor, Block.CRUMBLING);
                }
                for (int y = floor - RoomWriter.PIT_DEPTH - 1; y < floor; y++) {
                    for (int a = -1; a <= 1; a++) {
                        stone(mid - 2, a, y);
                        stone(mid + 3, a, y);
                    }
                    for (int t = mid - 1; t <= mid + 2; t++) {
                        stone(t, -2, y);
                        stone(t, 2, y);
                    }
                }
                // The way out: a ladder up the far end of the pit, through the floor.
                for (int y = floor - RoomWriter.PIT_DEPTH; y <= floor; y++)
                    put(mid + 2, 0, y, Block.LADDER, ladderOnPlusT());
                break;
            }
            case GALE: {
                for (int t = mid - 1; t <= mid + 1; t++) pit(t, 1, floor, false);
                for (int y = floor - RoomWriter.PIT_DEPTH - 1; y < floor; y++) {
                    stone(mid - 2, 1, y);
                    stone(mid + 2, 1, y);
                    for (int t = mid - 1; t <= mid + 1; t++) stone(t, 0, y);
                }
                for (int y = floor - RoomWriter.PIT_DEPTH; y <= floor; y++)
                    put(mid - 1, 1, y, Block.LADDER, ladderOnMinusT());
                put(mid - 1, -2, floor + 1, Block.VENT, RoomWriter.ventMeta(facingPlusA()));
                put(mid + 1, -2, floor + 1, Block.VENT, RoomWriter.ventMeta(facingPlusA()));
                break;
            }
            case TNT:
                for (int a = -1; a <= 1; a++) put(mid, a, floor, Block.TRIGGER);
                put(mid - 2, 0, floor - 1, Block.TNT);
                put(mid + 2, 0, floor - 1, Block.TNT);
                break;
            case AMBUSH: {
                for (int a = -1; a <= 1; a++) put(mid, a, floor, Block.TRIGGER);
                String[] mobs = { "Zombie", "Skeleton", "Zombie+", "Spider" };
                for (int t : new int[] { mid - 2, mid + 2 }) for (int a : new int[] { -2, 2 }) {
                    String mob = mobs[(int) (d.noise.unit(t, across + a, 34) * mobs.length)];
                    put(t, a, floor + 1, Block.ALCOVE, RoomWriter.alcoveMeta(mob));
                }
                break;
            }
            default:
                break;
        }
    }

    /** Opens a doorway through a room's wall at {@code t} (three wide, three high), or fills it. */
    void doorway(boolean alongX, int centre, int t, int floor, Block fill, int meta, boolean narrow) {
        this.alongX = alongX;
        this.across = centre;
        for (int a = -1; a <= 1; a++) for (int y = floor + 1; y <= floor + 3; y++) {
            if (narrow && (a != 0 || y == floor + 3)) continue;
            put(t, a, y, fill, meta);
        }
    }

    /**
     * A shaft between levels at (x, z): a ladder from the lower floor up through the upper floor, walled where it
     * crosses the rock; a gale lift adds a Gale Vent at the bottom that carries climbers up.
     */
    void shaft(int x, int z, int lowerFloor, int lowerCeiling, int upperFloor, boolean galeLift) {
        for (int y = lowerCeiling; y < upperFloor; y++) for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) d.stone(x + dx, y, z + dz);
        for (int y = lowerFloor + 1; y <= upperFloor; y++) d.put(x, y, z, Block.LADDER, 3);
        if (galeLift) d.put(x, lowerFloor, z, Block.VENT, RoomWriter.ventMeta(1));
    }
}
