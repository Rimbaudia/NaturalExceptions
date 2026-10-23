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
        this.floorY = floor;
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
                if (c == 'D') door(template, rotation, tx, y, tz, x, wy, z);
                else if (c >= '1' && c <= '8') woodStairs(template, x, wy, z, y, stairs(c, rotation));
                else if (c == ':') {
                    int wall = nearestSolid(template, rotation, tx, y, tz);
                    d.put(x, wy, z, Block.PANTRY_SHELF, wall < 0 ? SkyDungeon.SOUTH : (wall + 2) & 3);
                } else if (c == '\'') {
                    int wall = nearestSolid(template, rotation, tx, y, tz);
                    d.put(x, wy, z, Block.AWNING, template.awning << 2 | (wall < 0 ? SkyDungeon.NORTH : wall));
                } else if (SKY_STAIRS.indexOf(c) >= 0)
                    d.put(x, wy, z, Block.SKY_STAIRS, stairs((char) ('1' + SKY_STAIRS.indexOf(c)), rotation));
                else if (c == 'b' || c == 'u') bed(template, rotation, tx, y, tz, x, wy, z, c == 'b');
                else if (c == 'd') d.put(x, wy, z, Block.TORCH, torch(template, rotation, tx, y, tz));
                else if (c == '[' || c == '\\') {
                    int wall = nearestSolid(template, rotation, tx, y, tz);
                    if (wall < 0) throw new IllegalStateException(template.name + ": a shelf or rack with no wall");
                    d.put(x, wy, z, c == '[' ? Block.WALL_SHELF : Block.WEAPON_RACK, wall);
                } else if (c == ']') d.put(x, wy, z, Block.FENCE_GATE, gate(template, rotation, tx, y, tz));
                else if (c == '(' || c == ')')
                    d.put(x, wy, z, Block.PILLAR, (c == '(') == ((rotation & 1) == 0) ? 4 : 8);
                else if (c == '\u03a8' || c == '\u03c8')
                    d.put(x, wy, z, Block.CANDELABRA, (c == '\u03c8') == ((rotation & 1) == 0) ? 1 : 0);
                else if (PEWS.indexOf(c) >= 0) d.put(x, wy, z, Block.PEW, (PEWS.indexOf(c) + rotation) & 3);
                else if (BENCHES.indexOf(c) >= 0) {
                    // Its working front away from the wall beside it.
                    int wall = nearestSolid(template, rotation, tx, y, tz);
                    int front = wall < 0 ? SkyDungeon.SOUTH : (wall + 2) & 3;
                    d.put(x, wy, z, Block.CUTTING_BENCH, BENCHES.indexOf(c) | front << 3);
                } else if (c == '\u03c4') {
                    int wall = nearestSolid(template, rotation, tx, y, tz);
                    d.put(x, wy, z, Block.TANNING_RACK, wall < 0 ? SkyDungeon.SOUTH : (wall + 2) & 3);
                } else if (CHAIRS.indexOf(c) >= 0)
                    d.put(x, wy, z, Block.CHAIR, template.timber << 2 | ((CHAIRS.indexOf(c) + rotation) & 3));
                else if (c == '\u03c0' || c == '\u03a0')
                    d.put(x, wy, z, Block.BENCH, ((c == '\u03a0' ? 1 : 0) + rotation) & 3);
                else place(
                    c,
                    x,
                    wy,
                    z,
                    tx,
                    tz,
                    w,
                    dp,
                    shellLayer && !template.building,
                    warded,
                    template,
                    role,
                    y == 0 ? LAYER_FLOOR : y == h + 1 ? LAYER_CEILING : LAYER_WALL);
                if (y == 0 && (c == ',' || c == '%')) pits.add(new int[] { x, z });
            }
        }
        if (!pits.isEmpty()) dig(pits, floor, template.pit, minX, minZ, w, dp);
    }

    private static final int LAYER_FLOOR = 0, LAYER_WALL = 1, LAYER_CEILING = 2;
    /** Planks (and log) meta for each timber: oak, spruce, dark oak. */
    private static final int[] PLANKS = { 0, 1, 5 };
    /** The wood of stairs for each timber, as {@link Block#STAIRS} counts it: oak 1, spruce 0, dark oak 2. */
    private static final int[] STAIR_WOOD = { 1, 0, 2 };
    private int floorY;

    /** The template layer of world height {@code y}. */
    private int layerY(RoomTemplate t, int y) {
        return y - floorY;
    }

    /** Stairs of the roof's material from the eaves up, of the timber's wood below. */
    private void woodStairs(RoomTemplate t, int x, int y, int z, int layer, int meta) {
        if (layer >= t.eaves && t.roof == 2) d.put(x, y, z, Block.SHINGLE_STAIRS, meta);
        else if (layer >= t.eaves) d.put(x, y, z, Block.STAIRS, meta | (t.roof == 1 ? 2 : 0) << 3);
        else d.put(x, y, z, Block.STAIRS, meta | STAIR_WOOD[t.timber] << 3);
    }

    private void slab(RoomTemplate t, int x, int y, int z, int layer, boolean top) {
        int half = top ? 8 : 0;
        if (layer >= t.eaves && t.roof == 2) d.put(x, y, z, Block.SKYSLATE_SLAB, 1 | half);
        else if (layer >= t.eaves) d.put(x, y, z, Block.SLAB, (t.roof == 1 ? 5 : 1) | half);
        else d.put(x, y, z, Block.SLAB, PLANKS[t.timber] | half);
    }

    /** Pews with their backs to the north, east, south, west. */
    static final String PEWS = "\u03b1\u03b2\u03b3\u03b4";

    /** The working benches: Mason's Bench, Sawmill, Loom, Potter's Wheel, Glassblower's Furnace. */
    static final String BENCHES = "\u03b9\u03b6\u03b5\u03b7\u03b8";

    /** Wooden chairs (of the timber's wood) with their backs to the north, east, south, west. */
    static final String CHAIRS = "\u03ba\u03bb\u03bc\u03bd";

    /** Skystone stairs rising to the north, east, south, west; then the same upside down. */
    static final String SKY_STAIRS = "ilmnJUVX";

    /** Masonry for '#' and the like: tiles in a floor, polished in a ceiling, bricks between. */
    private void masonry(int x, int y, int z, int layer) {
        if (layer == LAYER_FLOOR) d.floorTile(x, y, z);
        else if (layer == LAYER_CEILING) d.polished(x, y, z);
        else d.stone(x, y, z);
    }

    private void place(char c, int x, int y, int z, int tx, int tz, int w, int dp, boolean shellLayer, boolean warded,
        RoomTemplate template, RoomTemplate.Role role, int layer) {
        switch (c) {
            case '.':
                d.put(x, y, z, Block.AIR);
                break;
            case '#':
                if (warded && shellLayer) d.put(x, y, z, Block.WARDED);
                else masonry(x, y, z, layer);
                break;
            case 'L':
                // On a forgotten level a third of the lamps have gone dark (never by a shaft: the way on stays lit).
                if (d.theme == SkyDungeon.Theme.FORGOTTEN && role != RoomTemplate.Role.SHAFT_TOP
                    && role != RoomTemplate.Role.SHAFT_FOOT
                    && d.noise.unit3(x, y, z, 72) < 0.33) masonry(x, y, z, layer);
                else d.put(x, y, z, Block.LAMP);
                break;
            case 'B':
                d.put(x, y, z, Block.BOOKSHELF);
                break;
            case '~':
                d.put(x, y, z, Block.WATER);
                if (shellLayer) d.polished(x, y - 1, z); // a basin's bottom (and the ceiling of what lies below)
                break;
            case '=':
                d.put(x, y, z, Block.LAVA);
                if (shellLayer) d.polished(x, y - 1, z);
                break;
            case 'x':
                d.put(x, y, z, Block.COBWEB);
                break;
            case 'R':
                d.put(x, y, z, Block.SKYSTONE, 1);
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
                chest(x, y, z, role, true, template.building);
                break;
            case 'C':
                chest(x, y, z, role, false, template.building);
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
                masonry(x, y, z, layer);
                d.put(x, y - 1, z, Block.TNT);
                break;
            case 'W':
                d.put(x, y, z, Block.SEAL);
                break;
            case '?':
                if (d.noise.unit3(x, y, z, 23) < 0.5) masonry(x, y, z, layer);
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
            case '"':
                d.put(x, y, z, Block.FURNACE, awayFromWall(tx, tz, w, dp) | 8);
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
                d.put(x, y, z, Block.SKYSTONE, 2);
                break;
            case 'Y':
                d.put(x, y, z, Block.LOG, PLANKS[template.timber]);
                break;
            case '_':
            case '-':
                slab(template, x, y, z, layerY(template, y), c == '-');
                break;
            case 'p':
                d.put(x, y, z, Block.PLANKS, 0);
                break;
            case 'P':
                d.put(x, y, z, Block.PLANKS, PLANKS[template.timber]);
                break;
            case '0':
                d.pillar(x, y, z);
                break;
            case '9':
                d.put(x, y, z, Block.CHISELED);
                break;
            case 'j':
                d.polished(x, y, z);
                break;
            case 'k':
                d.floorTile(x, y, z);
                break;
            case 'y':
                d.put(x, y, z, Block.SKY_SLAB, 2);
                break;
            case 'z':
                d.put(x, y, z, Block.SKY_SLAB, 2 | 8);
                break;
            case 'r':
                d.put(x, y, z, Block.SKY_SLAB, 0);
                break;
            case 'q':
                d.put(x, y, z, Block.CARPET, template.carpet);
                break;
            case '<':
                d.put(x, y, z, Block.STAINED_GLASS_PANE, template.glass);
                break;
            case '@':
                d.put(x, y, z, Block.FLOWER_POT, (int) (d.noise.unit3(x, y, z, 27) * SkyDungeon.POT_PLANTS));
                break;
            case '/':
                d.put(x, y, z, Block.WOODEN_TABLE, template.timber);
                break;
            case '!':
                d.put(x, y, z, Block.BARREL);
                break;
            case '{':
                d.put(x, y, z, Block.CRATE);
                break;
            case '`':
                d.put(x, y, z, Block.MARKET_COUNTER, template.goods);
                break;
            case 'Q':
                d.put(x, y, z, Block.SKYSLATE, 0);
                break;
            case 'e':
                d.put(x, y, z, Block.SKYSLATE, 1);
                break;
            case '&':
                d.put(x, y, z, Block.BREWING_STAND);
                break;
            case '$':
                d.put(x, y, z, Block.ENCHANTING_TABLE);
                break;
            case '+':
                d.put(x, y, z, Block.JUKEBOX);
                break;
            case '}':
                d.put(x, y, z, Block.CHEST); // furniture, empty
                break;
            case 'Z':
                d.put(x, y, z, Block.HAY);
                break;
            case 'E':
                d.put(x, y, z, Block.GLASS);
                break;
            case '*':
                d.put(x, y, z, Block.LEAVES);
                break;
            case '\u03c9':
                d.put(x, y, z, Block.BRAZIER);
                break;
            case '\u03c6':
                d.put(x, y, z, Block.FOUNTAIN);
                break;
            case '\u0398':
                d.put(x, y, z, Block.ORRERY);
                break;
            case '\u03a3':
                d.put(x, y, z, Block.CHART_TABLE);
                break;
            case '\u03be':
                d.put(x, y, z, Block.STORMGLASS_CLUSTER);
                break;
            case '>':
                d.put(x, y, z, Block.STORMGLASS_VEIN);
                break;
            default:
                masonry(x, y, z, layer);
                break;
        }
    }

    /**
     * Vanilla stairs meta for a template's '1'..'4' (rising to the north, east, south, west; '5'..'8' the same
     * upside down), once the room is turned {@code rotation} quarter turns clockwise.
     */
    static int stairs(char c, int rotation) {
        int n = c - '1';
        boolean upsideDown = n >= 4;
        int dir = ((n & 3) + rotation) & 3; // 0 north, 1 east, 2 south, 3 west
        int[] meta = { 3, 0, 2, 1 };
        return meta[dir] | (upsideDown ? 4 : 0);
    }

    /**
     * A door: the lower half where the layer below is not also a door (the upper half is written with it). It sits
     * in the plane of its wall: facing along z when the wall runs along x, along x otherwise.
     */
    private void door(RoomTemplate t, int rotation, int tx, int y, int tz, int x, int wy, int z) {
        if (y > 0 && t.at(tx, y - 1, tz, rotation) == 'D') return;
        int w = t.width(rotation), dp = t.depth(rotation);
        boolean wallAlongX = tx > 0 && tx < w - 1
            && solid(t.at(tx - 1, y, tz, rotation))
            && solid(t.at(tx + 1, y, tz, rotation));
        int facing;
        if (wallAlongX) facing = tz >= dp / 2 ? 1 : 3;
        else facing = tx >= w / 2 ? 0 : 2;
        d.put(x, wy, z, Block.DOOR, facing);
        // Its upper half as a piece of its own, so that nothing cleared there before (a street paved, the ground
        // levelled) is left standing in it to knock the door down.
        d.put(x, wy + 1, z, Block.DOOR, SkyDungeon.DOOR_TOP);
    }

    private static boolean solid(char c) {
        return c != '.' && c != 'D' && c != 't' && c != 'f' && c != 'w' && c != ',';
    }

    /** Solid enough to hang a torch or a trapdoor on: masonry, planks, logs, shelves. */
    private static boolean wall(char c) {
        return "#09jkRMQYepPBE".indexOf(c) >= 0;
    }

    /** Steps (north, east, south, west) as (dx, dz). */
    private static final int[][] STEP = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    /** The character one step away, or the room's wall ('#') past the edge of a room (a building has no shell). */
    private static char beside(RoomTemplate t, int rotation, int tx, int y, int tz, int dir) {
        int nx = tx + STEP[dir][0], nz = tz + STEP[dir][1];
        if (nx < 0 || nz < 0 || nx >= t.width(rotation) || nz >= t.depth(rotation)) return t.building ? '.' : '#';
        return t.at(nx, y, nz, rotation);
    }

    /** The first direction (north, east, south, west) with a wall beside, or -1. */
    private static int nearestSolid(RoomTemplate t, int rotation, int tx, int y, int tz) {
        for (int dir = 0; dir < 4; dir++) if (wall(beside(t, rotation, tx, y, tz, dir))) return dir;
        return -1;
    }

    /** A torch hangs on a wall beside it, or stands on the floor. */
    private static int torch(RoomTemplate t, int rotation, int tx, int y, int tz) {
        int dir = nearestSolid(t, rotation, tx, y, tz);
        return dir < 0 ? SkyDungeon.STANDING : dir;
    }

    /** A fence gate faces across its fence line: north-south when the fence runs east-west. */
    private static int gate(RoomTemplate t, int rotation, int tx, int y, int tz) {
        char e = beside(t, rotation, tx, y, tz, SkyDungeon.EAST), w = beside(t, rotation, tx, y, tz, SkyDungeon.WEST);
        boolean lineAlongX = e == 'o' || w == 'o' || wall(e) && wall(w);
        return lineAlongX ? SkyDungeon.SOUTH : SkyDungeon.EAST;
    }

    /**
     * Half a bed: 'b' its head (against the wall), 'u' its foot, side by side. Both halves carry the way from foot to
     * head; the head adds 8.
     */
    private void bed(RoomTemplate t, int rotation, int tx, int y, int tz, int x, int wy, int z, boolean head) {
        char other = head ? 'u' : 'b';
        int found = -1;
        for (int dir = 0; dir < 4; dir++) {
            if (beside(t, rotation, tx, y, tz, dir) != other) continue;
            if (found >= 0) throw new IllegalStateException(t.name + ": a bed half with two partners");
            found = dir;
        }
        if (found < 0) throw new IllegalStateException(t.name + ": a bed without its other half");
        if (head) d.put(x, wy, z, Block.BED, ((found + 2) & 3) | 8);
        else d.put(x, wy, z, Block.BED, found);
    }

    /**
     * 'c' is the room's chest (whose loot the role decides); 'C' the second one, if the role has one. A store's chests
     * in a building on the surface (a house, a shop) hold household things, not a dungeon store's.
     */
    private void chest(int x, int y, int z, RoomTemplate.Role role, boolean primary, boolean building) {
        Loot loot = null;
        switch (role) {
            case STORE:
            case SECRET:
                loot = primary || d.noise.unit3(x, y, z, 25) < 0.5 ? building ? Loot.HOUSEHOLD : Loot.STORE : null;
                break;
            case KEY:
                loot = primary ? Loot.KEY : null;
                break;
            case ENTRANCE:
                loot = primary ? Loot.CHART : null;
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
        int kind = name.equals("Skeleton") ? 1
            : name.equals("Spider") ? 2
                : name.equals("CaveSpider") ? 3 : name.equals("Stormwisp") ? 4 : name.equals("Sentinel") ? 5 : 0;
        // Kinds 4 and up keep their low two bits and set the high bit (8); 4 is the armed bit.
        return (kind & 3) | (kind >= 4 ? 8 : 0) | (armed ? 4 : 0);
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
