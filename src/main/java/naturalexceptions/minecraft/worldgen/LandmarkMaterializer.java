package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;

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
}
