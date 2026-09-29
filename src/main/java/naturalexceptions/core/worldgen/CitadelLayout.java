package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Loot;
import naturalexceptions.core.worldgen.SkyDungeon.Room;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;
import naturalexceptions.core.worldgen.dungeon.TemplateLibrary;

/**
 * Lays out a citadel on a huge island: a fortified town round a grand keep, over a catacomb.
 * <ul>
 * <li><b>The approach</b>: a paved causeway lit by lamp posts from the island's south rim, past a guard post, to a
 * drawbridge over a dry moat; a ruined watchtower stands elsewhere on the island.</li>
 * <li><b>The outer wall</b>: a square, three blocks thick, with a crenellated parapet and a wall-walk reached by stair
 * flights; towers at the corners and on three sides (floors, stairs inside, doors onto the walk, arrow slits); a
 * great gatehouse on the south with a raised portcullis, murder holes and an ambush in its passage.</li>
 * <li><b>The town</b>: a ring road and four streets, lamp posts along them, authored buildings on plots facing the
 * road (houses, barracks, smithy, chapel, stables, cistern, library tower, cloister, granary), yards between, and
 * an orchard, a herb garden, a market and a training yard in the corners.</li>
 * <li><b>The inner wall</b>: taller, with corner towers and its gatehouse on the east, so the way to the keep goes
 * through the town.</li>
 * <li><b>The grand keep</b>, a well square before it, and under it the catacomb ({@link SanctumLayout}) where the
 * Sky Key, the Sky Warden and the vault are.</li>
 * </ul>
 */
final class CitadelLayout {

    /** The level number of the citadel's own rooms (the catacomb's levels count from 0 downwards). */
    static final int SURFACE = -1;
    static final int OUTER = 50, OUTER_HEIGHT = 8;
    static final int INNER = 24, INNER_HEIGHT = 10;
    static final int THICK = 3;
    static final int MOAT_IN = 52, MOAT_OUT = 56, MOAT_DEPTH = 4;
    static final int ROAD_IN = 25, ROAD_OUT = 28;
    static final int PLOT_FRONT = 30, PLOT_BACK = 46;

    private final SkyDungeon d;
    private final Random random;
    private int cx, cz, b;
    private double radius;

    CitadelLayout(SkyDungeon dungeon) {
        this.d = dungeon;
        this.random = new Random(
            dungeon.field.seed() * 37 + dungeon.island.cellX() * 998_244_353L + dungeon.island.cellZ());
    }

    void build() {
        SkyIslands.Island island = d.island;
        cx = island.centreX();
        cz = island.centreZ();
        b = island.centreY() + 1;
        radius = island.radius();

        ground();
        moat();
        roads();
        wall(OUTER, OUTER_HEIGHT);
        wall(INNER, INNER_HEIGHT);
        gatehouse(0, OUTER, OUTER_HEIGHT, 7, true);
        gatehouse(1, INNER, INNER_HEIGHT, 6, false);
        outerTowers();
        innerTowers();
        town();
        corners();
        keep();
        square();
        stairsUp(); // last in the town: nothing is paved over the flights
        approach();
        watchtower();

        // Under the keep: the catacomb, levels of it, with the Sky Key, the Sky Warden and the vault.
        new SanctumLayout(d, b).build();

        d.entrance = new int[] { cx, b + 1, cz + MOAT_OUT + 4 };
        int reach = (int) Math.min(radius - 4, 84);
        d.surface = new int[] { cx - reach, cz - reach, cx + reach, cz + reach };
    }

    // --- Helpers --------------------------------------------------------------------------------------------

    private void put(int dx, int y, int dz, Block block, int meta) {
        d.put(cx + dx, y, cz + dz, block, meta);
    }

    private void put(int dx, int y, int dz, Block block) {
        d.put(cx + dx, y, cz + dz, block);
    }

    private void stone(int dx, int y, int dz) {
        d.stone(cx + dx, y, cz + dz);
    }

    private static boolean inSquare(int dx, int dz, int half) {
        return Math.abs(dx) <= half && Math.abs(dz) <= half;
    }

    /** A wall's ring: inside the square of {@code half}, outside the one {@link #THICK} smaller. */
    private static boolean inRing(int dx, int dz, int half) {
        return inSquare(dx, dz, half) && !inSquare(dx, dz, half - THICK);
    }

    /** Vanilla stairs meta rising towards (sx, sz). */
    private static int rising(int sx, int sz) {
        return sx > 0 ? 0 : sx < 0 ? 1 : sz > 0 ? 2 : 3;
    }

    private void lampPost(int dx, int dz) {
        put(dx, b + 1, dz, Block.FENCE);
        put(dx, b + 2, dz, Block.FENCE);
        put(dx, b + 3, dz, Block.LAMP);
    }

    // --- Ground, moat and walls -----------------------------------------------------------------------------

    /** The ground inside the moat is made level at the citadel's floor height (the island's rim sits a step lower). */
    private void ground() {
        for (int dx = -MOAT_OUT - 2; dx <= MOAT_OUT + 2; dx++) for (int dz = -MOAT_OUT - 2; dz <= MOAT_OUT + 2; dz++) {
            int[] column = d.field.column(d.island, cx + dx, cz + dz);
            if (column != null && column[1] < b) {
                put(dx, b, dz, Block.GRASS);
                stone(dx, b - 1, dz);
            }
        }
    }

    /** A dry moat, five wide and four deep, lined with stone, all round the outer wall. */
    private void moat() {
        for (int dx = -MOAT_OUT - 1; dx <= MOAT_OUT + 1; dx++) for (int dz = -MOAT_OUT - 1; dz <= MOAT_OUT + 1; dz++) {
            boolean ditch = inSquare(dx, dz, MOAT_OUT) && !inSquare(dx, dz, MOAT_IN - 1);
            boolean lining = inSquare(dx, dz, MOAT_OUT + 1) && !ditch && !inSquare(dx, dz, MOAT_IN - 2);
            if (ditch) {
                for (int y = b - MOAT_DEPTH + 1; y <= b; y++) put(dx, y, dz, Block.AIR);
                put(dx, b - MOAT_DEPTH, dz, Block.GRASS);
            } else if (lining) for (int y = b - MOAT_DEPTH; y <= b; y++) stone(dx, y, dz);
        }
    }

    /** A curtain wall, three thick, with a parapet and merlons on its outer edge; the wall-walk is its top. */
    private void wall(int half, int height) {
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            if (!inRing(dx, dz, half)) continue;
            stone(dx, b - 1, dz);
            for (int y = b; y <= b + height; y++) stone(dx, y, dz);
            boolean outerEdge = !inSquare(dx, dz, half - 1);
            if (outerEdge) {
                stone(dx, b + height + 1, dz);
                if ((dx + dz) % 2 == 0) stone(dx, b + height + 2, dz);
            }
        }
    }

    /** Flights of stairs up the inside of both walls onto their walks, on every side. */
    private void stairsUp() {
        flightsFor(OUTER, OUTER_HEIGHT);
        flightsFor(INNER, INNER_HEIGHT);
    }

    /** One flight up each side, clear of the gates, streets and towers (which are at the middles and corners). */
    private void flightsFor(int half, int height) {
        int face = half - THICK; // the first row of the bailey, against the wall
        int from = 9;
        flight(from, -face, 1, 0, height); // north wall, rising east
        flight(-face, from, 0, 1, height); // west wall, rising south
        flight(face, -from, 0, -1, height); // east wall, rising north
        flight(-from, face, -1, 0, height); // south wall, rising west
    }

    /** {@code height} steps from (dx, dz) rising along (sx, sz), each on a solid footing, headroom kept above. */
    private void flight(int dx, int dz, int sx, int sz, int height) {
        for (int i = 0; i < height; i++) {
            int x = dx + sx * i, z = dz + sz * i;
            for (int y = b + 1; y < b + 1 + i; y++) stone(x, y, z);
            put(x, b + 1 + i, z, Block.STAIRS, rising(sx, sz));
            for (int y = b + 2 + i; y <= b + 4 + i; y++) put(x, y, z, Block.AIR);
        }
    }

    // --- Towers ---------------------------------------------------------------------------------------------

    private void outerTowers() {
        int c = OUTER - 1;
        int[][] spots = { { -c, -c }, { c, -c }, { -c, c }, { c, c }, { 0, -c }, { -c, 0 }, { c, 0 } };
        boolean round = random.nextBoolean();
        for (int[] s : spots) tower(s[0], s[1], 4, OUTER_HEIGHT, OUTER_HEIGHT + 7 + random.nextInt(3), round);
    }

    private void innerTowers() {
        int c = INNER - 1;
        int[][] spots = { { -c, -c }, { c, -c }, { -c, c }, { c, c } };
        for (int[] s : spots) tower(s[0], s[1], 4, INNER_HEIGHT, INNER_HEIGHT + 8, false);
    }

    /**
     * A tower on a wall: floors at the ground, at the wall-walk and a roof; stairs inside from each to the next;
     * a door from the bailey and doors onto the walk where the wall meets it; arrow slits in its outer faces.
     */
    private void tower(int tx, int tz, int half, int walk, int top, boolean round) {
        int outX = Integer.signum(tx), outZ = Integer.signum(tz);
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int rr = dx * dx + dz * dz;
            if (round && rr > half * half + 2) continue;
            boolean rim = round ? rr > (half - 1) * (half - 1) + 1 : Math.abs(dx) == half || Math.abs(dz) == half;
            int x = tx + dx, z = tz + dz;
            stone(x, b - 1, z);
            stone(x, b, z);
            for (int y = b + 1; y < b + top; y++) {
                if (rim) stone(x, y, z);
                else put(x, y, z, Block.AIR);
            }
            if (!rim) put(x, b + walk, z, Block.PLANKS, 1);
            stone(x, b + top, z);
            if (rim) {
                stone(x, b + top + 1, z);
                if ((dx + dz) % 2 == 0) stone(x, b + top + 2, z);
            }
        }
        // Stairs: ground to the walk floor up one inner side, walk to roof up the other.
        int side = tx > 0 ? -1 : 1;
        int sx = tx + side * (half - 2);
        for (int i = 0; i < walk && i < 2 * half - 3; i++) {
            int z = tz - (half - 2) + i;
            for (int y = b + 1; y < b + 1 + i; y++) stone(sx, y, z);
            put(sx, b + 1 + i, z, Block.STAIRS, rising(0, 1));
            for (int y = b + 2 + i; y <= b + walk; y++) put(sx, y, z, Block.AIR);
        }
        int rx = tx - side * (half - 2);
        for (int i = 0; i < top - walk - 1 && i < 2 * half - 3; i++) {
            int z = tz + (half - 2) - i;
            for (int y = b + walk + 1; y < b + walk + 1 + i; y++) stone(rx, y, z);
            put(rx, b + walk + 1 + i, z, Block.STAIRS, rising(0, -1));
            for (int y = b + walk + 2 + i; y <= b + top; y++) put(rx, y, z, Block.AIR);
        }
        put(tx, b + walk - 1, tz, Block.LAMP);
        put(tx, b + top - 1, tz, Block.LAMP);
        // A door from the bailey, on the face towards the middle.
        int inX = -outX, inZ = -outZ;
        if (inX != 0 && inZ != 0) inZ = 0;
        int doorX = tx + inX * half, doorZ = tz + inZ * half;
        for (int y = b + 1; y <= b + 2; y++) put(doorX, y, doorZ, Block.AIR);
        // Doors onto the walk wherever the wall runs into the tower; arrow slits in the faces looking out.
        int[][] faces = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] f : faces) {
            int fx = tx + f[0] * half, fz = tz + f[1] * half;
            int beyondX = fx + f[0], beyondZ = fz + f[1];
            boolean wallThere = inRing(beyondX, beyondZ, OUTER) && inRing(tx, tz, OUTER)
                || inRing(beyondX, beyondZ, INNER) && inRing(tx, tz, INNER);
            boolean outward = f[0] != 0 && f[0] == outX || f[1] != 0 && f[1] == outZ;
            if (wallThere) {
                for (int y = b + walk + 1; y <= b + walk + 2; y++) put(fx, y, fz, Block.AIR);
            } else if (outward) {
                for (int y = b + 3; y <= b + 4; y++) put(fx, y, fz, Block.IRON_BARS);
                for (int y = b + walk + 2; y <= b + walk + 3; y++) put(fx, y, fz, Block.IRON_BARS);
            }
        }
    }

    // --- Gatehouses -----------------------------------------------------------------------------------------

    /** Maps a gatehouse's own (u across, v outward) to the citadel's (dx, dz): side 0 south, 1 east. */
    private static int gx(int side, int u, int v) {
        return side == 0 ? u : v;
    }

    private static int gz(int side, int u, int v) {
        return side == 0 ? v : u;
    }

    private void gput(int side, int u, int y, int v, Block block, int meta) {
        put(gx(side, u, v), y, gz(side, u, v), block, meta);
    }

    private void gstone(int side, int u, int y, int v) {
        stone(gx(side, u, v), y, gz(side, u, v));
    }

    /**
     * A gatehouse through the wall of {@code half}: a passage five wide and six high, a guard room over it level
     * with the wall-walk (murder holes in its floor), thick flanks; the outer one's portcullis hangs raised over its
     * mouth, its passage hides an ambush, and a drawbridge crosses the moat before it.
     */
    private void gatehouse(int side, int half, int walk, int flank, boolean outer) {
        int front = half + 1, back = half - THICK - 2;
        for (int u = -flank; u <= flank; u++) for (int v = back; v <= front; v++) {
            gstone(side, u, b - 1, v);
            gstone(side, u, b, v);
            boolean rim = Math.abs(u) == flank || v == back || v == front;
            for (int y = b + 1; y <= b + walk + 4; y++) {
                boolean passage = Math.abs(u) <= 2 && y <= b + 6;
                boolean room = !rim && y >= b + walk + 1 && y <= b + walk + 3;
                if (passage || room) gput(side, u, y, v, Block.AIR, 0);
                else gstone(side, u, y, v);
            }
            if (rim && (u + v) % 2 == 0) gstone(side, u, b + walk + 5, v);
        }
        for (int u = -2; u <= 2; u++) for (int v = back; v <= front; v++) gput(side, u, b, v, Block.SKYSTONE, 0);
        // Murder holes over the passage.
        for (int v = back + 2; v <= front - 2; v += 2)
            for (int y = b + 7; y <= b + walk; y++) gput(side, 0, y, v, Block.AIR, 0);
        // The raised portcullis hanging in the outer mouth.
        for (int u = -2; u <= 2; u++) gput(side, u, b + 6, front, Block.IRON_BARS, 0);
        // Doors from the wall-walk into the guard room on either side.
        for (int s : new int[] { -1, 1 })
            for (int y = b + walk + 1; y <= b + walk + 2; y++) gput(side, s * flank, y, half - 1, Block.AIR, 0);
        gput(side, 2, b + walk + 3, (back + front) / 2, Block.LAMP, 0);
        for (int v = back + 1; v <= front - 1; v += 3) {
            gput(side, 3, b + 4, v, Block.LAMP, 0);
            gput(side, -3, b + 4, v, Block.LAMP, 0);
        }
        int mid = (back + front) / 2;
        if (!outer) return;
        // The ambush: a trigger across the passage, alcoves in its walls.
        for (int u = -2; u <= 2; u++) gput(side, u, b, mid, Block.TRIGGER, 0);
        String[] mobs = { "Zombie", "Skeleton", "Zombie+" };
        for (int v : new int[] { mid - 2, mid + 2 }) for (int u : new int[] { -3, 3 })
            gput(side, u, b + 1, v, Block.ALCOVE, RoomWriter.alcoveMeta(mobs[random.nextInt(mobs.length)]));
        d.rooms.add(
            new Room(
                cx + gx(side, 0, mid),
                b,
                cz + gz(side, 0, mid),
                Role.ENTRANCE,
                null,
                SURFACE,
                cx - 2,
                cx + 2,
                cz + back,
                cz + front,
                6,
                true));
        // The drawbridge over the moat, railed.
        for (int v = front + 1; v <= MOAT_OUT + 1; v++) {
            for (int u = -2; u <= 2; u++) gput(side, u, b, v, Block.PLANKS, 1);
            gput(side, -3, b + 1, v, Block.FENCE, 0);
            gput(side, 3, b + 1, v, Block.FENCE, 0);
        }
    }

    // --- The town -------------------------------------------------------------------------------------------

    private void pave(int dx, int dz) {
        double r = d.noise.unit(cx + dx, cz + dz, 60);
        if (r < 0.12) put(dx, b, dz, Block.RUBBLE, 1);
        else put(dx, b, dz, Block.SKYSTONE, r < 0.3 ? 1 : 0);
        for (int y = b + 1; y <= b + 3; y++) put(dx, y, dz, Block.AIR);
    }

    /** The ring road round the inner wall and four streets out to the outer wall's towers and gate. */
    private void roads() {
        for (int dx = -ROAD_OUT; dx <= ROAD_OUT; dx++)
            for (int dz = -ROAD_OUT; dz <= ROAD_OUT; dz++) if (!inSquare(dx, dz, ROAD_IN - 1)) pave(dx, dz);
        for (int t = ROAD_OUT + 1; t <= OUTER - THICK; t++) for (int w = -2; w <= 2; w++) {
            pave(w, t);
            pave(w, -t);
            pave(t, w);
            pave(-t, w);
        }
        for (int t = -ROAD_OUT + 3; t <= ROAD_OUT - 3; t += 8) {
            if (Math.abs(t) <= 3) continue;
            lampPost(t, ROAD_OUT + 1);
            lampPost(t, -ROAD_OUT - 1);
            lampPost(ROAD_OUT + 1, t);
            lampPost(-ROAD_OUT - 1, t);
        }
        for (int t = ROAD_OUT + 5; t < OUTER - THICK - 3; t += 8) for (int s : new int[] { -3, 3 }) {
            lampPost(s, t);
            lampPost(s, -t);
            lampPost(t, s);
            lampPost(-t, s);
        }
    }

    /**
     * Buildings on plots along each side of the ring road, facing it, between the streets; the gaps become yards.
     * A town has one each of the barracks, smithy, chapel, library tower, cloister and cistern house, a stable or
     * two, a granary or two, and houses for the rest.
     */
    private void town() {
        List<String> deck = new ArrayList<>();
        Collections.addAll(
            deck,
            "barracks",
            "smithy",
            "chapel",
            "library_tower",
            "cloister",
            "cistern_house",
            "stables",
            "granary");
        if (random.nextBoolean()) deck.add("stables");
        if (random.nextBoolean()) deck.add("granary");
        Collections.shuffle(deck, random);
        for (int side = 0; side < 4; side++) {
            for (int[] segment : new int[][] { { -ROAD_OUT, -4 }, { 4, ROAD_OUT } }) {
                int at = segment[0];
                while (at <= segment[1]) {
                    RoomTemplate t = nextBuilding(deck, segment[1] - at + 1);
                    if (t == null) {
                        yard(side, at, segment[1]);
                        break;
                    }
                    place(side, at, t);
                    int gap = 2 + random.nextInt(2);
                    if (gap >= 3) yard(side, at + t.width(), Math.min(segment[1], at + t.width() + gap - 1));
                    at += t.width() + gap;
                }
            }
        }
    }

    private RoomTemplate nextBuilding(List<String> deck, int room) {
        for (int i = 0; i < deck.size(); i++) {
            RoomTemplate t = TemplateLibrary.building(deck.get(i));
            if (t.width() <= room && t.depth() <= PLOT_BACK - PLOT_FRONT + 1) {
                deck.remove(i);
                return t;
            }
        }
        RoomTemplate house = TemplateLibrary.building(random.nextInt(3) == 0 ? "house_tall" : "house_small");
        if (house.width() <= room) return house;
        RoomTemplate small = TemplateLibrary.building("house_small");
        return small.width() <= room ? small : null;
    }

    /** A building on side (0 north .. 3 west) starting {@code along} blocks along it, facing the ring road. */
    private void place(int side, int along, RoomTemplate t) {
        int w = t.width(), dp = t.depth();
        int rotation, minX, minZ;
        switch (side) {
            case 0: // north side, front south
                rotation = 0;
                minX = along;
                minZ = -PLOT_FRONT - dp + 1;
                break;
            case 1: // east side, front west
                rotation = 1;
                minX = PLOT_FRONT;
                minZ = along;
                break;
            case 2: // south side, front north
                rotation = 2;
                minX = along;
                minZ = PLOT_FRONT;
                break;
            default: // west side, front east
                rotation = 3;
                minX = -PLOT_FRONT - dp + 1;
                minZ = along;
                break;
        }
        int sizeX = t.width(rotation), sizeZ = t.depth(rotation);
        for (int x = minX; x < minX + sizeX; x++) for (int z = minZ; z < minZ + sizeZ; z++) stone(x, b - 1, z);
        Role role = t.roles.iterator()
            .next();
        new RoomWriter(d).write(t, rotation, cx + minX, cz + minZ, b, role, false);
        // A short paved path from its door to the road.
        int mid = along + w / 2;
        switch (side) {
            case 0:
                pave(mid, -PLOT_FRONT + 1);
                break;
            case 1:
                pave(PLOT_FRONT - 1, mid);
                break;
            case 2:
                pave(mid, PLOT_FRONT - 1);
                break;
            default:
                pave(-PLOT_FRONT + 1, mid);
                break;
        }
    }

    /** A yard between buildings: a garden bed, a hay pile or a stack of crates. */
    private void yard(int side, int from, int to) {
        if (to - from < 1) return;
        int kind = random.nextInt(3);
        for (int a = from; a <= to; a++) for (int depth = 1; depth <= 4; depth++) {
            int dx, dz;
            switch (side) {
                case 0:
                    dx = a;
                    dz = -PLOT_FRONT - depth;
                    break;
                case 1:
                    dx = PLOT_FRONT + depth;
                    dz = a;
                    break;
                case 2:
                    dx = a;
                    dz = PLOT_FRONT + depth;
                    break;
                default:
                    dx = -PLOT_FRONT - depth;
                    dz = a;
                    break;
            }
            double r = d.noise.unit(cx + dx, cz + dz, 61);
            if (kind == 0) {
                put(dx, b, dz, Block.GRASS);
                if (r < 0.35) put(dx, b + 1, dz, Block.WINDBLOOM);
                else if (r < 0.55) put(dx, b + 1, dz, Block.FLOWER, r < 0.45 ? 0 : 1);
            } else if (kind == 1) {
                if (r < 0.4) put(dx, b + 1, dz, Block.HAY);
                if (r < 0.15) put(dx, b + 2, dz, Block.HAY);
            } else if (r < 0.3) {
                put(dx, b + 1, dz, Block.PLANKS, r < 0.15 ? 0 : 1);
                if (r < 0.1) put(dx, b + 2, dz, Block.LOG);
            }
        }
    }

    /** The four corners of the town: an orchard, a herb garden of Windbloom, a market and a training yard. */
    private void corners() {
        int lo = ROAD_OUT + 3, hi = OUTER - THICK - 2;
        List<Integer> kinds = new ArrayList<>();
        Collections.addAll(kinds, 0, 1, 2, 3);
        Collections.shuffle(kinds, random);
        int[][] signs = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        for (int c = 0; c < 4; c++) {
            int sx = signs[c][0], sz = signs[c][1];
            int kind = kinds.get(c);
            for (int a = lo; a <= hi; a++) for (int e = lo; e <= hi; e++) {
                int dx = sx * a, dz = sz * e;
                if (inSquare(dx - sx * (OUTER - 1), dz - sz * (OUTER - 1), 5)) continue; // the corner tower
                double r = d.noise.unit(cx + dx, cz + dz, 62);
                switch (kind) {
                    case 0: // orchard: a grid of small trees on grass
                        put(dx, b, dz, Block.GRASS);
                        if ((a - lo) % 5 == 2 && (e - lo) % 5 == 2) tree(dx, dz);
                        else if (r < 0.2) put(dx, b + 1, dz, Block.TALL_GRASS);
                        break;
                    case 1: // herb garden: rows of Windbloom with paths between
                        if ((e - lo) % 3 == 0) pave(dx, dz);
                        else {
                            put(dx, b, dz, Block.GRASS);
                            if (r < 0.85) put(dx, b + 1, dz, Block.WINDBLOOM);
                            else put(dx, b + 1, dz, Block.FLOWER, r < 0.92 ? 0 : 1);
                        }
                        break;
                    case 2: // market: a paved square with stalls under blue awnings
                        pave(dx, dz);
                        break;
                    default: // training yard: packed gravel, straw targets on posts
                        put(dx, b, dz, Block.RUBBLE, 1);
                        if ((a - lo) % 5 == 2 && (e - lo) % 6 == 3) {
                            put(dx, b + 1, dz, Block.FENCE);
                            put(dx, b + 2, dz, Block.HAY);
                        }
                        break;
                }
            }
            if (kind == 2) for (int a = lo + 1; a + 2 <= hi; a += 6) for (int e = lo + 1; e + 2 <= hi; e += 6) {
                int dx = sx > 0 ? a : -a - 2, dz = sz > 0 ? e : -e - 2;
                if (!inSquare(dx + 1 - sx * (OUTER - 1), dz + 1 - sz * (OUTER - 1), 6)) stall(dx, dz);
            }
        }
    }

    private void tree(int dx, int dz) {
        for (int y = b + 1; y <= b + 3; y++) put(dx, y, dz, Block.LOG);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = b + 3; y <= b + 5; y++) {
            if (x == 0 && z == 0 && y == b + 3) continue;
            if (Math.abs(x) + Math.abs(z) + Math.abs(y - (b + 4)) <= 2) put(dx + x, y, dz + z, Block.LEAVES);
        }
    }

    /** A market stall: four posts, a counter, a blue awning. */
    private void stall(int dx, int dz) {
        for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) {
            boolean post = (x == 0 || x == 2) && (z == 0 || z == 2);
            if (post) for (int y = b + 1; y <= b + 2; y++) put(dx + x, y, dz + z, Block.FENCE);
            put(dx + x, b + 3, dz + z, Block.WOOL, 11);
        }
        put(dx + 1, b + 1, dz + 2, Block.SLAB, 8);
        if (random.nextInt(3) == 0) d.put(cx + dx + 1, b + 1, cz + dz, Block.CHEST, 0, Loot.STORE, null);
    }

    // --- The keep and its square ----------------------------------------------------------------------------

    private void keep() {
        RoomTemplate keep = TemplateLibrary.building("grand_keep");
        int half = keep.width() / 2;
        for (int x = -half; x <= half; x++) for (int z = -half; z <= half; z++) stone(x, b - 1, z);
        new RoomWriter(d).write(keep, 0, cx - half, cz - half, b, Role.STORE, false);
        d.rooms.add(new Room(cx, b, cz, Role.HALL, keep.name, SURFACE, cx - 9, cx + 9, cz - 9, cz + 9, 10, false));
    }

    /** Before the keep: a square with a well, benches facing it and lamp posts; lawns round the keep's sides. */
    private void square() {
        int inside = INNER - THICK - 1;
        int z0 = 13, z1 = inside;
        for (int dx = -inside; dx <= inside; dx++) for (int dz = -inside; dz <= inside; dz++) {
            if (inSquare(dx, dz, 12)) continue;
            if (inSquare(dx - (INNER - 1), dz, 0) || inSquare(dx, dz, inside) && dz >= z0) continue;
            if (Math.abs(dx) >= inside - 4 && Math.abs(dz) >= inside - 4) continue; // corner towers
            put(dx, b, dz, Block.GRASS);
            double r = d.noise.unit(cx + dx, cz + dz, 63);
            if (r < 0.12) put(dx, b + 1, dz, Block.WINDBLOOM);
            else if (r < 0.25) put(dx, b + 1, dz, Block.TALL_GRASS);
        }
        for (int dx = -inside; dx <= inside; dx++) for (int dz = z0; dz <= z1; dz++) {
            if (Math.abs(dx) >= inside - 4 && dz >= inside - 4) continue;
            pave(dx, dz);
        }
        for (int dz = -2; dz <= 2; dz++) for (int dx = 13; dx <= inside; dx++) pave(dx, dz); // to the east gate
        int wz = (z0 + z1) / 2;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            for (int y = b - 3; y <= b; y++) stone(x, y, wz + z);
            if (Math.abs(x) == 2 || Math.abs(z) == 2) stone(x, b + 1, wz + z);
        }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) for (int y = b - 2; y <= b; y++) put(x, y, wz + z, Block.WATER);
        for (int s : new int[] { -2, 2 }) for (int y = b + 2; y <= b + 3; y++) put(s, y, wz, Block.FENCE);
        for (int x = -2; x <= 2; x++) put(x, b + 4, wz, Block.SLAB, 0);
        for (int x : new int[] { -6, -5, 5, 6 }) put(x, b + 1, wz, Block.STAIRS, rising(x > 0 ? 1 : -1, 0));
        for (int x : new int[] { -9, 9 }) lampPost(x, wz);
    }

    // --- The approach ---------------------------------------------------------------------------------------

    /** A causeway from the island's south rim to the drawbridge, lit, with a guard post beside it. */
    private void approach() {
        int end = (int) (radius * 0.9);
        for (int t = MOAT_OUT + 2; t <= end; t++) {
            if (d.field.column(d.island, cx, cz + t) == null) break;
            for (int w = -2; w <= 2; w++) {
                pave(w, t);
                stone(w, b - 1, t);
            }
            if ((t - MOAT_OUT) % 8 == 4) {
                lampPost(-3, t);
                lampPost(3, t);
            }
        }
        RoomTemplate post = TemplateLibrary.building("house_small");
        int pz = MOAT_OUT + 12;
        for (int x = 5; x < 5 + post.width(1); x++) for (int z = pz; z < pz + post.depth(1); z++) {
            stone(x, b - 1, z);
            put(x, b, z, Block.GRASS);
        }
        new RoomWriter(d).write(post, 1, cx + 5, cz + pz, b, Role.GUARD, false);
    }

    /** A ruined watchtower out on the island: a broken round shell, rubble round its foot, a ladder to a lookout. */
    private void watchtower() {
        double angle = Math.PI * (1.15 + 0.7 * random.nextDouble()); // somewhere on the north side
        int r = (int) (radius * 0.72);
        int tx = cx + (int) Math.round(Math.cos(angle) * r), tz = cz + (int) Math.round(Math.sin(angle) * r);
        int[] column = d.field.column(d.island, tx, tz);
        if (column == null) return;
        int ground = column[1];
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            int rr = x * x + z * z;
            if (rr > 12) continue;
            boolean rim = rr >= 5;
            int height = rim ? 6 + (int) (d.noise.unit(tx + x, tz + z, 64) * 9) : 0;
            d.stone(tx + x, ground, tz + z);
            for (int y = ground + 1; y <= ground + 15; y++) {
                if (rim && y <= ground + height) d.stone(tx + x, y, tz + z);
                else d.put(tx + x, y, tz + z, Block.AIR);
            }
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) d.put(tx + x, ground + 7, tz + z, Block.PLANKS, 1);
        d.stone(tx, ground + 1, tz - 2);
        for (int y = ground + 1; y <= ground + 7; y++) d.put(tx, y, tz - 1, Block.LADDER, 3);
        d.put(tx + 1, ground + 8, tz + 1, Block.CHEST, 0, Loot.STORE, null);
        for (int k = 0; k < 12; k++) {
            int x = (int) ((d.noise.unit(tx + k, tz, 65) - 0.5) * 14),
                z = (int) ((d.noise.unit(tx, tz + k, 66) - 0.5) * 14);
            if (x * x + z * z > 13) d.put(tx + x, ground + 1, tz + z, Block.RUBBLE, k % 3);
        }
        for (int y = ground + 1; y <= ground + 2; y++) d.put(tx, y, tz + 3, Block.AIR);
    }
}
