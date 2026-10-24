package naturalexceptions.minecraft.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import naturalexceptions.core.power.Cable;
import naturalexceptions.core.power.Grid;
import naturalexceptions.core.power.PowerTopology;

/**
 * The electrical networks of a world, on the server: found again where blocks changed, solved five times a second,
 * their machines told what they get, their cables warmed by what they carry (and burnt out by too much). A network
 * with nothing to drive it is dead and costs nothing but cooling.
 */
public final class PowerNetworks {

    /** A contact's resistance: a machine's terminal, a switch's blade, a relay's post. */
    public static final double CONTACT_OHMS = 0.0005;
    /** Solve every so many ticks. */
    public static final int TICKS = 4;

    private static final Map<World, PowerNetworks> WORLDS = new WeakHashMap<>();

    public static synchronized PowerNetworks of(World world) {
        return WORLDS.computeIfAbsent(world, PowerNetworks::new);
    }

    /** Something at (x, y, z) changed: its network is found again before the next solve. */
    public static void dirty(World world, int x, int y, int z) {
        if (world != null && !world.isRemote) of(world).dirty.add(PowerTopology.key(x, y, z));
    }

    /** A relay was set or taken away: the relays it reached are joined anew. */
    public static void relay(World world, int x, int y, int z, boolean present) {
        if (world == null || world.isRemote) return;
        PowerNetworks networks = of(world);
        long k = PowerTopology.key(x, y, z);
        if (present) networks.relays.add(k);
        else networks.relays.remove(k);
        networks.dirty.add(k);
        for (int[] r : networks.view.relays())
            if (PowerTopology.distance(r, new int[] { x, y, z }) <= PowerTopology.RELAY_REACH)
                networks.dirty.add(PowerTopology.key(r[0], r[1], r[2]));
    }

    private static final class Net {

        final List<int[]> blocks;
        final Map<Long, Integer> index;
        double[] volts;
        boolean live;

        Net(List<int[]> blocks) {
            this.blocks = blocks;
            this.index = PowerTopology.index(blocks);
            this.volts = new double[blocks.size()];
        }
    }

    private final World world;
    private final Set<Long> dirty = new HashSet<>();
    private final Map<Long, Net> byPos = new HashMap<>();
    private final List<Net> nets = new ArrayList<>();
    private final Set<Long> relays = new HashSet<>();
    /** Cable temperatures that are not ambient. */
    private final Map<Long, Double> heat = new HashMap<>();

    private final PowerTopology.View view = new PowerTopology.View() {

        @Override
        public boolean node(int x, int y, int z) {
            if (y < 0 || y > 255
                || !world.getChunkProvider()
                    .chunkExists(x >> 4, z >> 4))
                return false;
            Block block = world.getBlock(x, y, z);
            if (block instanceof PowerBlock) return ((PowerBlock) block).conducts(world, x, y, z);
            return world.getTileEntity(x, y, z) instanceof PowerDevice;
        }

        @Override
        public boolean relay(int x, int y, int z) {
            return relays.contains(PowerTopology.key(x, y, z)) && node(x, y, z);
        }

        @Override
        public Collection<int[]> relays() {
            List<int[]> out = new ArrayList<>();
            for (long k : relays) out.add(PowerTopology.unkey(k));
            return out;
        }
    };

    private PowerNetworks(World world) {
        this.world = world;
    }

    /** The voltage at (x, y, z) at the last solve (0 if it is in no live network). */
    public double voltage(int x, int y, int z) {
        long k = PowerTopology.key(x, y, z);
        Net net = byPos.get(k);
        if (net == null || !net.live) return 0.0;
        Integer i = net.index.get(k);
        return i == null ? 0.0 : net.volts[i];
    }

    /** A cable's temperature (kelvin). */
    public double temperature(int x, int y, int z) {
        Double t = heat.get(PowerTopology.key(x, y, z));
        return t == null ? Cable.AMBIENT_KELVIN : t;
    }

    /** How many blocks the network through (x, y, z) has (0 if none). */
    public int networkSize(int x, int y, int z) {
        Net net = byPos.get(PowerTopology.key(x, y, z));
        return net == null ? 0 : net.blocks.size();
    }

    // --- Each tick ------------------------------------------------------------------------------------------

    public void tick() {
        if (world.getTotalWorldTime() % TICKS != 0) return;
        if (!dirty.isEmpty()) rebuild();
        for (Net net : new ArrayList<>(nets)) solve(net, TICKS / 20.0);
    }

    private void rebuild() {
        Set<Long> seeds = new HashSet<>();
        for (long k : dirty) {
            int[] p = PowerTopology.unkey(k);
            seeds.add(k);
            for (int[] f : new int[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
                { 0, 0, -1 } }) seeds.add(PowerTopology.key(p[0] + f[0], p[1] + f[1], p[2] + f[2]));
        }
        dirty.clear();
        for (long k : new ArrayList<>(seeds)) {
            Net old = byPos.get(k);
            if (old == null || !nets.remove(old)) continue;
            for (int[] b : old.blocks) {
                long bk = PowerTopology.key(b[0], b[1], b[2]);
                byPos.remove(bk);
                seeds.add(bk);
            }
        }
        for (long k : seeds) {
            if (byPos.containsKey(k)) continue;
            int[] p = PowerTopology.unkey(k);
            List<int[]> blocks = PowerTopology.network(view, p[0], p[1], p[2]);
            if (blocks.isEmpty()) continue;
            Net net = new Net(blocks);
            nets.add(net);
            for (int[] b : blocks) byPos.put(PowerTopology.key(b[0], b[1], b[2]), net);
        }
    }

    private Cable cableAt(int[] p) {
        Block block = world.getBlock(p[0], p[1], p[2]);
        return block instanceof BlockCable ? Cable.of(world.getBlockMetadata(p[0], p[1], p[2])) : null;
    }

    private double ohms(int[] p) {
        Block block = world.getBlock(p[0], p[1], p[2]);
        return block instanceof PowerBlock ? ((PowerBlock) block).ohms(world, p[0], p[1], p[2]) : CONTACT_OHMS;
    }

    private void solve(Net net, double seconds) {
        int n = net.blocks.size();
        Grid grid = new Grid(n);
        grid.warmStart(net.volts);
        List<int[]> edges = new ArrayList<>(); // {a, b, resistor}
        double[] half = new double[n];
        for (int i = 0; i < n; i++) half[i] = ohms(net.blocks.get(i)) / 2.0;
        for (int i = 0; i < n; i++) {
            int[] p = net.blocks.get(i);
            for (int[] f : new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }) {
                Integer j = net.index.get(PowerTopology.key(p[0] + f[0], p[1] + f[1], p[2] + f[2]));
                if (j != null) edges.add(new int[] { i, j, grid.resistor(i, j, half[i] + half[j]) });
            }
            if (view.relay(p[0], p[1], p[2])) for (int[] r : PowerTopology.relayLinks(view, p)) {
                Integer j = net.index.get(PowerTopology.key(r[0], r[1], r[2]));
                if (j != null && j > i) edges.add(
                    new int[] { -1, -1, grid.resistor(i, j, PowerTopology.relayOhms(PowerTopology.distance(p, r))) });
            }
        }
        List<PowerDevice> devices = new ArrayList<>();
        List<Integer> deviceNodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] p = net.blocks.get(i);
            TileEntity tile = world.getTileEntity(p[0], p[1], p[2]);
            if (tile instanceof PowerDevice) {
                devices.add((PowerDevice) tile);
                deviceNodes.add(i);
                ((PowerDevice) tile).stamp(grid, i);
            }
        }
        boolean live = grid.hasSources();
        if (live) {
            grid.solve();
            net.volts = grid.voltages();
        } else java.util.Arrays.fill(net.volts, 0.0);
        net.live = live;
        for (int d = 0; d < devices.size(); d++) devices.get(d)
            .powered(grid, deviceNodes.get(d), seconds);
        warm(net, grid, edges, half, seconds, live);
    }

    /** Each cable warms by what it carries (half of each length it shares) and cools to the air; too hot, it fails. */
    private void warm(Net net, Grid grid, List<int[]> edges, double[] half, double seconds, boolean live) {
        int n = net.blocks.size();
        double[] watts = new double[n];
        if (live) for (int[] e : edges) {
            if (e[0] < 0) continue;
            double i = grid.current(e[2]);
            watts[e[0]] += i * i * half[e[0]];
            watts[e[1]] += i * i * half[e[1]];
        }
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            int[] p = net.blocks.get(i);
            long k = PowerTopology.key(p[0], p[1], p[2]);
            Double was = heat.get(k);
            if (watts[i] <= 0.0 && was == null) continue;
            Cable cable = cableAt(p);
            if (cable == null) continue;
            double t = cable.step(was == null ? Cable.AMBIENT_KELVIN : was, watts[i], seconds);
            if (t < Cable.AMBIENT_KELVIN + 0.5 && watts[i] <= 0.0) heat.remove(k);
            else heat.put(k, t);
            if (t >= cable.limitKelvin()) {
                fail(p, cable, t);
                changed = true;
            } else if (world instanceof WorldServer && world.getTotalWorldTime() % 20 == 0) {
                if (!cable.insulated && t >= Cable.GLOW_KELVIN) ((WorldServer) world)
                    .func_147487_a("flame", p[0] + 0.5, p[1] + 0.5, p[2] + 0.5, 1, 0.1, 0.1, 0.1, 0.0);
                else if (t >= cable.limitKelvin() - (cable.limitKelvin() - Cable.AMBIENT_KELVIN) * 0.15)
                    ((WorldServer) world)
                        .func_147487_a("smoke", p[0] + 0.5, p[1] + 0.6, p[2] + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
            }
        }
        if (changed) net.live = false;
    }

    /** Cloth chars off (leaving bare copper), or copper melts through (leaving nothing). */
    private void fail(int[] p, Cable cable, double t) {
        Cable left = cable.failed();
        world.playSoundEffect(p[0] + 0.5, p[1] + 0.5, p[2] + 0.5, "random.fizz", 0.8F, left == null ? 0.6F : 1.4F);
        if (world instanceof WorldServer) ((WorldServer) world).func_147487_a(
            left == null ? "lava" : "largesmoke",
            p[0] + 0.5,
            p[1] + 0.5,
            p[2] + 0.5,
            8,
            0.2,
            0.2,
            0.2,
            0.02);
        if (left == null) {
            world.setBlockToAir(p[0], p[1], p[2]);
            heat.remove(PowerTopology.key(p[0], p[1], p[2]));
        } else world.setBlockMetadataWithNotify(p[0], p[1], p[2], left.ordinal(), 3);
        dirty(world, p[0], p[1], p[2]);
    }

    /** Ticks every world's networks (server). */
    public static final class Ticker {

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.world.isRemote) of(event.world).tick();
        }
    }
}
