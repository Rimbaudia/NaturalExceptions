package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import naturalexceptions.core.worldgen.dungeon.RoomTemplate;

/**
 * A sky dungeon on a grand island, as a list of blocks in world coordinates. Two kinds:
 * <ul>
 * <li><b>Sanctum</b>: a ruined gateway on the island's top over a shaft into up to three levels of authored rooms
 * carved through the rock ({@link SanctumLayout}): loops and side wings, secret closets behind loose walls, a key
 * room far from the vault, and the Sky Warden's hall before the vault.</li>
 * <li><b>Citadel</b>: a walled fort on the island's top ({@link CitadelLayout}) with the vault in a basement.</li>
 * </ul>
 * Pure: the same island always gives the same dungeon, so every chunk writes its own share of one whole.
 */
public final class SkyDungeon {

    public enum Kind {
        SANCTUM,
        CITADEL
    }

    /** Half the width of a citadel's walls (its towers reach three further). */
    public static final int CITADEL_HALF = 15;
    public static final int TOWER_HALF = 3;
    public static final int GATEWAY_HALF = 3;

    public enum Block {
        /** Skystone Bricks; meta 0 whole, 1 cracked, 2 mossy, 3 loose (a secret wall). */
        SKYSTONE,
        /** Warded Skystone: the vault's unbreakable shell. */
        WARDED,
        AIR,
        LAMP,
        /** Meta: vanilla ladder facing (2..5). */
        LADDER,
        GLASS_PANE,
        CHEST,
        SPAWNER,
        /** Meta: vanilla facing (2..5); plus {@link #FIRE} for fire charges instead of arrows. */
        DISPENSER,
        TRIGGER,
        CRUMBLING,
        /** Meta: Gale Vent meta (the side it blows towards, lowest bit flipped). */
        VENT,
        /** Meta: Sealed Alcove meta (creature and armour). */
        ALCOVE,
        SEAL,
        TNT,
        WATER,
        LAVA,
        /** Sealed Vault Gate; meta {@link SkyDungeon#KEY_GATE} or {@link SkyDungeon#WARDEN_BOUND}. */
        GATE,
        BOOKSHELF,
        COBWEB,
        IRON_BARS,
        GRASS,
        TALL_GRASS,
        /** Meta 0 poppy, 1 dandelion. */
        FLOWER,
        WINDBLOOM,
        CAULDRON,
        /** Meta: vanilla facing (2..5). */
        FURNACE,
        CRAFTING_TABLE,
        ANVIL,
        FENCE,
        /** Quartz; meta 0 pillar, 1 chiseled, 2 plain. */
        QUARTZ,
        LOG,
        LEAVES,
        /** Spruce stairs; meta: vanilla stairs meta (0 up to the east, 1 west, 2 south, 3 north; +4 upside down). */
        STAIRS,
        /** Spruce slab; meta 0 bottom half, 8 top half. */
        SLAB,
        /** A wooden door's lower half; meta: vanilla door facing (0..3). The upper half is written with it. */
        DOOR,
        /** Planks; meta 0 oak, 1 spruce. */
        PLANKS,
        /** Wool; meta: vanilla colour. */
        WOOL,
        HAY,
        GLASS,
        GRAVEL,
        /** Skystone Tiles, the floors; meta 1 cracked. */
        TILES,
        /** Polished Skystone: ceilings, plinths, sills. */
        POLISHED,
        /** Chiseled Skystone: keystones, altar faces, lamp surrounds. */
        CHISELED,
        /** Skystone Pillar; meta: vanilla pillar axis (0 upright, 4 along x, 8 along z). */
        PILLAR,
        /** Skystone Brick Stairs; meta as {@link #STAIRS}. */
        SKY_STAIRS,
        /** Skystone slab; meta 0 brick, 1 tile, 2 polished; plus 8 for the top half. */
        SKY_SLAB,
        /** Half a bed; meta: the way from its foot to its head ({@link #NORTH} ..), plus 8 for the head. */
        BED,
        /** Carpet; meta: vanilla colour. */
        CARPET,
        /** Torch; meta: the wall it hangs on ({@link #NORTH} ..), or {@link #STANDING}. */
        TORCH,
        /** Flower pot; meta: which plant ({@link #POT_PLANTS}). */
        FLOWER_POT,
        /** Trapdoor, closed, in the top half of its block (a shelf); meta: the wall it is hinged to. */
        TRAPDOOR,
        /** Fence gate, closed; meta: the way it faces ({@link #NORTH} ..). */
        FENCE_GATE,
        /** Wooden pressure plate (a table's top, on a fence). */
        PRESSURE_PLATE,
        BREWING_STAND,
        ENCHANTING_TABLE,
        JUKEBOX,
        /** Stained glass pane; meta: vanilla colour. */
        STAINED_GLASS_PANE
    }

    /** Directions for beds, torches, trapdoors and fence gates. */
    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
    /** Torch meta: standing on the floor. */
    public static final int STANDING = 4;
    /** What a flower pot holds, by meta: poppy, dandelion, fern, oak sapling, blue orchid, allium. */
    public static final int POT_PLANTS = 6;

    /** Dispenser meta flag: loaded with fire charges. */
    public static final int FIRE = 8;
    /** Skystone meta of a loose secret wall. */
    public static final int LOOSE = 3;
    /** Gate meta: opened by a Sky Key. */
    public static final int KEY_GATE = 0;
    /** Gate meta: bound to the Sky Warden, opened only when it falls. */
    public static final int WARDEN_BOUND = 1;

    public enum Loot {
        STORE,
        KEY,
        VAULT_TREASURE,
        VAULT_RICH
    }

    /** How a corridor looks. */
    public enum Style {
        PLAIN,
        ARCHED,
        CRAMPED,
        BRIDGE,
        FLOODED,
        COLLAPSED,
        /** Behind a loose wall, to a secret closet. */
        SECRET,
        /** Between the Warden's hall and the vault, sealed by a gate that opens when the Warden falls. */
        VAULT,
        /** The one way into the Warden's hall, sealed by a gate the Sky Key opens. */
        GATED,
        /** A ladder shaft (half the time with a gale lift) between levels. */
        SHAFT
    }

    public enum Trap {
        NONE,
        VOLLEY,
        FIRE_VOLLEY,
        CRUMBLE,
        GALE,
        TNT,
        AMBUSH
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

        /** The middle of the room and its floor. */
        public final int x;
        public final int floorY;
        public final int z;
        public final RoomTemplate.Role role;
        /** The authored room it was built from, or null for a citadel's own parts. */
        public final String template;
        public final int level;
        /** Its inside, walls excluded, and how many layers are open above the floor. */
        public final int minX, maxX, minZ, maxZ, height;
        /** Whether a trap was built inside it (it counts against the trap budget). */
        public final boolean trapped;

        Room(int x, int floorY, int z, RoomTemplate.Role role, String template, int level, int minX, int maxX, int minZ,
            int maxZ, int height, boolean trapped) {
            this.x = x;
            this.floorY = floorY;
            this.z = z;
            this.role = role;
            this.template = template;
            this.level = level;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.height = height;
            this.trapped = trapped;
        }

        /** A room of a citadel's own making: known by its middle, floor and extent. */
        Room(int x, int floorY, int z, RoomTemplate.Role role, int half, int height) {
            this(x, floorY, z, role, null, 0, x - half, x + half, z - half, z + half, height, false);
        }
    }

    public static final class Connection {

        public final Room a;
        public final Room b;
        public final Style style;
        public final Trap trap;
        /** On the way from the entrance to the key and on to the Warden. */
        public final boolean critical;

        Connection(Room a, Room b, Style style, Trap trap, boolean critical) {
            this.a = a;
            this.b = b;
            this.style = style;
            this.trap = trap;
            this.critical = critical;
        }
    }

    private final Kind kind;
    final SkyIslands field;
    final SkyIslands.Island island;
    final LandmarkNoise noise;
    /**
     * The blocks, packed: a dungeon holds a couple of hundred thousand, and several may be cached while chunks
     * generate, so each is a position (long) and its data (int) in parallel arrays, found through an open hash.
     */
    private long[] positions = new long[1 << 12];
    private int[] data = new int[1 << 12];
    private int count;
    private long[] table = new long[1 << 13];
    private int[] tableIndex = new int[1 << 13];
    private final List<String> mobNames = new ArrayList<>();
    final List<Room> rooms = new ArrayList<>();
    final List<Connection> connections = new ArrayList<>();
    int[] entrance;
    /** {minX, minZ, maxX, maxZ} of what stands on the island's surface. */
    int[] surface;
    private volatile Map<Long, int[]> byChunk;

    private SkyDungeon(SkyIslands field, SkyIslands.Island island, Kind kind) {
        this.field = field;
        this.island = island;
        this.kind = kind;
        this.noise = new LandmarkNoise(field.seed() ^ 0x534b5944554e4745L ^ island.cellX() * 31 ^ island.cellZ());
    }

    // --- Cache ------------------------------------------------------------------------------------------------

    private static final int CACHE_SIZE = 3;
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
        if (kind == Kind.SANCTUM) new SanctumLayout(dungeon).build();
        else new CitadelLayout(dungeon).build();
        synchronized (CACHE) {
            CACHE.put(key, dungeon);
        }
        return dungeon;
    }

    public Kind kind() {
        return kind;
    }

    public Collection<Piece> pieces() {
        return new java.util.AbstractList<Piece>() {

            @Override
            public Piece get(int i) {
                return piece(i);
            }

            @Override
            public int size() {
                return count;
            }
        };
    }

    /** The pieces inside one chunk. */
    public List<Piece> piecesInChunk(int chunkX, int chunkZ) {
        Map<Long, int[]> index = byChunk;
        if (index == null) {
            synchronized (this) {
                if (byChunk == null) {
                    Map<Long, int[]> sizes = new HashMap<>();
                    for (int i = 0; i < count; i++) {
                        long k = chunkKey(unpackX(positions[i]) >> 4, unpackZ(positions[i]) >> 4);
                        int[] c = sizes.get(k);
                        if (c == null) sizes.put(k, c = new int[1]);
                        c[0]++;
                    }
                    Map<Long, int[]> built = new HashMap<>();
                    Map<Long, int[]> fill = new HashMap<>();
                    for (Map.Entry<Long, int[]> e : sizes.entrySet()) {
                        built.put(e.getKey(), new int[e.getValue()[0]]);
                        fill.put(e.getKey(), new int[1]);
                    }
                    for (int i = 0; i < count; i++) {
                        long k = chunkKey(unpackX(positions[i]) >> 4, unpackZ(positions[i]) >> 4);
                        built.get(k)[fill.get(k)[0]++] = i;
                    }
                    byChunk = built;
                }
                index = byChunk;
            }
        }
        final int[] found = index.get(chunkKey(chunkX, chunkZ));
        if (found == null) return Collections.<Piece>emptyList();
        return new java.util.AbstractList<Piece>() {

            @Override
            public Piece get(int i) {
                return piece(found[i]);
            }

            @Override
            public int size() {
                return found.length;
            }
        };
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    public List<Room> rooms() {
        return Collections.unmodifiableList(rooms);
    }

    public List<Connection> connections() {
        return Collections.unmodifiableList(connections);
    }

    /** Where a player enters: the top of the sanctum's shaft, or outside the citadel's gate. */
    public int[] entrance() {
        return entrance.clone();
    }

    /** Whether the dungeon stands on this column of the island's surface (no plants or trees there). */
    public boolean coversSurface(int x, int z) {
        return surface != null && x >= surface[0] && x <= surface[2] && z >= surface[1] && z <= surface[3];
    }

    public Piece at(int x, int y, int z) {
        int i = find(key(x, y, z));
        return i < 0 ? null : piece(i);
    }

    private Piece piece(int i) {
        long k = positions[i];
        int v = data[i];
        int loot = (v >>> 16) & 0x7, mob = v >>> 19;
        return new Piece(
            unpackX(k),
            unpackY(k),
            unpackZ(k),
            BLOCKS[v & 0xFF],
            (v >>> 8) & 0xFF,
            loot == 0 ? null : LOOTS[loot - 1],
            mob == 0 ? null : mobNames.get(mob - 1));
    }

    private static final Block[] BLOCKS = Block.values();
    private static final Loot[] LOOTS = Loot.values();

    private static int unpackX(long k) {
        return (int) (k >> 38);
    }

    private static int unpackY(long k) {
        return (int) ((k >>> 26) & 0xFFF);
    }

    private static int unpackZ(long k) {
        return (int) (k << 38 >> 38);
    }

    /** Index of the piece at a packed position, or -1. */
    private int find(long k) {
        int mask = table.length - 1;
        for (int h = mix(k) & mask;; h = (h + 1) & mask) {
            int i = tableIndex[h];
            if (i == 0) return -1;
            if (table[h] == k) return i - 1;
        }
    }

    private static int mix(long k) {
        long h = k * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }

    private void grow() {
        long[] oldTable = table;
        int[] oldIndex = tableIndex;
        table = new long[oldTable.length * 2];
        tableIndex = new int[oldIndex.length * 2];
        int mask = table.length - 1;
        for (int h = 0; h < oldTable.length; h++) {
            if (oldIndex[h] == 0) continue;
            int n = mix(oldTable[h]) & mask;
            while (tableIndex[n] != 0) n = (n + 1) & mask;
            table[n] = oldTable[h];
            tableIndex[n] = oldIndex[h];
        }
    }

    // --- Writing blocks (used by the layouts) ---------------------------------------------------------------

    /** x in the top 26 bits (signed), y in the next 12, z in the low 26 (signed). */
    private static long key(int x, int y, int z) {
        return ((long) x << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFFL);
    }

    void put(int x, int y, int z, Block block, int meta, Loot loot, String mob) {
        long k = key(x, y, z);
        int mobIndex = 0;
        if (mob != null) {
            mobIndex = mobNames.indexOf(mob) + 1;
            if (mobIndex == 0) {
                mobNames.add(mob);
                mobIndex = mobNames.size();
            }
        }
        int v = block.ordinal() | (meta & 0xFF) << 8 | (loot == null ? 0 : loot.ordinal() + 1) << 16 | mobIndex << 19;
        int i = find(k);
        if (i >= 0) { // the latest write is the one that stands
            data[i] = v;
            return;
        }
        if (count == positions.length) {
            positions = java.util.Arrays.copyOf(positions, count * 2);
            data = java.util.Arrays.copyOf(data, count * 2);
        }
        positions[count] = k;
        data[count] = v;
        count++;
        if (count * 2 > table.length) grow();
        int mask = table.length - 1;
        int h = mix(k) & mask;
        while (tableIndex[h] != 0) h = (h + 1) & mask;
        table[h] = k;
        tableIndex[h] = count;
    }

    void put(int x, int y, int z, Block block) {
        put(x, y, z, block, 0, null, null);
    }

    void put(int x, int y, int z, Block block, int meta) {
        put(x, y, z, block, meta, null, null);
    }

    /** Skystone, weathered by hash: mostly whole bricks, some cracked, some mossy. */
    /** Wall masonry: Skystone Bricks, now and then cracked or mossy. */
    void stone(int x, int y, int z) {
        double roll = noise.unit3(x, y, z, 1);
        put(x, y, z, Block.SKYSTONE, roll < 0.12 ? 1 : roll < 0.2 ? 2 : 0);
    }

    /** A floor: Skystone Tiles, one in ten cracked. */
    void floorTile(int x, int y, int z) {
        put(x, y, z, Block.TILES, noise.unit3(x, y, z, 2) < 0.1 ? 1 : 0);
    }

    /** A ceiling, plinth or sill: Polished Skystone. */
    void polished(int x, int y, int z) {
        put(x, y, z, Block.POLISHED);
    }

    /** An upright Skystone Pillar. */
    void pillar(int x, int y, int z) {
        put(x, y, z, Block.PILLAR);
    }

    /** Whether this block is dungeon masonry (walls, floors, ceilings, trim): what a wall may be built of. */
    public static boolean masonry(Block block) {
        switch (block) {
            case SKYSTONE:
            case TILES:
            case POLISHED:
            case CHISELED:
            case PILLAR:
            case WARDED:
                return true;
            default:
                return false;
        }
    }

    /**
     * A hollow box: floor tiles below, a polished ceiling above, and brick walls standing on a polished plinth; or
     * all warded.
     */
    void shell(int x0, int y0, int z0, int x1, int y1, int z1, boolean warded) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
            if (!edge) continue;
            if (warded) put(x, y, z, Block.WARDED);
            else if (y == y0) floorTile(x, y, z);
            else if (y == y1) polished(x, y, z);
            else if (y == y0 + 1) polished(x, y, z);
            else stone(x, y, z);
        }
    }

    void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) put(x, y, z, Block.AIR);
    }

    static final String[] DEFAULT_MOBS = { "Zombie", "Skeleton", "Spider" };
}
