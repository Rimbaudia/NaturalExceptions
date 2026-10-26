package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Connection;
import naturalexceptions.core.worldgen.SkyDungeon.Piece;
import naturalexceptions.core.worldgen.SkyDungeon.Room;
import naturalexceptions.core.worldgen.SkyDungeon.Style;
import naturalexceptions.core.worldgen.SkyDungeon.Trap;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;
import naturalexceptions.core.worldgen.dungeon.TrapLinks;

/** Sky dungeons: whole, enterable, varied, the key reachable, the vault sealed until the gate opens. */
final class SkyDungeonTest {

    private static final class Sample {

        final SkyIslands field;
        final SkyIslands.Island island;
        final SkyDungeon dungeon;
        final long seed, cellX, cellZ;

        Sample(SkyIslands field, SkyIslands.Island island, SkyDungeon dungeon, long seed, long cellX, long cellZ) {
            this.field = field;
            this.island = island;
            this.dungeon = dungeon;
            this.seed = seed;
            this.cellX = cellX;
            this.cellZ = cellZ;
        }

        /** The island's rock by column, packed as {bottom, top} pairs (MISSING where the column is open sky). */
        private int[] rock;
        private int rockMinX, rockMinZ, rockSize;
        private static final int MISSING = Integer.MIN_VALUE;

        /** The island's rock {bottom, top} at a column, cached (the noise is costly), or null for open sky. */
        int[] rock(int x, int z) {
            if (rock == null) {
                int r = (int) Math.ceil(island.radius()) + 4;
                rockMinX = island.centreX() - r;
                rockMinZ = island.centreZ() - r;
                rockSize = 2 * r + 1;
                rock = new int[rockSize * rockSize * 2];
                for (int dx = 0; dx < rockSize; dx++) for (int dz = 0; dz < rockSize; dz++) {
                    int[] column = field.column(island, rockMinX + dx, rockMinZ + dz);
                    int i = (dx * rockSize + dz) * 2;
                    rock[i] = column == null ? MISSING : column[0];
                    rock[i + 1] = column == null ? MISSING : column[1];
                }
            }
            int dx = x - rockMinX, dz = z - rockMinZ;
            if (dx < 0 || dz < 0 || dx >= rockSize || dz >= rockSize) return field.column(island, x, z);
            int i = (dx * rockSize + dz) * 2;
            return rock[i] == MISSING ? null : new int[] { rock[i], rock[i + 1] };
        }

        String name() {
            return dungeon.kind() + " at " + island.centreX() + "," + island.centreZ() + " (seed " + seed + ")";
        }
    }

    private static List<Sample> samples;

    /** Dungeons from several seeds, so both kinds and many layouts are covered. */
    private static synchronized List<Sample> samples() {
        if (samples != null) return samples;
        samples = new ArrayList<>();
        for (long s = 1; s <= 6; s++) {
            long seed = s * 7919L;
            SkyIslands field = new SkyIslands(seed);
            for (long cx = 0; cx < 3; cx++) for (long cz = 0; cz < 3; cz++) {
                SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
                if (island != null) samples.add(new Sample(field, island, SkyDungeon.of(field, island), seed, cx, cz));
            }
        }
        return samples;
    }

    /**
     * A citadel's town houses and outbuildings hold household things, not dungeon store loot (which can hold a spare
     * Sky Key): the town no longer leaks the way past its key room.
     */
    @Test
    void aCitadelsHousesHoldHouseholdLootNotTheDungeonsStores() {
        int citadels = 0;
        for (Sample sample : samples()) {
            if (sample.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int household = 0, store = 0;
            for (Piece p : sample.dungeon.pieces()) {
                if (p.loot == SkyDungeon.Loot.HOUSEHOLD) household++;
                if (p.loot == SkyDungeon.Loot.STORE) store++;
            }
            assertTrue(household >= 10, "a citadel's town has household chests, found " + household);
            assertTrue(
                store < household,
                "dungeon stores are its few rooms below, not every house: " + store
                    + " stores, "
                    + household
                    + " houses");
        }
        assertTrue(citadels > 0, "the samples include a citadel");
    }

    /**
     * The dead city's machines: a few still stand guard, dormant, inside its gates; many more are run down at the
     * edges of its streets, never in a doorway and always with room to walk past.
     */
    @Test
    void aCitadelsStreetsHoldItsRunDownMachines() {
        int citadels = 0;
        for (Sample sample : samples()) {
            if (sample.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int broken = 0, standing = 0;
            for (Piece p : sample.dungeon.pieces()) {
                if (p.block == SkyDungeon.Block.GUARD) standing++;
                if (p.block != SkyDungeon.Block.BROKEN_AUTOMATON) continue;
                broken++;
                Piece floor = sample.dungeon.at(p.x, p.y - 1, p.z), over = sample.dungeon.at(p.x, p.y + 1, p.z);
                assertTrue(floor != null && floor.block != SkyDungeon.Block.AIR, "it sits on the street");
                assertTrue(over != null && over.block == SkyDungeon.Block.AIR, "nothing over it");
                for (int[] n : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                    Piece beside = sample.dungeon.at(p.x + n[0], p.y, p.z + n[1]);
                    assertTrue(
                        beside == null
                            || beside.block != SkyDungeon.Block.DOOR && beside.block != SkyDungeon.Block.GATE,
                        "not in a doorway");
                }
            }
            assertTrue(broken >= 15, "many run down in its streets: " + broken);
            assertTrue(standing >= 6 && standing < broken, "a few still on guard: " + standing);
        }
        assertTrue(citadels > 0, "the samples include a citadel");
    }

    /**
     * The town is long dead: grass through its paving, vines up its walls, moss on its Skystone; a few houses fallen
     * in, their rubble low enough to walk over; no fire burning anywhere, and a forge's furnace left mid-work.
     */
    @Test
    void aCitadelIsLongDead() {
        int citadels = 0;
        for (Sample sample : samples()) {
            if (sample.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int grass = 0, vines = 0, moss = 0, rubble = 0, midWork = 0;
            int b = sample.dungeon.island.centreY() + 1;
            for (Piece p : sample.dungeon.pieces()) {
                if (p.block == Block.FURNACE) {
                    assertEquals(0, p.meta & 8, "no fire burns in a dead city");
                    if ((p.meta & SkyDungeon.MID_WORK) != 0) midWork++;
                }
                if (p.y == b && p.block == Block.GRASS) grass++;
                if (p.block == Block.VINE) vines++;
                if (p.block == Block.SKYSTONE && p.meta == 2 && p.y > b && p.y <= b + 3) moss++;
                if (p.y == b + 1 && (p.block == Block.GRAVEL || p.block == Block.ROCK && p.meta == 1)) {
                    rubble++;
                    for (int up = 1; up <= 2; up++) {
                        Piece over = sample.dungeon.at(p.x, p.y + up, p.z);
                        assertTrue(
                            over == null || over.block == Block.AIR,
                            "rubble is a block high: it can be walked over");
                    }
                }
            }
            assertTrue(grass > 50, "grass through the paving: " + grass);
            assertTrue(vines > 10, "vines on the walls: " + vines);
            assertTrue(moss > 20, "moss on the Skystone: " + moss);
            assertTrue(rubble > 5, "a few houses fallen in: " + rubble);
            assertTrue(midWork > 0, "the smithy's furnace left mid-work");
        }
        assertTrue(citadels > 0, "the samples include a citadel");
    }

    /** Every citadel's artisans' quarter has its foundry: the Foundry Colossus asleep in it, buckets by the door. */
    @Test
    void aCitadelsFoundryHoldsItsColossus() {
        int citadels = 0;
        for (Sample sample : samples()) {
            if (sample.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int colossi = 0, chests = 0;
            for (Piece p : sample.dungeon.pieces()) {
                if (p.block == Block.COLOSSUS) colossi++;
                if (p.block == Block.CHEST && p.loot == SkyDungeon.Loot.FOUNDRY) chests++;
            }
            assertEquals(1, colossi, sample.name() + ": one Foundry Colossus");
            assertEquals(1, chests, sample.name() + ": its chest of buckets");
            assertTrue(
                sample.dungeon.features()
                    .contains("foundry"));
        }
        assertTrue(citadels > 0, "the samples include a citadel");
    }

    /**
     * A sanctum's deepest hall is the Gathered's: its seal wakes them, and four candelabra stand unlit in its corners.
     * A citadel's catacomb still keeps the Castellan.
     */
    @Test
    void aSanctumsHallIsTheGatheredsWithItsCandles() {
        int sanctums = 0, citadels = 0;
        for (Sample sample : samples()) {
            int gatheredSeals = 0, castellanSeals = 0, unlit = 0;
            for (Piece p : sample.dungeon.pieces()) {
                if (p.block == Block.SEAL) {
                    if (p.meta == SkyDungeon.SEAL_GATHERED) gatheredSeals++;
                    else castellanSeals++;
                }
                if (p.block == Block.CANDELABRA && (p.meta & SkyDungeon.CANDLES_UNLIT) != 0) unlit++;
            }
            if (sample.dungeon.kind() == SkyDungeon.Kind.SANCTUM) {
                sanctums++;
                assertTrue(gatheredSeals > 0 && castellanSeals == 0, sample.name() + ": the Gathered's seal");
                assertEquals(4, unlit, sample.name() + ": four candelabra unlit in its hall");
            } else {
                citadels++;
                assertTrue(castellanSeals > 0 && gatheredSeals == 0, sample.name() + ": the Castellan keeps a citadel");
                assertEquals(0, unlit, sample.name() + ": no unlit candles in a citadel");
            }
        }
        assertTrue(sanctums > 0 && citadels > 0);
    }

    private static List<Sample> sanctums() {
        List<Sample> out = new ArrayList<>();
        for (Sample s : samples()) if (s.dungeon.kind() == SkyDungeon.Kind.SANCTUM) out.add(s);
        return out;
    }

    /** A walkable-space model: the island's rock, with the dungeon's blocks over it. */
    private static final class Model {

        final Sample s;
        final boolean keyGateOpen, wardenGateOpen, secretsOpen, crumbled;
        final int minX, maxX, minY, maxY, minZ, maxZ;

        Model(Sample s, boolean keyGateOpen, boolean wardenGateOpen, boolean secretsOpen, boolean crumbled) {
            this.s = s;
            this.keyGateOpen = keyGateOpen;
            this.wardenGateOpen = wardenGateOpen;
            this.secretsOpen = secretsOpen;
            this.crumbled = crumbled;
            // The dungeon's own extent (everything it places, and the way in), with room to walk round it: the
            // island itself is far bigger than anything a flood needs to cross.
            int x0 = s.dungeon.entrance()[0], x1 = x0, z0 = s.dungeon.entrance()[2], z1 = z0, y1 = s.island.centreY();
            for (Piece p : s.dungeon.pieces()) {
                x0 = Math.min(x0, p.x);
                x1 = Math.max(x1, p.x);
                z0 = Math.min(z0, p.z);
                z1 = Math.max(z1, p.z);
                y1 = Math.max(y1, p.y);
            }
            minX = x0 - 8;
            maxX = x1 + 8;
            minZ = z0 - 8;
            maxZ = z1 + 8;
            minY = s.island.centreY() - 60;
            maxY = y1 + 2;
        }

        boolean passable(int x, int y, int z) {
            Piece piece = s.dungeon.at(x, y, z);
            if (piece != null) {
                switch (piece.block) {
                    case AIR:
                    case DOOR: // it opens
                    case GUARD: // a man standing, not a block
                    case LADDER:
                    case WATER:
                    case TALL_GRASS:
                    case FLOWER:
                    case WINDBLOOM:
                    case COBWEB:
                    case CARPET:
                    case TORCH:
                    case PRESSURE_PLATE:
                    case VINE:
                    case STORMGLASS_CLUSTER:
                        return true;
                    case GATE:
                        return piece.meta == SkyDungeon.WARDEN_BOUND ? wardenGateOpen : keyGateOpen;
                    case CRUMBLING:
                        return crumbled;
                    case SKYSTONE:
                        return secretsOpen && piece.meta == SkyDungeon.LOOSE;
                    default:
                        return false;
                }
            }
            int[] column = s.rock(x, z);
            return column == null || y < column[0] || y > column[1];
        }

        /** Everything reachable from {@code start}, as a set over this model's box (see {@link Reached}). */
        Reached flood(int[] start) {
            Reached seen = new Reached(this);
            assertTrue(passable(start[0], start[1], start[2]), s.name() + ": the entrance is open");
            int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
            int[] queue = new int[1 << 16]; // grown as the flood spreads: most of the box is never reached
            int head = 0, tail = 0;
            int first = seen.index(start[0], start[1], start[2]);
            seen.bits.set(first);
            queue[tail++] = first;
            while (head < tail) {
                int i = queue[head++];
                int x = minX + i / (sy * sz), y = minY + (i / sz) % sy, z = minZ + i % sz;
                int[][] steps = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
                for (int[] st : steps) {
                    int nx = x + st[0], ny = y + st[1], nz = z + st[2];
                    if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) continue;
                    int n = seen.index(nx, ny, nz);
                    if (seen.bits.get(n) || !passable(nx, ny, nz)) continue;
                    seen.bits.set(n);
                    if (tail == queue.length) {
                        // Drop what has been taken off the front before growing.
                        System.arraycopy(queue, head, queue, 0, tail - head);
                        tail -= head;
                        head = 0;
                        if (tail * 2 > queue.length) queue = java.util.Arrays.copyOf(queue, queue.length * 2);
                    }
                    queue[tail++] = n;
                }
            }
            return seen;
        }

        /** Whether a block (a chest) can be reached: open space beside it was reached. */
        static boolean touches(Reached reached, int x, int y, int z) {
            return reached.contains(x + 1, y, z) || reached.contains(x - 1, y, z)
                || reached.contains(x, y + 1, z)
                || reached.contains(x, y, z + 1)
                || reached.contains(x, y, z - 1);
        }

        static boolean entered(Reached reached, Room room) {
            for (int x = room.minX; x <= room.maxX; x++) for (int z = room.minZ; z <= room.maxZ; z++)
                for (int y = room.floorY + 1; y <= room.floorY + 2; y++) if (reached.contains(x, y, z)) return true;
            return false;
        }
    }

    /** Reached blocks, one bit each over a model's box. */
    private static final class Reached {

        final Model m;
        final java.util.BitSet bits = new java.util.BitSet();

        Reached(Model m) {
            this.m = m;
        }

        int index(int x, int y, int z) {
            int sy = m.maxY - m.minY + 1, sz = m.maxZ - m.minZ + 1;
            return ((x - m.minX) * sy + (y - m.minY)) * sz + (z - m.minZ);
        }

        boolean contains(int x, int y, int z) {
            if (x < m.minX || x > m.maxX || y < m.minY || y > m.maxY || z < m.minZ || z > m.maxZ) return false;
            return bits.get(index(x, y, z));
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    private static List<Piece> chests(SkyDungeon dungeon, SkyDungeon.Loot loot) {
        List<Piece> out = new ArrayList<>();
        for (Piece p : dungeon.pieces()) if (p.block == Block.CHEST && p.loot == loot) out.add(p);
        return out;
    }

    @Test
    void floorsWallsAndCeilingsAreDifferentStone() {
        for (Sample s : samples()) {
            for (Room room : s.dungeon.rooms()) {
                // Authored catacomb rooms; the vault is warded all round.
                if (room.template == null || room.level < 0 || room.role == Role.VAULT) continue;
                int floorBricks = 0, ceilingBricks = 0, tiles = 0, polished = 0;
                for (int x = room.minX; x <= room.maxX; x++) for (int z = room.minZ; z <= room.maxZ; z++) {
                    Piece f = s.dungeon.at(x, room.floorY, z), c = s.dungeon.at(x, room.floorY + room.height + 1, z);
                    if (f != null && f.block == Block.SKYSTONE) floorBricks++;
                    if (f != null && f.block == Block.TILES) tiles++;
                    if (c != null && c.block == Block.SKYSTONE) ceilingBricks++;
                    if (c != null && c.block == Block.POLISHED) polished++;
                }
                String where = s.name() + " " + room.template;
                assertEquals(0, floorBricks, where + ": no wall bricks in the floor");
                assertEquals(0, ceilingBricks, where + ": no wall bricks in the ceiling");
                assertTrue(tiles > 0 && polished > 0, where + ": a tiled floor, a polished ceiling");
            }
            if (s.dungeon.kind() == SkyDungeon.Kind.CITADEL) {
                int cx = s.island.centreX(), cz = s.island.centreZ(), b = s.island.centreY() + 1;
                int tiles = 0;
                for (int t = CitadelLayout.ROAD_OUT + 2; t <= CitadelLayout.ROAD_OUT + 12; t++) {
                    Piece street = s.dungeon.at(cx, b, cz + t);
                    assertNotNull(street, s.name() + ": the south street");
                    assertTrue(
                        street.block == Block.TILES || street.block == Block.GRASS,
                        s.name() + ": the streets are paved with tiles (grass coming through here and there)");
                    if (street.block == Block.TILES) tiles++;
                }
                assertTrue(tiles >= 7, s.name() + ": mostly paving still: " + tiles + " of 11");
            }
        }
    }

    @Test
    void theCitadelPierReachesOutOverTheRim() {
        int citadels = 0;
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int cx = s.island.centreX(), cz = s.island.centreZ(), far = Integer.MIN_VALUE;
            for (Piece p : s.dungeon.pieces())
                if (Math.abs(p.x - cx) <= 3 && p.block != SkyDungeon.Block.AIR) far = Math.max(far, p.z - cz);
            assertNull(
                s.field.column(s.island, cx, cz + far),
                s.name() + ": the pier's end is still over land, " + far + " out");
            boolean landBefore = false;
            for (int t = far - 12; t < far; t++) landBefore |= s.field.column(s.island, cx, cz + t) != null;
            assertTrue(landBefore, s.name() + ": the pier is not joined to the island");
        }
        assertTrue(citadels >= 3, "enough citadels to judge: " + citadels);
    }

    @Test
    void eachCitadelsLandsLieTheirOwnWayAndTheQuarryKeepsOffTheGate() {
        List<Double> bearings = new ArrayList<>();
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            int cx = s.island.centreX(), cz = s.island.centreZ();
            double sx = 0, sz = 0;
            int seams = 0;
            for (Piece p : s.dungeon.pieces()) if (p.block == Block.SEAM) {
                sx += p.x - cx;
                sz += p.z - cz;
                seams++;
            }
            if (seams == 0) continue;
            double bearing = Math.toDegrees(Math.atan2(sz / seams, sx / seams));
            double fromGate = Math.abs(((bearing - CitadelLayout.GATE_BEARING) % 360 + 540) % 360 - 180);
            assertTrue(fromGate >= CitadelLayout.QUARRY_FROM_GATE - 25, s.name() + ": the quarry by the gate road");
            bearings.add(bearing);
        }
        assertTrue(bearings.size() >= 2, "enough quarries to judge: " + bearings);
        double spread = 0;
        for (double a : bearings) for (double b : bearings)
            spread = Math.max(spread, Math.abs(((a - b) % 360 + 540) % 360 - 180));
        assertTrue(spread > 30, "the quarries do not all lie the same way: " + bearings);
    }

    @Test
    void citadelsHaveAWetMoatAClearWallLaneAndLandsOutside() {
        int citadels = 0, quarries = 0;
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int cx = s.island.centreX(), cz = s.island.centreZ(), b = s.island.centreY() + 1;
            int m = (CitadelLayout.MOAT_IN + CitadelLayout.MOAT_OUT) / 2;
            for (int[] at : new int[][] { { cx - m, cz }, { cx + m, cz }, { cx, cz - m }, { cx + 20, cz + m } }) {
                Piece water = s.dungeon.at(at[0], b - 1, at[1]);
                assertTrue(water != null && water.block == Block.WATER, s.name() + ": the moat holds water");
            }
            // Behind the plots and in front of the wall, the lane stays open where the stairs go up.
            int face = CitadelLayout.OUTER - CitadelLayout.THICK;
            for (int x = 9; x < 9 + CitadelLayout.OUTER_HEIGHT; x++)
                for (int row = face - 3; row < face; row++) for (int y = b + 1; y <= b + 2; y++) {
                    Piece p = s.dungeon.at(cx + x, y, cz - row);
                    assertTrue(
                        p == null || passableForWalk(p.block),
                        s.name() + ": the lane before the north wall's stairs is clear at " + x + "," + row);
                }
            assertTrue(
                s.dungeon.features()
                    .size() >= 7,
                s.name() + ": lands outside the walls: " + s.dungeon.features());
            // The quarry is out towards the rim, never by the gate.
            if (s.dungeon.features()
                .contains("quarry")) {
                quarries++;
                for (Piece p : s.dungeon.pieces()) if (p.block == Block.SEAM) {
                    double far = Math.hypot(p.x - cx, p.z - cz);
                    assertTrue(far >= CitadelLayout.MOAT_OUT + 20, s.name() + ": a quarry face by the town: " + far);
                }
            }
            // Each landmark on its own ground: apart from the others, clear of the moat and of the road to the gate.
            int town = CitadelLayout.MOAT_OUT + 8;
            List<int[]> sites = s.dungeon.sites;
            for (int i = 0; i < sites.size(); i++) {
                int[] a = sites.get(i);
                assertTrue(
                    a[2] <= cx - town || a[0] >= cx + town || a[3] <= cz - town || a[1] >= cz + town,
                    s.name() + ": a landmark by the moat at " + (a[0] - cx) + "," + (a[1] - cz));
                assertTrue(
                    a[1] <= cz || a[2] < cx - 8 || a[0] > cx + 8,
                    s.name() + ": a landmark on the road at " + (a[0] - cx) + "," + (a[1] - cz));
                for (int j = i + 1; j < sites.size(); j++) {
                    int[] o = sites.get(j);
                    assertTrue(
                        a[0] > o[2] + 4 || o[0] > a[2] + 4 || a[1] > o[3] + 4 || o[1] > a[3] + 4,
                        s.name() + ": two landmarks on top of each other at " + (a[0] - cx) + "," + (a[1] - cz));
                }
            }
        }
        assertTrue(citadels >= 3, "enough citadels to judge: " + citadels);
        assertTrue(quarries * 2 >= citadels, "most citadels have their quarry: " + quarries + " of " + citadels);
        int[] seen = new int[4];
        String[] places = { "lumber camp", "condensers", "stormglass spire", "lighthouse" };
        for (Sample s : samples()) for (int i = 0; i < places.length; i++) if (s.dungeon.features()
            .contains(places[i])) seen[i]++;
        for (int i = 0; i < places.length; i++)
            assertTrue(seen[i] * 3 >= citadels, "the " + places[i] + " is found on the islands: " + seen[i]);
        for (Sample s : samples()) if (s.dungeon.features()
            .contains("guardhouse")) {
                int guards = 0;
                int cx = s.dungeon.island.centreX(), cz = s.dungeon.island.centreZ();
                for (Piece p : s.dungeon.pieces()) if (p.block == Block.GUARD
                    && Math.max(Math.abs(p.x - cx), Math.abs(p.z - cz)) > CitadelLayout.MOAT_OUT) guards++;
                assertEquals(4, guards, s.name() + ": the guardhouse outside the walls has its watch of four");
            }
        for (Sample s : samples())
            if (s.dungeon.kind() == SkyDungeon.Kind.CITADEL) for (String district : new String[] { "market plaza",
                "tourney ground", "artisans' quarter", "manor quarter" })
                assertTrue(
                    s.dungeon.features()
                        .contains(district),
                    s.name() + ": the town has its " + district);
    }

    @Test
    void everyDoorIsWholeWithItsUpperHalfAboveIt() {
        for (Sample s : samples()) for (Piece p : s.dungeon.pieces()) {
            if (p.block != Block.DOOR || p.meta == SkyDungeon.DOOR_TOP) continue;
            Piece above = s.dungeon.at(p.x, p.y + 1, p.z);
            assertTrue(
                above != null && above.block == Block.DOOR && above.meta == SkyDungeon.DOOR_TOP,
                s.name() + ": a door at "
                    + p.x
                    + ","
                    + p.y
                    + ","
                    + p.z
                    + " has "
                    + (above == null ? "nothing" : above.block)
                    + " over it");
        }
    }

    @Test
    void nothingInACitadelStandsOnNothing() {
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            for (Piece p : s.dungeon.pieces()) {
                boolean fence = p.block == Block.FENCE;
                boolean step = p.block == Block.SKY_STAIRS && (p.meta & 4) == 0;
                if (!fence && !step) continue;
                if (fence && framed(s, p)) continue; // an arm of a crane or a windmill's sail frame
                assertTrue(
                    carried(s, p.x, p.y - 1, p.z),
                    s.name() + ": the " + p.block + " at " + p.x + "," + p.y + "," + p.z + " stands on nothing");
            }
        }
    }

    @Test
    void gardenFencesHaveTheirGates() {
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            int cx = s.island.centreX(), cz = s.island.centreZ(), b = s.island.centreY() + 1, gates = 0;
            for (Piece p : s.dungeon.pieces()) {
                int ring = Math.max(Math.abs(p.x - cx), Math.abs(p.z - cz));
                if (ring <= CitadelLayout.ROAD_OUT || ring > CitadelLayout.PLOT_BACK || p.y != b + 1) continue;
                if (p.block == Block.FENCE_GATE) gates++;
                if (p.block != Block.AIR) continue;
                boolean ew = isFence(s, p.x - 1, p.y, p.z) && isFence(s, p.x + 1, p.y, p.z);
                boolean ns = isFence(s, p.x, p.y, p.z - 1) && isFence(s, p.x, p.y, p.z + 1);
                Piece under = s.dungeon.at(p.x, p.y - 1, p.z);
                assertTrue(
                    !(ew || ns) || under == null || under.block != Block.TILES,
                    s.name() + ": a gap in a fence with no gate at " + (p.x - cx) + "," + (p.z - cz));
            }
            assertTrue(gates > 0, s.name() + ": gardens have gates");
        }
    }

    private static boolean isFence(Sample s, int x, int y, int z) {
        Piece p = s.dungeon.at(x, y, z);
        return p != null && p.block == Block.FENCE;
    }

    /** A fence that is part of a frame: joined sideways to other fences, logs or canvas. */
    private static boolean framed(Sample s, Piece p) {
        int joined = 0;
        for (int[] st : new int[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 }, { 0, 1, 0 } }) {
            Piece q = s.dungeon.at(p.x + st[0], p.y + st[1], p.z + st[2]);
            if (q != null && (q.block == Block.FENCE || q.block == Block.LOG || q.block == Block.WOOL)) joined++;
        }
        return joined >= 2;
    }

    /** Whether a block stands at (x, y, z) to carry what is above: a piece that is not air or water, or rock. */
    private static boolean carried(Sample s, int x, int y, int z) {
        Piece p = s.dungeon.at(x, y, z);
        if (p != null) return p.block != Block.AIR && p.block != Block.WATER;
        int[] column = s.rock(x, z);
        return column != null && y >= column[0] && y <= column[1];
    }

    @Test
    void towersCanBeClimbedToTheWalkAndTheRoof() {
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            int cx = s.island.centreX(), cz = s.island.centreZ(), b = s.island.centreY() + 1;
            int oc = CitadelLayout.OUTER - 1, ic = CitadelLayout.INNER - 1;
            int[][] towers = { { oc, oc, CitadelLayout.OUTER_HEIGHT }, { -oc, -oc, CitadelLayout.OUTER_HEIGHT },
                { 0, -oc, CitadelLayout.OUTER_HEIGHT }, { -oc, 0, CitadelLayout.OUTER_HEIGHT },
                { oc, 0, CitadelLayout.OUTER_HEIGHT }, { ic, ic, CitadelLayout.INNER_HEIGHT },
                { -ic, ic, CitadelLayout.INNER_HEIGHT } };
            for (int[] t : towers) {
                int tx = cx + t[0], tz = cz + t[1], walkY = b + t[2] + 1;
                Set<Long> reached = new HashSet<>();
                for (boolean round : new boolean[] { false, true }) {
                    int[] door = CitadelLayout.door(4, Integer.signum(t[0]), Integer.signum(t[1]), round);
                    for (int[] st : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                        int ox = tx + door[0] + st[0], oz = tz + door[1] + st[1];
                        if (Math.abs(ox - tx) <= 4 && Math.abs(oz - tz) <= 4) continue;
                        if (standable(s, ox, b + 1, oz)) reached
                            .addAll(climb(s, new int[] { ox, b + 1, oz }, tx - 5, tx + 5, tz - 5, tz + 5, b + 40));
                    }
                }
                boolean walk = false, roof = false;
                for (long k : reached) {
                    int y = (int) (k >> 40);
                    if (y == walkY) walk = true;
                    if (y >= walkY + 6) roof = true;
                }
                String where = s.name() + ": the tower at " + t[0] + "," + t[1];
                assertTrue(walk, where + ": up its stair to the wall-walk");
                assertTrue(roof, where + ": and on up to its roof");
            }
        }
    }

    /** Places a player can stand in, from {@code start}, within the box; jumps of one, falls of any height. */
    private static Set<Long> climb(Sample s, int[] start, int x0, int x1, int z0, int z1, int yMax) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(walkKey(start[0], start[1], start[2]));
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int[] st : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                int x = p[0] + st[0], z = p[2] + st[1];
                if ((x < x0 || x > x1 || z < z0 || z > z1) && !(x == start[0] && z == start[2])) continue;
                int y = -1;
                if (p[1] + 1 <= yMax && open(s, p[0], p[1] + 2, p[2]) && standable(s, x, p[1] + 1, z)) y = p[1] + 1;
                else if (standable(s, x, p[1], z)) y = p[1];
                else if (open(s, x, p[1], z) && open(s, x, p[1] + 1, z)) {
                    int f = p[1];
                    while (f > p[1] - 12 && open(s, x, f - 1, z)) f--;
                    if (standable(s, x, f, z)) y = f;
                }
                if (y >= 0 && seen.add(walkKey(x, y, z))) queue.add(new int[] { x, y, z });
            }
        }
        return seen;
    }

    private static boolean standable(Sample s, int x, int y, int z) {
        return open(s, x, y, z) && open(s, x, y + 1, z) && !open(s, x, y - 1, z);
    }

    private static long walkKey(int x, int y, int z) {
        return (long) y << 40 | (long) (x & 0xFFFFF) << 20 | (z & 0xFFFFF);
    }

    private static boolean passableForWalk(Block block) {
        return block == Block.AIR || block == Block.CARPET
            || block == Block.TORCH
            || block == Block.TALL_GRASS
            || block == Block.FLOWER
            || block == Block.WINDBLOOM
            || block == Block.VINE; // walked through in the game
    }

    @Test
    void everyBedHasBothHalves() {
        int beds = 0;
        int[][] step = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        for (Sample s : samples()) {
            for (Piece p : s.dungeon.pieces()) {
                if (p.block != Block.BED) continue;
                boolean head = (p.meta & 8) != 0;
                int dir = p.meta & 3, sign = head ? -1 : 1;
                Piece other = s.dungeon.at(p.x + sign * step[dir][0], p.y, p.z + sign * step[dir][1]);
                assertNotNull(other, s.name() + ": a bed half alone at " + p.x + "," + p.y + "," + p.z);
                assertEquals(Block.BED, other.block, s.name() + ": a bed's other half");
                assertEquals(dir | (head ? 0 : 8), other.meta, s.name() + ": both halves face the same way");
                if (!head) beds++;
            }
        }
        assertTrue(beds > 0, "beds turn up");
    }

    @Test
    void bothKindsTurnUp() {
        Map<SkyDungeon.Kind, Integer> kinds = new EnumMap<>(SkyDungeon.Kind.class);
        for (Sample s : samples()) kinds.merge(s.dungeon.kind(), 1, Integer::sum);
        assertEquals(2, kinds.size(), "sanctums and citadels: " + kinds);
        assertTrue(kinds.get(SkyDungeon.Kind.SANCTUM) >= 5, "enough sanctums to judge: " + kinds);
    }

    @Test
    void theKeyOpensTheWardenAndOnlyTheWardenOpensTheVault() {
        for (Sample s : samples()) {
            String name = s.name();
            SkyDungeon dungeon = s.dungeon;
            assertEquals(1, chests(dungeon, SkyDungeon.Loot.KEY).size(), name + ": one key");
            assertEquals(1, chests(dungeon, SkyDungeon.Loot.CHART).size(), name + ": a chart in the entrance");
            assertEquals(1, chests(dungeon, SkyDungeon.Loot.VAULT_TREASURE).size(), name + ": one treasure");
            int keyGates = 0, wardenGates = 0;
            for (Piece p : dungeon.pieces()) if (p.block == Block.GATE) {
                if (p.meta == SkyDungeon.WARDEN_BOUND) wardenGates++;
                else keyGates++;
            }
            assertEquals(9, keyGates, name + ": a 3 x 3 gate into the Warden's hall");
            assertEquals(9, wardenGates, name + ": a 3 x 3 Warden-bound gate on the vault");
            Room hall = null;
            for (Room room : dungeon.rooms()) if (room.role == Role.WARDEN) hall = room;
            assertNotNull(hall, name + ": a Warden's hall");
            Reached shut = new Model(s, false, false, false, false).flood(dungeon.entrance());
            Reached keyed = new Model(s, true, false, false, false).flood(dungeon.entrance());
            Reached open = new Model(s, true, true, false, false).flood(dungeon.entrance());
            Reached broken = new Model(s, false, false, true, false).flood(dungeon.entrance());
            Piece key = chests(dungeon, SkyDungeon.Loot.KEY).get(0);
            Piece treasure = chests(dungeon, SkyDungeon.Loot.VAULT_TREASURE).get(0);
            assertTrue(Model.touches(shut, key.x, key.y, key.z), name + ": the key is reachable without it");
            assertTrue(!Model.entered(shut, hall), name + ": the Warden's hall is sealed until the key");
            assertTrue(Model.entered(keyed, hall), name + ": the key lets you into the Warden's hall");
            assertTrue(
                !Model.touches(keyed, treasure.x, treasure.y, treasure.z),
                name + ": the vault waits for the Warden");
            assertTrue(Model.touches(open, treasure.x, treasure.y, treasure.z), name + ": its fall opens the vault");
            for (Piece p : chests(dungeon, SkyDungeon.Loot.STORE)) assertTrue(
                Model.touches(broken, p.x, p.y, p.z),
                name + ": every storeroom chest can be reached (secrets by breaking in), "
                    + p.x
                    + ","
                    + p.y
                    + ","
                    + p.z);
        }
    }

    /** Rooms between two rooms along the dungeon's passages (not secret ones). */
    private static int roomsBetween(SkyDungeon dungeon, Room from, Room to) {
        Map<Room, Integer> distance = new HashMap<>();
        ArrayDeque<Room> queue = new ArrayDeque<>();
        distance.put(from, 0);
        queue.add(from);
        while (!queue.isEmpty()) {
            Room here = queue.poll();
            for (Connection c : dungeon.connections()) {
                if (c.style == Style.SECRET || c.a != here && c.b != here) continue;
                Room next = c.a == here ? c.b : c.a;
                if (distance.containsKey(next)) continue;
                distance.put(next, distance.get(here) + 1);
                queue.add(next);
            }
        }
        return distance.getOrDefault(to, -1);
    }

    @Test
    void theWardenIsDeepAndTheKeyIsElsewhere() {
        int rooms = 0, dungeons = 0, guards = 0, ordinary = 0, deep = 0;
        for (Sample s : samples()) {
            Room hall = null, key = null, entrance = null;
            int deepest = 0;
            for (Room room : s.dungeon.rooms()) {
                deepest = Math.max(deepest, room.level);
                if (room.role == Role.WARDEN) hall = room;
                if (room.role == Role.KEY) key = room;
                if (room.role == Role.ENTRANCE && room.level >= 0) entrance = room;
                if (room.level >= 0 && room.role != Role.SECRET) rooms++;
                if (room.level >= 0 && (room.role == Role.GUARD || room.role == Role.HALL || room.role == Role.STORE)) {
                    ordinary++;
                    if (room.role == Role.GUARD) guards++;
                }
            }
            dungeons++;
            if (deepest >= 2) deep++;
            assertTrue(hall.level >= Math.min(2, deepest), s.name() + ": the Warden is deep, level " + hall.level);
            assertTrue(key.level != hall.level, s.name() + ": the key is on another level");
            int between = roomsBetween(s.dungeon, entrance, hall);
            assertTrue(between >= 8, s.name() + ": the Warden is far in, " + between + " rooms");
        }
        assertTrue(deep * 4 >= dungeons * 3, "most dungeons go three levels down: " + deep + " of " + dungeons);
        assertTrue(rooms >= 35 * dungeons, "big dungeons: " + rooms / (double) dungeons + " rooms on average");
        assertTrue(guards <= ordinary * 0.2, "spawners are sparse: " + guards + " guard rooms of " + ordinary);
    }

    @Test
    void everyRoomIsReachableAndSecretsOnlyThroughALooseWall() {
        for (Sample s : samples()) {
            Reached shut = new Model(s, false, false, false, false).flood(s.dungeon.entrance());
            Reached broken = new Model(s, false, false, true, false).flood(s.dungeon.entrance());
            Reached fallen = new Model(s, true, false, false, true).flood(s.dungeon.entrance());
            int secrets = 0;
            for (Room room : s.dungeon.rooms()) {
                String what = s.name() + ": "
                    + room.role
                    + " "
                    + room.template
                    + " at "
                    + room.x
                    + ","
                    + room.floorY
                    + ","
                    + room.z;
                if (room.role == Role.VAULT || room.role == Role.WARDEN) continue;
                if (room.role == Role.SECRET) {
                    secrets++;
                    assertTrue(!Model.entered(shut, room), what + " is hidden");
                    assertTrue(Model.entered(broken, room), what + " opens up once its loose wall is broken");
                    continue;
                }
                assertTrue(Model.entered(shut, room), what + " is reachable");
                assertTrue(Model.entered(fallen, room), what + " is still reachable once floors have crumbled");
            }
            assertTrue(secrets >= 1 && secrets <= 3, s.name() + ": one to three secret closets, " + secrets);
        }
    }

    @Test
    void catacombsHaveLoopsAWardenAndAVault() {
        int withLoops = 0;
        for (Sample s : samples()) {
            int rooms = 0, passages = 0, seals = 0;
            for (Room room : s.dungeon.rooms())
                if (room.level >= 0 && room.role != Role.SECRET && room.role != Role.VAULT) rooms++;
            for (Connection c : s.dungeon.connections())
                if (c.style != Style.SECRET && c.style != Style.VAULT) passages++;
            if (passages > rooms - 1) withLoops++;
            for (Piece p : s.dungeon.pieces()) if (p.block == Block.SEAL) seals++;
            assertEquals(1, seals, s.name() + ": one Warden");
            Connection vault = null;
            for (Connection c : s.dungeon.connections()) if (c.style == Style.VAULT) vault = c;
            assertNotNull(vault, s.name() + ": the vault has its door");
            assertTrue(vault.a.role == Role.WARDEN || vault.b.role == Role.WARDEN, "only through the Warden's hall");
            assertTrue(rooms >= 8, s.name() + ": a real dungeon, not a shed: " + rooms + " rooms");
        }
        assertTrue(withLoops * 4 >= samples().size() * 3, "most dungeons have loops: " + withLoops);
    }

    @Test
    void mostWaysInAreSafe() {
        for (Sample s : samples()) {
            Map<Integer, Integer> trapsPerLevel = new HashMap<>();
            Map<Integer, Integer> corridorsPerLevel = new HashMap<>();
            for (Room room : s.dungeon.rooms()) if (room.trapped) trapsPerLevel.merge(room.level, 1, Integer::sum);
            for (Connection c : s.dungeon.connections()) {
                if (c.style == Style.SHAFT || c.style == Style.VAULT || c.style == Style.SECRET) continue;
                corridorsPerLevel.merge(c.a.level, 1, Integer::sum);
                if (c.trap == Trap.NONE) continue;
                trapsPerLevel.merge(c.a.level, 1, Integer::sum);
                for (Room end : new Room[] { c.a, c.b }) {
                    assertTrue(
                        end.role != Role.ENTRANCE && end.role != Role.KEY && end.role != Role.WARDEN,
                        s.name() + ": no trap on the way into the " + end.role);
                    assertTrue(!end.trapped, s.name() + ": a trapped room's doors are safe");
                }
                if (c.critical) for (Connection other : s.dungeon.connections()) {
                    boolean neighbour = other != c && other.critical
                        && (other.a == c.a || other.a == c.b || other.b == c.a || other.b == c.b);
                    assertTrue(!neighbour || other.trap == Trap.NONE, s.name() + ": never two traps in a row");
                }
            }
            for (Map.Entry<Integer, Integer> e : trapsPerLevel.entrySet()) {
                assertTrue(
                    e.getValue() <= SanctumLayout.MAX_TRAPS_PER_LEVEL,
                    s.name() + ": at most three traps a level");
                int corridors = corridorsPerLevel.getOrDefault(e.getKey(), 0);
                assertTrue(
                    e.getValue() <= Math.max(1, Math.round(corridors * 0.25 + 0.5)),
                    s.name() + ": about one way in five is trapped, level "
                        + e.getKey()
                        + ": "
                        + e.getValue()
                        + " of "
                        + corridors);
            }
        }
    }

    @Test
    void varietyAcrossDungeons() {
        Set<String> templates = new HashSet<>();
        Set<Style> styles = EnumSet.noneOf(Style.class);
        Set<Trap> traps = EnumSet.noneOf(Trap.class);
        for (Sample s : samples()) {
            for (Room room : s.dungeon.rooms()) templates.add(room.template);
            for (Connection c : s.dungeon.connections()) {
                styles.add(c.style);
                traps.add(c.trap);
            }
        }
        traps.remove(Trap.NONE);
        assertTrue(templates.size() >= 12, "at least 12 kinds of room: " + templates);
        assertTrue(styles.size() >= 4 + 3, "corridor looks (besides shaft, vault, secret): " + styles);
        assertTrue(traps.size() >= 4, "several kinds of trap: " + traps);
    }

    @Test
    void everyTriggerSetsSomethingOffAndEveryDeviceHasATrigger() {
        for (Sample s : samples()) {
            List<Piece> triggers = new ArrayList<>(), devices = new ArrayList<>(), seals = new ArrayList<>();
            for (Piece p : s.dungeon.pieces()) {
                if (p.block == Block.TRIGGER) triggers.add(p);
                if (p.block == Block.DISPENSER || p.block == Block.TNT || p.block == Block.ALCOVE) devices.add(p);
                if (p.block == Block.SEAL) seals.add(p);
            }
            for (Piece t : triggers) {
                boolean any = false;
                for (Piece dv : devices) any |= TrapLinks.reaches(t.x, t.y + 1, t.z, dv.x, dv.y, dv.z);
                assertTrue(any, s.name() + ": the trigger at " + t.x + "," + t.y + "," + t.z + " sets something off");
            }
            for (Piece dv : devices) {
                boolean any = false;
                for (Piece t : triggers) any |= TrapLinks.reaches(t.x, t.y + 1, t.z, dv.x, dv.y, dv.z);
                for (Piece seal : seals)
                    any |= dv.block == Block.ALCOVE && Math.abs(seal.x - dv.x) <= 14 && Math.abs(seal.z - dv.z) <= 14; // the
                                                                                                                       // Warden
                                                                                                                       // opens
                                                                                                                       // its
                                                                                                                       // own
                                                                                                                       // hall's
                                                                                                                       // alcoves
                assertTrue(
                    any,
                    s.name() + ": the " + dv.block + " at " + dv.x + "," + dv.y + "," + dv.z + " is set off");
            }
        }
    }

    @Test
    void theDungeonStaysOnItsIslandAndIsTheSameEveryTime() {
        for (Sample s : samples()) {
            // Only a citadel's pier reaches out over the sky, and no further than the island's chunks are written.
            for (Piece piece : s.dungeon.pieces()) assertTrue(
                s.field.reach(s.island, piece.x, piece.z) < (s.dungeon.kind() == SkyDungeon.Kind.CITADEL ? 1.18 : 1.0),
                s.name() + ": piece off the island at " + piece.x + "," + piece.z);
        }
        Sample s = sanctums().get(0);
        SkyIslands fresh = new SkyIslands(s.seed);
        SkyDungeon again = SkyDungeon.of(fresh, fresh.atCell(SkyIslands.Tier.GRAND, s.cellX, s.cellZ));
        assertEquals(
            s.dungeon.pieces()
                .size(),
            again.pieces()
                .size());
        for (Piece piece : s.dungeon.pieces()) {
            Piece other = again.at(piece.x, piece.y, piece.z);
            assertTrue(other != null && other.block == piece.block && other.meta == piece.meta);
        }
        int inChunks = 0;
        Set<Long> chunks = new HashSet<>();
        for (Piece piece : s.dungeon.pieces())
            chunks.add(((long) (piece.x >> 4) << 32) ^ ((piece.z >> 4) & 0xffffffffL));
        for (long c : chunks) inChunks += s.dungeon.piecesInChunk((int) (c >> 32), (int) c)
            .size();
        assertEquals(
            s.dungeon.pieces()
                .size(),
            inChunks,
            "every piece is in exactly one chunk's share");
    }

    @Test
    void citadelsAreGuardedByAWardenAndNoTwoAreAlike() {
        Set<String> looks = new HashSet<>();
        int citadels = 0;
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            citadels++;
            int seals = 0, logs = 0, quartz = 0, furnaces = 0, windbloom = 0, walls = 0;
            for (Piece p : s.dungeon.pieces()) {
                if (p.block == Block.SEAL) seals++;
                if (p.block == Block.LOG) logs++;
                if (p.block == Block.QUARTZ) quartz++;
                if (p.block == Block.FURNACE) furnaces++;
                if (p.block == Block.WINDBLOOM) windbloom++;
                if (SkyDungeon.masonry(p.block) && p.y == s.island.centreY() + 2) walls++;
            }
            assertEquals(1, seals, s.name() + ": one Warden");
            Connection vault = null;
            for (Connection c : s.dungeon.connections()) if (c.style == Style.VAULT) vault = c;
            assertNotNull(vault, s.name() + ": the vault has its door");
            assertTrue(vault.a.role == Role.WARDEN || vault.b.role == Role.WARDEN, "only through the Warden's hall");
            looks.add(walls + "/" + logs + "/" + quartz + "/" + furnaces + "/" + windbloom);
        }
        assertTrue(citadels >= 3, "enough citadels to judge: " + citadels);
        assertEquals(citadels, looks.size(), "every citadel is put together differently: " + looks);
    }

    /**
     * A walk at ground level from the causeway: only where there is ground (or a bridge) underfoot and two blocks of
     * room, never up or down. {@code blocked} cells count as walls.
     */
    private static boolean walksTo(Sample s, int tx, int tz, Set<Long> blocked) {
        int b = s.island.centreY() + 1, cx = s.island.centreX(), cz = s.island.centreZ(),
            r = CitadelLayout.MOAT_OUT + 4;
        int[] start = { cx, b + 1, cz + CitadelLayout.MOAT_OUT }; // on the drawbridge
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { start[0], start[2] });
        seen.add(key(start[0], 0, start[2]));
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            if (p[0] == tx && p[1] == tz) return true;
            for (int[] st : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                int x = p[0] + st[0], z = p[1] + st[1];
                if (Math.abs(x - cx) > r || Math.abs(z - cz) > r || !seen.add(key(x, 0, z))) continue;
                if (blocked.contains(key(x, 0, z))) continue;
                if (!open(s, x, b + 1, z) || !open(s, x, b + 2, z) || open(s, x, b, z)) continue;
                queue.add(new int[] { x, z });
            }
        }
        return false;
    }

    private static boolean open(Sample s, int x, int y, int z) {
        Piece p = s.dungeon.at(x, y, z);
        if (p != null) return p.block == Block.AIR || p.block == Block.DOOR
            || p.block == Block.GUARD
            || p.block == Block.VINE
            || p.block == Block.CARPET
            || p.block == Block.TORCH
            || p.block == Block.TALL_GRASS
            || p.block == Block.FLOWER
            || p.block == Block.WINDBLOOM;
        int[] column = s.rock(x, z);
        return column == null || y > column[1] || y < column[0];
    }

    @Test
    void citadelsAreWalledWithNoWayInButTheGates() {
        for (Sample s : samples()) {
            if (s.dungeon.kind() != SkyDungeon.Kind.CITADEL) continue;
            int cx = s.island.centreX(), cz = s.island.centreZ(), b = s.island.centreY() + 1;
            // The keep's hall, just inside its archway.
            int kx = cx, kz = cz + 9;
            assertTrue(walksTo(s, kx, kz, new HashSet<>()), s.name() + ": the keep can be walked to from the causeway");
            Set<Long> outerGate = new HashSet<>(), innerGate = new HashSet<>();
            for (int w = -2; w <= 2; w++) {
                outerGate.add(key(cx + w, 0, cz + CitadelLayout.OUTER - 1));
                innerGate.add(key(cx + CitadelLayout.INNER - 1, 0, cz + w));
            }
            assertTrue(!walksTo(s, kx, kz, outerGate), s.name() + ": no way in but the outer gate");
            assertTrue(!walksTo(s, kx, kz, innerGate), s.name() + ": no way to the keep but the inner gate");
            // The walls are three thick, all along.
            for (int t = -40; t <= 40; t += 5) {
                if (Math.abs(t) <= 6) continue;
                for (int depth = 0; depth < 3; depth++) {
                    int y = b + 4;
                    int[][] cells = { { cx + t, cz - CitadelLayout.OUTER + depth },
                        { cx - CitadelLayout.OUTER + depth, cz + t } };
                    for (int[] c : cells) {
                        Piece p = s.dungeon.at(c[0], y, c[1]);
                        boolean inTower = Math.abs(c[0] - cx) <= 5 || Math.abs(c[1] - cz) <= 5;
                        assertTrue(
                            inTower || p != null && SkyDungeon.masonry(p.block),
                            s.name() + ": a solid wall at " + c[0] + "," + c[1]);
                    }
                }
            }
            int stairs = 0;
            for (Piece p : s.dungeon.pieces()) if ((p.block == Block.STAIRS || p.block == Block.SKY_STAIRS) && p.y > b
                && p.y <= b + CitadelLayout.INNER_HEIGHT) stairs++;
            assertTrue(
                stairs >= 8 * 8,
                s.name() + ": stairs up onto both walls on every side, and in the towers: " + stairs);
        }
    }

    @Test
    void pitsWithoutLavaHaveALadderOut() {
        for (Sample s : samples()) {
            for (Room room : s.dungeon.rooms()) {
                if (room.template == null || room.level < 0) continue; // the citadel's own buildings have no pits
                RoomTemplate t = naturalexceptions.core.worldgen.dungeon.TemplateLibrary.named(room.template);
                if (!(t.has(',') || t.has('%')) || t.pit.equals("lava")) continue;
                boolean ladder = false;
                for (int x = room.minX; x <= room.maxX; x++) for (int z = room.minZ; z <= room.maxZ; z++)
                    for (int y = room.floorY - RoomWriter.PIT_DEPTH; y < room.floorY; y++) {
                        Piece p = s.dungeon.at(x, y, z);
                        ladder |= p != null && p.block == Block.LADDER;
                    }
                assertTrue(ladder, s.name() + ": " + room.template + " has a ladder out of its pit");
            }
        }
    }

    @Test
    void theWaysDownAreRoomsOfTheirOwn() {
        int shafts = 0;
        for (Sample s : samples()) {
            for (Connection c : s.dungeon.connections()) {
                if (c.style != Style.SHAFT) continue;
                Room top = c.a.level < c.b.level ? c.a : c.b, bottom = top == c.a ? c.b : c.a;
                String name = s.name() + " shaft at " + top.x + "," + top.z;
                assertEquals(Role.SHAFT_TOP, top.role, name + ": the room above is a shaft room");
                assertEquals(Role.SHAFT_FOOT, bottom.role, name + ": and so is the one below");
                int x = (top.minX + top.maxX) / 2, z = (top.minZ + top.maxZ) / 2;
                assertEquals(x, (bottom.minX + bottom.maxX) / 2, name + ": one above the other");
                for (int y = bottom.floorY + 1; y <= top.floorY; y++) {
                    Piece ladder = s.dungeon.at(x, y, z), back = s.dungeon.at(x, y, z - 1);
                    assertTrue(ladder != null && ladder.block == Block.LADDER, name + ": ladder all the way, y" + y);
                    assertTrue(
                        back != null && back.block != Block.AIR && back.block != Block.LADDER,
                        name + ": something behind the ladder, y" + y);
                }
                Piece foot = s.dungeon.at(x, bottom.floorY, z);
                assertTrue(foot.block == Block.BEACON || foot.block == Block.VENT, name + ": a beacon or a lift");
                boolean lit = false;
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++) for (int y = top.floorY; y <= top.floorY + top.height + 1; y++) {
                        Piece p = s.dungeon.at(x + dx, y, z + dz);
                        lit |= p != null
                            && (p.block == Block.LAMP || p.block == Block.CANDELABRA || p.block == Block.BRAZIER);
                    }
                assertTrue(lit, name + ": a light by the hole");
                shafts++;
            }
        }
        assertTrue(shafts >= 10, "enough shafts to judge: " + shafts);
    }

    @Test
    void eachLevelHasItsOwnLook() {
        int judged = 0;
        for (Sample s : samples()) {
            int levels = 0;
            for (Room room : s.dungeon.rooms()) levels = Math.max(levels, room.level + 1);
            if (levels < 3) continue;
            int[] mossy = new int[levels], slate = new int[levels], walls = new int[levels];
            for (Room room : s.dungeon.rooms()) {
                if (room.level < 0 || room.role == Role.VAULT) continue;
                // The wall just outside the room's west side, up its height.
                for (int z = room.minZ; z <= room.maxZ; z++)
                    for (int y = room.floorY + 2; y <= room.floorY + room.height; y++) {
                        Piece p = s.dungeon.at(room.minX - 1, y, z);
                        if (p == null) continue;
                        walls[room.level]++;
                        if (p.block == Block.SKYSTONE && p.meta == 2) mossy[room.level]++;
                        if (p.block == Block.SKYSLATE || p.block == Block.STORMGLASS_VEIN) slate[room.level]++;
                    }
            }
            String name = s.name();
            assertEquals(0, slate[0], name + ": no Skyslate up top");
            assertTrue(mossy[0] < walls[0] * 0.2, name + ": the upper halls are clean");
            assertTrue(mossy[1] > walls[1] * 0.3, name + ": the second level is overgrown");
            assertTrue(slate[levels - 1] > walls[levels - 1] * 0.5, name + ": the deepest is storm-struck");
            judged++;
        }
        assertTrue(judged >= 3, "enough deep dungeons: " + judged);
    }

    @Test
    void aLevelSeldomRepeatsARoom() {
        for (Sample s : samples()) {
            java.util.Map<Integer, java.util.Map<String, Integer>> per = new java.util.TreeMap<>();
            for (Room room : s.dungeon.rooms()) if (room.level >= 0 && room.template != null)
                per.computeIfAbsent(room.level, k -> new java.util.TreeMap<>())
                    .merge(room.template, 1, Integer::sum);
            for (java.util.Map.Entry<Integer, java.util.Map<String, Integer>> e : per.entrySet()) {
                int repeats = 0;
                for (int n : e.getValue()
                    .values()) repeats += n - 1;
                assertTrue(repeats <= 3, s.name() + ": level " + e.getKey() + " repeats " + repeats + " rooms");
            }
        }
    }
}
