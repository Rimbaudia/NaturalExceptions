package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Sky dungeons: whole, enterable, the key reachable, the vault sealed until the gate opens. */
final class SkyDungeonTest {

    /** Dungeons from several seeds, so both kinds and many layouts are covered. */
    private static List<Object[]> samples() {
        List<Object[]> found = new ArrayList<>();
        for (long seed = 1; seed <= 12; seed++) {
            SkyIslands field = new SkyIslands(seed * 7919L);
            for (long cx = 0; cx < 3; cx++) for (long cz = 0; cz < 3; cz++) {
                SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
                if (island != null)
                    found.add(new Object[] { field, island, SkyDungeon.of(field, island), cx, cz, seed * 7919L });
            }
        }
        return found;
    }

    /** A walkable-space model: the island's rock, with the dungeon's blocks over it. */
    private static final class Model {

        final SkyIslands field;
        final SkyIslands.Island island;
        final SkyDungeon dungeon;
        final boolean gateOpen;
        final int minX, maxX, minY, maxY, minZ, maxZ;

        Model(SkyIslands field, SkyIslands.Island island, SkyDungeon dungeon, boolean gateOpen) {
            this.field = field;
            this.island = island;
            this.dungeon = dungeon;
            this.gateOpen = gateOpen;
            int r = (int) Math.ceil(island.radius()) + 4;
            minX = island.centreX() - r;
            maxX = island.centreX() + r;
            minZ = island.centreZ() - r;
            maxZ = island.centreZ() + r;
            minY = island.centreY() - 60;
            maxY = island.centreY() + 30;
        }

        boolean passable(int x, int y, int z) {
            SkyDungeon.Piece piece = dungeon.at(x, y, z);
            if (piece != null) {
                switch (piece.block) {
                    case AIR:
                    case LADDER:
                    case PRESSURE_PLATE:
                    case WATER:
                        return true;
                    case GATE:
                        return gateOpen;
                    default:
                        return false;
                }
            }
            int[] column = field.column(island, x, z);
            return column == null || y < column[0] || y > column[1];
        }

        Set<Long> flood(int[] start) {
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            assertTrue(passable(start[0], start[1], start[2]), "the entrance is open");
            queue.add(start);
            seen.add(key(start[0], start[1], start[2]));
            int[][] steps = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                for (int[] s : steps) {
                    int x = p[0] + s[0], y = p[1] + s[1], z = p[2] + s[2];
                    if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) continue;
                    if (!passable(x, y, z) || !seen.add(key(x, y, z))) continue;
                    queue.add(new int[] { x, y, z });
                }
            }
            return seen;
        }

        /** Whether a block (a chest, a spawner) can be reached: open space beside it was reached. */
        static boolean touches(Set<Long> reached, int x, int y, int z) {
            return reached.contains(key(x + 1, y, z)) || reached.contains(key(x - 1, y, z))
                || reached.contains(key(x, y + 1, z))
                || reached.contains(key(x, y, z + 1))
                || reached.contains(key(x, y, z - 1));
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    @Test
    void bothKindsTurnUp() {
        Map<SkyDungeon.Kind, Integer> kinds = new EnumMap<>(SkyDungeon.Kind.class);
        for (Object[] s : samples()) kinds.merge(((SkyDungeon) s[2]).kind(), 1, Integer::sum);
        assertEquals(2, kinds.size(), "sanctums and citadels: " + kinds);
    }

    @Test
    void oneKeyOneTreasureOneGateAndTheKeyNeedsNoKey() {
        for (Object[] s : samples()) {
            SkyIslands field = (SkyIslands) s[0];
            SkyIslands.Island island = (SkyIslands.Island) s[1];
            SkyDungeon dungeon = (SkyDungeon) s[2];
            String name = dungeon.kind() + " at " + island.centreX() + "," + island.centreZ();
            Map<SkyDungeon.Loot, List<SkyDungeon.Piece>> chests = new EnumMap<>(SkyDungeon.Loot.class);
            int gates = 0, spawners = 0;
            for (SkyDungeon.Piece piece : dungeon.pieces()) {
                if (piece.block == SkyDungeon.Block.CHEST) chests.computeIfAbsent(piece.loot, k -> new ArrayList<>())
                    .add(piece);
                if (piece.block == SkyDungeon.Block.GATE) gates++;
                if (piece.block == SkyDungeon.Block.SPAWNER) {
                    spawners++;
                    assertNotNull(piece.mob);
                }
            }
            assertEquals(
                1,
                chests.get(SkyDungeon.Loot.KEY)
                    .size(),
                name + ": one key");
            assertEquals(
                1,
                chests.get(SkyDungeon.Loot.VAULT_TREASURE)
                    .size(),
                name + ": one treasure");
            assertEquals(9, gates, name + ": a 3 x 3 gate");
            assertTrue(spawners >= 2, name + ": guarded, " + spawners);

            Set<Long> shut = new Model(field, island, dungeon, false).flood(dungeon.entrance());
            Set<Long> open = new Model(field, island, dungeon, true).flood(dungeon.entrance());
            SkyDungeon.Piece key = chests.get(SkyDungeon.Loot.KEY)
                .get(0);
            SkyDungeon.Piece treasure = chests.get(SkyDungeon.Loot.VAULT_TREASURE)
                .get(0);
            assertTrue(Model.touches(shut, key.x, key.y, key.z), name + ": the key is reachable without it");
            assertTrue(!Model.touches(shut, treasure.x, treasure.y, treasure.z), name + ": the vault is sealed");
            assertTrue(Model.touches(open, treasure.x, treasure.y, treasure.z), name + ": the gate lets you in");
            for (SkyDungeon.Piece piece : chests.getOrDefault(SkyDungeon.Loot.STORE, new ArrayList<>())) assertTrue(
                Model.touches(shut, piece.x, piece.y, piece.z),
                name + ": every storeroom chest is reachable");
            for (SkyDungeon.Room room : dungeon.rooms()) {
                if (room.role == SkyDungeon.Role.VAULT) continue;
                assertTrue(
                    shut.contains(key(room.x, room.floorY + 3, room.z)),
                    name + ": "
                        + room.role
                        + " room at "
                        + room.x
                        + ","
                        + room.floorY
                        + ","
                        + room.z
                        + " is reachable");
            }
        }
    }

    @Test
    void trapsAreSprungByPlatesAndTheDungeonStaysOnItsIsland() {
        int traps = 0;
        for (Object[] s : samples()) {
            SkyIslands field = (SkyIslands) s[0];
            SkyIslands.Island island = (SkyIslands.Island) s[1];
            SkyDungeon dungeon = (SkyDungeon) s[2];
            for (SkyDungeon.Piece piece : dungeon.pieces()) {
                assertTrue(
                    field.reach(island, piece.x, piece.z) < 1.0,
                    dungeon.kind() + " piece off the island at " + piece.x + "," + piece.z);
                if (piece.block != SkyDungeon.Block.ARROW_TRAP) continue;
                traps++;
                int[] ahead = step(piece.meta);
                SkyDungeon.Piece plate = dungeon.at(piece.x + ahead[0], piece.y, piece.z + ahead[1]);
                assertTrue(plate != null && plate.block == SkyDungeon.Block.PRESSURE_PLATE, "a plate right before it");
            }
        }
        assertTrue(traps > 0, "some arrow traps");
    }

    @Test
    void theSameIslandAlwaysGivesTheSameDungeon() {
        Object[] s = samples().get(0);
        SkyIslands.Island island = (SkyIslands.Island) s[1];
        SkyIslands fresh = new SkyIslands((Long) s[5]);
        SkyIslands.Island again = fresh.atCell(SkyIslands.Tier.GRAND, (Long) s[3], (Long) s[4]);
        SkyDungeon a = (SkyDungeon) s[2], b = SkyDungeon.of(fresh, again);
        assertEquals(
            a.pieces()
                .size(),
            b.pieces()
                .size());
        for (SkyDungeon.Piece piece : a.pieces()) {
            SkyDungeon.Piece other = b.at(piece.x, piece.y, piece.z);
            assertTrue(other != null && other.block == piece.block && other.meta == piece.meta);
        }
    }

    /** {dx, dz} of a dispenser's facing. */
    private static int[] step(int facing) {
        switch (facing) {
            case 2:
                return new int[] { 0, -1 };
            case 3:
                return new int[] { 0, 1 };
            case 4:
                return new int[] { -1, 0 };
            default:
                return new int[] { 1, 0 };
        }
    }
}
