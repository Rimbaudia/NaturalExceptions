package naturalexceptions.core.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sky dungeon on a grand island, as a list of blocks in world coordinates. Two kinds:
 * <ul>
 * <li><b>Sanctum</b>: a ruined gateway on the island's top over a ladder shaft into up to three levels of rooms carved
 * through the rock, joined by corridors; the vault lies deepest, behind a sealed gate.</li>
 * <li><b>Citadel</b>: a walled fort on the island's top with corner towers, a courtyard well and a three-floor keep;
 * the vault lies in a basement carved under the keep, behind a sealed gate.</li>
 * </ul>
 * Guard rooms hold spawners, corridors hide arrow traps and lava pits, one chest holds the Sky Key that opens the
 * vault. Pure: the same island always gives the same dungeon, so every chunk writes its own share of one whole.
 */
public final class SkyDungeon {

    public enum Kind {
        SANCTUM,
        CITADEL
    }

    /** Half the width of a citadel's walls, and of a sanctum's gateway on the surface. */
    public static final int CITADEL_HALF = 15;
    public static final int GATEWAY_HALF = 3;

    public enum Block {
        /** Skystone Bricks; meta 0 whole, 1 cracked, 2 mossy. */
        SKYSTONE,
        /** Warded Skystone: the vault's unbreakable shell. */
        WARDED,
        AIR,
        /** A Lumenite Lamp in a ceiling. */
        LAMP,
        /** A ladder; meta is its vanilla facing (2..5). */
        LADDER,
        GLASS_PANE,
        CHEST,
        SPAWNER,
        /** A dispenser loaded with arrows; meta is its vanilla facing (2..5). */
        ARROW_TRAP,
        PRESSURE_PLATE,
        LAVA,
        WATER,
        /** Sealed Vault Gate, opened with a Sky Key. */
        GATE
    }

    public enum Loot {
        STORE,
        KEY,
        VAULT_TREASURE,
        VAULT_RICH
    }

    public enum Role {
        ENTRANCE,
        HALL,
        GUARD,
        STORE,
        KEY,
        STAIR,
        VAULT
    }

    public static final class Piece {

        public final int x;
        public final int y;
        public final int z;
        public final Block block;
        public final int meta;
        public final Loot loot;
        public final String mob;

        Piece(int x, int y, int z, Block block, int meta, Loot loot, String mob) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.loot = loot;
            this.mob = mob;
        }
    }

    public static final class Room {

        public final int x;
        public final int floorY;
        public final int z;
        public final Role role;

        Room(int x, int floorY, int z, Role role) {
            this.x = x;
            this.floorY = floorY;
            this.z = z;
            this.role = role;
        }
    }

    // Vanilla facings for ladders (the side of the block they hang on) and dispensers (where they shoot).
    static final int NORTH = 2;
    static final int SOUTH = 3;
    static final int WEST = 4;
    static final int EAST = 5;
    /** A ladder hanging on a wall to its north has meta 3 (attached to z - 1); east 5 means attached to x - 1. */
    static final int LADDER_ON_NORTH_WALL = 3;
    static final int LADDER_ON_WEST_WALL = 5;
    static final int LADDER_ON_EAST_WALL = 4;
    private static final String[] MOBS = { "Zombie", "Skeleton", "Spider" };

    private final Kind kind;
    private final Map<Long, Piece> pieces = new LinkedHashMap<>();
    private final List<Room> rooms = new ArrayList<>();
    private int[] entrance;
    private final LandmarkNoise noise;
    private final SkyIslands field;
    private final SkyIslands.Island island;

    private SkyDungeon(SkyIslands field, SkyIslands.Island island, Kind kind) {
        this.field = field;
        this.island = island;
        this.kind = kind;
        this.noise = new LandmarkNoise(field.seed() ^ 0x534b5944554e4745L ^ island.cellX() * 31 ^ island.cellZ());
    }

    // --- Cache ------------------------------------------------------------------------------------------------

    private static final int CACHE_SIZE = 8;
    private static final Map<String, SkyDungeon> CACHE = new LinkedHashMap<String, SkyDungeon>(16, 0.75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SkyDungeon> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** The dungeon of a grand island (computed once, then reused by every chunk it touches), or null. */
    public static SkyDungeon of(SkyIslands field, SkyIslands.Island island) {
        Kind kind = field.dungeonKind(island);
        if (kind == null) return null;
        String key = field.seed() + ":" + island.cellX() + ":" + island.cellZ();
        synchronized (CACHE) {
            SkyDungeon cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        SkyDungeon dungeon = new SkyDungeon(field, island, kind);
        if (kind == Kind.SANCTUM) dungeon.buildSanctum();
        else dungeon.buildCitadel();
        synchronized (CACHE) {
            CACHE.put(key, dungeon);
        }
        return dungeon;
    }

    public Kind kind() {
        return kind;
    }

    public Collection<Piece> pieces() {
        return Collections.unmodifiableCollection(pieces.values());
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    /** Where a player enters: the top of the sanctum's shaft, or the citadel's gate. */
    public int[] entrance() {
        return entrance.clone();
    }

    public Piece at(int x, int y, int z) {
        return pieces.get(key(x, y, z));
    }

    // --- Writing blocks -----------------------------------------------------------------------------------------

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    private void put(int x, int y, int z, Block block, int meta, Loot loot, String mob) {
        long k = key(x, y, z);
        pieces.remove(k); // keep insertion order meaningful: the latest write is the one that stands
        pieces.put(k, new Piece(x, y, z, block, meta, loot, mob));
    }

    private void put(int x, int y, int z, Block block) {
        put(x, y, z, block, 0, null, null);
    }

    /** Skystone, weathered by hash: mostly whole bricks, some cracked, some mossy. */
    private void stone(int x, int y, int z) {
        double roll = noise.unit3(x, y, z, 1);
        put(x, y, z, Block.SKYSTONE, roll < 0.15 ? 1 : roll < 0.27 ? 2 : 0, null, null);
    }

    /** A hollow box: its shell in stone (or warded), its inside cleared. Shell and inside are separate passes. */
    private void shell(int x0, int y0, int z0, int x1, int y1, int z1, boolean warded) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
            if (!edge) continue;
            if (warded) put(x, y, z, Block.WARDED);
            else stone(x, y, z);
        }
    }

    private void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) put(x, y, z, Block.AIR);
    }

    private String mob(int x, int z) {
        return MOBS[(int) (noise.unit(x, z, 7) * MOBS.length)];
    }

    // --- Sanctum ----------------------------------------------------------------------------------------------

    static final int CELL = 11;
    static final int ROOM_HALF = 4; // walls at +-4, inside +-3
    static final int ROOM_HEIGHT = 6; // floor at 0, ceiling at 6
    static final int LEVEL_SPACING = 8;
    static final int GRID = 3; // cells -3..3

    private final class Cell {

        final int level;
        final int i;
        final int j;
        Cell parent;
        int depth;
        final List<Cell> children = new ArrayList<>();
        Role role = Role.HALL;

        Cell(int level, int i, int j) {
            this.level = level;
            this.i = i;
            this.j = j;
        }

        int x() {
            return island.centreX() + i * CELL;
        }

        int z() {
            return island.centreZ() + j * CELL;
        }

        int floor() {
            return levelFloor(level);
        }
    }

    private int levelFloor(int level) {
        return island.centreY() - LEVEL_SPACING - LEVEL_SPACING * level;
    }

    /** Whether a room fits here: rock all round it, below the surface and above the underside. */
    private boolean fits(int level, int i, int j) {
        int cx = island.centreX() + i * CELL, cz = island.centreZ() + j * CELL;
        int floor = levelFloor(level);
        int[][] probes = { { 0, 0 }, { -5, -5 }, { 5, -5 }, { -5, 5 }, { 5, 5 } };
        for (int[] p : probes) {
            int[] column = field.column(island, cx + p[0], cz + p[1]);
            if (column == null || field.reach(island, cx + p[0], cz + p[1]) > 0.85) return false;
            if (column[0] >= floor - 2 || column[1] <= floor + ROOM_HEIGHT + 1) return false;
        }
        return true;
    }

    private void buildSanctum() {
        List<List<Cell>> levels = new ArrayList<>();
        Cell arrival = new Cell(0, 0, 0);
        for (int level = 0; level < 3 && arrival != null; level++) {
            List<Cell> cells = spanningTree(level, arrival);
            levels.add(cells);
            Cell stair = null;
            if (level < 2) {
                for (Cell cell : cells) {
                    if (cell == arrival || !fits(level + 1, cell.i, cell.j)) continue;
                    if (stair == null || cell.depth > stair.depth) stair = cell;
                }
            }
            if (stair != null) stair.role = Role.STAIR;
            arrival = stair == null ? null : new Cell(level + 1, stair.i, stair.j);
        }
        levels.get(0)
            .get(0).role = Role.ENTRANCE;

        // The vault: the deepest dead end of the deepest level that has one; the key: a dead end elsewhere.
        Cell vault = null;
        for (int l = levels.size() - 1; l >= 0 && vault == null; l--) {
            for (Cell cell : levels.get(l)) {
                if (!cell.children.isEmpty() || cell.parent == null || cell.role != Role.HALL) continue;
                if (vault == null || cell.depth > vault.depth) vault = cell;
            }
        }
        if (vault == null) { // a one-room level: fall back to a new room beside the entrance
            vault = levels.get(0)
                .get(0);
        }
        vault.role = Role.VAULT;
        Cell keyRoom = null;
        for (List<Cell> cells : levels) for (Cell cell : cells) {
            if (cell.role != Role.HALL) continue;
            boolean otherLevel = cell.level != vault.level;
            if (keyRoom == null || (otherLevel && keyRoom.level == vault.level)
                || (otherLevel == (keyRoom.level != vault.level) && cell.depth > keyRoom.depth)) keyRoom = cell;
        }
        if (keyRoom != null) keyRoom.role = Role.KEY;
        for (List<Cell> cells : levels) for (Cell cell : cells) {
            if (cell.role != Role.HALL) continue;
            if (vault.parent == cell) {
                cell.role = Role.GUARD; // the room before the vault is always guarded
                continue;
            }
            double roll = noise.unit(cell.x(), cell.z(), 8 + cell.level);
            cell.role = roll < 0.3 ? Role.GUARD : roll < 0.55 ? Role.STORE : Role.HALL;
        }

        // Pass 1: shells. Corridors first, so rooms (and the vault's warded shell) stand over them.
        for (List<Cell> cells : levels)
            for (Cell cell : cells) if (cell.parent != null) corridorShell(cell.parent, cell);
        for (int l = 0; l + 1 < levels.size(); l++) {
            Cell stair = stairOf(levels.get(l));
            shaftShell(stair.x(), stair.z() - 3, levelFloor(l + 1) + 1, levelFloor(l));
        }
        Cell entranceCell = levels.get(0)
            .get(0);
        shaftShell(entranceCell.x(), entranceCell.z() - 3, levelFloor(0) + 1, island.centreY());
        for (List<Cell> cells : levels) for (Cell cell : cells) {
            int x = cell.x(), z = cell.z(), f = cell.floor();
            shell(
                x - ROOM_HALF,
                f,
                z - ROOM_HALF,
                x + ROOM_HALF,
                f + ROOM_HEIGHT,
                z + ROOM_HALF,
                cell.role == Role.VAULT);
        }
        gateway(entranceCell.x(), island.centreY() + 1, entranceCell.z());

        // Pass 2: insides.
        for (List<Cell> cells : levels) for (Cell cell : cells) {
            int x = cell.x(), z = cell.z(), f = cell.floor();
            clear(
                x - ROOM_HALF + 1,
                f + 1,
                z - ROOM_HALF + 1,
                x + ROOM_HALF - 1,
                f + ROOM_HEIGHT - 1,
                z + ROOM_HALF - 1);
            if (cell.parent != null) corridorInside(cell.parent, cell);
        }

        // Pass 3: what is in them.
        for (int l = 0; l + 1 < levels.size(); l++) {
            Cell stair = stairOf(levels.get(l));
            ladder(stair.x(), stair.z() - 3, levelFloor(l + 1) + 1, levelFloor(l) + 1, LADDER_ON_NORTH_WALL);
        }
        ladder(entranceCell.x(), entranceCell.z() - 3, levelFloor(0) + 1, island.centreY() + 1, LADDER_ON_NORTH_WALL);
        entrance = new int[] { entranceCell.x(), island.centreY() + 2, entranceCell.z() - 3 };
        if (keyRoom == null) { // no spare room: the key waits in the entrance hall
            Cell hall = levels.get(0)
                .get(0);
            put(hall.x() + 2, hall.floor() + 1, hall.z() + 2, Block.CHEST, 0, Loot.KEY, null);
        }
        for (List<Cell> cells : levels) for (Cell cell : cells) {
            furnish(cell.x(), cell.floor(), cell.z(), cell.role);
            rooms.add(new Room(cell.x(), cell.floor(), cell.z(), cell.role));
            if (cell.parent != null) {
                if (cell.role == Role.VAULT) vaultGate(cell.parent, cell);
                else if (cell.parent.role != Role.ENTRANCE || cell.parent.parent != null) trap(cell.parent, cell);
            }
        }
    }

    private Cell stairOf(List<Cell> cells) {
        for (Cell cell : cells) if (cell.role == Role.STAIR) return cell;
        return cells.get(0);
    }

    /** A random spanning tree over the cells of a level that fit, grown from {@code start}. */
    private List<Cell> spanningTree(int level, Cell start) {
        List<Cell> cells = new ArrayList<>();
        Map<Long, Cell> seen = new HashMap<>();
        Deque<Cell> stack = new ArrayDeque<>();
        cells.add(start);
        seen.put(cellKey(start.i, start.j), start);
        stack.push(start);
        int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!stack.isEmpty()) {
            Cell cell = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] step : steps) {
                int i = cell.i + step[0], j = cell.j + step[1];
                if (Math.abs(i) > GRID || Math.abs(j) > GRID || seen.containsKey(cellKey(i, j))) continue;
                if (fits(level, i, j)) options.add(new int[] { i, j });
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] pick = options
                .get((int) (noise.unit(cell.i * 13 + cell.j, level * 7 + cells.size(), 9) * options.size()));
            Cell next = new Cell(level, pick[0], pick[1]);
            next.parent = cell;
            next.depth = cell.depth + 1;
            cell.children.add(next);
            seen.put(cellKey(next.i, next.j), next);
            cells.add(next);
            stack.push(next);
        }
        return cells;
    }

    private static long cellKey(int i, int j) {
        return ((long) i << 32) ^ (j & 0xffffffffL);
    }

    private void corridorShell(Cell a, Cell b) {
        int f = a.floor();
        int[] box = corridorBox(a, b);
        shell(
            box[0] - (box[4] == 0 ? 0 : 1),
            f,
            box[2] - (box[4] == 0 ? 1 : 0),
            box[1] + (box[4] == 0 ? 0 : 1),
            f + 4,
            box[3] + (box[4] == 0 ? 1 : 0),
            false);
    }

    private void corridorInside(Cell a, Cell b) {
        int f = a.floor();
        int[] box = corridorBox(a, b);
        clear(box[0], f + 1, box[2], box[1], f + 3, box[3]);
    }

    /** {minX, maxX, minZ, maxZ, axis}: the corridor's inside from wall to wall; axis 0 runs along x, 1 along z. */
    private int[] corridorBox(Cell a, Cell b) {
        if (a.j == b.j) {
            int x0 = Math.min(a.x(), b.x()) + ROOM_HALF, x1 = Math.max(a.x(), b.x()) - ROOM_HALF;
            return new int[] { x0, x1, a.z() - 1, a.z() + 1, 0 };
        }
        int z0 = Math.min(a.z(), b.z()) + ROOM_HALF, z1 = Math.max(a.z(), b.z()) - ROOM_HALF;
        return new int[] { a.x() - 1, a.x() + 1, z0, z1, 1 };
    }

    private void shaftShell(int x, int z, int y0, int y1) {
        shell(x - 1, y0 - 1, z - 1, x + 1, y1, z + 1, false);
    }

    private void ladder(int x, int z, int y0, int y1, int meta) {
        for (int y = y0; y <= y1; y++) put(x, y, z, Block.LADDER, meta, null, null);
    }

    /** The ruined gateway over the shaft: a floor, broken corner pillars, a lintel if two stand whole. */
    private void gateway(int x, int y, int z) {
        for (int dx = -GATEWAY_HALF; dx <= GATEWAY_HALF; dx++) for (int dz = -GATEWAY_HALF; dz <= GATEWAY_HALF; dz++) {
            stone(x + dx, y - 1, z + dz);
            for (int dy = 0; dy <= 5; dy++) put(x + dx, y + dy, z + dz, Block.AIR);
        }
        int[][] corners = { { -GATEWAY_HALF, -GATEWAY_HALF }, { GATEWAY_HALF, -GATEWAY_HALF },
            { -GATEWAY_HALF, GATEWAY_HALF }, { GATEWAY_HALF, GATEWAY_HALF } };
        boolean[] whole = new boolean[4];
        for (int c = 0; c < 4; c++) {
            int broken = (int) (noise.unit(x + c, z, 10) * 3);
            whole[c] = broken == 0;
            for (int dy = 0; dy < 4 - broken; dy++) stone(x + corners[c][0], y + dy, z + corners[c][1]);
        }
        if (whole[0] && whole[1])
            for (int dx = -GATEWAY_HALF; dx <= GATEWAY_HALF; dx++) stone(x + dx, y + 4, z - GATEWAY_HALF);
    }

    private void furnish(int x, int f, int z, Role role) {
        put(x, f + ROOM_HEIGHT, z, Block.LAMP);
        switch (role) {
            case GUARD:
                put(x, f + 1, z, Block.SPAWNER, 0, null, mob(x, z));
                break;
            case STORE:
                put(x - 2, f + 1, z + 2, Block.CHEST, 0, Loot.STORE, null);
                if (noise.unit(x, z, 11) < 0.5) put(x + 2, f + 1, z + 2, Block.CHEST, 0, Loot.STORE, null);
                break;
            case KEY:
                put(x, f + 1, z + 2, Block.CHEST, 0, Loot.KEY, null);
                break;
            case VAULT:
                put(x, f + ROOM_HEIGHT, z, Block.WARDED); // no lamp in the vault's warded ceiling: it stays dark
                put(x, f + 1, z, Block.CHEST, 0, Loot.VAULT_TREASURE, null);
                put(x + 2, f + 1, z + 2, Block.CHEST, 0, Loot.VAULT_RICH, null);
                put(x - 2, f + 1, z + 2, Block.LAMP);
                break;
            default:
                break;
        }
    }

    /** The vault's doorway (where the corridor meets its wall) is filled with the sealed gate. */
    private void vaultGate(Cell outer, Cell vault) {
        int f = vault.floor();
        int[] box = corridorBox(outer, vault);
        if (box[4] == 0) {
            int x = vault.x() > outer.x() ? vault.x() - ROOM_HALF : vault.x() + ROOM_HALF;
            for (int z = box[2]; z <= box[3]; z++) for (int y = f + 1; y <= f + 3; y++) put(x, y, z, Block.GATE);
        } else {
            int z = vault.z() > outer.z() ? vault.z() - ROOM_HALF : vault.z() + ROOM_HALF;
            for (int x = box[0]; x <= box[1]; x++) for (int y = f + 1; y <= f + 3; y++) put(x, y, z, Block.GATE);
        }
    }

    /** Three in ten corridors are trapped: arrows from the walls when you step on a plate, or a lava pit. */
    private void trap(Cell a, Cell b) {
        double roll = noise.unit(a.x() + b.x(), a.z() + b.z(), 12);
        if (roll >= 0.3) return;
        int f = a.floor();
        int[] box = corridorBox(a, b);
        if (box[4] == 0 ? box[1] - box[0] < 2 : box[3] - box[2] < 2) return;
        int mx = (box[0] + box[1]) / 2, mz = (box[2] + box[3]) / 2;
        if (roll < 0.18) {
            if (box[4] == 0) {
                for (int dz = -1; dz <= 1; dz++) put(mx, f + 1, mz + dz, Block.PRESSURE_PLATE);
                put(mx, f + 1, mz - 2, Block.ARROW_TRAP, SOUTH, null, null);
                put(mx, f + 1, mz + 2, Block.ARROW_TRAP, NORTH, null, null);
            } else {
                for (int dx = -1; dx <= 1; dx++) put(mx + dx, f + 1, mz, Block.PRESSURE_PLATE);
                put(mx - 2, f + 1, mz, Block.ARROW_TRAP, EAST, null, null);
                put(mx + 2, f + 1, mz, Block.ARROW_TRAP, WEST, null, null);
            }
        } else {
            // A lava pit across the corridor with a one-wide ledge down its middle.
            for (int s = -1; s <= 1; s++) for (int side : new int[] { -1, 1 }) {
                int x = box[4] == 0 ? mx + s : mx + side, z = box[4] == 0 ? mz + side : mz + s;
                put(x, f, z, Block.AIR);
                put(x, f - 1, z, Block.LAVA);
                stone(x, f - 2, z);
            }
            for (int s = -1; s <= 1; s++) for (int side : new int[] { -2, 2 }) {
                int x = box[4] == 0 ? mx + s : mx + side, z = box[4] == 0 ? mz + side : mz + s;
                stone(x, f - 1, z);
            }
        }
    }

    // --- Citadel ----------------------------------------------------------------------------------------------

    static final int TOWER_HALF = 3;
    static final int TOWER_HEIGHT = 12;
    static final int WALL_HEIGHT = 7;
    static final int KEEP_HALF = 7;
    static final int KEEP_FLOOR_HEIGHT = 6;
    static final int VAULT_DEPTH = 10;

    private void buildCitadel() {
        int cx = island.centreX(), cz = island.centreZ();
        int b = island.centreY() + 1; // the citadel's floor level
        int h = CITADEL_HALF;
        int kx = cx, kz = cz - 3; // the keep stands back from the gate
        int vaultX = kx - 5, anteX = kx + 6, vf = b - VAULT_DEPTH;

        // Pass 1: shells and solid parts.
        for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++) stone(cx + dx, b, cz + dz);
        for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++) {
            if (Math.abs(dx) != h && Math.abs(dz) != h) continue;
            for (int y = b + 1; y <= b + WALL_HEIGHT; y++) stone(cx + dx, y, cz + dz);
            if ((dx + dz) % 2 == 0) stone(cx + dx, b + WALL_HEIGHT + 1, cz + dz);
        }
        int[][] towers = { { -h, -h }, { h, -h }, { -h, h }, { h, h } };
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            shell(tx - TOWER_HALF, b, tz - TOWER_HALF, tx + TOWER_HALF, b + TOWER_HEIGHT, tz + TOWER_HALF, false);
            for (int dx = -TOWER_HALF; dx <= TOWER_HALF; dx++) for (int dz = -TOWER_HALF; dz <= TOWER_HALF; dz++) {
                boolean ring = Math.abs(dx) == TOWER_HALF || Math.abs(dz) == TOWER_HALF;
                if (ring && (dx + dz) % 2 == 0) stone(tx + dx, b + TOWER_HEIGHT + 1, tz + dz);
            }
        }
        int keepTop = b + 3 * KEEP_FLOOR_HEIGHT;
        shell(kx - KEEP_HALF, b, kz - KEEP_HALF, kx + KEEP_HALF, keepTop, kz + KEEP_HALF, false);
        for (int floor = 1; floor < 3; floor++) for (int dx = -KEEP_HALF; dx <= KEEP_HALF; dx++)
            for (int dz = -KEEP_HALF; dz <= KEEP_HALF; dz++) stone(kx + dx, b + floor * KEEP_FLOOR_HEIGHT, kz + dz);
        for (int dx = -KEEP_HALF; dx <= KEEP_HALF; dx++) for (int dz = -KEEP_HALF; dz <= KEEP_HALF; dz++) {
            boolean ring = Math.abs(dx) == KEEP_HALF || Math.abs(dz) == KEEP_HALF;
            if (ring && (dx + dz) % 2 == 0) stone(kx + dx, keepTop + 1, kz + dz);
        }
        // Basement: the antechamber, the vault beside it, the shaft down from the keep, the short corridor between.
        shell(anteX - ROOM_HALF, vf, kz - ROOM_HALF, anteX + ROOM_HALF, vf + ROOM_HEIGHT, kz + ROOM_HALF, false);
        shaftShell(anteX, kz - 3, vf + 1, b);
        for (int x = vaultX + ROOM_HALF; x <= anteX - ROOM_HALF; x++)
            for (int y = vf; y <= vf + 4; y++) for (int z = kz - 2; z <= kz + 2; z++) {
                boolean edge = y == vf || y == vf + 4 || Math.abs(z - kz) == 2;
                if (edge) stone(x, y, z);
            }
        shell(vaultX - ROOM_HALF, vf, kz - ROOM_HALF, vaultX + ROOM_HALF, vf + ROOM_HEIGHT, kz + ROOM_HALF, true);
        // The well in the courtyard.
        int wx = cx + 9, wz = cz + 10;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (int y = b - 3; y <= b; y++) stone(wx + dx, y, wz + dz);
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) stone(wx + dx, b + 1, wz + dz);
        }

        // Pass 2: insides.
        clearCourtyard(cx, cz, b, kx, kz, towers);
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            clear(
                tx - TOWER_HALF + 1,
                b + 1,
                tz - TOWER_HALF + 1,
                tx + TOWER_HALF - 1,
                b + TOWER_HEIGHT - 1,
                tz + TOWER_HALF - 1);
            // The door faces the courtyard, two blocks in from the outer wall so it does not breach it.
            int doorX = tx - Integer.signum(t[0]) * TOWER_HALF, doorZ = tz - Integer.signum(t[1]) * 2;
            for (int y = b + 1; y <= b + 2; y++) put(doorX, y, doorZ, Block.AIR);
        }
        for (int floor = 0; floor < 3; floor++) {
            int f = b + floor * KEEP_FLOOR_HEIGHT;
            clear(
                kx - KEEP_HALF + 1,
                f + 1,
                kz - KEEP_HALF + 1,
                kx + KEEP_HALF - 1,
                f + KEEP_FLOOR_HEIGHT - 1,
                kz + KEEP_HALF - 1);
        }
        for (int dx = -1; dx <= 1; dx++) for (int y = b + 1; y <= b + 3; y++) put(cx + dx, y, cz + h, Block.AIR); // gate
        for (int y = b + 1; y <= b + 2; y++) put(kx, y, kz + KEEP_HALF, Block.AIR); // keep door
        clear(
            anteX - ROOM_HALF + 1,
            vf + 1,
            kz - ROOM_HALF + 1,
            anteX + ROOM_HALF - 1,
            vf + ROOM_HEIGHT - 1,
            kz + ROOM_HALF - 1);
        clear(
            vaultX - ROOM_HALF + 1,
            vf + 1,
            kz - ROOM_HALF + 1,
            vaultX + ROOM_HALF - 1,
            vf + ROOM_HEIGHT - 1,
            kz + ROOM_HALF - 1);
        clear(vaultX + ROOM_HALF, vf + 1, kz - 1, anteX - ROOM_HALF, vf + 3, kz + 1);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            put(wx + dx, b, wz + dz, Block.WATER);
            put(wx + dx, b - 1, wz + dz, Block.WATER);
            put(wx + dx, b - 2, wz + dz, Block.WATER);
        }

        // Pass 3: ladders, gates, lamps, chests, spawners, traps, windows.
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            int lx = tx + Integer.signum(t[0]) * (TOWER_HALF - 1);
            ladder(lx, tz, b + 1, b + TOWER_HEIGHT, t[0] > 0 ? LADDER_ON_EAST_WALL : LADDER_ON_WEST_WALL);
            put(tx, b + TOWER_HEIGHT - 1, tz, Block.LAMP);
        }
        // Key on the north-east tower's roof; a storeroom chest in the north-west tower; a guard in the south-west.
        put(cx + h - 1, b + TOWER_HEIGHT + 1, cz - h + 1, Block.CHEST, 0, Loot.KEY, null);
        put(cx - h + 1, b + 1, cz - h + 1, Block.CHEST, 0, Loot.STORE, null);
        put(cx - h, b + 1, cz + h, Block.SPAWNER, 0, null, mob(cx - h, cz + h));
        int ladderX = kx - KEEP_HALF + 1;
        ladder(ladderX, kz, b + 1, keepTop, LADDER_ON_WEST_WALL);
        for (int floor = 0; floor < 3; floor++) {
            int f = b + floor * KEEP_FLOOR_HEIGHT;
            put(kx, f + KEEP_FLOOR_HEIGHT, kz, Block.LAMP);
            for (int[] w : new int[][] { { KEEP_HALF, 0 }, { -KEEP_HALF, 0 }, { 0, -KEEP_HALF } })
                put(kx + w[0], f + 3, kz + w[1], Block.GLASS_PANE);
        }
        put(kx + 2, b + 1, kz, Block.SPAWNER, 0, null, mob(kx, kz));
        put(kx - 3, b + KEEP_FLOOR_HEIGHT + 1, kz + 3, Block.CHEST, 0, Loot.STORE, null);
        put(kx + 3, b + KEEP_FLOOR_HEIGHT + 1, kz + 3, Block.CHEST, 0, Loot.STORE, null);
        put(kx + 2, b + 2 * KEEP_FLOOR_HEIGHT + 1, kz, Block.SPAWNER, 0, null, mob(kx + 1, kz));
        // Down to the basement: a hole in the keep floor over the shaft, a guarded antechamber, a trapped corridor.
        ladder(anteX, kz - 3, vf + 1, b, LADDER_ON_NORTH_WALL);
        furnish(anteX, vf, kz, Role.GUARD);
        int midX = (vaultX + ROOM_HALF + anteX - ROOM_HALF) / 2;
        for (int dz = -1; dz <= 1; dz++) put(midX, vf + 1, kz + dz, Block.PRESSURE_PLATE);
        put(midX, vf + 1, kz - 2, Block.ARROW_TRAP, SOUTH, null, null);
        put(midX, vf + 1, kz + 2, Block.ARROW_TRAP, NORTH, null, null);
        furnish(vaultX, vf, kz, Role.VAULT);
        for (int z = kz - 1; z <= kz + 1; z++)
            for (int y = vf + 1; y <= vf + 3; y++) put(vaultX + ROOM_HALF, y, z, Block.GATE);

        entrance = new int[] { cx, b + 1, cz + h + 1 };
        rooms.add(new Room(cx, b, cz + h, Role.ENTRANCE));
        rooms.add(new Room(kx, b, kz, Role.GUARD));
        rooms.add(new Room(kx, b + KEEP_FLOOR_HEIGHT, kz, Role.STORE));
        rooms.add(new Room(cx + h, b, cz - h, Role.KEY));
        rooms.add(new Room(anteX, vf, kz, Role.GUARD));
        rooms.add(new Room(vaultX, vf, kz, Role.VAULT));
    }

    /** Clears the courtyard: everything inside the walls up to well above them, except the keep and towers. */
    private void clearCourtyard(int cx, int cz, int b, int kx, int kz, int[][] towers) {
        int h = CITADEL_HALF;
        for (int dx = -h + 1; dx <= h - 1; dx++) for (int dz = -h + 1; dz <= h - 1; dz++) {
            int x = cx + dx, z = cz + dz;
            if (Math.abs(x - kx) <= KEEP_HALF && Math.abs(z - kz) <= KEEP_HALF) continue;
            boolean inTower = false;
            for (int[] t : towers)
                inTower |= Math.abs(x - (cx + t[0])) <= TOWER_HALF && Math.abs(z - (cz + t[1])) <= TOWER_HALF;
            if (inTower) continue;
            if (Math.abs(x - (cx + 9)) <= 2 && Math.abs(z - (cz + 10)) <= 2) continue; // the well
            for (int y = b + 1; y <= b + 20; y++) put(x, y, z, Block.AIR);
        }
    }
}
