package naturalexceptions.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Networks are what touches what, and what relays reach; nothing else joins them. */
final class PowerTopologyTest {

    private static final class World implements PowerTopology.View {

        final Set<Long> nodes = new HashSet<>(), relays = new HashSet<>();

        World put(int x, int y, int z) {
            nodes.add(PowerTopology.key(x, y, z));
            return this;
        }

        World putRelay(int x, int y, int z) {
            put(x, y, z);
            relays.add(PowerTopology.key(x, y, z));
            return this;
        }

        @Override
        public boolean node(int x, int y, int z) {
            return nodes.contains(PowerTopology.key(x, y, z));
        }

        @Override
        public boolean relay(int x, int y, int z) {
            return relays.contains(PowerTopology.key(x, y, z));
        }

        @Override
        public Collection<int[]> relays() {
            List<int[]> out = new ArrayList<>();
            for (long k : relays) out.add(PowerTopology.unkey(k));
            return out;
        }
    }

    @Test
    void keysRoundTripEvenBelowZero() {
        for (int[] p : new int[][] { { 0, 0, 0 }, { -5, 64, 12 }, { 30000, 255, -30000 }, { -1, 1, -1 } }) {
            int[] back = PowerTopology.unkey(PowerTopology.key(p[0], p[1], p[2]));
            assertEquals(p[0], back[0]);
            assertEquals(p[1], back[1]);
            assertEquals(p[2], back[2]);
        }
    }

    @Test
    void aNetworkIsWhatTouches() {
        World w = new World();
        for (int x = 0; x < 10; x++) w.put(x, 64, 0);
        w.put(20, 64, 0); // apart
        w.put(9, 65, 0)
            .put(9, 66, 0); // up a wall
        assertEquals(
            12,
            PowerTopology.network(w, 0, 64, 0)
                .size());
        assertEquals(
            1,
            PowerTopology.network(w, 20, 64, 0)
                .size());
        assertEquals(
            0,
            PowerTopology.network(w, 5, 70, 5)
                .size(),
            "air is no network");
        // Diagonals do not touch.
        World d = new World().put(0, 64, 0)
            .put(1, 65, 0);
        assertEquals(
            1,
            PowerTopology.network(d, 0, 64, 0)
                .size());
    }

    @Test
    void relaysJoinWhatTheyReach() {
        World w = new World();
        w.put(0, 64, 0)
            .putRelay(1, 64, 0);
        w.putRelay(14, 64, 0)
            .put(15, 64, 0);
        w.putRelay(40, 64, 0)
            .put(41, 64, 0); // out of reach
        List<int[]> net = PowerTopology.network(w, 0, 64, 0);
        assertEquals(4, net.size(), "across the gap by relay");
        assertEquals(
            2,
            PowerTopology.network(w, 41, 64, 0)
                .size());
        assertTrue(PowerTopology.relayOhms(13) > PowerTopology.relayOhms(2), "the longer beam loses more");
    }
}
