package naturalexceptions.core.worldgen.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One authored room of a sky dungeon, read from a {@code .room} text file. The file gives the room's inside only;
 * the walls, the doorways cut through them and the corridors beyond are the layout's business.
 *
 * <pre>
 * name: library           ; a comment
 * size: 1x1               ; grid cells along x and z (a cell is 19 blocks), or "small" (a 5 x 5 closet)
 * height: 5               ; open layers above the floor (5, or 10 for a tall room)
 * roles: hall store guard
 * kinds: sanctum citadel
 * weight: 3
 * mobs: Skeleton Witch    ; for spawners (s, S) and alcoves (a); "Zombie+" is armoured
 * pit: lava               ; what fills the pits under ',' and '%': lava, water or empty
 * trapped: true           ; holds trap devices (counts against the dungeon's trap budget)
 * type: building          ; a building on a citadel's surface: the file draws its own walls and roof
 * footprint: 9x11         ; a building's size in blocks, walls included (x by z); its front (door) is south
 * carpet: red             ; the colour of its carpets (q)
 * glass: light_blue       ; the colour of its stained glass panes (&lt;)
 * roof: spruce            ; what stairs (1-8) and slabs (_ -) from the eaves up are: spruce, dark_oak or skyslate
 * eaves: 5                ; the first layer of the roof (below it, stairs and slabs are of the timber wood)
 * timber: spruce          ; what its logs (Y), spruce planks (P) and tables (/) are: oak, spruce or dark_oak
 * goods: produce          ; what its market counters (`) hold: produce, fish, pottery or cloth
 * awning: red             ; the colour of its awnings ('): red, blue, green or yellow
 * ---
 * floor:                  ; then one row per z (north first), one character per x (west first)
 * layer 1-3:              ; the open layers, from 1 (standing on the floor) up to height
 * ceiling:
 * </pre>
 *
 * Unlisted layers are open air, an unlisted floor or ceiling is plain stone. The legend is in {@link #LEGEND}.
 */
public final class RoomTemplate {

    /** What each character in a template means. */
    public static final String LEGEND = ".#LB~=xRIGtfwcCsS^vF%,ghaTW?HKONA|oMQY*12345678_-DpPZeE"
        + "09jkilmnJUVXyzrbuqd<@/&$+[]()}!{:`'\"";

    /** Vanilla dye colours by name, for carpets and stained glass. */
    public static final List<String> COLOURS = java.util.Arrays.asList(
        "white",
        "orange",
        "magenta",
        "light_blue",
        "yellow",
        "lime",
        "pink",
        "gray",
        "light_gray",
        "cyan",
        "purple",
        "blue",
        "brown",
        "green",
        "red",
        "black");

    public enum Role {
        ENTRANCE,
        HALL,
        GUARD,
        STORE,
        KEY,
        WARDEN,
        VAULT,
        SECRET
    }

    public enum Kind {
        SANCTUM,
        CITADEL
    }

    public final String name;
    public final int cellsX;
    public final int cellsZ;
    public final boolean small;
    public final int height;
    public final Set<Role> roles;
    public final Set<Kind> kinds;
    public final int weight;
    public final List<String> mobs;
    public final String pit;
    public final boolean trapped;
    /** A surface building: the file holds its walls and roof, its layers start at the ground floor. */
    public final boolean building;
    /** Vanilla colours of its carpets and stained glass. */
    public final int carpet, glass;
    /** Its materials and wares, by index into {@link #ROOFS}, {@link #TIMBERS}, {@link #GOODS}, {@link #AWNINGS}. */
    public final int roof, timber, goods, awning;
    /** The first layer of its roof. */
    public final int eaves;

    public static final List<String> ROOFS = java.util.Arrays.asList("spruce", "dark_oak", "skyslate");
    public static final List<String> TIMBERS = java.util.Arrays.asList("oak", "spruce", "dark_oak");
    public static final List<String> GOODS = java.util.Arrays.asList("produce", "fish", "pottery", "cloth");
    public static final List<String> AWNINGS = java.util.Arrays.asList("red", "blue", "green", "yellow");
    /** [y][z][x]: y 0 is the floor, 1..height the open layers, height + 1 the ceiling. */
    private final char[][][] grid;

    private RoomTemplate(String name, int cellsX, int cellsZ, boolean small, int height, Set<Role> roles,
        Set<Kind> kinds, int weight, List<String> mobs, String pit, boolean trapped, boolean building, int carpet,
        int glass, int[] wares, char[][][] grid) {
        this.name = name;
        this.cellsX = cellsX;
        this.cellsZ = cellsZ;
        this.small = small;
        this.height = height;
        this.roles = roles;
        this.kinds = kinds;
        this.weight = weight;
        this.mobs = mobs;
        this.pit = pit;
        this.trapped = trapped;
        this.building = building;
        this.carpet = carpet;
        this.glass = glass;
        this.roof = wares[0];
        this.timber = wares[1];
        this.goods = wares[2];
        this.awning = wares[3];
        this.eaves = wares[4];
        this.grid = grid;
    }

    /** Inside width along x (before rotation). */
    public int width() {
        return grid[0][0].length;
    }

    /** Inside depth along z (before rotation). */
    public int depth() {
        return grid[0].length;
    }

    /** A sky dungeon's grid cell: a one-cell room's walls stand 11 apart, leaving 8 blocks of corridor between. */
    public static final int CELL = 19;

    /** Inside size for a room of this many cells (walls excluded): 9 for one cell, 28 for two, 5 for a closet. */
    public static int insideSize(int cells, boolean small) {
        return small ? 5 : CELL * cells - 10;
    }

    /** The character at an inside position of the room turned {@code rotation} quarter turns clockwise. */
    public char at(int x, int y, int z, int rotation) {
        int w = width(), d = depth();
        int ox, oz;
        switch (rotation & 3) {
            case 1:
                ox = z;
                oz = d - 1 - x;
                break;
            case 2:
                ox = w - 1 - x;
                oz = d - 1 - z;
                break;
            case 3:
                ox = w - 1 - z;
                oz = x;
                break;
            default:
                ox = x;
                oz = z;
                break;
        }
        return grid[y][oz][ox];
    }

    /** Width along x once turned. */
    public int width(int rotation) {
        return (rotation & 1) == 0 ? width() : depth();
    }

    public int depth(int rotation) {
        return (rotation & 1) == 0 ? depth() : width();
    }

    /** Whether any layer holds this character. */
    public boolean has(char c) {
        for (char[][] layer : grid) for (char[] row : layer) for (char ch : row) if (ch == c) return true;
        return false;
    }

    // --- Parsing --------------------------------------------------------------------------------------------

    public static RoomTemplate parse(String source, List<String> lines) {
        String name = source;
        int cellsX = 1, cellsZ = 1, height = 5, weight = 1;
        boolean small = false, trapped = false, building = false;
        int footprintX = 0, footprintZ = 0;
        int carpet = COLOURS.indexOf("red"), glass = COLOURS.indexOf("light_blue");
        int[] wares = { 0, 1, 0, 0, 0 }; // spruce roof, spruce timber, produce, red awnings; all roof (no eaves)
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Set<Kind> kinds = EnumSet.allOf(Kind.class);
        List<String> mobs = new ArrayList<>();
        String pit = "empty";
        int i = 0;
        for (; i < lines.size(); i++) {
            String line = strip(lines.get(i));
            if (line.equals("---")) {
                i++;
                break;
            }
            if (line.isEmpty()) continue;
            int colon = line.indexOf(':');
            if (colon < 0) throw error(source, i, "expected 'key: value'");
            String key = line.substring(0, colon)
                .trim()
                .toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1)
                .trim();
            switch (key) {
                case "name":
                    name = value;
                    break;
                case "size":
                    if (value.equals("small")) small = true;
                    else {
                        String[] parts = value.split("x");
                        cellsX = Integer.parseInt(parts[0].trim());
                        cellsZ = Integer.parseInt(parts[1].trim());
                    }
                    break;
                case "height":
                    height = Integer.parseInt(value);
                    break;
                case "weight":
                    weight = Integer.parseInt(value);
                    break;
                case "roles":
                    for (String r : value.split("\\s+")) roles.add(Role.valueOf(r.toUpperCase(Locale.ROOT)));
                    break;
                case "kinds":
                    kinds = EnumSet.noneOf(Kind.class);
                    for (String k : value.split("\\s+")) kinds.add(Kind.valueOf(k.toUpperCase(Locale.ROOT)));
                    break;
                case "mobs":
                    Collections.addAll(mobs, value.split("\\s+"));
                    break;
                case "pit":
                    pit = value;
                    break;
                case "trapped":
                    trapped = Boolean.parseBoolean(value);
                    break;
                case "type":
                    building = value.equals("building");
                    break;
                case "footprint": {
                    String[] parts = value.split("x");
                    footprintX = Integer.parseInt(parts[0].trim());
                    footprintZ = Integer.parseInt(parts[1].trim());
                    break;
                }
                case "carpet":
                case "glass": {
                    int colour = COLOURS.indexOf(value.toLowerCase(Locale.ROOT));
                    if (colour < 0) throw error(source, i, "unknown colour " + value);
                    if (key.equals("carpet")) carpet = colour;
                    else glass = colour;
                    break;
                }
                case "eaves":
                    wares[4] = Integer.parseInt(value);
                    break;
                case "roof":
                case "timber":
                case "goods":
                case "awning": {
                    List<String> names = key.equals("roof") ? ROOFS
                        : key.equals("timber") ? TIMBERS : key.equals("goods") ? GOODS : AWNINGS;
                    int index = names.indexOf(value.toLowerCase(Locale.ROOT));
                    if (index < 0) throw error(source, i, "unknown " + key + " " + value);
                    wares[key.equals("roof") ? 0 : key.equals("timber") ? 1 : key.equals("goods") ? 2 : 3] = index;
                    break;
                }
                default:
                    throw error(source, i, "unknown key " + key);
            }
        }
        int w = footprintX > 0 ? footprintX : insideSize(cellsX, small);
        int d = footprintZ > 0 ? footprintZ : insideSize(cellsZ, small);
        char[][][] grid = new char[height + 2][d][w];
        for (int y = 0; y < grid.length; y++)
            for (char[] row : grid[y]) java.util.Arrays.fill(row, y == 0 || y == height + 1 && !building ? '#' : '.');
        while (i < lines.size()) {
            String header = strip(lines.get(i));
            if (header.isEmpty()) {
                i++;
                continue;
            }
            int from, to;
            if (header.equals("floor:")) from = to = 0;
            else if (header.equals("ceiling:")) from = to = height + 1;
            else if (header.startsWith("layer ") && header.endsWith(":")) {
                String range = header.substring(6, header.length() - 1)
                    .trim();
                int dash = range.indexOf('-');
                from = Integer.parseInt(dash < 0 ? range : range.substring(0, dash));
                to = dash < 0 ? from : Integer.parseInt(range.substring(dash + 1));
                if (from < 1 || to > height || from > to) throw error(source, i, "layer outside 1.." + height);
            } else throw error(source, i, "expected floor:, layer N:, layer N-M: or ceiling:");
            i++;
            for (int z = 0; z < d; z++, i++) {
                if (i >= lines.size()) throw error(source, i, "section ends early");
                String row = lines.get(i);
                int semicolon = row.indexOf(';');
                if (semicolon >= 0) row = row.substring(0, semicolon);
                row = row.trim();
                if (row.length() != w) throw error(source, i, "row is " + row.length() + " wide, expected " + w);
                for (int x = 0; x < w; x++) {
                    char c = row.charAt(x);
                    if (LEGEND.indexOf(c) < 0) throw error(source, i, "unknown character '" + c + "'");
                    for (int y = from; y <= to; y++) grid[y][z][x] = c;
                }
            }
        }
        if (roles.isEmpty()) throw error(source, 0, "no roles");
        return new RoomTemplate(
            name,
            cellsX,
            cellsZ,
            small,
            height,
            roles,
            kinds,
            weight,
            Collections.unmodifiableList(mobs),
            pit,
            trapped,
            building,
            carpet,
            glass,
            wares,
            grid);
    }

    private static String strip(String line) {
        int semicolon = line.indexOf(';');
        return (semicolon >= 0 ? line.substring(0, semicolon) : line).trim();
    }

    private static IllegalArgumentException error(String source, int line, String message) {
        return new IllegalArgumentException(source + ":" + (line + 1) + ": " + message);
    }

    @Override
    public String toString() {
        return name;
    }
}
