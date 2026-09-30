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
    static final int OUTER = 80, OUTER_HEIGHT = 8;
    static final int INNER = 36, INNER_HEIGHT = 10;
    static final int THICK = 3;
    static final int MOAT_IN = 82, MOAT_OUT = 86, MOAT_DEPTH = 4;
    static final int ROAD_IN = 37, ROAD_OUT = 40;
    /** Plots run from the ring road back to 58. */
    static final int PLOT_FRONT = 42, PLOT_BACK = 58;
    /**
     * Behind them a back street, and a second row of plots facing it back to 73; the lane behind those, along the
     * outer wall, is four wide.
     */
    static final int BACK_STREET = 59, BACK_FRONT = 62, BACK_BACK = 73;

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
        hangGates();
        sites = new CitadelSites(this::ground);
        int town = MOAT_OUT + 1;
        sites.reserve("town", -town, -town, town, town);
        approach();
        outskirts();

        // Under the keep: the catacomb, levels of it, with the Sky Key, the Sky Warden and the vault.
        new SanctumLayout(d, b).build();

        d.entrance = new int[] { cx, groundAt(0, MOAT_OUT + 4) + 1, cz + MOAT_OUT + 4 };
        d.surfaces.add(new int[] { cx - town - 1, cz - town - 1, cx + town + 1, cz + town + 1 });
        claimSurfaces();
    }

    // --- Helpers --------------------------------------------------------------------------------------------

    private void put(int dx, int y, int dz, Block block, int meta) {
        d.put(cx + dx, y, cz + dz, block, meta);
    }

    private void put(int dx, int y, int dz, Block block) {
        d.put(cx + dx, y, cz + dz, block);
    }

    private void put(int dx, int y, int dz, Block block, int meta, Loot loot, String mob) {
        d.put(cx + dx, y, cz + dz, block, meta, loot, mob);
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
        lampPost(dx, dz, b);
    }

    private void lampPost(int dx, int dz, int ground) {
        put(dx, ground + 1, dz, Block.FENCE);
        put(dx, ground + 2, dz, Block.FENCE);
        put(dx, ground + 3, dz, Block.LAMP);
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

    /**
     * A moat five wide and four deep all round the outer wall, lined with stone and full of water to a block below
     * the ground; a kerb of polished slabs along its outer lip, open where the drawbridge lands.
     */
    private void moat() {
        for (int dx = -MOAT_OUT - 1; dx <= MOAT_OUT + 1; dx++) for (int dz = -MOAT_OUT - 1; dz <= MOAT_OUT + 1; dz++) {
            boolean ditch = inSquare(dx, dz, MOAT_OUT) && !inSquare(dx, dz, MOAT_IN - 1);
            boolean lining = inSquare(dx, dz, MOAT_OUT + 1) && !ditch && !inSquare(dx, dz, MOAT_IN - 2);
            if (ditch) {
                put(dx, b, dz, Block.AIR);
                for (int y = b - MOAT_DEPTH + 1; y <= b - 1; y++) put(dx, y, dz, Block.WATER);
                stone(dx, b - MOAT_DEPTH, dz);
            } else if (lining) {
                for (int y = b - MOAT_DEPTH; y <= b - 1; y++) stone(dx, y, dz);
                put(dx, b, dz, Block.POLISHED);
                boolean outerLip = !inSquare(dx, dz, MOAT_OUT);
                if (outerLip && !(dz > 0 && Math.abs(dx) <= 3)) put(dx, b + 1, dz, Block.SKY_SLAB, 2);
            }
        }
    }

    /**
     * A curtain wall, three thick: a polished plinth along both faces, pillar buttresses every six blocks on its
     * outer face, a tiled wall-walk on top, and a parapet with merlons capped in polished slabs on its outer edge.
     */
    private void wall(int half, int height) {
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            if (!inRing(dx, dz, half)) continue;
            boolean outerEdge = !inSquare(dx, dz, half - 1);
            boolean innerEdge = inSquare(dx, dz, half - THICK + 1);
            int along = Math.abs(dx) == half ? dz : dx;
            boolean buttress = outerEdge && Math.floorMod(along, 6) == 3;
            stone(dx, b - 1, dz);
            stone(dx, b, dz);
            for (int y = b + 1; y < b + height; y++) {
                if (buttress) put(dx, y, dz, Block.PILLAR);
                else if (y == b + 1 && (outerEdge || innerEdge)) put(dx, y, dz, Block.SKYSLATE, 0);
                else stone(dx, y, dz);
            }
            if (outerEdge) {
                stone(dx, b + height, dz);
                stone(dx, b + height + 1, dz);
                if ((dx + dz) % 2 == 0) {
                    stone(dx, b + height + 2, dz);
                    put(dx, b + height + 3, dz, Block.SKYSLATE_SLAB, 0);
                }
            } else d.floorTile(cx + dx, b + height, cz + dz);
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
        // Clear of the gates and streets at the middles, and ending two short of the corner towers' doors.
        int from = Math.min(9, half - 1 - 4 - height - 2);
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
            put(x, b + 1 + i, z, Block.SKY_STAIRS, rising(sx, sz));
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
     * A tower on a wall: a floor at the wall-walk and a walkable roof, joined to the ground by one stair winding up
     * round the tower's middle ({@link #helix}); a door from the bailey, doors onto the walk where the wall meets it,
     * arrow slits in the faces looking out. The stair passes each floor through an opening cut for its headroom, and
     * comes out on the roof through a hatch railed on the far side.
     */
    private void tower(int tx, int tz, int half, int walk, int top, boolean round) {
        int outX = Integer.signum(tx), outZ = Integer.signum(tz);
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int rr = dx * dx + dz * dz;
            if (round && rr > half * half + 2) continue;
            boolean rim = round ? rr > (half - 1) * (half - 1) + 1 : Math.abs(dx) == half || Math.abs(dz) == half;
            boolean corner = !round && Math.abs(dx) == half && Math.abs(dz) == half;
            int x = tx + dx, z = tz + dz;
            stone(x, b - 1, z);
            if (rim) stone(x, b, z);
            else d.floorTile(cx + x, b, cz + z);
            for (int y = b + 1; y < b + top; y++) {
                if (!rim) put(x, y, z, Block.AIR);
                else if (corner) put(x, y, z, Block.PILLAR);
                else if (y == b + 1) put(x, y, z, Block.SKYSLATE, 0); // plinth
                else if (y == b + walk) put(x, y, z, Block.POLISHED); // string course at the walk
                else stone(x, y, z);
            }
            if (!rim) put(x, b + walk, z, Block.PLANKS, 1);
            if (rim) put(x, b + top, z, Block.POLISHED);
            else d.floorTile(cx + x, b + top, cz + z);
            if (rim) {
                stone(x, b + top + 1, z);
                if ((dx + dz) % 2 == 0) {
                    stone(x, b + top + 2, z);
                    put(x, b + top + 3, z, Block.SKYSLATE_SLAB, 0);
                }
            }
        }
        int[] door = door(half, outX, outZ, round);
        helix(tx, tz, walk, top, nearestOnRing(door[0], door[1]));
        // A door from the bailey, on the face towards the middle; a corner tower's two inner faces meet walls, so
        // its door is at the inner corner, where the tower stands clear of them.
        for (int y = b + 1; y <= b + 3; y++) {
            put(tx + door[0], y, tz + door[1], Block.AIR);
            // A round tower's wall is two thick on the diagonal: the doorway goes through both.
            if (round && outX != 0 && outZ != 0) put(tx + door[0] + outX, y, tz + door[1], Block.AIR);
        }
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

    /**
     * A tower's ground door, as an offset from its middle: on the face towards the citadel's middle, or for a corner
     * tower at its inner corner (the diagonal of a round one, beside the corner of a square one).
     */
    static int[] door(int half, int outX, int outZ, boolean round) {
        if (outX == 0 || outZ == 0) return new int[] { -outX * half, -outZ * half };
        if (round) return new int[] { -outX * (half - 1), -outZ * (half - 1) };
        return new int[] { -outX * half, -outZ * (half - 2) };
    }

    /** The square ring two in from a tower's middle, walked clockwise from its north-west corner: 16 places. */
    static int[][] helixRing() {
        int[][] ring = new int[16][];
        int k = 0;
        for (int x = -2; x < 2; x++) ring[k++] = new int[] { x, -2 };
        for (int z = -2; z < 2; z++) ring[k++] = new int[] { 2, z };
        for (int x = 2; x > -2; x--) ring[k++] = new int[] { x, 2 };
        for (int z = 2; z > -2; z--) ring[k++] = new int[] { -2, z };
        return ring;
    }

    /**
     * The stair round a tower: step {@code i} is at the ring's place {@code i % 16}, one higher than the last, rising
     * from the ground to the roof round a solid core (a newel of masonry with lamps set in its faces). Every step is
     * carried on stone down to the step a lap below it (three blocks of headroom kept) or to the ground. The walk
     * floor keeps its outer ring; the roof opens over the last steps, a curb of slabs round the hatch.
     */
    private void helix(int tx, int tz, int walk, int top, int start) {
        int[][] ring = rotate(helixRing(), start);
        int steps = top - 1; // the last step's top is one below the roof: step up from it onto the roof
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = b + 1; y < b + top; y++) {
            boolean lamp = (dx == 0) != (dz == 0) && (y == b + walk - 1 || y == b + top - 2 || y == b + 3);
            if (dx == 0 && dz == 0) put(tx, y, tz, Block.PILLAR);
            else put(tx + dx, y, tz + dz, lamp ? Block.LAMP : Block.SKYSTONE, 0);
        }
        for (int i = 0; i < steps; i++) {
            int[] at = ring[i % ring.length], next = ring[(i + 1) % ring.length];
            int[] from = i == 0 ? null : ring[(i - 1) % ring.length];
            int y = b + 1 + i;
            for (int under = Math.max(b + 1, y - ring.length + 4); under < y; under++)
                stone(tx + at[0], under, tz + at[1]);
            // Each step faces the way it is walked onto, so at a corner you step straight onto its low front and turn
            // on it; the first faces on round the ring.
            int facing = from == null ? rising(next[0] - at[0], next[1] - at[1])
                : rising(at[0] - from[0], at[1] - from[1]);
            put(tx + at[0], y, tz + at[1], Block.SKY_STAIRS, facing);
            // Headroom over the step: three open blocks, cutting through the walk floor or the roof.
            for (int h = y + 1; h <= y + 3 && h <= b + top; h++) put(tx + at[0], h, tz + at[1], Block.AIR);
        }
        stairHead(tx, tz, top, ring, steps);
    }

    /**
     * The stair-head hut on a tower's roof: Skystone walls three high round the stair's last steps and the place it
     * comes out on, a polished roof over it all, and a doorway onto the roof beyond that place.
     */
    private void stairHead(int tx, int tz, int top, int[][] ring, int steps) {
        java.util.Set<Long> inside = new java.util.HashSet<>();
        java.util.List<int[]> cells = new ArrayList<>();
        for (int k = steps - 3; k <= steps; k++) {
            int[] at = ring[Math.floorMod(k, ring.length)];
            inside.add(cell(at[0], at[1]));
            cells.add(at);
        }
        int[] out = ring[steps % ring.length], beyond = ring[(steps + 1) % ring.length];
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int[] at : cells) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = at[0] + dx, z = at[1] + dz;
            if (Math.abs(x) > 3 || Math.abs(z) > 3 || !done.add(cell(x, z))) continue;
            put(tx + x, b + top + 4, tz + z, Block.SKY_SLAB, 2);
            if (inside.contains(cell(x, z))) continue;
            boolean door = x == beyond[0] && z == beyond[1];
            for (int y = b + top + 1; y <= b + top + 3; y++)
                if (door && y <= b + top + 2) put(tx + x, y, tz + z, Block.AIR);
            else stone(tx + x, y, tz + z);
        }
        for (int k = steps - 3; k <= steps; k++) {
            int[] at = ring[Math.floorMod(k, ring.length)];
            for (int y = b + top + 1; y <= b + top + 3; y++) put(tx + at[0], y, tz + at[1], Block.AIR);
        }
        put(tx + out[0], b + top, tz + out[1], Block.POLISHED); // the floor it steps out onto
    }

    private static long cell(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** The ring's place nearest a tower's ground door: where its stair starts. */
    static int nearestOnRing(int x, int z) {
        int[][] ring = helixRing();
        int best = 0;
        for (int k = 1; k < ring.length; k++) {
            int dk = (ring[k][0] - x) * (ring[k][0] - x) + (ring[k][1] - z) * (ring[k][1] - z);
            int db = (ring[best][0] - x) * (ring[best][0] - x) + (ring[best][1] - z) * (ring[best][1] - z);
            if (dk < db) best = k;
        }
        return best;
    }

    private static int[][] rotate(int[][] ring, int start) {
        int[][] out = new int[ring.length][];
        for (int k = 0; k < ring.length; k++) out[k] = ring[(start + k) % ring.length];
        return out;
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
        for (int u = -2; u <= 2; u++) for (int v = back; v <= front; v++) gput(side, u, b, v, Block.TILES, 0);
        // Each mouth framed: pillar jambs, springers of an arch, and a carved keystone.
        for (int v : new int[] { back, front }) {
            for (int y = b + 1; y <= b + 6; y++) {
                gput(side, -3, y, v, Block.PILLAR, 0);
                gput(side, 3, y, v, Block.PILLAR, 0);
            }
            gput(side, 0, b + 7, v, Block.CHISELED, 0);
        }
        for (int v = back; v <= front; v++) {
            gput(side, -2, b + 6, v, Block.SKY_STAIRS, (side == 0 ? 1 : 3) | 4);
            gput(side, 2, b + 6, v, Block.SKY_STAIRS, (side == 0 ? 0 : 2) | 4);
        }
        // Murder holes over the passage.
        for (int v = back + 2; v <= front - 2; v += 2)
            for (int y = b + 7; y <= b + walk; y++) gput(side, 0, y, v, Block.AIR, 0);
        // The raised portcullis hanging in the outer mouth.
        for (int u = -1; u <= 1; u++) gput(side, u, b + 6, front, Block.IRON_BARS, 0);
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
        // The drawbridge over the moat: a plank deck between two log beams, its rails standing on the beams, the
        // span carried on posts down into the water at both banks.
        for (int v = front + 1; v <= MOAT_OUT + 1; v++) {
            for (int u = -2; u <= 2; u++) gput(side, u, b, v, Block.PLANKS, 1);
            for (int u : new int[] { -3, 3 }) {
                gput(side, u, b, v, Block.LOG, 1);
                gput(side, u, b + 1, v, Block.FENCE, 0);
            }
        }
        for (int v : new int[] { MOAT_IN, MOAT_OUT }) for (int u : new int[] { -3, 3 })
            for (int y = b - MOAT_DEPTH + 1; y < b; y++) gput(side, u, y, v, Block.LOG, 1);
    }

    // --- The town -------------------------------------------------------------------------------------------

    private void pave(int dx, int dz) {
        d.floorTile(cx + dx, b, cz + dz);
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
            "granary",
            "tavern",
            "bakery",
            "market_hall",
            "guardhouse");
        if (random.nextBoolean()) deck.add("stables");
        if (random.nextBoolean()) deck.add("granary");
        Collections.shuffle(deck, random);
        for (int side = 0; side < 4; side++) {
            for (int[] segment : new int[][] { { -ROAD_OUT, -4 }, { 4, ROAD_OUT } }) {
                int at = segment[0];
                while (at <= segment[1]) {
                    RoomTemplate t = nextBuilding(deck, segment[1] - at + 1, PLOT_BACK - PLOT_FRONT + 1);
                    if (t == null) {
                        yard(side, at, segment[1], PLOT_FRONT);
                        break;
                    }
                    place(side, at, t, PLOT_FRONT, PLOT_BACK);
                    int gap = 2 + random.nextInt(2);
                    if (gap >= 3)
                        yard(side, at + t.width(), Math.min(segment[1], at + t.width() + gap - 1), PLOT_FRONT);
                    at += t.width() + gap;
                }
            }
        }
        backStreets();
    }

    /** The houses of the back row, the small workshops among them, and what is left for kitchen gardens. */
    private static final String[] BACK_ROW = { "row_house", "cottage", "merchant_house", "house_small",
        "house_timber", "house_corner", "house_tall", "bakery" };

    /**
     * Behind the plots on the ring road, a back street on every side, and a second row of plots facing it: houses
     * and a merchant's shop or two, with kitchen gardens between them (about two plots in five), so that the row is
     * lived in and never a solid wall of fronts.
     */
    private void backStreets() {
        int reach = ROAD_OUT + 2; // through to the corners
        for (int t = -reach; t <= reach; t++) for (int depth = BACK_STREET; depth < BACK_FRONT; depth++) {
            pave(t, -depth);
            pave(t, depth);
            pave(depth, t);
            pave(-depth, t);
        }
        int deep = BACK_BACK - BACK_FRONT + 1;
        for (int side = 0; side < 4; side++) {
            for (int[] segment : new int[][] { { -ROAD_OUT, -4 }, { 4, ROAD_OUT } }) {
                int at = segment[0];
                while (at <= segment[1]) {
                    int room = segment[1] - at + 1;
                    if (random.nextInt(5) < 2 && room >= 6) {
                        int width = Math.min(room, 6 + random.nextInt(3));
                        garden(side, at, width, 1, BACK_FRONT, BACK_BACK, random.nextInt(4) == 0 ? 1 : 0);
                        at += width + 1;
                        continue;
                    }
                    RoomTemplate t = null;
                    for (int tries = 0; tries < 8 && t == null; tries++) {
                        RoomTemplate c = TemplateLibrary.building(BACK_ROW[random.nextInt(BACK_ROW.length)]);
                        if (c.width() <= room && c.depth() <= deep) t = c;
                    }
                    if (t == null) {
                        yard(side, at, segment[1], BACK_FRONT);
                        break;
                    }
                    place(side, at, t, BACK_FRONT, BACK_BACK);
                    at += t.width() + 1 + random.nextInt(2);
                }
            }
        }
        // Lamp posts down the back streets, on the edge by the back row, never before a door or a gate.
        for (int side = 0; side < 4; side++) for (int a = -ROAD_OUT + 6; a <= ROAD_OUT - 6; a += 10) {
            if (Math.abs(a) <= 4) continue;
            int[] at = plotPoint(side, a, 0, BACK_FRONT), before = plotPoint(side, a, 1, BACK_FRONT);
            SkyDungeon.Piece p = d.at(cx + before[0], b + 1, cz + before[1]);
            if (p != null && (p.block == Block.DOOR || p.block == Block.FENCE_GATE)) continue;
            lampPost(at[0], at[1]);
        }
    }

    private RoomTemplate nextBuilding(List<String> deck, int room, int deep) {
        for (int i = 0; i < deck.size(); i++) {
            RoomTemplate t = TemplateLibrary.building(deck.get(i));
            if (t.width() <= room && t.depth() <= deep) {
                deck.remove(i);
                return t;
            }
        }
        String[] houses = { "house_small", "house_timber", "house_corner", "house_tall" };
        RoomTemplate house = TemplateLibrary.building(houses[random.nextInt(houses.length)]);
        if (house.width() <= room) return house;
        for (String name : houses) {
            RoomTemplate t = TemplateLibrary.building(name);
            if (t.width() <= room) return t;
        }
        return null;
    }

    /**
     * A building on side (0 north .. 3 west) starting {@code along} blocks along it, facing the middle, its front on
     * the row starting {@code front} out (the ring road's plots, or the back row's); its garden behind it to
     * {@code back}.
     */
    private void place(int side, int along, RoomTemplate t, int front, int back) {
        int w = t.width(), dp = t.depth();
        int rotation, minX, minZ;
        switch (side) {
            case 0: // north side, front south
                rotation = 0;
                minX = along;
                minZ = -front - dp + 1;
                break;
            case 1: // east side, front west
                rotation = 1;
                minX = front;
                minZ = along;
                break;
            case 2: // south side, front north
                rotation = 2;
                minX = along;
                minZ = front;
                break;
            default: // west side, front east
                rotation = 3;
                minX = -front - dp + 1;
                minZ = along;
                break;
        }
        int sizeX = t.width(rotation), sizeZ = t.depth(rotation);
        for (int x = minX; x < minX + sizeX; x++) for (int z = minZ; z < minZ + sizeZ; z++) stone(x, b - 1, z);
        Role role = t.roles.iterator()
            .next();
        new RoomWriter(d).write(t, rotation, cx + minX, cz + minZ, b, role, false);
        garden(side, along, w, dp + 1, front, back, -1);
        // A short paved path from its door to the road.
        int[] path = plotPoint(side, along + w / 2, 0, front);
        pave(path[0], path[1]);
    }

    /**
     * The citadel offset of a place on a plot: {@code a} along side (0 north .. 3 west), {@code depth} back from the
     * road.
     */
    private static int[] plotPoint(int side, int a, int depth, int front) {
        switch (side) {
            case 0:
                return new int[] { a, -front + 1 - depth };
            case 1:
                return new int[] { front - 1 + depth, a };
            case 2:
                return new int[] { a, front - 1 + depth };
            default:
                return new int[] { -front + 1 - depth, a };
        }
    }

    /** The garden gates, set last so that nothing laid out after a garden can take one away. */
    private final List<int[]> gates = new ArrayList<>();

    private void hangGates() {
        for (int[] gate : gates) {
            put(gate[0], b, gate[1], Block.TILES);
            put(gate[0], b + 1, gate[1], Block.FENCE_GATE, gate[2]);
        }
    }

    /**
     * A yard between buildings, four deep from the road: a flower bed, a hay store, or a stack of crates and barrels.
     */
    private void yard(int side, int from, int to, int front) {
        if (to - from < 1) return;
        int kind = random.nextInt(3);
        for (int a = from; a <= to; a++) for (int depth = 1; depth <= 4; depth++) {
            int[] p = plotPoint(side, a, depth, front);
            int dx = p[0], dz = p[1];
            double r = d.noise.unit(cx + dx, cz + dz, 61);
            if (kind == 0) {
                put(dx, b, dz, Block.GRASS);
                if (r < 0.35) put(dx, b + 1, dz, Block.WINDBLOOM);
                else if (r < 0.55) put(dx, b + 1, dz, Block.FLOWER, r < 0.45 ? 0 : 1);
            } else if (kind == 1) {
                if (depth >= 2 && r < 0.5) put(dx, b + 1, dz, Block.HAY);
                if (depth >= 3 && r < 0.2) put(dx, b + 2, dz, Block.HAY);
            } else if (depth >= 2 && r < 0.45) {
                put(dx, b + 1, dz, r < 0.2 ? Block.BARREL : Block.CRATE);
                if (r < 0.12) put(dx, b + 2, dz, Block.CRATE);
            }
        }
    }

    /**
     * Behind a building, the rest of its plot: a fenced kitchen garden of crops watered by a channel down its middle,
     * or a flower garden, or (behind the big buildings) a paved court with barrels and crates; a gate on the side.
     * {@code kind} chooses (0 kitchen, 1 flowers, 2 court), or -1 to go by the width.
     */
    private void garden(int side, int along, int width, int from, int front, int back, int kind) {
        int to = back - front + 1;
        if (to - from < 3 || width < 4) return;
        if (kind < 0) kind = width >= 13 ? 2 : random.nextInt(4) == 0 ? 1 : 0;
        int crop = random.nextInt(3);
        int mid = (from + to) / 2;
        for (int a = along; a < along + width; a++) for (int depth = from; depth <= to; depth++) {
            int[] p = plotPoint(side, a, depth, front);
            int dx = p[0], dz = p[1];
            // A garden of its own on the back row is fenced along the street too; one behind a house has the house.
            boolean edge = a == along || a == along + width - 1 || depth == to || depth == 1 && from == 1;
            if (kind == 2) {
                pave(dx, dz);
                double r = d.noise.unit(cx + dx, cz + dz, 67);
                if (!edge && depth > from && r < 0.12) put(dx, b + 1, dz, r < 0.06 ? Block.BARREL : Block.CRATE);
                continue;
            }
            if (edge) {
                put(dx, b, dz, Block.GRASS);
                put(dx, b + 1, dz, Block.FENCE);
                continue;
            }
            if (kind == 1) {
                put(dx, b, dz, Block.GRASS);
                double r = d.noise.unit(cx + dx, cz + dz, 68);
                put(dx, b + 1, dz, r < 0.5 ? Block.WINDBLOOM : Block.FLOWER, r < 0.75 ? 0 : 1);
            } else if (depth == mid) {
                put(dx, b, dz, Block.WATER);
                put(dx, b - 1, dz, Block.POLISHED);
            } else {
                put(dx, b, dz, Block.FARMLAND);
                put(dx, b + 1, dz, Block.CROP, crop);
            }
        }
        if (kind != 2 && from == 1) {
            int[] gate = plotPoint(side, along + width / 2, 1, front); // its gate on the street
            gates.add(new int[] { gate[0], gate[1], side == 0 || side == 2 ? SkyDungeon.SOUTH : SkyDungeon.EAST });
        } else if (kind != 2) {
            int[] gate = plotPoint(side, along, (from + to) / 2 + 1, front);
            gates.add(new int[] { gate[0], gate[1], side == 0 || side == 2 ? SkyDungeon.EAST : SkyDungeon.SOUTH });
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
                    case 2: // market: a paved square with stalls under striped awnings
                        pave(dx, dz);
                        break;
                    default: // training yard: packed gravel, straw dummies on posts, barrels of water at the edge
                        put(dx, b, dz, Block.GRAVEL);
                        if ((a - lo) % 5 == 2 && (e - lo) % 6 == 3) {
                            put(dx, b + 1, dz, Block.FENCE);
                            put(dx, b + 2, dz, Block.HAY);
                            put(dx, b + 3, dz, Block.FENCE);
                        } else if ((a == lo || e == lo) && r < 0.15) put(dx, b + 1, dz, Block.BARREL, 1);
                        break;
                }
            }
            if (kind == 2) for (int a = lo + 1; a + 3 <= hi; a += 5) for (int e = lo + 1; e + 2 <= hi; e += 5) {
                int dx = sx > 0 ? a : -a - 3, dz = sz > 0 ? e : -e - 2;
                if (!inSquare(dx + 1 - sx * (OUTER - 1), dz + 1 - sz * (OUTER - 1), 7)) stall(dx, dz);
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

    /**
     * A market stall facing south, three wide: two spruce posts at the back carrying a beam, three counters of one
     * kind of goods before it, a striped awning hung from the beam low over the counters, and a barrel or crate at
     * its side.
     */
    private void stall(int dx, int dz) {
        int goods = random.nextInt(4), colour = random.nextInt(4);
        for (int x = 0; x <= 2; x++) {
            if (x != 1) put(dx + x, b + 1, dz, Block.LOG, 1);
            else put(dx + x, b + 1, dz, Block.AIR);
            put(dx + x, b + 2, dz, Block.PLANKS, 1); // the beam
            put(dx + x, b + 1, dz + 1, Block.MARKET_COUNTER, goods);
            put(dx + x, b + 2, dz + 1, Block.AWNING, colour << 2 | SkyDungeon.NORTH);
            put(dx + x, b + 3, dz + 1, Block.AIR);
        }
        put(dx + 3, b + 1, dz, random.nextBoolean() ? Block.BARREL : Block.CRATE);
    }

    // --- The keep and its square ----------------------------------------------------------------------------

    private void keep() {
        RoomTemplate keep = TemplateLibrary.building("grand_keep");
        int half = keep.width() / 2;
        for (int x = -half; x <= half; x++) for (int z = -half; z <= half; z++) stone(x, b - 1, z);
        new RoomWriter(d).write(keep, 0, cx - half, cz - half, b, Role.STORE, false);
        d.rooms.add(new Room(cx, b, cz, Role.HALL, keep.name, SURFACE, cx - 9, cx + 9, cz - 9, cz + 9, 10, false));
    }

    /**
     * Before the keep: a square with a well, benches facing it and lamp posts. Round the keep's other sides, the
     * lord's garden: a gravel walk round the keep, and beyond it a parterre of flower beds edged in low box hedges,
     * gravel paths between them, benches on the walk; open lawn along the inner wall, where its stairs go up.
     */
    private void square() {
        int inside = INNER - THICK - 1;
        int z0 = 13, z1 = inside;
        for (int dx = -inside; dx <= inside; dx++) for (int dz = -inside; dz <= inside; dz++) {
            if (inSquare(dx, dz, 12)) continue;
            if (inSquare(dx - (INNER - 1), dz, 0) || inSquare(dx, dz, inside) && dz >= z0) continue;
            if (Math.abs(dx) >= inside - 4 && Math.abs(dz) >= inside - 4) continue; // corner towers
            boolean eastPath = dx >= 13 && Math.abs(dz) <= 2;
            int ring = Math.max(Math.abs(dx), Math.abs(dz));
            double r = d.noise.unit(cx + dx, cz + dz, 63);
            if (eastPath) continue;
            if (ring == 15 || ring == 16) {
                put(dx, b, dz, Block.GRAVEL); // the walk round the keep
                continue;
            }
            put(dx, b, dz, Block.GRASS);
            if (ring >= 18 && ring <= 27) {
                int u = Math.floorMod(dx, 5), v = Math.floorMod(dz, 5);
                if (u == 0 || v == 0) put(dx, b, dz, Block.GRAVEL); // the paths between the beds
                else if (u == 1 || u == 4 || v == 1 || v == 4) put(dx, b + 1, dz, Block.LEAVES); // box hedge
                else if (r < 0.5) put(dx, b + 1, dz, Block.WINDBLOOM);
                else put(dx, b + 1, dz, Block.FLOWER, r < 0.75 ? 0 : 1);
            } else if (ring > 27) {
                if (r < 0.08) put(dx, b + 1, dz, Block.WINDBLOOM);
                else if (r < 0.16) put(dx, b + 1, dz, Block.TALL_GRASS);
            }
        }
        // Benches on the walk's outer edge looking in at the keep, lamps at its corners.
        for (int k : new int[] { -6, 6 }) {
            put(k, b + 1, -17, Block.BENCH, SkyDungeon.NORTH);
            put(-17, b + 1, k, Block.BENCH, SkyDungeon.WEST);
            put(17, b + 1, k, Block.BENCH, SkyDungeon.EAST);
        }
        for (int[] c : new int[][] { { -14, -14 }, { 14, -14 }, { -14, 11 }, { 14, 11 } }) lampPost(c[0], c[1]);
        for (int dx = -inside; dx <= inside; dx++) for (int dz = z0; dz <= z1; dz++) {
            if (Math.abs(dx) >= inside - 4 && dz >= inside - 4) continue;
            pave(dx, dz);
        }
        for (int dz = -2; dz <= 2; dz++) for (int dx = 13; dx <= inside; dx++) pave(dx, dz); // to the east gate
        int wz = (z0 + z1) / 2;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            put(x, b - 3, wz + z, Block.POLISHED); // its floor (the catacomb's ceiling may be just below)
            for (int y = b - 2; y <= b; y++) stone(x, y, wz + z);
            if (Math.abs(x) == 2 || Math.abs(z) == 2) put(x, b + 1, wz + z, Block.POLISHED);
        }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) for (int y = b - 2; y <= b; y++) put(x, y, wz + z, Block.WATER);
        for (int s : new int[] { -2, 2 }) for (int y = b + 2; y <= b + 3; y++) put(s, y, wz, Block.FENCE);
        for (int x = -2; x <= 2; x++) put(x, b + 4, wz, Block.SLAB, 1);
        for (int x : new int[] { -5, 5 }) for (int z = wz - 1; z <= wz + 1; z++)
            put(x, b + 1, z, Block.BENCH, x < 0 ? SkyDungeon.WEST : SkyDungeon.EAST); // backs to the square's sides
        for (int x : new int[] { -9, 9 }) lampPost(x, wz);
    }

    // --- The approach ---------------------------------------------------------------------------------------

    /**
     * A causeway from the drawbridge to the island's south rim, laid on the ground as it falls (a stair down where it
     * steps), lit by lamp posts, a pier out over the sky where it ends, and the guardhouse beside the road.
     */
    private void approach() {
        int end = MOAT_OUT + 2, last = b;
        for (int t = MOAT_OUT + 2; t <= radius + 4; t++) {
            boolean land = true;
            for (int w = -3; w <= 3; w++) land &= d.field.column(d.island, cx + w, cz + t) != null;
            if (!land) break;
            int g = Math.min(last, groundAt(0, t));
            level(-3, t, 3, t, g);
            for (int w = -2; w <= 2; w++) {
                paveAt(w, t, g);
                if (g < last) put(w, g + 1, t, Block.SKY_STAIRS, rising(0, -1)); // a step up towards the gate
            }
            if ((t - MOAT_OUT) % 8 == 4) {
                lampPost(-3, t, g);
                lampPost(3, t, g);
            }
            last = g;
            end = t;
        }
        pier(end, last);
        sites.reserve("causeway", -4, MOAT_OUT + 2, 8, end + 11); // the road, and the pier with its crane
        if (building("guardhouse", 6, MOAT_OUT + 8, "guardhouse"))
            sites.reserve("guardhouse", 4, MOAT_OUT + 6, 16, MOAT_OUT + 8 + 11 + 4);
    }

    private void paveAt(int dx, int dz, int ground) {
        d.floorTile(cx + dx, ground, cz + dz);
        for (int y = ground + 1; y <= ground + 3; y++) put(dx, y, dz, Block.AIR);
    }

    /** The island's surface height at a citadel offset (the citadel's own level where there is none). */
    private int groundAt(int dx, int dz) {
        int[] column = d.field.column(d.island, cx + dx, cz + dz);
        return column == null ? b : Math.min(b, column[1]);
    }

    /** A timber pier out over the sky from the causeway's end at {@code g}: railed, with a crane, cargo and lamps. */
    private void pier(int end, int g) {
        int from = end - 2, to = end + 10;
        for (int t = from; t <= to; t++) {
            for (int w = -2; w <= 2; w++) {
                put(w, g, t, Block.PLANKS, 1);
                for (int y = g + 1; y <= g + 3; y++) put(w, y, t, Block.AIR);
            }
            put(-3, g + 1, t, Block.FENCE);
            put(3, g + 1, t, Block.FENCE);
            put(-3, g, t, Block.LOG, 1);
            put(3, g, t, Block.LOG, 1);
            if (t > end && (t - end) % 4 == 0)
                for (int w : new int[] { -3, 3 }) for (int y = g - 3; y < g; y++) put(w, y, t, Block.LOG, 1); // posts
                                                                                                              // down
                                                                                                              // into
                                                                                                              // the
                                                                                                              // cloud
        }
        for (int w = -2; w <= 2; w++) put(w, g + 1, to, Block.FENCE);
        // The crane: a mast, an arm out over the edge, a chain hanging from it with a crate on the hook.
        for (int y = g + 1; y <= g + 6; y++) put(3, y, to - 2, Block.LOG, 1);
        for (int k = 1; k <= 4; k++) put(3 + k, g + 6, to - 2, Block.FENCE);
        for (int y = g + 3; y <= g + 5; y++) put(7, y, to - 2, Block.IRON_BARS);
        put(7, g + 2, to - 2, Block.CRATE);
        put(-2, g + 1, end + 2, Block.CRATE);
        put(-2, g + 2, end + 2, Block.CRATE);
        put(-2, g + 1, end + 3, Block.BARREL);
        put(2, g + 1, end + 6, Block.BARREL);
        put(-2, g + 1, end + 7, Block.CRATE);
        lampPost(-3, end + 4, g);
        lampPost(3, end + 8, g);
        d.features.add("pier");
    }

    /** The ground height most of the rectangle stands at, or -1 if any of it is off the island or too uneven. */
    private int ground(int x0, int z0, int x1, int z1) {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        int least = Integer.MAX_VALUE, most = Integer.MIN_VALUE;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int[] column = d.field.column(d.island, cx + x, cz + z);
            if (column == null) return -1;
            counts.merge(column[1], 1, Integer::sum);
            least = Math.min(least, column[1]);
            most = Math.max(most, column[1]);
        }
        if (most - least > 4) return -1;
        int best = -1, bestCount = -1;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) if (e.getValue() > bestCount) {
            best = e.getKey();
            bestCount = e.getValue();
        }
        return Math.min(best, b);
    }

    /** Brings the rectangle's ground to height {@code g}: earth filled under it, grass on top, rock cut away. */
    private void level(int x0, int z0, int x1, int z1, int g) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int[] column = d.field.column(d.island, cx + x, cz + z);
            if (column == null) continue;
            int top = column[1];
            for (int y = Math.min(top + 1, g - 1); y < g; y++) put(x, y, z, Block.DIRT);
            put(x, g, z, Block.GRASS);
            for (int y = g + 1; y <= top + 1; y++) put(x, y, z, Block.AIR);
        }
    }

    /**
     * An authored building out on the island at citadel offset (minX, minZ), facing south, on its ground levelled
     * with a margin of two all round and a Skyslate footing under it; false if the ground there will not take it.
     */
    private boolean building(String name, int minX, int minZ, String feature) {
        RoomTemplate t = TemplateLibrary.building(name);
        int g = ground(minX - 2, minZ - 2, minX + t.width() + 1, minZ + t.depth() + 3);
        if (g < 0) return false;
        building(name, minX, minZ, feature, g);
        return true;
    }

    /** An authored building out on the island at (minX, minZ), facing south, set on ground {@code g}. */
    private void building(String name, int minX, int minZ, String feature, int g) {
        RoomTemplate t = TemplateLibrary.building(name);
        int maxX = minX + t.width() - 1, maxZ = minZ + t.depth() - 1;
        level(minX - 2, minZ - 2, maxX + 2, maxZ + 4, g);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) put(x, g - 1, z, Block.SKYSLATE, 0);
        new RoomWriter(d).write(
            t,
            0,
            cx + minX,
            cz + minZ,
            g,
            t.roles.iterator()
                .next(),
            false);
        paveAt((minX + maxX) / 2, maxZ + 1, g);
        paveAt((minX + maxX) / 2, maxZ + 2, g);
        if (feature != null) d.features.add(feature);
    }

    /** A fenced field of one crop on tilled soil at ground {@code g}, watered by a channel down its middle. */
    private void field(int x0, int z0, int w, int dp, int g) {
        int crop = random.nextInt(3);
        level(x0, z0, x0 + w - 1, z0 + dp - 1, g);
        int mid = x0 + w / 2;
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + dp; z++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + dp - 1;
            if (edge) put(x, g + 1, z, Block.FENCE);
            else if (x == mid) {
                put(x, g, z, Block.WATER);
                put(x, g - 1, z, Block.POLISHED);
            } else {
                put(x, g, z, Block.FARMLAND);
                put(x, g + 1, z, Block.CROP, crop);
            }
        }
        put(mid + 1, g + 1, z0 + dp - 1, Block.FENCE_GATE, SkyDungeon.SOUTH);
    }

    /** Where the citadel's lands are laid out; made with the approach, which claims the road first. */
    private CitadelSites sites;

    /** The ring the countryside's working lands keep to, and how far out the rim's landmarks may go. */
    private int near() {
        return MOAT_OUT + 12;
    }

    private int far() {
        return (int) (radius * 0.8);
    }

    /**
     * The citadel's lands between the moat and the rim, each on its own site (see {@link CitadelSites}): a farmstead
     * to the west, a windmill farm to the east, the graveyard to the north, a pasture to the north-west, an orchard
     * to the south-east, and a ruined watchtower out towards the north-eastern rim. Each goes only where the island
     * will take it; what stands on the island is then kept clear of its wild growth.
     */
    private void outskirts() {
        int near = near(), far = far();
        CitadelSites.Site s;
        if ((s = sites.fit("farmstead", 28, 33, 160, 200, near, near + 40, false)) != null) farmstead(s);
        if ((s = sites.fit("windmill", 29, 25, -25, 25, near, near + 40, false)) != null) windmillFarm(s);
        if ((s = sites.fit("graveyard", 23, 21, 250, 290, near, near + 30, false)) != null) graveyard(s);
        if ((s = sites.fit("pasture", 21, 17, 205, 245, near, near + 50, false)) != null) pasture(s);
        if ((s = sites.fit("orchard", 23, 19, 35, 70, near, near + 40, false)) != null) orchard(s);
        if ((s = sites.fit("watchtower", 15, 15, 290, 340, near + 30, far, true)) != null) watchtower(s);
        if ((s = sites.fit("quarry", 33, 29, 110, 155, near + 40, far, true)) != null) quarry(s);
    }

    /**
     * The Skystone quarry, out towards the south-western rim: a pit cut in four benches, each three deep and two
     * in from the last, down to a floor twelve below the land; Skystone faces with working seams on them (they
     * open again after they are cut); a timber stair down the south face to the floor; a derrick at the east lip,
     * its boom out over the pit and its chain down to the hoist's basket on the floor, the windlass at its foot; and
     * a yard: cut stone stacked on pallets, a spoil heap, and an open shed with the Mason's Bench and the tools.
     */
    private void quarry(CitadelSites.Site s) {
        int g = s.ground;
        level(s.x0, s.z0, s.x1, s.z1, g);
        int px = s.x0 + 1, pz = s.z0 + 1, pw = 23, pd = 19, zc = pz + pd / 2;
        // The pit: each column cut to the floor of its bench, rock under it and in every face it opens.
        int[][] floor = new int[pw][pd];
        for (int i = 0; i < pw; i++) for (int k = 0; k < pd; k++) {
            int inset = Math.min(Math.min(i, pw - 1 - i), Math.min(k, pd - 1 - k));
            floor[i][k] = g - 3 * (Math.min(3, inset / 2) + 1);
        }
        for (int i = 0; i < pw; i++) for (int k = 0; k < pd; k++) {
            int x = px + i, z = pz + k, f = floor[i][k];
            for (int y = f + 1; y <= g; y++) put(x, y, z, Block.AIR);
            double r = d.noise.unit(cx + x, cz + z, 73);
            put(x, f, z, r < 0.1 ? Block.GRAVEL : Block.ROCK);
            put(x, f - 1, z, Block.ROCK);
            for (int[] n : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                int ni = i + n[0], nk = k + n[1];
                boolean inPit = ni >= 0 && ni < pw && nk >= 0 && nk < pd;
                int top = inPit ? floor[ni][nk] : g - 2; // the land's top two blocks stay soil
                for (int y = f + 1; y <= top; y++) put(x + n[0], y, z + n[1], Block.ROCK);
            }
        }
        // Working seams: a stretch along the north face of each of the three lower benches, one on the west.
        for (int bench = 0; bench < 3; bench++) {
            int z = pz + 2 * bench + 1, stand = g - 3 * (bench + 2) + 1;
            for (int x = px + 8; x <= px + 14; x++) for (int y = stand; y <= stand + 1; y++) put(x, y, z, Block.SEAM);
        }
        for (int z = pz + 7; z <= pz + 11; z++) for (int y = g - 8; y <= g - 7; y++) put(px + 3, y, z, Block.SEAM);
        // The stair down the south face, two wide, on rock built up from the benches it crosses.
        for (int step = 0; step < 12; step++) {
            int z = pz + pd - 1 - step, y = g - 1 - step;
            for (int x = px + 10; x <= px + 11; x++) {
                for (int under = floor[x - px][z - pz] + 1; under < y; under++) put(x, under, z, Block.ROCK);
                put(x, y, z, Block.STAIRS, rising(0, 1)); // spruce, rising towards the lip
                for (int h = y + 1; h <= y + 3 && h <= g; h++) put(x, h, z, Block.AIR);
            }
        }
        // The derrick: its mast at the east lip on a cobbled footing, the boom out over the floor, the chain down to
        // the basket; a guy line back to a stake; the windlass at the mast's foot.
        int mx = px + pw + 1, bx = px + 14;
        put(mx, g, zc, Block.ROCK, 1);
        for (int y = g + 1; y <= g + 11; y++) put(mx, y, zc, Block.LOG, 1);
        for (int x = bx; x < mx; x++) put(x, g + 11, zc, Block.LOG, 1 | 16);
        for (int y = g - 8; y <= g + 10; y++) put(bx, y, zc, Block.IRON_BARS);
        put(bx, g - 11, zc, Block.HOIST, 1);
        put(mx, g + 1, zc + 1, Block.HOIST, 0);
        put(mx + 3, g + 1, zc, Block.LOG, 1);
        for (int y = g + 2; y <= g + 11; y++) put(mx + 3, y, zc, Block.FENCE);
        for (int x = mx + 1; x <= mx + 2; x++) put(x, g + 11, zc, Block.FENCE);
        lampPost(mx, zc - 4, g);
        // The yard: pallets of cut stone, the spoil heap, the shed.
        int yard = pz + pd + 2;
        for (int p = 0; p < 4; p++) {
            int x0 = s.x0 + 3 + p * 3;
            for (int x = x0; x <= x0 + 1; x++) for (int z = yard; z <= yard + 1; z++) {
                put(x, g + 1, z, Block.SLAB, 1);
                put(x, g + 2, z, Block.SKYSTONE, 0);
                if ((x + z + p) % 2 == 0) put(x, g + 3, z, Block.SKYSTONE, 0);
            }
        }
        for (int x = s.x0 + 17; x <= s.x0 + 22; x++) for (int z = yard; z <= Math.min(s.z1, yard + 4); z++) {
            int dx = x - (s.x0 + 19), dz = z - (yard + 2);
            if (dx * dx + dz * dz <= 6) put(x, g + 1, z, Block.GRAVEL);
            if (dx * dx + dz * dz <= 1) put(x, g + 2, z, Block.GRAVEL);
        }
        shed(s.x0 + 26, yard - 1, g);
        d.features.add("quarry");
    }

    /**
     * The quarry's shed at (x0, z0), open to the west: log posts, plank walls on its other three sides, a slab roof;
     * inside, the Mason's Bench, the tool chest, a water barrel and an anvil for dressing the picks.
     */
    private void shed(int x0, int z0, int g) {
        int x1 = x0 + 5, z1 = z0 + 4;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            put(x, g, z, Block.PLANKS, 1);
            boolean post = (x == x0 || x == x1) && (z == z0 || z == z1);
            boolean wall = x == x1 || z == z0 || z == z1;
            for (int y = g + 1; y <= g + 3; y++) put(x, y, z, post ? Block.LOG : wall ? Block.PLANKS : Block.AIR,
                post ? 1 : wall ? 1 : 0);
            put(x, g + 4, z, Block.SLAB, 1);
        }
        put(x1 - 1, g + 1, z0 + 1, Block.CUTTING_BENCH, 0);
        put(x1 - 1, g + 1, z0 + 2, Block.CHEST, 0, Loot.QUARRY, null);
        put(x1 - 1, g + 1, z0 + 3, Block.BARREL, 1);
        put(x0 + 2, g + 1, z0 + 1, Block.ANVIL);
        put(x0 + 2, g + 3, z0 + 3, Block.LAMP);
    }

    /** Every site's ground, with the road and the town, counts as the citadel's: nothing wild grows there. */
    private void claimSurfaces() {
        for (CitadelSites.Site s : sites.sites) {
            d.surfaces.add(new int[] { cx + s.x0 - 2, cz + s.z0 - 2, cx + s.x1 + 2, cz + s.z1 + 2 });
            if (s.ground >= 0) d.sites.add(new int[] { cx + s.x0, cz + s.z0, cx + s.x1, cz + s.z1 });
        }
    }

    /** The farmstead: the farmhouse and its barn side by side, a field before each, hay stacked by the barn. */
    private void farmstead(CitadelSites.Site s) {
        int g = s.ground, wx = s.x0 + 2, wz = s.z0 + 2;
        level(s.x0, s.z0, s.x1, s.z1, g);
        building("farmhouse", wx, wz, null, g);
        building("barn", wx + 12, wz, null, g);
        field(wx, wz + 15, 10, 12, g);
        field(wx + 12, wz + 19, 11, 12, g);
        for (int k = 0; k < 4; k++) put(wx + 23, g + 1, wz + 2 + k, Block.HAY);
        d.features.add("farmstead");
    }

    /** The windmill with its sails turned to the south, its field below it, and a barn for the harvest. */
    private void windmillFarm(CitadelSites.Site s) {
        int g = s.ground, wx = s.x0 + 2, wz = s.z0 + 2;
        level(s.x0, s.z0, s.x1, s.z1, g);
        building("windmill", wx, wz, "windmill", g);
        sails(wx, wz, g);
        field(wx - 1, wz + 12, 11, 11, g);
        building("barn", wx + 14, wz, "barn", g);
    }

    /** A windmill's sails on its south face at the cap: four lattice arms of fence with white canvas. */
    private void sails(int minX, int minZ, int g) {
        int hx = minX + 4, hy = g + 12, hz = minZ + 9;
        put(hx, hy, hz - 1, Block.LOG);
        put(hx, hy, hz, Block.LOG);
        for (int k = 1; k <= 5; k++) {
            put(hx, hy + k, hz, Block.FENCE);
            put(hx, hy - k, hz, Block.FENCE);
            put(hx - k, hy, hz, Block.FENCE);
            put(hx + k, hy, hz, Block.FENCE);
            if (k >= 2) {
                put(hx + 1, hy + k, hz, Block.WOOL, 0);
                put(hx - 1, hy - k, hz, Block.WOOL, 0);
                put(hx - k, hy + 1, hz, Block.WOOL, 0);
                put(hx + k, hy - 1, hz, Block.WOOL, 0);
            }
        }
    }

    /**
     * A graveyard: a low Skyslate wall with a gate, rows of carved headstones with flowers before them, a gravel
     * path, lamp posts, and the mortuary chapel at the back.
     */
    private void graveyard(CitadelSites.Site s) {
        int w = 21, dp = 19, x0 = s.x0 + 1, z0 = s.z0 + 1, g = s.ground;
        level(x0 - 1, z0 - 1, x0 + w, z0 + dp, g);
        int gate = x0 + w / 2;
        building("mortuary_chapel", gate - 3, z0 + 1, null, g); // first: its levelled margin must not cut the wall
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + dp; z++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + dp - 1;
            if (edge && !(z == z0 + dp - 1 && Math.abs(x - gate) <= 1)) {
                put(x, g + 1, z, Block.SKYSLATE, 0);
                put(x, g + 2, z, Block.SKYSLATE_SLAB, 0);
            } else if (Math.abs(x - gate) <= 1 && z > z0 + 11) put(x, g, z, Block.GRAVEL);
        }
        for (int x = x0 + 2; x < x0 + w - 2; x += 2) {
            if (Math.abs(x - gate) <= 4) continue;
            for (int z = z0 + 3; z < z0 + dp - 2; z += 3) {
                put(x, g + 1, z, Block.CHISELED);
                double r = d.noise.unit(cx + x, cz + z, 69);
                if (r < 0.5) put(x, g + 1, z + 1, Block.FLOWER, r < 0.25 ? 0 : 1);
                else if (r < 0.7) put(x, g + 1, z + 1, Block.FLOWER_POT, (int) (r * 10) % SkyDungeon.POT_PLANTS);
            }
        }
        lampPost(gate - 2, z0 + dp, g);
        lampPost(gate + 2, z0 + dp, g);
        d.features.add("graveyard");
    }

    /** An orchard: rows of small trees on grass inside a fence, a gate, crates for the picking. */
    private void orchard(CitadelSites.Site s) {
        int w = 23, dp = 19, x0 = s.x0, z0 = s.z0, g = s.ground;
        level(x0, z0, x0 + w - 1, z0 + dp - 1, g);
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + dp; z++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + dp - 1;
            if (edge) put(x, g + 1, z, Block.FENCE);
            else if ((x - x0) % 5 == 2 && (z - z0) % 5 == 2) treeAt(x, z, g);
        }
        put(x0 + w / 2, g + 1, z0, Block.FENCE_GATE, SkyDungeon.SOUTH);
        put(x0 + 1, g + 1, z0 + 1, Block.CRATE);
        put(x0 + 2, g + 1, z0 + 1, Block.CRATE);
        put(x0 + 1, g + 1, z0 + 2, Block.BARREL);
        d.features.add("orchard");
    }

    /** A pasture: a fenced meadow with a gate, a lean-to shelter of spruce, hay and water barrels. */
    private void pasture(CitadelSites.Site s) {
        int w = 21, dp = 17, x0 = s.x0, z0 = s.z0, g = s.ground;
        level(x0, z0, x0 + w - 1, z0 + dp - 1, g);
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + dp; z++) {
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + dp - 1;
            double r = d.noise.unit(cx + x, cz + z, 71);
            if (edge) put(x, g + 1, z, Block.FENCE);
            else if (r < 0.2) put(x, g + 1, z, Block.TALL_GRASS);
            else if (r < 0.24) put(x, g + 1, z, Block.FLOWER, r < 0.22 ? 0 : 1);
        }
        put(x0 + w - 1, g + 1, z0 + dp / 2, Block.FENCE_GATE, SkyDungeon.EAST);
        // The shelter in the north-west corner: log posts, a sloping spruce roof, hay and water inside.
        for (int x = x0 + 1; x <= x0 + 5; x++) {
            for (int z = z0 + 1; z <= z0 + 3; z++)
                put(x, g + 3 + (z == z0 + 1 ? 1 : 0), z, Block.STAIRS, rising(0, -1));
            put(x, g + 1, z0 + 1, Block.HAY);
        }
        for (int[] post : new int[][] { { x0 + 1, z0 + 3 }, { x0 + 5, z0 + 3 } })
            for (int y = g + 1; y <= g + 2; y++) put(post[0], y, post[1], Block.LOG, 1);
        for (int x = x0 + 1; x <= x0 + 5; x++) for (int y = g + 1; y <= g + 3; y++) put(x, y, z0, Block.PLANKS, 1);
        put(x0 + 2, g + 1, z0 + 2, Block.BARREL, 1);
        put(x0 + 4, g + 1, z0 + 2, Block.BARREL, 1);
        d.features.add("pasture");
    }

    private void treeAt(int dx, int dz, int g) {
        for (int y = g + 1; y <= g + 3; y++) put(dx, y, dz, Block.LOG);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = g + 3; y <= g + 5; y++) {
            if (x == 0 && z == 0 && y == g + 3) continue;
            if (Math.abs(x) + Math.abs(z) + Math.abs(y - (g + 4)) <= 2) put(dx + x, y, dz + z, Block.LEAVES);
        }
    }

    /** A ruined watchtower out on the island: a broken round shell, rubble round its foot, a ladder to a lookout. */
    private void watchtower(CitadelSites.Site s) {
        level(s.x0, s.z0, s.x1, s.z1, s.ground);
        int tx = cx + (s.x0 + s.x1) / 2, tz = cz + (s.z0 + s.z1) / 2, ground = s.ground;
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
            if (x * x + z * z > 13) {
                if (k % 3 == 0) d.put(tx + x, ground + 1, tz + z, Block.SKYSTONE, 1);
                else d.put(tx + x, ground + 1, tz + z, Block.SKY_SLAB, 0);
            }
        }
        for (int y = ground + 1; y <= ground + 2; y++) d.put(tx, y, tz + 3, Block.AIR);
        d.features.add("watchtower");
    }
}
