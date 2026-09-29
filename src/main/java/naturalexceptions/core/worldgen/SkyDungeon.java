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
        GATE,
        BOOKSHELF,
        COBWEB,
        /** Meta 0 cobblestone, 1 gravel, 2 mossy cobblestone. */
        RUBBLE,
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
        QUARTZ,
        LOG,
        LEAVES
    }

    /** Dispenser meta flag: loaded with fire charges. */
    public static final int FIRE = 8;
    /** Skystone meta of a loose secret wall. */
    public static final int LOOSE = 3;

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
        /** Between the Warden's hall and the vault, sealed by the gate. */
        VAULT,
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
    private final Map<Long, Piece> pieces = new LinkedHashMap<>();
    final List<Room> rooms = new ArrayList<>();
    final List<Connection> connections = new ArrayList<>();
    int[] entrance;
    /** {minX, minZ, maxX, maxZ} of what stands on the island's surface. */
    int[] surface;
    private volatile Map<Long, List<Piece>> byChunk;

    private SkyDungeon(SkyIslands field, SkyIslands.Island island, Kind kind) {
        this.field = field;
        this.island = island;
        this.kind = kind;
        this.noise = new LandmarkNoise(field.seed() ^ 0x534b5944554e4745L ^ island.cellX() * 31 ^ island.cellZ());
    }

    // --- Cache ------------------------------------------------------------------------------------------------

    private static final int CACHE_SIZE = 4;
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
        return Collections.unmodifiableCollection(pieces.values());
    }

    /** The pieces inside one chunk. */
    public List<Piece> piecesInChunk(int chunkX, int chunkZ) {
        Map<Long, List<Piece>> index = byChunk;
        if (index == null) {
            synchronized (this) {
                if (byChunk == null) {
                    Map<Long, List<Piece>> built = new HashMap<>();
                    for (Piece piece : pieces.values())
                        built.computeIfAbsent(chunkKey(piece.x >> 4, piece.z >> 4), k -> new ArrayList<>())
                            .add(piece);
                    byChunk = built;
                }
                index = byChunk;
            }
        }
        List<Piece> found = index.get(chunkKey(chunkX, chunkZ));
        return found == null ? Collections.<Piece>emptyList() : found;
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
        return pieces.get(key(x, y, z));
    }

    // --- Writing blocks (used by the layouts) ---------------------------------------------------------------

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    void put(int x, int y, int z, Block block, int meta, Loot loot, String mob) {
        long k = key(x, y, z);
        pieces.remove(k); // keep insertion order meaningful: the latest write is the one that stands
        pieces.put(k, new Piece(x, y, z, block, meta, loot, mob));
    }

    void put(int x, int y, int z, Block block) {
        put(x, y, z, block, 0, null, null);
    }

    void put(int x, int y, int z, Block block, int meta) {
        put(x, y, z, block, meta, null, null);
    }

    /** Skystone, weathered by hash: mostly whole bricks, some cracked, some mossy. */
    void stone(int x, int y, int z) {
        double roll = noise.unit3(x, y, z, 1);
        put(x, y, z, Block.SKYSTONE, roll < 0.15 ? 1 : roll < 0.27 ? 2 : 0);
    }

    /** A hollow box: its shell in stone (or warded). */
    void shell(int x0, int y0, int z0, int x1, int y1, int z1, boolean warded) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
            if (!edge) continue;
            if (warded) put(x, y, z, Block.WARDED);
            else stone(x, y, z);
        }
    }

    void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) put(x, y, z, Block.AIR);
    }

    static final String[] DEFAULT_MOBS = { "Zombie", "Skeleton", "Spider" };
}
