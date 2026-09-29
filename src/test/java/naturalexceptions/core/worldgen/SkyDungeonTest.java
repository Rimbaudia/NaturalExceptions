package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        private int[][] rock;
        private int rockMinX, rockMinZ;

        /** The island's rock {bottom, top} at a column, cached (the noise is costly), or null for open sky. */
        int[] rock(int x, int z) {
            if (rock == null) {
                int r = (int) Math.ceil(island.radius()) + 4;
                rockMinX = island.centreX() - r;
                rockMinZ = island.centreZ() - r;
                rock = new int[(2 * r + 1) * (2 * r + 1)][];
                for (int dx = 0; dx <= 2 * r; dx++) for (int dz = 0; dz <= 2 * r; dz++)
                    rock[dx * (2 * r + 1) + dz] = field.column(island, rockMinX + dx, rockMinZ + dz);
            }
            int size = (int) Math.round(Math.sqrt(rock.length));
            int dx = x - rockMinX, dz = z - rockMinZ;
            if (dx < 0 || dz < 0 || dx >= size || dz >= size) return field.column(island, x, z);
            return rock[dx * size + dz];
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
            int r = (int) Math.ceil(s.island.radius()) + 4;
            minX = s.island.centreX() - r;
            maxX = s.island.centreX() + r;
            minZ = s.island.centreZ() - r;
            maxZ = s.island.centreZ() + r;
            minY = s.island.centreY() - 60;
            maxY = s.island.centreY() + (s.dungeon.kind() == SkyDungeon.Kind.CITADEL ? 22 : 8);
        }

        boolean passable(int x, int y, int z) {
            Piece piece = s.dungeon.at(x, y, z);
            if (piece != null) {
                switch (piece.block) {
                    case AIR:
                    case LADDER:
                    case WATER:
                    case TALL_GRASS:
                    case FLOWER:
                    case WINDBLOOM:
                    case COBWEB:
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
            int[] queue = new int[sx * sy * sz];
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
            for (Piece piece : s.dungeon.pieces()) assertTrue(
                s.field.reach(s.island, piece.x, piece.z) < 1.0,
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
                if (p.block == Block.SKYSTONE && p.y == s.island.centreY() + 2) walls++;
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

    @Test
    void pitsWithoutLavaHaveALadderOut() {
        for (Sample s : samples()) {
            for (Room room : s.dungeon.rooms()) {
                if (room.template == null) continue;
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
}
