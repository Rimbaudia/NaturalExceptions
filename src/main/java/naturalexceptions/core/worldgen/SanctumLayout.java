package naturalexceptions.core.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Style;
import naturalexceptions.core.worldgen.SkyDungeon.Trap;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;
import naturalexceptions.core.worldgen.dungeon.TemplateLibrary;

/**
 * Lays out a sanctum (or the catacomb under a citadel): up to four levels of rooms on a 19-block grid carved into
 * a grand island.
 * <ol>
 * <li>Each level's cells that sit wholly in rock are found; each level is entered by one or two shafts from the one
 * above (from the surface, or from a citadel's keep, for the first).</li>
 * <li>The deepest level holds the Sky Warden's hall (two by two, ten high, if it fits) with the vault beside it. The
 * hall has one way in, sealed by a gate the Sky Key opens; the vault's gate opens only when the Warden falls.</li>
 * <li>The other cells become rooms of one, two or four cells, joined by a random spanning tree plus about a third
 * of the remaining neighbours, so there are loops and more than one way round. Some dead ends are left as rock.</li>
 * <li>Closets beside rooms are hidden behind loose walls. The key room is on another level from the Warden (two
 * up if it can be), as far from it as the layout allows.</li>
 * <li>Rooms get authored templates for their roles (never the same one twice if it can be helped), corridors get
 * a style, and a few corridors (about one in five, never two in a row on the way to the key and the Warden, never
 * into the entrance, the key room or the Warden's hall) get a trap.</li>
 * </ol>
 */
final class SanctumLayout {

    static final int CELL = RoomTemplate.CELL;
    static final int LEVEL_SPACING = 12;
    static final int MAX_LEVELS = 4;
    /** Share of ordinary rooms that are guard rooms (with a spawner). */
    static final double GUARD_SHARE = 0.15;
    static final int NORMAL_HEIGHT = 5;
    static final int TALL_HEIGHT = 10;
    static final double EXTRA_EDGES = 0.33;
    static final double TRAP_SHARE = 0.2;
    static final int MAX_TRAPS_PER_LEVEL = 3;

    private final SkyDungeon d;
    private final SkyIslands.Island island;
    private final Random random;
    /** How many cells out from the middle the grid reaches. */
    private final int grid;
    /** Under a citadel: entered from its keep (whose ground floor is at this height), not a surface gateway. */
    private final int keepFloor;

    /** A room's place: its cells, what it is, and the template chosen for it. */
    private final class Slot {

        final int level, i0, j0, nx, nz;
        final boolean small;
        Role role = Role.HALL;
        boolean link, tall, noPits;
        RoomTemplate template;
        int rotation;
        SkyDungeon.Room room;
        final List<Edge> edges = new ArrayList<>();

        Slot(int level, int i0, int j0, int nx, int nz, boolean small) {
            this.level = level;
            this.i0 = i0;
            this.j0 = j0;
            this.nx = nx;
            this.nz = nz;
            this.small = small;
        }

        int minX() {
            return small ? cellX(i0) - 2 : cellX(i0) - 4;
        }

        int maxX() {
            return small ? cellX(i0) + 2 : cellX(i0 + nx - 1) + 4;
        }

        int minZ() {
            return small ? cellZ(j0) - 2 : cellZ(j0) - 4;
        }

        int maxZ() {
            return small ? cellZ(j0) + 2 : cellZ(j0 + nz - 1) + 4;
        }

        int floor() {
            return levelFloor(level);
        }

        boolean special() {
            return role == Role.ENTRANCE || role == Role.WARDEN
                || role == Role.VAULT
                || role == Role.SECRET
                || role == Role.KEY;
        }
    }

    private final class Edge {

        final Slot a, b;
        /** The cells either side of the doorway (a's, then b's); both on one level unless a shaft. */
        final int ai, aj, bi, bj;
        Style style = Style.PLAIN;
        Trap trap = Trap.NONE;
        boolean critical, tree;

        Edge(Slot a, Slot b, int ai, int aj, int bi, int bj) {
            this.a = a;
            this.b = b;
            this.ai = ai;
            this.aj = aj;
            this.bi = bi;
            this.bj = bj;
        }

        Slot other(Slot s) {
            return s == a ? b : a;
        }
    }

    private final List<Slot> slots = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Long, Slot> byCell = new HashMap<>();
    private final Map<RoomTemplate, Integer> used = new HashMap<>();

    SanctumLayout(SkyDungeon dungeon) {
        this(dungeon, Integer.MIN_VALUE);
    }

    /** A catacomb entered by a shaft up into a citadel keep's ground floor, at {@code keepFloor}. */
    SanctumLayout(SkyDungeon dungeon, int keepFloor) {
        this.d = dungeon;
        this.island = dungeon.island;
        this.random = new Random(dungeon.field.seed() * 31 + island.cellX() * 1_000_003L + island.cellZ());
        this.grid = 2; // five by five cells a level: with four levels, forty to sixty rooms
        this.keepFloor = keepFloor;
    }

    private boolean underKeep() {
        return keepFloor != Integer.MIN_VALUE;
    }

    int cellX(int i) {
        return island.centreX() + i * CELL;
    }

    int cellZ(int j) {
        return island.centreZ() + j * CELL;
    }

    int levelFloor(int level) {
        return island.centreY() - LEVEL_SPACING * (level + 1);
    }

    private static long cellKey(int level, int i, int j) {
        return ((long) level << 40) ^ ((long) (i & 0xFFFFF) << 20) ^ (j & 0xFFFFF);
    }

    private Slot slotAt(int level, int i, int j) {
        return byCell.get(cellKey(level, i, j));
    }

    /** Whether a room of this height (or a closet) fits at a cell: rock all round it, with room for pits below. */
    private boolean fits(int level, int i, int j, int height, boolean small) {
        int cx = cellX(i), cz = cellZ(j), f = levelFloor(level);
        int r = small ? 4 : 6;
        int[][] probes = { { 0, 0 }, { -r, -r }, { r, -r }, { -r, r }, { r, r }, { 0, -r }, { 0, r }, { -r, 0 },
            { r, 0 } };
        for (int[] p : probes) {
            int[] column = d.field.column(island, cx + p[0], cz + p[1]);
            if (column == null || d.field.reach(island, cx + p[0], cz + p[1]) > 0.88) return false;
            if (column[0] >= f - RoomWriter.PIT_DEPTH - 3 || column[1] <= f + height + 2) return false;
        }
        return true;
    }

    // --- Build ----------------------------------------------------------------------------------------------

    void build() {
        List<Set<Long>> reachable = new ArrayList<>();
        Slot entrance = placeEntrance();
        Set<Long> first = component(0, entrance.i0, entrance.j0);
        reachable.add(first);
        List<Slot> arrivals = new ArrayList<>();
        arrivals.add(entrance);
        // Levels below: each entered by a shaft from a cell of the one above, as far from its arrival as can be.
        for (int level = 1; level < MAX_LEVELS; level++) {
            Set<Long> above = reachable.get(level - 1);
            Slot arrivalAbove = arrivals.get(level - 1);
            int[] best = null;
            int bestDistance = -1;
            for (long key : above) {
                int i = cellI(key), j = cellJ(key);
                if (slotAt(level - 1, i, j) != null || !fits(level, i, j, NORMAL_HEIGHT, false)) continue;
                int distance = Math.abs(i - arrivalAbove.i0) + Math.abs(j - arrivalAbove.j0);
                if (distance > bestDistance || distance == bestDistance && random.nextBoolean()) {
                    best = new int[] { i, j };
                    bestDistance = distance;
                }
            }
            if (best == null) break;
            Set<Long> below = component(level, best[0], best[1]);
            if (below.size() < 3) break;
            Slot bottom = link(level, best[0], best[1]);
            reachable.add(below);
            arrivals.add(bottom);
            // A second way down, well apart from the first, so the levels loop into one another.
            List<int[]> seconds = new ArrayList<>();
            for (long key : above) {
                int i = cellI(key), j = cellJ(key);
                if (Math.abs(i - best[0]) + Math.abs(j - best[1]) < 3 || slotAt(level - 1, i, j) != null) continue;
                if (!below.contains(cellKey(level, i, j)) || slotAt(level, i, j) != null) continue;
                seconds.add(new int[] { i, j });
            }
            if (!seconds.isEmpty() && random.nextDouble() < 0.75) {
                int[] second = seconds.get(random.nextInt(seconds.size()));
                link(level, second[0], second[1]);
            }
        }
        int deepest = reachable.size() - 1;
        placeWarden(reachable, arrivals, deepest);
        for (int level = 0; level < reachable.size(); level++) fill(level, reachable.get(level));
        for (int level = 0; level < reachable.size(); level++) join(level, arrivals.get(level));
        pruneDeadEnds();
        secrets();
        roles();
        templates();
        styles();
        write();
    }

    /** A shaft from a room on the level above down to a room on {@code level}, at one cell; returns the lower room. */
    private Slot link(int level, int i, int j) {
        Slot top = add(level - 1, i, j, 1, 1, false);
        Slot bottom = add(level, i, j, 1, 1, false);
        top.link = true;
        bottom.link = true;
        Edge shaft = new Edge(top, bottom, i, j, i, j);
        shaft.style = Style.SHAFT;
        connect(shaft);
        return bottom;
    }

    private static int cellI(long key) {
        return (int) (((key >> 20) & 0xFFFFF) << 12) >> 12;
    }

    private static int cellJ(long key) {
        return (int) ((key & 0xFFFFF) << 12) >> 12;
    }

    /** The cells of a level joined to (i, j) through cells that fit. */
    private Set<Long> component(int level, int i, int j) {
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { i, j });
        seen.add(cellKey(level, i, j));
        int[][] steps = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] s : steps) {
                int ni = c[0] + s[0], nj = c[1] + s[1];
                if (Math.abs(ni) > grid || Math.abs(nj) > grid) continue;
                long key = cellKey(level, ni, nj);
                if (seen.contains(key) || !fits(level, ni, nj, NORMAL_HEIGHT, false)) continue;
                seen.add(key);
                queue.add(new int[] { ni, nj });
            }
        }
        return seen;
    }

    private Slot add(int level, int i, int j, int nx, int nz, boolean small) {
        Slot slot = new Slot(level, i, j, nx, nz, small);
        slots.add(slot);
        for (int a = 0; a < nx; a++) for (int b = 0; b < nz; b++) byCell.put(cellKey(level, i + a, j + b), slot);
        return slot;
    }

    private void connect(Edge edge) {
        edges.add(edge);
        edge.a.edges.add(edge);
        edge.b.edges.add(edge);
    }

    private void disconnect(Edge edge) {
        edges.remove(edge);
        edge.a.edges.remove(edge);
        edge.b.edges.remove(edge);
    }

    private Slot placeEntrance() {
        int[] best = underKeep() ? new int[] { 0, 0 } : null;
        for (int ring = 0; ring <= grid && best == null; ring++) {
            for (int i = -ring; i <= ring && best == null; i++) for (int j = -ring; j <= ring && best == null; j++) {
                if (Math.max(Math.abs(i), Math.abs(j)) == ring && fits(0, i, j, NORMAL_HEIGHT, false))
                    best = new int[] { i, j };
            }
        }
        if (best == null) best = new int[] { 0, 0 }; // a thin island: the sanctum is carved regardless
        Slot entrance = add(0, best[0], best[1], 1, 1, false);
        entrance.role = Role.ENTRANCE;
        return entrance;
    }

    /** The Warden's hall (2 x 2 and tall if it fits, else 1 x 1 tall) on the deepest level it fits, vault beside. */
    private void placeWarden(List<Set<Long>> reachable, List<Slot> arrivals, int deepest) {
        // Only the deepest level, or failing that the one above it if that is still at least the third.
        for (int level = deepest; level >= 0; level--) {
            if (level < deepest && level < 2) break;
            Set<Long> cells = reachable.get(level);
            Slot arrival = arrivals.get(level);
            // Best a great two-by-two hall, ten high; else a tall one-cell hall; else a plain chamber.
            for (int[] shape : new int[][] { { 2, TALL_HEIGHT }, { 1, TALL_HEIGHT }, { 1, NORMAL_HEIGHT } }) {
                int size = shape[0], height = shape[1];
                List<int[]> options = new ArrayList<>();
                for (long key : cells) {
                    int i = cellI(key), j = cellJ(key);
                    if (!hallFits(level, i, j, size, height, cells)) continue;
                    int[] vault = vaultBeside(level, i, j, size);
                    if (vault == null || !stillJoined(level, cells, arrival, i, j, size, vault)) continue;
                    int distance = Math.abs(i - arrival.i0) + Math.abs(j - arrival.j0);
                    options.add(new int[] { i, j, vault[0], vault[1], distance });
                }
                if (options.isEmpty()) continue;
                // Among the placements, the furthest from where the level is entered.
                Collections.shuffle(options, random);
                options.sort((x, y) -> y[4] - x[4]);
                int[] pick = options.get(0);
                Slot hall = add(level, pick[0], pick[1], size, size, false);
                hall.role = Role.WARDEN;
                hall.tall = height > NORMAL_HEIGHT;
                if (hall.tall) for (int a = 0; a < size; a++) for (int b = 0; b < size; b++) {
                    Slot above = level > 0 ? slotAt(level - 1, pick[0] + a, pick[1] + b) : null;
                    if (above != null) above.noPits = true;
                    reserved.add(cellKey(level - 1, pick[0] + a, pick[1] + b));
                }
                Slot vault = add(level, pick[2], pick[3], 1, 1, false);
                vault.role = Role.VAULT;
                Edge edge = new Edge(
                    hall,
                    vault,
                    pick[2] - (pick[2] >= pick[0] + size ? 1 : pick[2] < pick[0] ? -1 : 0),
                    pick[3] - (pick[3] >= pick[1] + size ? 1 : pick[3] < pick[1] ? -1 : 0),
                    pick[2],
                    pick[3]);
                edge.style = Style.VAULT;
                connect(edge);
                return;
            }
        }
    }

    /**
     * Whether, with the hall and the vault taken out of the level, every other cell can still be reached from the
     * arrival (the hall must not cut the level in two) and the hall has a neighbour to be entered from.
     */
    private boolean stillJoined(int level, Set<Long> cells, Slot arrival, int i, int j, int size, int[] vault) {
        Set<Long> out = new HashSet<>();
        out.add(cellKey(level, vault[0], vault[1]));
        for (int a = 0; a < size; a++) for (int b = 0; b < size; b++) out.add(cellKey(level, i + a, j + b));
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { arrival.i0, arrival.j0 });
        seen.add(cellKey(level, arrival.i0, arrival.j0));
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] s : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                long key = cellKey(level, c[0] + s[0], c[1] + s[1]);
                if (out.contains(key) || !cells.contains(key) || !seen.add(key)) continue;
                queue.add(new int[] { c[0] + s[0], c[1] + s[1] });
            }
        }
        for (long key : cells) if (!out.contains(key) && !seen.contains(key)) return false;
        for (int a = -1; a <= size; a++) for (int b = -1; b <= size; b++) {
            boolean side = (a == -1 || a == size) != (b == -1 || b == size);
            if (side && seen.contains(cellKey(level, i + a, j + b))) return true;
        }
        return false;
    }

    /** Cells above a tall hall: their rooms may not dig pits (they would break through its ceiling). */
    private final Set<Long> reserved = new HashSet<>();

    private boolean hallFits(int level, int i, int j, int size, int height, Set<Long> cells) {
        for (int a = 0; a < size; a++) for (int b = 0; b < size; b++) {
            long key = cellKey(level, i + a, j + b);
            if (!cells.contains(key) || byCell.containsKey(key)) return false;
            if (!fits(level, i + a, j + b, height, false)) return false;
            // A shaft coming down through a tall hall's ceiling would spoil the fight.
            if (height > NORMAL_HEIGHT && level > 0 && slotAt(level - 1, i + a, j + b) != null) return false;
        }
        return true;
    }

    /** A free cell beside a hall at (i, j) of this size, for the vault (the one furthest from the middle). */
    private int[] vaultBeside(int level, int i, int j, int size) {
        int[] best = null;
        int bestReach = -1;
        for (int a = -1; a <= size; a++) for (int b = -1; b <= size; b++) {
            boolean side = (a == -1 || a == size) != (b == -1 || b == size);
            if (!side) continue;
            int vi = i + a, vj = j + b;
            if (Math.abs(vi) > grid || Math.abs(vj) > grid || byCell.containsKey(cellKey(level, vi, vj))) continue;
            if (!fits(level, vi, vj, NORMAL_HEIGHT, false)) continue;
            if (level > 0 && slotAt(level - 1, vi, vj) != null && slotAt(level - 1, vi, vj).link) continue;
            int reach = Math.abs(vi) + Math.abs(vj);
            if (reach > bestReach) {
                best = new int[] { vi, vj };
                bestReach = reach;
            }
        }
        return best;
    }

    /** Fills a level's free cells with rooms of one, two or four cells. */
    private void fill(int level, Set<Long> cells) {
        List<Long> order = new ArrayList<>(cells);
        Collections.sort(order);
        Collections.shuffle(order, random);
        for (long key : order) {
            if (byCell.containsKey(key)) continue;
            int i = cellI(key), j = cellJ(key);
            double roll = random.nextDouble();
            if (roll < 0.1 && free(level, i, j, 2, 2, cells)) add(level, i, j, 2, 2, false);
            else if (roll < 0.22 && free(level, i, j, 2, 1, cells)) add(level, i, j, 2, 1, false);
            else if (roll < 0.34 && free(level, i, j, 1, 2, cells)) add(level, i, j, 1, 2, false);
            else add(level, i, j, 1, 1, false);
        }
        for (Slot slot : slots)
            if (slot.level == level - 1 && reserved.contains(cellKey(level - 1, slot.i0, slot.j0))) slot.noPits = true;
    }

    private boolean free(int level, int i, int j, int nx, int nz, Set<Long> cells) {
        for (int a = 0; a < nx; a++) for (int b = 0; b < nz; b++) {
            long key = cellKey(level, i + a, j + b);
            if (!cells.contains(key) || byCell.containsKey(key) || reserved.contains(key)) return false;
        }
        return true;
    }

    /** A random spanning tree over a level's rooms from its arrival, plus some of the remaining neighbours. */
    private void join(int level, Slot start) {
        Map<String, List<Edge>> candidates = new HashMap<>();
        for (Slot slot : slots) {
            if (slot.level != level || slot.role == Role.VAULT || slot.role == Role.WARDEN) continue;
            for (int a = 0; a < slot.nx; a++) for (int b = 0; b < slot.nz; b++) {
                int i = slot.i0 + a, j = slot.j0 + b;
                for (int[] s : new int[][] { { 1, 0 }, { 0, 1 } }) {
                    Slot other = slotAt(level, i + s[0], j + s[1]);
                    if (other == null || other == slot || other.role == Role.VAULT || other.role == Role.WARDEN)
                        continue;
                    int p = slots.indexOf(slot), q = slots.indexOf(other);
                    String pair = String.format("%04d:%04d", Math.min(p, q), Math.max(p, q));
                    candidates.computeIfAbsent(pair, k -> new ArrayList<>())
                        .add(new Edge(slot, other, i, j, i + s[0], j + s[1]));
                }
            }
        }
        // One doorway per pair of rooms (a random one of the cell edges they share).
        List<String> pairs = new ArrayList<>(candidates.keySet());
        Collections.sort(pairs);
        Map<Slot, List<Edge>> options = new HashMap<>();
        List<Edge> chosen = new ArrayList<>();
        for (String pair : pairs) {
            List<Edge> list = candidates.get(pair);
            Edge edge = list.get(random.nextInt(list.size()));
            chosen.add(edge);
            options.computeIfAbsent(edge.a, k -> new ArrayList<>())
                .add(edge);
            options.computeIfAbsent(edge.b, k -> new ArrayList<>())
                .add(edge);
        }
        Set<Slot> inTree = new HashSet<>();
        Deque<Slot> stack = new ArrayDeque<>();
        inTree.add(start);
        stack.push(start);
        while (!stack.isEmpty()) {
            Slot here = stack.peek();
            List<Edge> out = new ArrayList<>();
            for (Edge e : options.getOrDefault(here, Collections.<Edge>emptyList()))
                if (!inTree.contains(e.other(here))) out.add(e);
            if (out.isEmpty()) {
                stack.pop();
                continue;
            }
            Edge e = out.get(random.nextInt(out.size()));
            e.tree = true;
            connect(e);
            inTree.add(e.other(here));
            stack.push(e.other(here));
        }
        for (Edge e : chosen) {
            if (e.tree || !inTree.contains(e.a) || !inTree.contains(e.b)) continue;
            if (random.nextDouble() < EXTRA_EDGES) connect(e);
        }
        // Rooms the tree never reached are left as rock.
        for (Slot slot : new ArrayList<>(slots)) {
            if (slot.level == level && !inTree.contains(slot) && slot.role != Role.VAULT && slot.role != Role.WARDEN)
                remove(slot);
        }
        // The Warden's hall: one way in, from a neighbouring room, through a gate the Sky Key opens.
        for (Slot hall : new ArrayList<>(slots)) {
            if (hall.level != level || hall.role != Role.WARDEN) continue;
            List<Edge> ways = new ArrayList<>();
            for (int a = 0; a < hall.nx; a++) for (int b = 0; b < hall.nz; b++) {
                int i = hall.i0 + a, j = hall.j0 + b;
                for (int[] s : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                    Slot other = slotAt(level, i + s[0], j + s[1]);
                    if (other == null || other == hall || !inTree.contains(other) || other.role == Role.VAULT) continue;
                    ways.add(new Edge(other, hall, i + s[0], j + s[1], i, j));
                }
            }
            if (ways.isEmpty()) continue;
            Edge way = ways.get(random.nextInt(ways.size()));
            way.style = Style.GATED;
            connect(way);
        }
    }

    private void remove(Slot slot) {
        for (Edge e : new ArrayList<>(slot.edges)) disconnect(e);
        slots.remove(slot);
        for (int a = 0; a < slot.nx; a++)
            for (int b = 0; b < slot.nz; b++) byCell.remove(cellKey(slot.level, slot.i0 + a, slot.j0 + b));
    }

    /** Some plain dead ends are left as rock, so levels have ragged edges and not every cell is a room. */
    private void pruneDeadEnds() {
        for (Slot slot : new ArrayList<>(slots)) {
            if (slot.special() || slot.link || slot.nx * slot.nz != 1 || slot.edges.size() != 1) continue;
            if (random.nextDouble() < 0.25) remove(slot);
        }
    }

    /** One to three closets behind loose walls, beside rooms, where a closet fits in the rock. */
    private void secrets() {
        List<int[]> options = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.role == Role.VAULT || slot.role == Role.WARDEN || slot.small) continue;
            for (int a = 0; a < slot.nx; a++) for (int b = 0; b < slot.nz; b++) {
                for (int[] s : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                    int i = slot.i0 + a + s[0], j = slot.j0 + b + s[1];
                    if (Math.abs(i) > grid + 1 || Math.abs(j) > grid + 1) continue;
                    if (byCell.containsKey(cellKey(slot.level, i, j)) || reserved.contains(cellKey(slot.level, i, j)))
                        continue;
                    if (slot.level > 0 && slotAt(slot.level - 1, i, j) != null && slotAt(slot.level - 1, i, j).tall)
                        continue;
                    if (!fits(slot.level, i, j, 4, true)) continue;
                    options.add(new int[] { slots.indexOf(slot), i, j, i - s[0], j - s[1] });
                }
            }
        }
        Collections.shuffle(options, random);
        int wanted = 1 + random.nextInt(3);
        for (int[] o : options) {
            if (wanted == 0) break;
            Slot parent = slots.get(o[0]);
            if (byCell.containsKey(cellKey(parent.level, o[1], o[2]))) continue;
            Slot closet = add(parent.level, o[1], o[2], 1, 1, true);
            closet.role = Role.SECRET;
            Edge edge = new Edge(parent, closet, o[3], o[4], o[1], o[2]);
            edge.style = Style.SECRET;
            connect(edge);
            wanted--;
        }
    }

    /** Distances in rooms from {@code from}, not through the vault or secret closets. */
    private Map<Slot, Integer> distances(Slot from) {
        Map<Slot, Integer> distance = new HashMap<>();
        Deque<Slot> queue = new ArrayDeque<>();
        distance.put(from, 0);
        queue.add(from);
        while (!queue.isEmpty()) {
            Slot here = queue.poll();
            for (Edge e : here.edges) {
                Slot next = e.other(here);
                if (next.role == Role.VAULT || next.role == Role.SECRET || distance.containsKey(next)) continue;
                distance.put(next, distance.get(here) + 1);
                queue.add(next);
            }
        }
        return distance;
    }

    private Slot warden() {
        for (Slot slot : slots) if (slot.role == Role.WARDEN) return slot;
        return null;
    }

    private Slot entranceSlot() {
        for (Slot slot : slots) if (slot.role == Role.ENTRANCE) return slot;
        throw new IllegalStateException("no entrance");
    }

    /** The key room furthest from the Warden; guards and storerooms among the rest. */
    private void roles() {
        Slot warden = warden();
        Map<Slot, Integer> fromWarden = distances(warden != null ? warden : entranceSlot());
        Slot key = null;
        int best = -1;
        for (Slot slot : slots) {
            if (slot.special() || slot.link || slot.nx * slot.nz != 1 || !fromWarden.containsKey(slot)) continue;
            int levelsApart = warden == null ? 0 : Math.abs(warden.level - slot.level);
            // Another level from the Warden, two up if possible; then as far away along the way as can be.
            int distance = fromWarden.get(slot) * 4 + random.nextInt(3)
                + (levelsApart == 0 ? -1000 : levelsApart >= 2 ? 200 : 100);
            if (distance > best) {
                key = slot;
                best = distance;
            }
        }
        if (key == null) for (Slot slot : slots) if (!slot.special() && slot.nx * slot.nz == 1) key = slot;
        if (key != null) key.role = Role.KEY;
        for (Slot slot : slots) {
            if (slot.special()) continue;
            double roll = random.nextDouble();
            slot.role = roll < GUARD_SHARE ? Role.GUARD : roll < GUARD_SHARE + 0.27 ? Role.STORE : Role.HALL;
        }
        // The way from the entrance to the key, and from the key (or the entrance) to the Warden.
        Slot entrance = entranceSlot();
        markCritical(entrance, key != null ? key : entrance);
        if (warden != null) markCritical(key != null ? key : entrance, warden);
    }

    private void markCritical(Slot from, Slot to) {
        Map<Slot, Edge> via = new HashMap<>();
        Deque<Slot> queue = new ArrayDeque<>();
        Set<Slot> seen = new HashSet<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            Slot here = queue.poll();
            if (here == to) break;
            for (Edge e : here.edges) {
                Slot next = e.other(here);
                if (next.role == Role.SECRET || next.role == Role.VAULT || !seen.add(next)) continue;
                via.put(next, e);
                queue.add(next);
            }
        }
        for (Slot at = to; at != from && via.containsKey(at);) {
            Edge e = via.get(at);
            e.critical = true;
            at = e.other(at);
        }
    }

    // --- Templates ------------------------------------------------------------------------------------------

    private final int[] trapsOnLevel = new int[MAX_LEVELS];
    private final int[] trapBudget = new int[MAX_LEVELS];

    private void templates() {
        for (int level = 0; level < MAX_LEVELS; level++) {
            int count = 0;
            for (Edge e : edges) if (e.a.level == level && e.b.level == level && e.style == Style.PLAIN) count++;
            trapBudget[level] = Math.min(MAX_TRAPS_PER_LEVEL, Math.max(1, (int) Math.round(count * TRAP_SHARE)));
        }
        List<Slot> order = new ArrayList<>(slots);
        // The rooms with the fewest choices first.
        order.sort((x, y) -> Integer.compare(x.special() ? 0 : 1, y.special() ? 0 : 1));
        for (Slot slot : order) choose(slot);
    }

    private void choose(Slot slot) {
        List<RoomTemplate> options = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        boolean trapAllowed = !slot.special() && !slot.link && trapsOnLevel[slot.level] < trapBudget[slot.level]
            || slot.role == Role.KEY && trapsOnLevel[slot.level] < trapBudget[slot.level];
        for (RoomTemplate t : TemplateLibrary.all()) {
            if (!t.roles.contains(slot.role) || !t.kinds.contains(RoomTemplate.Kind.SANCTUM)) continue;
            if (t.small != slot.small) continue;
            if (!slot.small
                && !(t.cellsX == slot.nx && t.cellsZ == slot.nz || t.cellsX == slot.nz && t.cellsZ == slot.nx))
                continue;
            if ((t.height > NORMAL_HEIGHT) != slot.tall) continue;
            if (t.trapped && !trapAllowed) continue;
            if (slot.noPits && (t.has(',') || t.has('%') || t.has('T'))) continue;
            if ((slot.link || slot.role == Role.ENTRANCE) && !cornersOpen(t)) continue;
            options.add(t);
            int times = used.getOrDefault(t, 0);
            weights.add(t.weight / (1.0 + 3.0 * times));
        }
        if (options.isEmpty()) {
            // Nothing authored fits these constraints: the plainest hall that does.
            for (RoomTemplate t : TemplateLibrary.all()) {
                if (!t.small && t.cellsX == 1
                    && t.cellsZ == 1
                    && t.height == NORMAL_HEIGHT
                    && !t.trapped
                    && cornersOpen(t)
                    && !t.has(',')
                    && !t.has('%')
                    && t.roles.contains(Role.HALL)) {
                    options.add(t);
                    weights.add(1.0);
                }
            }
            if (slot.role == Role.KEY || slot.role == Role.STORE) slot.role = Role.HALL;
        }
        double total = 0.0;
        for (double w : weights) total += w;
        double roll = random.nextDouble() * total;
        RoomTemplate pick = options.get(options.size() - 1);
        for (int k = 0; k < options.size(); k++) {
            roll -= weights.get(k);
            if (roll <= 0.0) {
                pick = options.get(k);
                break;
            }
        }
        slot.template = pick;
        used.merge(pick, 1, Integer::sum);
        if (pick.trapped) trapsOnLevel[slot.level]++;
        // Turn it to fit its footprint; a square room may face any way.
        List<Integer> turns = new ArrayList<>();
        for (int r = 0; r < 4; r++) if (slot.small || pick.width(r) == slot.maxX() - slot.minX() + 1) turns.add(r);
        slot.rotation = turns.get(random.nextInt(turns.size()));
    }

    /** Whether each corner's 2 x 2 is open on the lower three layers (room for a shaft). */
    static boolean cornersOpen(RoomTemplate t) {
        int w = t.width(), dp = t.depth();
        int[][] corners = { { 0, 0 }, { w - 2, 0 }, { 0, dp - 2 }, { w - 2, dp - 2 } };
        for (int[] c : corners)
            for (int x = c[0]; x < c[0] + 2; x++) for (int z = c[1]; z < c[1] + 2; z++) for (int y = 1; y <= 3; y++) {
                char ch = t.at(x, y, z, 0);
                if (ch != '.' && ch != 't' && ch != 'f' && ch != 'w') return false;
            }
        return true;
    }

    // --- Corridor looks and traps ---------------------------------------------------------------------------

    private void styles() {
        Trap last = Trap.NONE;
        for (Edge e : edges) {
            if (e.style != Style.PLAIN) continue;
            boolean eligible = !touches(e, Role.ENTRANCE) && !touches(e, Role.KEY)
                && !touches(e, Role.WARDEN)
                && !e.a.template.trapped
                && !e.b.template.trapped
                && !e.a.small
                && !e.b.small
                && trapsOnLevel[e.a.level] < trapBudget[e.a.level]
                && !(e.critical && trappedCriticalNeighbour(e))
                && !overTallHall(e);
            if (eligible && random.nextDouble() < 0.4) {
                e.trap = pickTrap(last);
                last = e.trap;
                trapsOnLevel[e.a.level]++;
            } else {
                double roll = random.nextDouble();
                e.style = roll < 0.24 ? Style.PLAIN
                    : roll < 0.48 ? Style.ARCHED
                        : roll < 0.62 ? Style.CRAMPED
                            : roll < 0.72 ? Style.BRIDGE : roll < 0.86 ? Style.FLOODED : Style.COLLAPSED;
                // Nothing digs down into a tall hall's ceiling from the level above.
                if (e.style == Style.BRIDGE && overTallHall(e)) e.style = Style.ARCHED;
            }
        }
    }

    /** Whether a corridor runs over a tall hall on the level below (so it must keep a solid floor). */
    private boolean overTallHall(Edge e) {
        int minX = Math.min(e.a.minX(), e.b.minX()), maxX = Math.max(e.a.maxX(), e.b.maxX());
        int minZ = Math.min(e.a.minZ(), e.b.minZ()), maxZ = Math.max(e.a.maxZ(), e.b.maxZ());
        for (Slot hall : slots) {
            if (!hall.tall || hall.level != e.a.level + 1) continue;
            if (maxX >= hall.minX() - 1 && minX <= hall.maxX() + 1
                && maxZ >= hall.minZ() - 1
                && minZ <= hall.maxZ() + 1) return true;
        }
        return false;
    }

    /** Whether a critical corridor next to this one (through a shared room) already has a trap. */
    private static boolean trappedCriticalNeighbour(Edge e) {
        for (Slot s : new Slot[] { e.a, e.b })
            for (Edge other : s.edges) if (other != e && other.critical && other.trap != Trap.NONE) return true;
        return false;
    }

    private static boolean touches(Edge e, Role role) {
        return e.a.role == role || e.b.role == role;
    }

    private Trap pickTrap(Trap last) {
        Trap[] kinds = { Trap.VOLLEY, Trap.VOLLEY, Trap.FIRE_VOLLEY, Trap.CRUMBLE, Trap.CRUMBLE, Trap.GALE, Trap.TNT,
            Trap.AMBUSH, Trap.AMBUSH };
        Trap pick;
        do pick = kinds[random.nextInt(kinds.length)]; while (pick == last);
        return pick;
    }

    // --- Writing --------------------------------------------------------------------------------------------

    private void write() {
        RoomWriter rooms = new RoomWriter(d);
        CorridorWriter corridors = new CorridorWriter(d);
        for (Slot slot : slots) {
            rooms.write(slot.template, slot.rotation, slot.minX(), slot.minZ(), slot.floor(), slot.role);
            slot.room = new SkyDungeon.Room(
                (slot.minX() + slot.maxX()) / 2,
                slot.floor(),
                (slot.minZ() + slot.maxZ()) / 2,
                slot.role,
                slot.template.name,
                slot.level,
                slot.minX(),
                slot.maxX(),
                slot.minZ(),
                slot.maxZ(),
                slot.template.height,
                slot.template.trapped);
            d.rooms.add(slot.room);
        }
        for (Edge e : edges) {
            if (e.style == Style.SHAFT) {
                Slot top = e.a.level < e.b.level ? e.a : e.b, bottom = e.other(top);
                corridors.shaft(
                    top.minX(),
                    top.minZ(),
                    bottom.floor(),
                    bottom.floor() + bottom.template.height + 1,
                    top.floor(),
                    random.nextBoolean());
            } else corridor(corridors, e);
            d.connections.add(new SkyDungeon.Connection(e.a.room, e.b.room, e.style, e.trap, e.critical));
        }
        entrance(corridors);
    }

    private void corridor(CorridorWriter corridors, Edge e) {
        Slot a = e.a, b = e.b;
        boolean alongX = e.aj == e.bj;
        int floor = a.floor();
        if (alongX) {
            Slot west = e.ai < e.bi ? a : b, east = west == a ? b : a;
            int centre = cellZ(e.aj);
            int t0 = west.maxX() + 2, t1 = east.minX() - 2;
            corridors.write(true, centre, t0, t1, floor, e.style, e.trap);
            doorways(corridors, true, centre, west, west.maxX() + 1, east, east.minX() - 1, floor, e);
        } else {
            Slot north = e.aj < e.bj ? a : b, south = north == a ? b : a;
            int centre = cellX(e.ai);
            int t0 = north.maxZ() + 2, t1 = south.minZ() - 2;
            corridors.write(false, centre, t0, t1, floor, e.style, e.trap);
            doorways(corridors, false, centre, north, north.maxZ() + 1, south, south.minZ() - 1, floor, e);
        }
    }

    private void doorways(CorridorWriter corridors, boolean alongX, int centre, Slot first, int firstWall, Slot second,
        int secondWall, int floor, Edge e) {
        for (Slot side : new Slot[] { first, second }) {
            int wall = side == first ? firstWall : secondWall;
            if (e.style == Style.VAULT && side.role == Role.VAULT)
                corridors.doorway(alongX, centre, wall, floor, Block.GATE, SkyDungeon.WARDEN_BOUND, false);
            else if (e.style == Style.GATED && side.role == Role.WARDEN)
                corridors.doorway(alongX, centre, wall, floor, Block.GATE, SkyDungeon.KEY_GATE, false);
            else if (e.style == Style.SECRET && side.role != Role.SECRET)
                corridors.doorway(alongX, centre, wall, floor, Block.SKYSTONE, SkyDungeon.LOOSE, true);
            else corridors.doorway(alongX, centre, wall, floor, Block.AIR, 0, e.style == Style.SECRET);
        }
        if (e.style == Style.SECRET) {
            // Round the narrow loose patch the doorway stays wall.
            int wall = first.role == Role.SECRET ? secondWall : firstWall;
            for (int a = -1; a <= 1; a++) for (int y = floor + 1; y <= floor + 3; y++) {
                if (a == 0 && y < floor + 3) continue;
                if (alongX) d.stone(wall, y, centre + a);
                else d.stone(centre + a, y, wall);
            }
        }
    }

    /** The ruined gateway on the surface over the shaft down into the entrance hall. */
    private void entrance(CorridorWriter corridors) {
        Slot hall = entranceSlot();
        int sx = hall.minX(), sz = hall.minZ();
        if (underKeep()) {
            // Up into the citadel's keep; the citadel sets the dungeon's entrance and surface itself.
            corridors.shaft(sx, sz, hall.floor(), hall.floor() + hall.template.height + 1, keepFloor, false);
            return;
        }
        int top = island.centreY();
        int gx = sx + 2, gz = sz + 2, gy = top + 1;
        int h = SkyDungeon.GATEWAY_HALF;
        for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++) {
            d.floorTile(gx + dx, gy - 1, gz + dz);
            for (int dy = 0; dy <= 5; dy++) d.put(gx + dx, gy + dy, gz + dz, Block.AIR);
        }
        int[][] corners = { { -h, -h }, { h, -h }, { -h, h }, { h, h } };
        boolean[] whole = new boolean[4];
        for (int c = 0; c < 4; c++) {
            int broken = (int) (d.noise.unit(gx + c, gz, 10) * 3);
            whole[c] = broken == 0;
            for (int dy = 0; dy < 4 - broken; dy++) d.pillar(gx + corners[c][0], gy + dy, gz + corners[c][1]);
            if (broken > 0) d.put(gx + corners[c][0], gy + 4 - broken, gz + corners[c][1], Block.SKY_SLAB, 0);
        }
        if (whole[0] && whole[1]) for (int dx = -h; dx <= h; dx++) d.polished(gx + dx, gy + 4, gz - h);
        corridors.shaft(sx, sz, hall.floor(), hall.floor() + hall.template.height + 1, top, false);
        d.entrance = new int[] { sx, gy, sz + 2 };
        d.surface = new int[] { gx - h - 1, gz - h - 1, gx + h + 1, gz + h + 1 };
    }
}
