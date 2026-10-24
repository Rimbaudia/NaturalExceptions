package naturalexceptions.core.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which blocks make up which electrical network: everything joined to everything else through touching faces, and
 * relays joined to the relays in their reach. Worked out over a {@link View} of the world, so the world itself (and
 * the chunks not loaded) stays behind it.
 */
public final class PowerTopology {

    /** What the topology needs to know of the world. */
    public interface View {

        /** Whether (x, y, z) is part of any network (a cable, a closed switch, a relay, a machine). */
        boolean node(int x, int y, int z);

        /** Whether (x, y, z) is a relay, reaching the relays round it. */
        boolean relay(int x, int y, int z);

        /** The relays known in the world, {x, y, z} each. */
        Collection<int[]> relays();
    }

    /** How far a relay reaches another. */
    public static final int RELAY_REACH = 16;
    /** No network grows past this many blocks (a runaway build is cut there). */
    public static final int MAX_NODES = 4096;

    private PowerTopology() {}

    public static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | (long) z & 0x3FFFFFFL;
    }

    public static int[] unkey(long k) {
        int x = (int) (k >> 38), y = (int) (k << 26 >> 52), z = (int) (k << 38 >> 38);
        return new int[] { x, y, z };
    }

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** The network grown from (x, y, z), in the order its blocks were reached; empty if it is not a node. */
    public static List<int[]> network(View view, int x, int y, int z) {
        List<int[]> out = new ArrayList<>();
        if (!view.node(x, y, z)) return out;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { x, y, z });
        seen.add(key(x, y, z));
        while (!queue.isEmpty() && out.size() < MAX_NODES) {
            int[] p = queue.poll();
            out.add(p);
            for (int[] f : FACES) visit(view, p[0] + f[0], p[1] + f[1], p[2] + f[2], seen, queue);
            if (view.relay(p[0], p[1], p[2]))
                for (int[] r : relayLinks(view, p)) visit(view, r[0], r[1], r[2], seen, queue);
        }
        return out;
    }

    private static void visit(View view, int x, int y, int z, Set<Long> seen, ArrayDeque<int[]> queue) {
        if (!seen.add(key(x, y, z)) || !view.node(x, y, z)) return;
        queue.add(new int[] { x, y, z });
    }

    /** The other relays a relay at {@code at} reaches. */
    public static List<int[]> relayLinks(View view, int[] at) {
        List<int[]> out = new ArrayList<>();
        for (int[] r : view.relays()) {
            if (r[0] == at[0] && r[1] == at[1] && r[2] == at[2]) continue;
            if (distance(at, r) <= RELAY_REACH && view.relay(r[0], r[1], r[2])) out.add(r);
        }
        return out;
    }

    public static double distance(int[] a, int[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** A relay link's resistance: a little for the beam, more the farther it reaches. */
    public static double relayOhms(double distance) {
        return 0.3 + 0.03 * distance;
    }

    /** Index of every block in a network, by its key. */
    public static Map<Long, Integer> index(List<int[]> network) {
        Map<Long, Integer> index = new HashMap<>();
        for (int i = 0; i < network.size(); i++) {
            int[] p = network.get(i);
            index.put(key(p[0], p[1], p[2]), i);
        }
        return index;
    }
}
