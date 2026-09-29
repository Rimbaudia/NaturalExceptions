package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Loot;
import naturalexceptions.core.worldgen.SkyDungeon.Room;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;
import naturalexceptions.core.worldgen.dungeon.TemplateLibrary;

/**
 * Lays out a citadel on a grand island's top, composed from parts so that no two are alike:
 * <ul>
 * <li>a square or octagonal curtain wall, 15 to 19 blocks from the middle, with a ruined gap or two;</li>
 * <li>a gatehouse on the south side with a portcullis and an ambush in its passage;</li>
 * <li>four to six towers, round or square, of different heights, some joined by rope bridges at the top;</li>
 * <li>courtyard works drawn from a garden, a forge, a barracks hut, a well and a ruin;</li>
 * <li>a keep of two to four floors, each an authored room;</li>
 * <li>under the keep, a catacomb of up to four levels ({@link SanctumLayout}) with the Sky Warden and the vault at
 * the bottom.</li>
 * </ul>
 */
final class CitadelLayout {

    static final int KEEP_HALF = 7;
    static final int FLOOR_HEIGHT = 6;
    /** The level number of the citadel's own rooms (the catacomb's levels count from 0 downwards). */
    static final int SURFACE = -1;
    static final int CELL = RoomTemplate.CELL;

    private final SkyDungeon d;
    private final Random random;
    private final Map<RoomTemplate, Integer> used = new HashMap<>();

    private int cx, cz, b, half;
    private boolean octagon;

    CitadelLayout(SkyDungeon dungeon) {
        this.d = dungeon;
        this.random = new Random(
            dungeon.field.seed() * 37 + dungeon.island.cellX() * 998_244_353L + dungeon.island.cellZ());
    }

    /** Whether (x, z) lies inside the curtain wall (walls included). */
    private boolean inside(int x, int z) {
        int dx = Math.abs(x - cx), dz = Math.abs(z - cz);
        if (Math.max(dx, dz) > half) return false;
        return !octagon || dx + dz <= half * 1.6;
    }

    void build() {
        SkyIslands.Island island = d.island;
        cx = island.centreX();
        cz = island.centreZ();
        b = island.centreY() + 1;
        half = 15 + random.nextInt(5);
        octagon = random.nextBoolean();
        int wallHeight = 6 + random.nextInt(3);

        // The platform and the curtain wall, crenellated, broken in a place or two.
        List<int[]> wall = new ArrayList<>();
        for (int x = cx - half; x <= cx + half; x++) for (int z = cz - half; z <= cz + half; z++) {
            if (!inside(x, z)) continue;
            d.stone(x, b, z);
            boolean edge = !inside(x + 1, z) || !inside(x - 1, z) || !inside(x, z + 1) || !inside(x, z - 1);
            if (edge) wall.add(new int[] { x, z });
        }
        int gaps = 1 + random.nextInt(2);
        List<int[]> gapCentres = new ArrayList<>();
        for (int g = 0; g < gaps; g++) {
            int[] w = wall.get(random.nextInt(wall.size()));
            if (Math.abs(w[0] - cx) < 5 && w[1] > cz) continue; // not at the gate
            gapCentres.add(w);
        }
        for (int[] w : wall) {
            boolean ruined = false;
            for (int[] g : gapCentres) ruined |= Math.abs(w[0] - g[0]) + Math.abs(w[1] - g[1]) <= 2;
            int top = ruined ? b + (int) (d.noise.unit(w[0], w[1], 40) * 2) : b + wallHeight;
            for (int y = b + 1; y <= top; y++) d.stone(w[0], y, w[1]);
            if (!ruined && (w[0] + w[1]) % 2 == 0) d.stone(w[0], top + 1, w[1]);
            if (ruined) for (int[] s : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                int rx = w[0] + s[0], rz = w[1] + s[1];
                if (d.noise.unit(rx, rz, 41) < 0.4)
                    d.put(rx, b + 1, rz, Block.RUBBLE, (int) (d.noise.unit(rz, rx, 42) * 3));
            }
        }

        List<int[]> towers = towers();
        keep();
        gatehouse();
        courtyard(towers);
        // Under the keep: the catacomb, levels of it, with the Sky Warden and the vault at the bottom.
        new SanctumLayout(d, b).build();

        d.entrance = new int[] { cx, b + 1, cz + half + 4 };
        int reach = half + 5;
        d.surface = new int[] { cx - reach, cz - reach, cx + reach, cz + reach };
    }

    // --- Towers ---------------------------------------------------------------------------------------------

    /** Four to six towers at the wall's corners (and middles), round or square, with ladders; returns {x, z, top}. */
    private List<int[]> towers() {
        List<int[]> spots = new ArrayList<>();
        if (octagon) {
            int s = (int) Math.round(half * 0.6);
            int[][] vertices = { { -half, -s }, { half, -s }, { -half, s }, { half, s }, { -s, -half }, { s, -half } };
            for (int[] v : vertices) spots.add(v);
        } else {
            int[][] corners = { { -half, -half }, { half, -half }, { -half, half }, { half, half }, { -half, 0 },
                { half, 0 } };
            for (int[] c : corners) spots.add(c);
        }
        int count = 4 + random.nextInt(3);
        List<int[]> towers = new ArrayList<>();
        int guardTower = random.nextInt(count), storeTower = (guardTower + 1 + random.nextInt(count - 1)) % count;
        for (int t = 0; t < count; t++) {
            int tx = cx + spots.get(t)[0], tz = cz + spots.get(t)[1];
            boolean round = random.nextBoolean();
            int top = b + 10 + random.nextInt(7);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                boolean in = round ? dx * dx + dz * dz <= 12 : true;
                boolean rim = in && (round ? dx * dx + dz * dz >= 5 : Math.abs(dx) == 3 || Math.abs(dz) == 3);
                if (!in) continue;
                d.stone(tx + dx, b, tz + dz);
                for (int y = b + 1; y < top; y++) {
                    if (rim) d.stone(tx + dx, y, tz + dz);
                    else d.put(tx + dx, y, tz + dz, Block.AIR);
                }
                d.stone(tx + dx, top, tz + dz);
                if (rim && (dx + dz) % 2 == 0) d.stone(tx + dx, top + 1, tz + dz);
                // A floor halfway up with a window, so the climb has a landing.
                if (!rim) d.stone(tx + dx, b + 6, tz + dz);
            }
            // The door faces the keep; the ladder climbs the far wall to the roof.
            int sx = Integer.signum(cx - tx), sz = Integer.signum(cz - tz);
            int doorX = Math.abs(tx - cx) >= Math.abs(tz - cz) ? tx + sx * 3 : tx;
            int doorZ = Math.abs(tx - cx) >= Math.abs(tz - cz) ? tz : tz + sz * 3;
            for (int y = b + 1; y <= b + 2; y++) d.put(doorX, y, doorZ, Block.AIR);
            int lx = tx - (doorX - tx == 0 ? 0 : Integer.signum(doorX - tx) * 2);
            int lz = tz - (doorZ - tz == 0 ? 0 : Integer.signum(doorZ - tz) * 2);
            int ladderMeta = doorX != tx ? (doorX > tx ? 5 : 4) : (doorZ > tz ? 3 : 2);
            for (int y = b + 1; y <= top; y++) d.put(lx, y, lz, Block.LADDER, ladderMeta);
            d.put(tx + (doorX == tx ? 3 : 0), b + 8, tz + (doorZ == tz ? 3 : 0), Block.GLASS_PANE);
            d.put(tx, top - 1, tz, Block.LAMP);
            if (t == guardTower) d.put(tx, b + 1, tz, Block.SPAWNER, 0, null, "Skeleton");
            if (t == storeTower) d.put(tx, top + 1, tz, Block.CHEST, 0, Loot.STORE, null);
            towers.add(new int[] { tx, tz, top });
        }
        // Rope bridges between some neighbouring towers of the same line, at the lower of their tops.
        for (int i = 0; i < towers.size(); i++) for (int j = i + 1; j < towers.size(); j++) {
            int[] p = towers.get(i), q = towers.get(j);
            boolean aligned = p[0] == q[0] || p[1] == q[1];
            if (!aligned || random.nextDouble() > 0.5) continue;
            int y = Math.min(p[2], q[2]);
            boolean alongX = p[1] == q[1];
            int from = alongX ? Math.min(p[0], q[0]) + 4 : Math.min(p[1], q[1]) + 4;
            int to = alongX ? Math.max(p[0], q[0]) - 4 : Math.max(p[1], q[1]) - 4;
            if (to - from > 32) continue;
            for (int t = from; t <= to; t++) {
                int x = alongX ? t : p[0], z = alongX ? p[1] : t;
                d.put(x, y, z, Block.LOG);
                d.put(alongX ? x : x - 1, y + 1, alongX ? z - 1 : z, Block.FENCE);
                d.put(alongX ? x : x + 1, y + 1, alongX ? z + 1 : z, Block.FENCE);
            }
        }
        return towers;
    }

    // --- The keep -------------------------------------------------------------------------------------------

    private int kx, kz, keepFloors;

    /** The keep: two to four floors, each an authored room; its ground floor opens down into the catacomb. */
    private void keep() {
        kx = cx;
        kz = cz; // over the catacomb's first room, whose corner shaft comes up into the keep's
        keepFloors = 2 + random.nextInt(3);
        int k = KEEP_HALF, roof = b + keepFloors * FLOOR_HEIGHT;
        d.shell(kx - k, b, kz - k, kx + k, roof, kz + k, false);
        for (int f = 1; f < keepFloors; f++) for (int dx = -k; dx <= k; dx++)
            for (int dz = -k; dz <= k; dz++) d.stone(kx + dx, b + f * FLOOR_HEIGHT, kz + dz);
        for (int dx = -k; dx <= k; dx++) for (int dz = -k; dz <= k; dz++) {
            boolean ring = Math.abs(dx) == k || Math.abs(dz) == k;
            if (ring && (dx + dz) % 2 == 0) d.stone(kx + dx, roof + 1, kz + dz);
        }
        RoomWriter writer = new RoomWriter(d);
        for (int f = 0; f < keepFloors; f++) {
            int floor = b + f * FLOOR_HEIGHT;
            d.clear(kx - k + 1, floor + 1, kz - k + 1, kx + k - 1, floor + FLOOR_HEIGHT - 1, kz + k - 1);
            Role role = f == 0 ? Role.GUARD : Role.STORE;
            RoomTemplate t = pick(role, f == 0);
            writer.write(t, random.nextInt(4), kx - 4, kz - 4, floor, role, false);
            for (int[] w : new int[][] { { k, 0 }, { -k, 0 }, { 0, -k }, { k, 3 }, { -k, -3 } })
                d.put(kx + w[0], floor + 3, kz + w[1], Block.GLASS_PANE);
            d.put(kx - k + 1, floor + FLOOR_HEIGHT, kz + k - 1, Block.LAMP);
            Room room = new Room(
                kx,
                floor,
                kz,
                role,
                t.name,
                SURFACE,
                kx - k + 1,
                kx + k - 1,
                kz - k + 1,
                kz + k - 1,
                5,
                t.trapped);
            d.rooms.add(room);
        }
        // The stair: a ladder up the north-west corner through every floor to the roof.
        for (int y = b + 1; y <= roof; y++) d.put(kx - k + 1, y, kz - k + 1, Block.LADDER, 3);
        for (int y = b + 1; y <= b + 3; y++) for (int dx = -1; dx <= 1; dx++) d.put(kx + dx, y, kz + k, Block.AIR);
    }

    // --- The gatehouse --------------------------------------------------------------------------------------

    /** A gatehouse over the south wall: a passage under a portcullis, and alcoves that burst open on intruders. */
    private void gatehouse() {
        int gz = cz + half;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) {
            d.stone(cx + dx, b, gz + dz);
            for (int y = b + 1; y <= b + 7; y++) {
                boolean passage = Math.abs(dx) <= 1 && y <= b + 4;
                if (passage) d.put(cx + dx, y, gz + dz, Block.AIR);
                else d.stone(cx + dx, y, gz + dz);
            }
            if ((dx + dz) % 2 == 0) d.stone(cx + dx, b + 8, gz + dz);
        }
        for (int dx = -1; dx <= 1; dx++) d.put(cx + dx, b + 4, gz - 2, Block.IRON_BARS); // the raised portcullis
        for (int dx = -1; dx <= 1; dx++) d.put(cx + dx, b, gz, Block.TRIGGER);
        String[] mobs = { "Zombie", "Skeleton", "Zombie+" };
        for (int dz : new int[] { -2, 2 }) for (int dx : new int[] { -2, 2 })
            d.put(cx + dx, b + 1, gz + dz, Block.ALCOVE, RoomWriter.alcoveMeta(mobs[random.nextInt(mobs.length)]));
        d.put(cx, b + 5, gz, Block.LAMP);
        Room gate = new Room(cx, b, gz, Role.ENTRANCE, null, SURFACE, cx - 1, cx + 1, gz - 2, gz + 2, 4, true);
        d.rooms.add(gate);
    }

    // --- The courtyard --------------------------------------------------------------------------------------

    private void courtyard(List<int[]> towers) {
        int[][] spots = { { -10, 11 }, { 10, 11 }, { -11, -7 }, { 11, -7 } };
        List<String> works = new ArrayList<>();
        java.util.Collections.addAll(works, "garden", "forge", "barracks", "well", "ruins");
        java.util.Collections.shuffle(works, random);
        int n = 3 + random.nextInt(2);
        for (int i = 0; i < n; i++) {
            int x = cx + spots[i][0], z = cz + spots[i][1];
            if (!inside(x - 3, z - 3) || !inside(x + 3, z + 3) || !inside(x - 3, z + 3) || !inside(x + 3, z - 3))
                continue;
            boolean clear = true;
            for (int[] t : towers) clear &= Math.abs(t[0] - x) > 7 || Math.abs(t[1] - z) > 7;
            if (!clear) continue;
            switch (works.get(i)) {
                case "garden":
                    garden(x, z);
                    break;
                case "forge":
                    forge(x, z);
                    break;
                case "barracks":
                    barracks(x, z);
                    break;
                case "well":
                    well(x, z);
                    break;
                default:
                    ruins(x, z);
                    break;
            }
        }
    }

    private void garden(int x, int z) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean border = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            if (border) {
                d.put(x + dx, b + 1, z + dz, Block.QUARTZ);
                continue;
            }
            d.put(x + dx, b, z + dz, Block.GRASS);
            double r = d.noise.unit(x + dx, z + dz, 43);
            if (r < 0.2) d.put(x + dx, b + 1, z + dz, Block.WINDBLOOM);
            else if (r < 0.35) d.put(x + dx, b + 1, z + dz, Block.FLOWER, r < 0.27 ? 0 : 1);
            else if (r < 0.6) d.put(x + dx, b + 1, z + dz, Block.TALL_GRASS);
        }
        for (int y = b + 1; y <= b + 4; y++) d.put(x, y, z, Block.LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = b + 4; y <= b + 6; y++) {
            if (dx == 0 && dz == 0 && y < b + 5) continue;
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(y - (b + 5)) <= 3) d.put(x + dx, y, z + dz, Block.LEAVES);
        }
    }

    private void forge(int x, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            d.put(x + dx, b, z - 3, Block.LAVA);
            d.stone(x + dx, b - 1, z - 3);
            d.put(x + dx, b + 1, z - 3, Block.IRON_BARS);
        }
        d.put(x - 2, b + 1, z - 1, Block.FURNACE, 3);
        d.put(x + 2, b + 1, z - 1, Block.FURNACE, 3);
        d.put(x, b + 1, z, Block.ANVIL, random.nextInt(4));
        d.put(x - 2, b + 1, z + 2, Block.CAULDRON);
        d.put(x + 2, b + 1, z + 2, Block.CRAFTING_TABLE);
        if (random.nextBoolean()) d.put(x + 3, b + 1, z + 2, Block.CHEST, 0, Loot.STORE, null);
        for (int dx = -3; dx <= 3; dx += 6) for (int y = b + 1; y <= b + 3; y++) d.put(x + dx, y, z - 2, Block.FENCE);
    }

    /** A dark hut with a spawner inside (spawners need the dark). */
    private void barracks(int x, int z) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int y = b + 1; y <= b + 4; y++) {
            boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3 || y == b + 4;
            if (shell) d.stone(x + dx, y, z + dz);
            else d.put(x + dx, y, z + dz, Block.AIR);
        }
        for (int y = b + 1; y <= b + 2; y++) d.put(x, y, z + (z > cz ? -3 : 3), Block.AIR);
        d.put(x, b + 1, z, Block.SPAWNER, 0, null, random.nextBoolean() ? "Zombie+" : "Skeleton+");
        d.put(x - 2, b + 1, z - 2, Block.FENCE);
        d.put(x + 2, b + 1, z - 2, Block.FENCE);
    }

    private void well(int x, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (int y = b - 3; y <= b; y++) d.stone(x + dx, y, z + dz);
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) d.stone(x + dx, b + 1, z + dz);
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) for (int y = b - 2; y <= b; y++) d.put(x + dx, y, z + dz, Block.WATER);
        for (int s : new int[] { -2, 2 }) for (int y = b + 2; y <= b + 3; y++) d.put(x + s, y, z, Block.FENCE);
        for (int dx = -2; dx <= 2; dx++) d.put(x + dx, b + 4, z, Block.LOG);
    }

    private void ruins(int x, int z) {
        for (int[] p : new int[][] { { -2, -2 }, { 2, -2 }, { -2, 2 }, { 2, 2 } }) {
            int height = 1 + (int) (d.noise.unit(x + p[0], z + p[1], 44) * 4);
            for (int y = b + 1; y <= b + height; y++) d.put(x + p[0], y, z + p[1], Block.QUARTZ);
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double r = d.noise.unit(x + dx, z + dz, 45);
            if (r < 0.2) d.put(x + dx, b + 1, z + dz, Block.RUBBLE, (int) (r * 15) % 3);
        }
        if (random.nextDouble() < 0.4) d.put(x, b + 1, z, Block.CHEST, 0, Loot.STORE, null);
    }

    /** An authored one-cell room for a role, favouring ones not used yet; no pits (floors stack here). */
    private RoomTemplate pick(Role role, boolean needCorners) {
        List<RoomTemplate> options = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (RoomTemplate t : TemplateLibrary.all()) {
            if (!t.roles.contains(role) || !t.kinds.contains(RoomTemplate.Kind.CITADEL)) continue;
            if (t.small || t.cellsX != 1 || t.cellsZ != 1 || t.height != 5) continue;
            if (t.trapped || t.has(',') || t.has('%') || t.has('T') || t.has('~') || t.has('=')) continue;
            if (needCorners && !SanctumLayout.cornersOpen(t)) continue;
            options.add(t);
            weights.add(t.weight / (1.0 + 3.0 * used.getOrDefault(t, 0)));
        }
        double total = 0;
        for (double w : weights) total += w;
        double roll = random.nextDouble() * total;
        RoomTemplate pick = options.get(options.size() - 1);
        for (int i = 0; i < options.size(); i++) {
            roll -= weights.get(i);
            if (roll <= 0) {
                pick = options.get(i);
                break;
            }
        }
        used.merge(pick, 1, Integer::sum);
        return pick;
    }
}
