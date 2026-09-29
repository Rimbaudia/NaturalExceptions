package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;

import naturalexceptions.core.worldgen.Landmarks;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Writes landmark shapes into one chunk. Every shape is a pure function of world coordinates, so each chunk fills
 * only its own columns and a landmark spanning many chunks comes out whole whatever order they generate in.
 */
final class LandmarkMaterializer {

    /** Ore share in island stone. */
    static final double COAL = 0.015;
    static final double IRON = 0.008;
    static final double COPPER = 0.010;
    /** Features that reach outside their column (trees) are placed only this far inside the chunk. */
    private static final int INSET = 3;

    private LandmarkMaterializer() {}

    static int skyIslands(World world, int chunkX, int chunkZ, SkyIslands field) {
        int x0 = chunkX * 16;
        int z0 = chunkZ * 16;
        int placed = 0;
        for (SkyIslands.Island island : field.near(x0, z0, x0 + 15, z0 + 15)) {
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int[] column = field.column(island, x, z);
                    if (column == null) continue;
                    boolean pond = field.pondAt(island, x, z);
                    for (int y = column[0]; y <= column[1]; y++) {
                        world.setBlock(x, y, z, islandBlock(field, x, y, z, column, pond), 0, 2);
                        placed++;
                    }
                }
            }
            if (island.hasTree()) plantTree(world, field, island, x0, z0);
            if (island.hasCache()) placeCache(world, field, island, x0, z0);
        }
        return placed;
    }

    private static Block islandBlock(SkyIslands field, int x, int y, int z, int[] column, boolean pond) {
        if (y == column[1]) return pond ? Blocks.water : Blocks.grass;
        if (y >= column[1] - 3) return Blocks.dirt;
        double roll = field.blockRoll(x, y, z, 11);
        if (roll < COAL) return Blocks.coal_ore;
        if (roll < COAL + IRON) return Blocks.iron_ore;
        if (roll < COAL + IRON + COPPER && NEBlocks.copperOre != null) return NEBlocks.copperOre;
        return Blocks.stone;
    }

    private static boolean insideChunk(int x, int z, int x0, int z0, int inset) {
        return x >= x0 + inset && x < x0 + 16 - inset && z >= z0 + inset && z < z0 + 16 - inset;
    }

    private static void plantTree(World world, SkyIslands field, SkyIslands.Island island, int x0, int z0) {
        int[] spot = field.treeSpot(island);
        if (!insideChunk(spot[0], spot[1], x0, z0, INSET)) return;
        int[] column = field.column(island, spot[0], spot[1]);
        if (column == null || field.pondAt(island, spot[0], spot[1])) return;
        Random random = new Random(island.cellX() * 341873128712L ^ island.cellZ() * 132897987541L);
        new WorldGenTrees(false).generate(world, random, spot[0], column[1] + 1, spot[1]);
    }

    private static void placeCache(World world, SkyIslands field, SkyIslands.Island island, int x0, int z0) {
        int[] spot = field.cacheSpot(island);
        if (!insideChunk(spot[0], spot[1], x0, z0, 0)) return;
        int[] column = field.column(island, spot[0], spot[1]);
        if (column == null || field.pondAt(island, spot[0], spot[1])) return;
        int y = column[1] + 1;
        world.setBlock(spot[0], y, spot[1], Blocks.chest, 0, 2);
        TileEntity tile = world.getTileEntity(spot[0], y, spot[1]);
        if (tile instanceof TileEntityChest)
            SkyCacheLoot.fill((TileEntityChest) tile, island.cellX() * 0x9E3779B97F4A7C15L ^ island.cellZ());
    }

    // --- Giant terrain features ------------------------------------------------------------------------------

    static int landmarks(World world, int chunkX, int chunkZ, Landmarks field) {
        int x0 = chunkX * 16;
        int z0 = chunkZ * 16;
        int changed = 0;
        for (Landmarks.Landmark landmark : field.near(x0, z0, x0 + 15, z0 + 15)) {
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    switch (landmark.kind()) {
                        case CHASM:
                            changed += chasmColumn(world, field, landmark, x, z);
                            break;
                        case SPIRES:
                            changed += spireColumn(world, field, landmark, x, z);
                            break;
                        case SINKHOLE:
                            changed += sinkholeColumn(world, field, landmark, x, z);
                            break;
                        case ARCH:
                        default:
                            changed += archColumn(world, landmark, x, z);
                            break;
                    }
                }
            }
        }
        return changed;
    }

    /** The topmost solid block of a column, ignoring water and plants above it. */
    private static int groundY(World world, int x, int z) {
        int y = world.getHeightValue(x, z) - 1;
        while (y > 1) {
            Block block = world.getBlock(x, y, z);
            if (block.getMaterial()
                .isSolid() && !block.isLeaves(world, x, y, z)
                && !block.isWood(world, x, y, z)) return y;
            y--;
        }
        return y;
    }

    private static boolean underWater(World world, int x, int z) {
        Block top = world.getBlock(x, world.getHeightValue(x, z) - 1, z);
        return top.getMaterial()
            .isLiquid() || top == Blocks.ice;
    }

    private static int chasmColumn(World world, Landmarks field, Landmarks.Landmark chasm, int x, int z) {
        double[] here = field.chasm(chasm, x, z);
        if (here == null || underWater(world, x, z)) return 0;
        int surface = world.getHeightValue(x, z) - 1;
        int changed = 0;
        for (int y = surface; y >= Landmarks.CHASM_FLOOR_Y; y--) {
            if (Landmarks.chasmOpen(here, y, surface)) {
                world.setBlock(x, y, z, Blocks.air, 0, 2);
                changed++;
            } else if (world.getBlock(x, y, z) == Blocks.stone && here[1] < here[0]
                * (0.45
                    + 0.55 * (y - Landmarks.CHASM_FLOOR_Y) / (double) Math.max(1, surface - Landmarks.CHASM_FLOOR_Y))
                + 1.5) {
                    // Freshly torn walls show their ore.
                    double roll = fieldRoll(x, y, z);
                    Block ore = roll < 0.020 ? Blocks.coal_ore
                        : roll < 0.035 ? Blocks.iron_ore
                            : roll < 0.045 && NEBlocks.copperOre != null ? NEBlocks.copperOre
                                : roll < 0.050 ? Blocks.gold_ore : null;
                    if (ore != null) world.setBlock(x, y, z, ore, 0, 2);
                }
        }
        if (Landmarks.chasmOpen(here, Landmarks.CHASM_FLOOR_Y, surface)) {
            world.setBlock(x, Landmarks.CHASM_FLOOR_Y - 1, z, Blocks.lava, 0, 2);
            changed++;
        }
        return changed;
    }

    private static int spireColumn(World world, Landmarks field, Landmarks.Landmark spires, int x, int z) {
        boolean close = false;
        for (Landmarks.Pillar pillar : spires.pillars()) {
            double dx = x + 0.5 - pillar.x, dz = z + 0.5 - pillar.z;
            if (dx * dx + dz * dz < (pillar.radius * 1.2 + 1) * (pillar.radius * 1.2 + 1)) close = true;
        }
        if (!close) return 0;
        int ground = groundY(world, x, z);
        int changed = 0;
        int top = -1;
        for (Landmarks.Pillar pillar : spires.pillars()) top = Math.max(top, pillar.topY);
        for (int y = ground + 1; y <= top; y++) {
            if (!field.spireAt(spires, x, y, z, ground)) continue;
            boolean cap = !field.spireAt(spires, x, y + 1, z, ground);
            Block block = cap ? Blocks.grass : (y / 7) % 3 == 0 ? Blocks.cobblestone : Blocks.stone;
            world.setBlock(x, y, z, block, 0, 2);
            changed++;
        }
        return changed;
    }

    private static int sinkholeColumn(World world, Landmarks field, Landmarks.Landmark sinkhole, int x, int z) {
        double dx = x + 0.5 - sinkhole.x(), dz = z + 0.5 - sinkhole.z();
        if (dx * dx + dz * dz > sinkhole.reach() * sinkhole.reach() || underWater(world, x, z)) return 0;
        int surface = world.getHeightValue(x, z) - 1;
        int changed = 0;
        for (int y = Landmarks.CAVERN_FLOOR_Y; y <= surface; y++) {
            if (!field.sinkholeOpen(sinkhole, x, y, z, surface)) continue;
            world.setBlock(x, y, z, y <= Landmarks.CAVERN_WATER_TOP_Y ? Blocks.water : Blocks.air, 0, 2);
            changed++;
        }
        return changed;
    }

    private static int archColumn(World world, Landmarks.Landmark arch, int x, int z) {
        double dx = x + 0.5 - arch.x(), dz = z + 0.5 - arch.z();
        if (dx * dx + dz * dz > arch.reach() * arch.reach()) return 0;
        int ground = groundY(world, x, z);
        int top = Landmarks.ARCH_BASE_Y + (int) Math.ceil(arch.height() + arch.width() * 1.6) + 1;
        int changed = 0;
        for (int y = ground + 1; y <= top; y++) {
            if (!Landmarks.archAt(arch, x, y, z, ground)) continue;
            boolean cap = !Landmarks.archAt(arch, x, y + 1, z, ground);
            world.setBlock(x, y, z, cap ? Blocks.grass : Blocks.stone, 0, 2);
            changed++;
        }
        return changed;
    }

    private static double fieldRoll(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((h ^ (h >>> 29)) >>> 11) * 0x1.0p-53;
    }
}
