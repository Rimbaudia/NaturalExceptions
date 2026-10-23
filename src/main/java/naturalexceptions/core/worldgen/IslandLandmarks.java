package naturalexceptions.core.worldgen;

import java.util.Random;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Loot;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.TemplateLibrary;

/**
 * The places out on a dungeon's island, away from its walls: a lumber camp with its Sawmill, the cloudwright's
 * condensers along the windward rim, a Stormglass Spire in its scorched crater, a lighthouse on the rim. Each is laid
 * out on a {@link CitadelSites.Site}, in offsets from the island's middle (cx, cz), with the helpers the citadel's own
 * outskirts use: judging and levelling ground, setting an authored building on it.
 */
final class IslandLandmarks {

    private final SkyDungeon d;
    private final int cx, cz, b;
    private final Random random;

    IslandLandmarks(SkyDungeon d, int cx, int cz, int b, Random random) {
        this.d = d;
        this.cx = cx;
        this.cz = cz;
        this.b = b;
        this.random = random;
    }

    // --- Helpers --------------------------------------------------------------------------------------------

    void put(int dx, int y, int dz, Block block, int meta) {
        d.put(cx + dx, y, cz + dz, block, meta);
    }

    void put(int dx, int y, int dz, Block block) {
        d.put(cx + dx, y, cz + dz, block);
    }

    void put(int dx, int y, int dz, Block block, int meta, Loot loot, String mob) {
        d.put(cx + dx, y, cz + dz, block, meta, loot, mob);
    }

    /** The ground height most of the rectangle stands at, or -1 if any of it is off the island or too uneven. */
    int ground(int x0, int z0, int x1, int z1) {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        int least = Integer.MAX_VALUE, most = Integer.MIN_VALUE;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int[] column = d.field.column(d.island, cx + x, cz + z);
            if (column == null) return -1;
            counts.merge(column[1], 1, Integer::sum);
            least = Math.min(least, column[1]);
            most = Math.max(most, column[1]);
        }
        if (most - least > 4) return -1;
        int best = -1, bestCount = -1;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) if (e.getValue() > bestCount) {
            best = e.getKey();
            bestCount = e.getValue();
        }
        return Math.min(best, b);
    }

    /** Brings the rectangle's ground to height {@code g}: earth filled under it, grass on top, rock cut away. */
    void level(int x0, int z0, int x1, int z1, int g) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int[] column = d.field.column(d.island, cx + x, cz + z);
            if (column == null) continue;
            int top = column[1];
            for (int y = Math.min(top + 1, g - 1); y < g; y++) put(x, y, z, Block.DIRT);
            put(x, g, z, Block.GRASS);
            for (int y = g + 1; y <= top + 1; y++) put(x, y, z, Block.AIR);
        }
    }

    /** A paved place on ground {@code g}, open above. */
    void paveAt(int dx, int dz, int g) {
        d.floorTile(cx + dx, g, cz + dz);
        for (int y = g + 1; y <= g + 3; y++) put(dx, y, dz, Block.AIR);
    }

    void lampPost(int dx, int dz, int g) {
        put(dx, g + 1, dz, Block.FENCE);
        put(dx, g + 2, dz, Block.FENCE);
        put(dx, g + 3, dz, Block.LAMP, SkyDungeon.LAMP_STANDING);
    }

    /** An authored building out on the island at (minX, minZ), facing south, set on ground {@code g}. */
    void building(String name, int minX, int minZ, String feature, int g) {
        RoomTemplate t = TemplateLibrary.building(name);
        int maxX = minX + t.width() - 1, maxZ = minZ + t.depth() - 1;
        level(minX - 2, minZ - 2, maxX + 2, maxZ + 4, g);
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) put(x, g - 1, z, Block.SKYSLATE, 0);
        new RoomWriter(d).write(
            t,
            0,
            cx + minX,
            cz + minZ,
            g,
            t.roles.iterator()
                .next(),
            false);
        paveAt((minX + maxX) / 2, maxZ + 1, g);
        paveAt((minX + maxX) / 2, maxZ + 2, g);
        if (feature != null) d.features.add(feature);
    }

    /**
     * A work shed at (x0, z0), six by five and open to the west: log posts, plank walls on its other three sides, a
     * slab roof; inside, a cutting bench ({@code bench}: 0 Mason's, 1 Sawmill), a chest of {@code loot}, a water
     * barrel, and an anvil or a crate.
     */
    void shed(int x0, int z0, int g, int bench, Loot loot) {
        int x1 = x0 + 5, z1 = z0 + 4;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            put(x, g, z, Block.PLANKS, 1);
            boolean post = (x == x0 || x == x1) && (z == z0 || z == z1);
            boolean wall = x == x1 || z == z0 || z == z1;
            for (int y = g + 1; y <= g + 3; y++)
                put(x, y, z, post ? Block.LOG : wall ? Block.PLANKS : Block.AIR, post || wall ? 1 : 0);
            put(x, g + 4, z, Block.SLAB, 1);
        }
        put(x1 - 1, g + 1, z0 + 1, Block.CUTTING_BENCH, bench | SkyDungeon.WEST << 3); // facing out of the shed
        put(x1 - 1, g + 1, z0 + 2, Block.CHEST, 0, loot, null);
        put(x1 - 1, g + 1, z0 + 3, Block.BARREL, 1);
        put(x0 + 2, g + 1, z0 + 1, bench == 0 ? Block.ANVIL : Block.CRATE);
        put(x0 + 2, g + 3, z0 + 3, Block.LAMP, SkyDungeon.LAMP_HANGING); // from the roof
    }

    /** A spruce as the island grows them: a straight trunk under a cone of boughs. */
    void spruce(int dx, int dz, int g, int height) {
        for (int y = g + 1; y <= g + height; y++) put(dx, y, dz, Block.LOG, 1);
        for (int y = g + 3; y <= g + height + 1; y++) {
            int r = Math.max(0, (g + height + 1 - y) / 2);
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (x == 0 && z == 0 && y <= g + height) continue;
                if (Math.abs(x) + Math.abs(z) <= r + (r > 1 ? 1 : 0)) put(dx + x, y, dz + z, Block.LEAVES);
            }
        }
    }

    // --- The lumber camp ------------------------------------------------------------------------------------

    /**
     * A lumber camp in a clearing of stumps ringed with standing spruce: the woodcutter's cabin, the Sawmill in its
     * shed, a pile of cut logs, and trees felled and lying where they came down, their crowns at their ends.
     */
    void lumberCamp(CitadelSites.Site s) {
        int g = s.ground;
        level(s.x0, s.z0, s.x1, s.z1, g);
        building("woodcutters_hut", s.x0 + 2, s.z0 + 2, null, g);
        shed(s.x0 + 16, s.z0 + 3, g, 1, Loot.HOUSEHOLD);
        // The log pile, cut lengths lying east to west, stacked two high.
        for (int x = s.x0 + 4; x <= s.x0 + 10; x++) {
            for (int z = s.z1 - 5; z <= s.z1 - 3; z++) put(x, g + 1, z, Block.LOG, 1 | 16);
            for (int z = s.z1 - 5; z <= s.z1 - 4; z++) put(x, g + 2, z, Block.LOG, 1 | 16);
        }
        // Felled trees: a trunk along the ground, its crown of boughs at the far end.
        int[][] felled = { { s.x0 + 14, s.z0 + 12, 1, 0, 7 }, { s.x1 - 3, s.z0 + 11, 0, 1, 6 } };
        for (int[] f : felled) {
            for (int k = 0; k < f[4]; k++)
                put(f[0] + f[2] * k, g + 1, f[1] + f[3] * k, Block.LOG, 1 | (f[2] != 0 ? 16 : 32));
            int ex = f[0] + f[2] * f[4], ez = f[1] + f[3] * f[4];
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 1; y <= 2; y++)
                if (Math.abs(x) + Math.abs(z) + (y - 1) <= 2) put(ex + x, g + y, ez + z, Block.LEAVES);
        }
        // Stumps where the felled trees stood, and the standing wood round the clearing.
        for (int[] st : new int[][] { { 13, 17 }, { 16, 10 }, { 20, 18 }, { 11, 14 }, { 22, 9 } })
            put(s.x0 + st[0], g + 1, s.z0 + st[1], Block.LOG, 1);
        for (int x = s.x0; x <= s.x1; x += 4) for (int z : new int[] { s.z0, s.z1 })
            if (x > s.x0 + 13 || z == s.z1) spruce(x, z, g, 5 + random.nextInt(3));
        for (int z = s.z0 + 4; z <= s.z1 - 4; z += 4) spruce(s.x1, z, g, 5 + random.nextInt(3));
        d.features.add("lumber camp");
    }

    // --- The condensers -------------------------------------------------------------------------------------

    /**
     * The cloudwright's terrace on the windward rim: two rows of Cloud Condensers on tiled pads inside a fence, a
     * gate towards the town, and the cloudwright's stone hut at its end.
     */
    void condensers(CitadelSites.Site s) {
        int g = s.ground;
        level(s.x0, s.z0, s.x1, s.z1, g);
        int fx0 = s.x0 + 1, fx1 = s.x0 + 14, fz0 = s.z0 + 2, fz1 = s.z0 + 12;
        for (int x = fx0; x <= fx1; x++) for (int z = fz0; z <= fz1; z++) {
            boolean edge = x == fx0 || x == fx1 || z == fz0 || z == fz1;
            if (edge) put(x, g + 1, z, Block.FENCE);
            else if ((z - fz0) % 2 == 0) put(x, g, z, Block.GRAVEL); // the walks between the rows
        }
        put(fx0, g + 1, (fz0 + fz1) / 2, Block.FENCE_GATE, SkyDungeon.EAST);
        for (int row : new int[] { fz0 + 3, fz0 + 7 }) for (int x = fx0 + 2; x <= fx1 - 2; x += 2) {
            d.floorTile(cx + x, g, cz + row);
            put(x, g + 1, row, Block.CONDENSER);
        }
        lampPost(fx0 - 1, fz0 + 2, g);
        building("cloudwrights_hut", s.x0 + 17, s.z0 + 2, null, g);
        d.features.add("condensers");
    }

    // --- The Stormglass Spire -------------------------------------------------------------------------------

    /**
     * A Stormglass Spire: a crater of scorched rock and gravel, and out of it a jagged spire of Stormcrystal thirty
     * to forty high, leaning, with three lesser spikes about its foot and Stormglass hanging under its overhangs. The
     * highest block is the crown, which calls the lightning in a storm.
     */
    void spire(CitadelSites.Site s) {
        int g = s.ground, mx = (s.x0 + s.x1) / 2, mz = (s.z0 + s.z1) / 2;
        level(s.x0, s.z0, s.x1, s.z1, g);
        int crater = Math.min(s.x1 - s.x0, s.z1 - s.z0) / 2 - 1;
        for (int x = s.x0; x <= s.x1; x++) for (int z = s.z0; z <= s.z1; z++) {
            double r = Math.hypot(x - mx, z - mz);
            if (r > crater) continue;
            int depth = (int) Math.round(3 * (1 - (r / crater) * (r / crater)));
            for (int y = g - depth + 1; y <= g; y++) put(x, y, z, Block.AIR);
            double n = d.noise.unit(cx + x, cz + z, 81);
            put(
                x,
                g - depth,
                z,
                r < crater - 2 ? (n < 0.5 ? Block.ROCK : Block.GRAVEL) : Block.GRAVEL,
                n < 0.25 ? 1 : 0);
            put(x, g - depth - 1, z, Block.ROCK);
        }
        int floor = g - 3, height = 30 + random.nextInt(11);
        double lean = random.nextDouble() * Math.PI * 2, reach = 5 + random.nextInt(4);
        java.util.Set<Long> crystal = new java.util.HashSet<>();
        int[] top = null;
        for (int h = 0; h <= height; h++) {
            double t = (double) h / height;
            double r = 3.4 * Math.pow(1 - t, 0.9) + 0.4;
            double ox = mx + Math.cos(lean) * reach * Math.pow(t, 1.5),
                oz = mz + Math.sin(lean) * reach * Math.pow(t, 1.5);
            for (int x = (int) Math.floor(ox - r); x <= Math.ceil(ox + r); x++)
                for (int z = (int) Math.floor(oz - r); z <= Math.ceil(oz + r); z++) {
                    double jag = d.noise.unit3(cx + x, floor + h, cz + z, 82) * 0.9;
                    if (Math.hypot(x + 0.5 - ox, z + 0.5 - oz) > r + 0.3 - jag * 0.5) continue;
                    put(x, floor + 1 + h, z, Block.STORMCRYSTAL, 0);
                    crystal.add(key(x, floor + 1 + h, z));
                    if (top == null || floor + 1 + h >= top[1]) top = new int[] { x, floor + 1 + h, z };
                }
        }
        // Three lesser spikes about its foot, leaning out.
        for (int k = 0; k < 3; k++) {
            double a = lean + Math.PI * (0.6 + 0.7 * k) + random.nextDouble() * 0.4;
            int len = 8 + random.nextInt(7);
            for (int i = 0; i <= len; i++) {
                double t = (double) i / len, r = 1.4 * (1 - t) + 0.3;
                double ox = mx + Math.cos(a) * (3 + i * 0.5), oz = mz + Math.sin(a) * (3 + i * 0.5);
                for (int x = (int) Math.floor(ox - r); x <= Math.ceil(ox + r); x++)
                    for (int z = (int) Math.floor(oz - r); z <= Math.ceil(oz + r); z++) {
                        if (Math.hypot(x + 0.5 - ox, z + 0.5 - oz) > r) continue;
                        put(x, floor + 1 + i, z, Block.STORMCRYSTAL, 0);
                        crystal.add(key(x, floor + 1 + i, z));
                    }
            }
        }
        if (top != null) put(top[0], top[1], top[2], Block.STORMCRYSTAL, 1);
        // Stormglass hanging where the crystal overhangs open air.
        for (long c : crystal) {
            int x = (int) (c >> 40), y = (int) ((c >> 20) & 0xFFFFF) - 512, z = (int) (c & 0xFFFFF) - 524288;
            if (crystal.contains(key(x, y - 1, z)) || y - 1 <= floor + 1) continue;
            if (d.noise.unit3(cx + x, y, cz + z, 83) < 0.35) put(x, y - 1, z, Block.STORMGLASS_CLUSTER);
        }
        d.features.add("stormglass spire");
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) | ((long) (y + 512) << 20) | (z + 524288);
    }

    // --- The lighthouse -------------------------------------------------------------------------------------

    /**
     * A lighthouse on the rim: a round Skystone tower thirty high, a ladder up its inside past a store and a watch
     * room to the lantern room, glazed all round, where the Lighthouse Lamp turns its beams; a finial over it; the
     * keeper's cottage at its foot.
     */
    void lighthouse(CitadelSites.Site s) {
        int g = s.ground;
        level(s.x0, s.z0, s.x1, s.z1, g);
        int tx = s.x0 + 4, tz = s.z0 + 7, height = 29;
        int[] floors = { g, g + 8, g + 16, g + 24 };
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int x = tx + dx, z = tz + dz, rr = dx * dx + dz * dz;
            boolean inside = Math.max(Math.abs(dx), Math.abs(dz)) <= 2, rim = !inside && rr <= 13;
            if (!inside && !rim) continue;
            d.stone(cx + x, g - 1, cz + z);
            for (int y = g; y <= g + height; y++) {
                boolean lantern = y > g + 24 && y < g + height;
                if (rim) {
                    if (lantern) put(x, y, z, Math.abs(dx) == Math.abs(dz) ? Block.PILLAR : Block.GLASS_PANE);
                    else if (y == g + 1 || y == g + height) put(x, y, z, Block.POLISHED);
                    else if (y == g + 8 || y == g + 16 || y == g + 24) put(x, y, z, Block.POLISHED); // string courses
                    else d.stone(cx + x, y, cz + z);
                } else if (y == g) d.floorTile(cx + x, y, cz + z);
                else if (y == g + height) put(x, y, z, Block.POLISHED);
                else if (y == g + 8 || y == g + 16) put(x, y, z, Block.PLANKS, 1);
                else if (y == g + 24) put(x, y, z, Block.POLISHED);
                else put(x, y, z, Block.AIR);
            }
        }
        // The roof: a Skyslate cap and a carved finial.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) put(tx + dx, g + height + 1, tz + dz, Block.SKYSLATE, 1);
        put(tx, g + height + 2, tz, Block.CHISELED);
        // The ladder up the north wall, through each floor to the lantern room.
        for (int y = g + 1; y <= g + 24; y++) put(tx, y, tz - 2, Block.LADDER, 3);
        // The door on the south, windows on the other faces of each storey.
        for (int y = g + 1; y <= g + 2; y++) put(tx, y, tz + 3, Block.AIR);
        put(tx, g + 3, tz + 3, Block.CHISELED);
        for (int f : new int[] { g + 4, g + 12, g + 20 }) {
            put(tx + 3, f, tz, Block.GLASS_PANE);
            put(tx - 3, f, tz, Block.GLASS_PANE);
            if (f > g + 4) put(tx, f, tz + 3, Block.GLASS_PANE);
        }
        // Ground floor: the oil store. First: crates and barrels. Second: the watch room, its chest and table.
        put(tx - 2, g + 1, tz + 1, Block.BARREL);
        put(tx - 2, g + 1, tz + 2, Block.BARREL);
        put(tx + 2, g + 1, tz + 2, Block.CRATE);
        put(tx - 2, g + 9, tz + 2, Block.CRATE);
        put(tx + 2, g + 9, tz + 2, Block.CRATE);
        put(tx + 2, g + 10, tz + 2, Block.CRATE);
        put(tx + 2, g + 9, tz - 1, Block.BARREL);
        put(tx - 1, g + 17, tz + 1, Block.WOODEN_TABLE, 1);
        put(tx - 2, g + 17, tz + 1, Block.CHAIR, 1 << 2 | SkyDungeon.WEST);
        put(tx + 2, g + 17, tz + 2, Block.CHEST, 0, Loot.HOUSEHOLD, null);
        // A lantern hanging from the ceiling of each storey, in the middle of the room.
        for (int ceiling : new int[] { g + 7, g + 15, g + 23 })
            put(tx, ceiling, tz + 1, Block.LAMP, SkyDungeon.LAMP_HANGING);
        // The lamp, on a polished pedestal in the middle of the lantern room.
        put(tx, g + 25, tz, Block.POLISHED);
        put(tx, g + 26, tz, Block.LIGHTHOUSE_LAMP);
        // The keeper's cottage at its foot, and a path from the tower door round to it.
        building("cottage", s.x0 + 10, s.z0 + 2, null, g);
        for (int z = tz + 4; z <= s.z1 - 1; z++) put(tx, g, z, Block.GRAVEL);
        d.features.add("lighthouse");
    }
}
