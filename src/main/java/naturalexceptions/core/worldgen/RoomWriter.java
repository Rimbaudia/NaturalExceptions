package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.List;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Loot;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;

/**
 * Writes one authored room into a dungeon: its walls, then the template's blocks turned to fit, with what depends on
 * the room's role (chests, spawners) decided here. Pits under the floor are dug and given a ladder out.
 */
final class RoomWriter {

    static final int PIT_DEPTH = 3;

    private final SkyDungeon d;

    RoomWriter(SkyDungeon dungeon) {
        this.d = dungeon;
    }

    /**
     * Writes {@code template} turned {@code rotation} quarter turns, its inside starting at (minX, minZ) with the
     * floor at {@code floor}. The vault's shell is warded all round, floor and ceiling included.
     */
    void write(RoomTemplate template, int rotation, int minX, int minZ, int floor, RoomTemplate.Role role) {
        write(template, rotation, minX, minZ, floor, role, true);
    }

    /**
     * As {@link #write}, but with {@code walls} false only the template's own blocks are written: for a room set
     * inside a larger building (a floor of a keep) whose walls are already there.
     */
    void write(RoomTemplate template, int rotation, int minX, int minZ, int floor, RoomTemplate.Role role,
        boolean walls) {
        int w = template.width(rotation), dp = template.depth(rotation), h = template.height;
        boolean warded = role == RoomTemplate.Role.VAULT;
        if (walls) d.shell(minX - 1, floor, minZ - 1, minX + w, floor + h + 1, minZ + dp, warded);
        if (warded) {
            for (int x = minX - 1; x <= minX + w; x++) for (int z = minZ - 1; z <= minZ + dp; z++) {
                d.put(x, floor - 1, z, Block.WARDED);
                d.put(x, floor + h + 2, z, Block.WARDED);
            }
        }
        List<int[]> pits = new ArrayList<>();
        for (int tz = 0; tz < dp; tz++) for (int tx = 0; tx < w; tx++) {
            for (int y = 0; y <= h + 1; y++) {
                char c = template.at(tx, y, tz, rotation);
                int x = minX + tx, z = minZ + tz, wy = floor + y;
                boolean shellLayer = y == 0 || y == h + 1;
                place(c, x, wy, z, tx, tz, w, dp, shellLayer, warded, template, role);
                if (y == 0 && (c == ',' || c == '%')) pits.add(new int[] { x, z });
            }
        }
        if (!pits.isEmpty()) dig(pits, floor, template.pit, minX, minZ, w, dp);
    }

    private void place(char c, int x, int y, int z, int tx, int tz, int w, int dp, boolean shellLayer, boolean warded,
        RoomTemplate template, RoomTemplate.Role role) {
        switch (c) {
            case '.':
                d.put(x, y, z, Block.AIR);
                break;
            case '#':
                if (warded && shellLayer) d.put(x, y, z, Block.WARDED);
                else d.stone(x, y, z);
                break;
            case 'L':
                d.put(x, y, z, Block.LAMP);
                break;
            case 'B':
                d.put(x, y, z, Block.BOOKSHELF);
                break;
            case '~':
                d.put(x, y, z, Block.WATER);
                if (shellLayer) d.stone(x, y - 1, z);
                break;
            case '=':
                d.put(x, y, z, Block.LAVA);
                if (shellLayer) d.stone(x, y - 1, z);
                break;
            case 'x':
                d.put(x, y, z, Block.COBWEB);
                break;
            case 'R':
                d.put(x, y, z, Block.RUBBLE, (int) (d.noise.unit3(x, y, z, 21) * 3));
                break;
            case 'I':
                d.put(x, y, z, Block.IRON_BARS);
                break;
            case 'G':
                d.put(x, y, z, Block.GRASS);
                break;
            case 't':
                d.put(x, y, z, Block.TALL_GRASS);
                break;
            case 'f':
                d.put(x, y, z, Block.FLOWER, d.noise.unit3(x, y, z, 22) < 0.5 ? 0 : 1);
                break;
            case 'w':
                d.put(x, y, z, Block.WINDBLOOM);
                break;
            case 'c':
                chest(x, y, z, role, true);
                break;
            case 'C':
                chest(x, y, z, role, false);
                break;
            case 's':
                if (role == RoomTemplate.Role.GUARD) d.put(x, y, z, Block.SPAWNER, 0, null, mob(template, x, z));
                else d.put(x, y, z, Block.AIR);
                break;
            case 'S':
                d.put(x, y, z, Block.SPAWNER, 0, null, mob(template, x, z));
                break;
            case '^':
                d.put(x, y, z, Block.TRIGGER);
                break;
            case 'v':
                d.put(x, y, z, Block.DISPENSER, awayFromWall(tx, tz, w, dp));
                break;
            case 'F':
                d.put(x, y, z, Block.DISPENSER, awayFromWall(tx, tz, w, dp) | SkyDungeon.FIRE);
                break;
            case '%':
                d.put(x, y, z, Block.CRUMBLING);
                break;
            case ',':
                d.put(x, y, z, Block.AIR);
                break;
            case 'g':
                d.put(x, y, z, Block.VENT, ventMeta(1));
                break;
            case 'h':
                d.put(x, y, z, Block.VENT, ventMeta(awayFromWall(tx, tz, w, dp)));
                break;
            case 'a':
                d.put(x, y, z, Block.ALCOVE, alcoveMeta(mob(template, x + y, z)));
                break;
            case 'T':
                d.stone(x, y, z);
                d.put(x, y - 1, z, Block.TNT);
                break;
            case 'W':
                d.put(x, y, z, Block.SEAL);
                break;
            case '?':
                if (d.noise.unit3(x, y, z, 23) < 0.5) d.stone(x, y, z);
                else d.put(x, y, z, Block.AIR);
                break;
            case 'H':
                d.put(x, y, z, Block.LADDER, awayFromWall(tx, tz, w, dp));
                break;
            case 'K':
                d.put(x, y, z, Block.CAULDRON);
                break;
            case 'O':
                d.put(x, y, z, Block.FURNACE, awayFromWall(tx, tz, w, dp));
                break;
            case 'N':
                d.put(x, y, z, Block.CRAFTING_TABLE);
                break;
            case 'A':
                d.put(x, y, z, Block.ANVIL, (int) (d.noise.unit3(x, y, z, 24) * 4));
                break;
            case '|':
                d.put(x, y, z, Block.GLASS_PANE);
                break;
            case 'o':
                d.put(x, y, z, Block.FENCE);
                break;
            case 'M':
                d.put(x, y, z, Block.RUBBLE, 2);
                break;
            case 'Q':
                d.put(x, y, z, Block.QUARTZ);
                break;
            case 'Y':
                d.put(x, y, z, Block.LOG);
                break;
            case '*':
                d.put(x, y, z, Block.LEAVES);
                break;
            default:
                d.stone(x, y, z);
                break;
        }
    }

    /** 'c' is the room's chest (whose loot the role decides); 'C' the second one, if the role has one. */
    private void chest(int x, int y, int z, RoomTemplate.Role role, boolean primary) {
        Loot loot = null;
        switch (role) {
            case STORE:
            case SECRET:
                loot = primary || d.noise.unit3(x, y, z, 25) < 0.5 ? Loot.STORE : null;
                break;
            case KEY:
                loot = primary ? Loot.KEY : null;
                break;
            case VAULT:
                loot = primary ? Loot.VAULT_TREASURE : Loot.VAULT_RICH;
                break;
            default:
                break;
        }
        if (loot == null) d.put(x, y, z, Block.AIR);
        else d.put(x, y, z, Block.CHEST, 0, loot, null);
    }

    private String mob(RoomTemplate template, int x, int z) {
        List<String> mobs = template.mobs;
        double roll = d.noise.unit(x, z, 7);
        if (mobs.isEmpty()) return SkyDungeon.DEFAULT_MOBS[(int) (roll * SkyDungeon.DEFAULT_MOBS.length)];
        return mobs.get((int) (roll * mobs.size()));
    }

    /** Sealed Alcove meta for a creature name ("Zombie+" is armoured). */
    static int alcoveMeta(String mob) {
        boolean armed = mob.endsWith("+");
        String name = armed ? mob.substring(0, mob.length() - 1) : mob;
        int kind = name.equals("Skeleton") ? 1 : name.equals("Spider") ? 2 : name.equals("CaveSpider") ? 3 : 0;
        return kind | (armed ? 4 : 0);
    }

    /** Vanilla facing (2 north .. 5 east) pointing away from the nearest wall of a w x d inside. */
    static int awayFromWall(int tx, int tz, int w, int dp) {
        int west = tx, east = w - 1 - tx, north = tz, south = dp - 1 - tz;
        int least = Math.min(Math.min(west, east), Math.min(north, south));
        if (least == north) return 3;
        if (least == south) return 2;
        if (least == west) return 5;
        return 4;
    }

    /** Gale Vent meta for a vent blowing towards a vanilla side. */
    static int ventMeta(int side) {
        return side ^ 1;
    }

    /** Digs the pits under ',' and '%': three deep, walled, filled per the room, with a ladder out unless lava. */
    private void dig(List<int[]> pits, int floor, String fill, int minX, int minZ, int w, int dp) {
        java.util.Set<Long> open = new java.util.HashSet<>();
        for (int[] p : pits) open.add(col(p[0], p[1]));
        for (int[] p : pits) {
            for (int y = floor - PIT_DEPTH; y <= floor - 1; y++) d.put(p[0], y, p[1], Block.AIR);
            d.stone(p[0], floor - PIT_DEPTH - 1, p[1]);
            if (fill.equals("lava")) d.put(p[0], floor - PIT_DEPTH, p[1], Block.LAVA);
            else if (fill.equals("water")) d.put(p[0], floor - PIT_DEPTH, p[1], Block.WATER);
            int[][] around = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] a : around) {
                if (open.contains(col(p[0] + a[0], p[1] + a[1]))) continue;
                for (int y = floor - PIT_DEPTH - 1; y <= floor - 1; y++) d.stone(p[0] + a[0], y, p[1] + a[1]);
            }
        }
        if (fill.equals("lava")) return;
        // Ladders: against the room's own floor, one in every few pit squares along an edge, and never none.
        int placed = 0;
        int[] fallback = null;
        for (int[] p : pits) {
            int[][] around = { { 0, -1, 3 }, { 0, 1, 2 }, { -1, 0, 5 }, { 1, 0, 4 } };
            for (int[] a : around) {
                int nx = p[0] + a[0], nz = p[1] + a[1];
                boolean inside = nx >= minX && nx < minX + w && nz >= minZ && nz < minZ + dp;
                if (!inside || open.contains(col(nx, nz))) continue;
                if (fallback == null) fallback = new int[] { p[0], p[1], a[2] };
                if (d.noise.unit(p[0], p[1], 26) < 0.2) {
                    ladder(p[0], p[1], floor, a[2]);
                    placed++;
                }
                break;
            }
        }
        if (placed == 0 && fallback != null) ladder(fallback[0], fallback[1], floor, fallback[2]);
    }

    private void ladder(int x, int z, int floor, int meta) {
        for (int y = floor - PIT_DEPTH; y <= floor; y++) d.put(x, y, z, Block.LADDER, meta);
    }

    private static long col(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
