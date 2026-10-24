package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.feature.WorldGenTrees;

import naturalexceptions.core.worldgen.DeepLayers;
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
        // Island rock, soil, cloud and decor go straight into the chunk and it is lit once at the end: through
        // world.setBlock each block relit its column and neighbours, a quarter-second a chunk over a grand island.
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        List<int[]> glowing = new ArrayList<>();
        batchChunk = chunk;
        batchGlowing = glowing;
        try {
            placed = materializeIslands(world, chunk, x0, z0, field, glowing);
        } finally {
            batchChunk = null;
            batchGlowing = null;
        }
        if (placed > 0) relight(world, chunk, glowing);
        return placed;
    }

    /** The chunk being materialized, while it is: dungeon pieces written into it may skip world.setBlock too. */
    private static Chunk batchChunk;
    private static List<int[]> batchGlowing;
    /** Whether a block's class reacts to being placed (overrides onBlockAdded), by class. */
    private static final Map<Class<?>, Boolean> REACTS = new IdentityHashMap<>();

    private static int materializeIslands(World world, Chunk chunk, int x0, int z0, SkyIslands field,
        List<int[]> glowing) {
        int placed = 0;
        for (SkyIslands.Island island : field.near(x0, z0, x0 + 15, z0 + 15)) {
            // 1. Rock, soil and the kind's surface; the pond and the waterfall's channel.
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int[] column = field.column(island, x, z);
                    if (column == null) continue;
                    boolean water = field.pondAt(island, x, z) || field.waterfallAt(island, x, z);
                    SkyIslands.Surface surface = field.surface(island, x, z);
                    boolean grand = island.tier() == SkyIslands.Tier.GRAND;
                    for (int y = column[0]; y <= column[1]; y++) {
                        place(chunk, x, y, z, islandBlock(field, x, y, z, column, water, surface, grand), 0, glowing);
                        placed++;
                    }
                }
            }
            // 2. The cloud rim and puffs under the island.
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int[] cloud = field.cloud(island, x, z);
                    if (cloud == null || NEBlocks.cloud == null
                        || field.pondAt(island, x, z)
                        || field.waterfallAt(island, x, z)) continue;
                    for (int y = cloud[0]; y <= cloud[1]; y++) {
                        place(chunk, x, y, z, NEBlocks.cloud, 0, glowing);
                        placed++;
                    }
                }
            }
            // 2b. Stormglass hanging in points from the underside, where nothing (no cloud) is in the way.
            if (NEBlocks.stormglassCluster != null) for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int length = field.stormglassHang(island, x, z);
                    int[] column = length > 0 ? field.column(island, x, z) : null;
                    if (column == null) continue;
                    for (int y = column[0] - 1; y >= column[0] - length && world.isAirBlock(x, y, z); y--) {
                        place(chunk, x, y, z, NEBlocks.stormglassCluster, 0, glowing);
                        placed++;
                    }
                }
            }
            // 3. The waterfall: its lip is a source that starts flowing over the edge once the chunk is live.
            int[] lip = field.waterfallLip(island);
            if (lip != null && insideChunk(lip[0], lip[1], x0, z0, 0)) {
                int[] column = field.column(island, lip[0], lip[1]);
                if (column != null) world.scheduleBlockUpdate(lip[0], column[1], lip[1], Blocks.water, 5);
            }
            // 4. What grows and lies on top.
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    SkyIslands.Decor decor = field.decor(island, x, z);
                    if (decor == SkyIslands.Decor.NONE) continue;
                    int[] column = field.column(island, x, z);
                    if (column == null || world.getBlock(x, column[1], z) == NEBlocks.cloud) continue;
                    placed += placeDecor(chunk, field, decor, x, column[1] + 1, z, glowing);
                }
            }
            // 5. Trees, each only when it fits wholly inside this chunk.
            for (int[] spot : field.treeSpots(island)) plantTree(world, field, island, spot, x0, z0);
            // 6. A hidden cache in its ring of boulders, or the island's sky dungeon (this chunk's share of it).
            placeCache(world, field, island, x0, z0);
            placed += buildDungeon(world, field, island, x0, z0);
        }
        return placed;
    }

    /**
     * True when a block can be written straight into storage: it holds no tile entity and does nothing on being
     * placed. Still water is the exception kept in: its placement check only matters beside lava. A class that
     * overrides either of Block's (World, x, y, z) hooks counts as reacting, which errs safe.
     */
    private static boolean plain(Block block, int meta) {
        if (block == Blocks.water) return true;
        if (block.hasTileEntity(meta)) return false;
        Boolean reacts = REACTS.get(block.getClass());
        if (reacts == null) {
            reacts = false;
            for (Class<?> c = block.getClass(); c != Block.class && c != null && !reacts; c = c.getSuperclass()) {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 4 && p[0] == World.class
                        && p[1] == int.class
                        && p[2] == int.class
                        && p[3] == int.class
                        && m.getReturnType() == void.class) {
                        reacts = true;
                        break;
                    }
                }
            }
            REACTS.put(block.getClass(), reacts);
        }
        return !reacts;
    }

    /**
     * One block written into the chunk's storage, with none of world.setBlock's per-block lighting; what gives
     * light is noted so {@link #relight} can light it. Only for plain blocks: nothing here needs onBlockAdded.
     */
    private static void place(Chunk chunk, int x, int y, int z, Block block, int meta, List<int[]> glowing) {
        if (y < 0 || y > 255) return;
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        ExtendedBlockStorage section = sections[y >> 4];
        if (section == null) {
            if (block == Blocks.air) return;
            section = sections[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !chunk.worldObj.provider.hasNoSky);
        }
        int lx = x & 15, ly = y & 15, lz = z & 15;
        Block old = section.getBlockByExtId(lx, ly, lz);
        if (old.hasTileEntity(section.getExtBlockMetadata(lx, ly, lz))) chunk.removeTileEntity(lx, y, lz);
        section.func_150818_a(lx, ly, lz, block);
        section.setExtBlockMetadata(lx, ly, lz, meta);
        if (block.getLightValue() > 0) glowing.add(new int[] { x, y, z });
    }

    /**
     * Lights a chunk after {@link #place}: skylight down every column at once, daylight spread in under the
     * island from beside it, then what glows. The chunk is then marked lit, so vanilla's first-tick pass skips
     * it: that pass relights every open block under every overhang, which under a sky island is some eighteen
     * thousand light updates and a sixth of a second a chunk, where only the island's rim needs any.
     */
    private static void relight(World world, Chunk chunk, List<int[]> glowing) {
        chunk.generateSkylightMap();
        if (!world.provider.hasNoSky) {
            darkenBelow(chunk);
            spreadIntoShade(world, chunk);
        }
        chunk.isLightPopulated = true;
        for (int[] p : glowing) world.func_147451_t(p[0], p[1], p[2]);
        chunk.setChunkModified();
    }

    /**
     * Skylight down each whole column, as {@link Chunk#generateSkylightMap} works it but on to the bottom: that
     * stops once the light is spent, which over an island leaves the open air below still at full daylight. The
     * first-tick pass would then walk every one of those blocks back down, about a third of a second a chunk;
     * dark from the start, it only has to spread light in from the island's rim.
     */
    private static void darkenBelow(Chunk chunk) {
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        int top = chunk.getTopFilledSegment() + 15;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int light = 15;
            for (int y = top; y >= 0; y--) {
                int opacity = chunk.func_150808_b(x, y, z);
                if (opacity == 0 && light != 15) opacity = 1;
                light = Math.max(0, light - opacity);
                ExtendedBlockStorage section = sections[y >> 4];
                if (section != null) section.setExtSkylightValue(x, y & 15, z, light);
            }
        }
    }

    /**
     * Daylight into the shade under the island: each open block beneath a covered column that has open sky
     * beside it (in this chunk or a generated neighbour, either way across the border) is relit, and the light
     * spreads on inward from there by itself.
     */
    private static void spreadIntoShade(World world, Chunk chunk) {
        int x0 = chunk.xPosition << 4, z0 = chunk.zPosition << 4;
        int[][] sides = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int here = chunk.heightMap[lz << 4 | lx];
            for (int[] side : sides) {
                int nx = lx + side[0], nz = lz + side[1];
                boolean inside = nx >= 0 && nx < 16 && nz >= 0 && nz < 16;
                Chunk other = inside ? chunk : neighbour(world, x0 + nx, z0 + nz);
                if (other == null) continue;
                int there = other.heightMap[(nz & 15) << 4 | (nx & 15)];
                // This column is covered where the one beside it is open...
                for (int y = there; y < here; y++) if (chunk.func_150808_b(lx, y, lz) < 15)
                    world.updateLightByType(EnumSkyBlock.Sky, x0 + lx, y, z0 + lz);
                // ...and across the border, the other way round (inside the chunk the loop above sees both).
                if (!inside) for (int y = here; y < there; y++) if (other.func_150808_b(nx & 15, y, nz & 15) < 15)
                    world.updateLightByType(EnumSkyBlock.Sky, x0 + nx, y, z0 + nz);
            }
        }
    }

    /** The chunk holding (x, z) if it has been generated, without generating it. */
    private static Chunk neighbour(World world, int x, int z) {
        return world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4) ? world.getChunkFromChunkCoords(x >> 4, z >> 4) : null;
    }

    /** The block of an island at (x, y, z): its surface, then soil, then rock (Skystone on a dungeon's island). */
    private static Block islandBlock(SkyIslands field, int x, int y, int z, int[] column, boolean water,
        SkyIslands.Surface surface, boolean grand) {
        boolean rocky = surface != SkyIslands.Surface.GRASS;
        if (y == column[1]) {
            if (water) return Blocks.water;
            switch (surface) {
                case STONE:
                    return Blocks.stone;
                case GRAVEL:
                    return Blocks.gravel;
                case COBBLE:
                    return Blocks.cobblestone;
                case COAL_ORE:
                    return Blocks.coal_ore;
                case IRON_ORE:
                    return Blocks.iron_ore;
                case COPPER_ORE:
                    return NEBlocks.copperOre != null ? NEBlocks.copperOre : Blocks.stone;
                case GRASS:
                default:
                    return Blocks.grass;
            }
        }
        if (y >= column[1] - 3 && !rocky) return Blocks.dirt;
        if (NEBlocks.stormglassVein != null && field.stormglassVein(x, y, z, column[0])) return NEBlocks.stormglassVein;
        double roll = field.blockRoll(x, y, z, 11);
        if (roll < COAL) return Blocks.coal_ore;
        if (roll < COAL + IRON) return Blocks.iron_ore;
        if (roll < COAL + IRON + COPPER && NEBlocks.copperOre != null) return NEBlocks.copperOre;
        return grand && NEBlocks.skystoneRock != null ? NEBlocks.skystoneRock : Blocks.stone;
    }

    private static int placeDecor(Chunk chunk, SkyIslands field, SkyIslands.Decor decor, int x, int y, int z,
        List<int[]> glowing) {
        switch (decor) {
            case TALL_GRASS:
                place(chunk, x, y, z, Blocks.tallgrass, 1, glowing);
                return 1;
            case FERN:
                place(chunk, x, y, z, Blocks.tallgrass, 2, glowing);
                return 1;
            case RED_FLOWER:
                place(chunk, x, y, z, Blocks.red_flower, (int) (field.blockRoll(x, y, z, 12) * 9), glowing);
                return 1;
            case YELLOW_FLOWER:
                place(chunk, x, y, z, Blocks.yellow_flower, 0, glowing);
                return 1;
            case WINDBLOOM:
                if (NEBlocks.windbloom == null) return 0;
                place(chunk, x, y, z, NEBlocks.windbloom, 0, glowing);
                return 1;
            case BUSH:
                place(chunk, x, y, z, Blocks.leaves, 4, glowing); // oak leaves that never decay
                return 1;
            case DEAD_BUSH:
                place(chunk, x, y, z, Blocks.deadbush, 0, glowing);
                return 1;
            case TALL_BOULDER:
                place(chunk, x, y, z, Blocks.mossy_cobblestone, 0, glowing);
                place(chunk, x, y + 1, z, Blocks.cobblestone, 0, glowing);
                return 2;
            case BOULDER:
                place(
                    chunk,
                    x,
                    y,
                    z,
                    field.blockRoll(x, y, z, 13) < 0.5 ? Blocks.mossy_cobblestone : Blocks.cobblestone,
                    0,
                    glowing);
                return 1;
            case NONE:
            default:
                return 0;
        }
    }

    private static boolean insideChunk(int x, int z, int x0, int z0, int inset) {
        return x >= x0 + inset && x < x0 + 16 - inset && z >= z0 + inset && z < z0 + 16 - inset;
    }

    private static void plantTree(World world, SkyIslands field, SkyIslands.Island island, int[] spot, int x0, int z0) {
        if (!insideChunk(spot[0], spot[1], x0, z0, INSET)) return;
        int[] column = field.column(island, spot[0], spot[1]);
        if (column == null) return;
        world.setBlock(spot[0], column[1] + 1, spot[1], Blocks.air, 0, 2); // clear any grass where the trunk goes
        world.setBlock(spot[0], column[1], spot[1], Blocks.grass, 0, 2);
        Random random = new Random(spot[0] * 341873128712L ^ spot[1] * 132897987541L);
        if (field.birch(island, spot)) new net.minecraft.world.gen.feature.WorldGenForest(false, false)
            .generate(world, random, spot[0], column[1] + 1, spot[1]);
        else new WorldGenTrees(false).generate(world, random, spot[0], column[1] + 1, spot[1]);
    }

    private static void placeCache(World world, SkyIslands field, SkyIslands.Island island, int x0, int z0) {
        if (!island.hasCache()) return;
        int[] spot = field.cacheSpot(island);
        if (!insideChunk(spot[0], spot[1], x0, z0, 0)) return;
        int[] column = field.column(island, spot[0], spot[1]);
        if (column == null || field.pondAt(island, spot[0], spot[1])) return;
        chest(world, spot[0], column[1] + 1, spot[1], SkyCacheLoot.Tier.ISLAND, island);
    }

    private static int buildDungeon(World world, SkyIslands field, SkyIslands.Island island, int x0, int z0) {
        naturalexceptions.core.worldgen.SkyDungeon dungeon = naturalexceptions.core.worldgen.SkyDungeon
            .of(field, island);
        if (dungeon == null) return 0;
        int placed = 0;
        List<naturalexceptions.core.worldgen.SkyDungeon.Piece> torches = new ArrayList<>();
        for (naturalexceptions.core.worldgen.SkyDungeon.Piece piece : dungeon.piecesInChunk(x0 >> 4, z0 >> 4)) {
            if (piece.y < 1 || piece.y > 254) continue;
            if (piece.block == naturalexceptions.core.worldgen.SkyDungeon.Block.TORCH) torches.add(piece);
            else writePiece(world, piece, island);
            placed++;
        }
        // Torches last, once the walls they hang on are in.
        for (naturalexceptions.core.worldgen.SkyDungeon.Piece torch : torches) hangTorch(world, dungeon, torch);
        return placed;
    }

    /**
     * A torch on the side the layout gives it if something solid is there, else on another side that is, else
     * standing on what is below, else none. Through world.setBlock a torch drops at once when nothing beside it is
     * solid yet (its wall later in the chunk, or in a neighbour not yet generated), and one hung on air drops the
     * first time anything beside it changes: dozens a citadel, left lying in its rooms. So it is written straight
     * into the chunk, with no such check, once its own chunk's walls are in.
     */
    private static void hangTorch(World world, naturalexceptions.core.worldgen.SkyDungeon dungeon,
        naturalexceptions.core.worldgen.SkyDungeon.Piece torch) {
        int given = Math.min(torch.meta, 4);
        int[] order = { given, 0, 1, 2, 3, 4 };
        for (int side : order) {
            if (!holds(world, dungeon, torch.x, torch.y, torch.z, side)) continue;
            Chunk chunk = batchChunk;
            int meta = TORCH_META[side];
            if (chunk != null && torch.x >> 4 == chunk.xPosition && torch.z >> 4 == chunk.zPosition) {
                place(chunk, torch.x, torch.y, torch.z, Blocks.torch, meta, batchGlowing);
            } else world.setBlock(torch.x, torch.y, torch.z, Blocks.torch, meta, 2);
            return;
        }
    }

    /** Whether a torch at (x, y, z) can hang on its side {@code side} (a dungeon direction; 4 is standing). */
    private static boolean holds(World world, naturalexceptions.core.worldgen.SkyDungeon dungeon, int x, int y, int z,
        int side) {
        int sx = x, sy = y, sz = z;
        net.minecraftforge.common.util.ForgeDirection face;
        switch (side) {
            case 0:
                sz--;
                face = net.minecraftforge.common.util.ForgeDirection.SOUTH;
                break;
            case 1:
                sx++;
                face = net.minecraftforge.common.util.ForgeDirection.WEST;
                break;
            case 2:
                sz++;
                face = net.minecraftforge.common.util.ForgeDirection.NORTH;
                break;
            case 3:
                sx--;
                face = net.minecraftforge.common.util.ForgeDirection.EAST;
                break;
            default:
                sy--;
                face = net.minecraftforge.common.util.ForgeDirection.UP;
                break;
        }
        naturalexceptions.core.worldgen.SkyDungeon.Piece there = dungeon.at(sx, sy, sz);
        if (there != null && there.block == naturalexceptions.core.worldgen.SkyDungeon.Block.AIR) return false;
        // Not built yet (not generated, or not yet populated with the island and its buildings): trust the layout,
        // where what it puts there is a wall and, with nothing, the island's rock is.
        Chunk beside = neighbour(world, sx, sz);
        if (beside == null || !beside.isTerrainPopulated) return true;
        Block block = world.getBlock(sx, sy, sz);
        return side == 4 ? block.canPlaceTorchOnTop(world, sx, sy, sz) : block.isSideSolid(world, sx, sy, sz, face);
    }

    private static void writePiece(World world, naturalexceptions.core.worldgen.SkyDungeon.Piece piece,
        SkyIslands.Island island) {
        int x = piece.x, y = piece.y, z = piece.z, meta = piece.meta;
        switch (piece.block) {
            case SKYSTONE:
                set(world, x, y, z, orElse(NEBlocks.skystoneBricks, Blocks.stonebrick), meta);
                break;
            case WARDED:
                set(world, x, y, z, orElse(NEBlocks.wardedSkystone, Blocks.bedrock), 0);
                break;
            case LAMP:
                // A whole lamp set into the stone, or a small lantern (its meta is the lantern's own).
                set(
                    world,
                    x,
                    y,
                    z,
                    orElse(NEBlocks.stormglassLantern, Blocks.glowstone),
                    meta == naturalexceptions.core.worldgen.SkyDungeon.LAMP_STANDING
                        || meta == naturalexceptions.core.worldgen.SkyDungeon.LAMP_HANGING ? meta : 0);
                break;
            case LADDER:
                set(world, x, y, z, Blocks.ladder, meta);
                break;
            case GLASS_PANE:
                set(world, x, y, z, Blocks.glass_pane, 0);
                break;
            case CHEST:
                // A chest with no loot is furniture: a barracks footlocker, a cupboard.
                if (piece.loot == null) set(world, x, y, z, Blocks.chest, 0);
                else chest(world, x, y, z, tierOf(piece.loot), island);
                break;
            case SPAWNER:
                spawner(world, x, y, z, piece.mob);
                break;
            case DISPENSER: {
                set(world, x, y, z, Blocks.dispenser, meta & 7);
                // A dispenser aims itself at open air when placed; the dungeon's aim is the one that counts.
                world.setBlockMetadataWithNotify(x, y, z, meta & 7, 2);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.tileentity.TileEntityDispenser) {
                    net.minecraft.tileentity.TileEntityDispenser dispenser = (net.minecraft.tileentity.TileEntityDispenser) tile;
                    boolean fire = (meta & naturalexceptions.core.worldgen.SkyDungeon.FIRE) != 0;
                    for (int slot = 0; slot < 3; slot++) dispenser.setInventorySlotContents(
                        slot,
                        new net.minecraft.item.ItemStack(
                            fire ? net.minecraft.init.Items.fire_charge : net.minecraft.init.Items.arrow,
                            fire ? 8 : 16));
                }
                break;
            }
            case TRIGGER:
                set(world, x, y, z, orElse(NEBlocks.triggerStone, Blocks.stone_pressure_plate), 0);
                break;
            case CRUMBLING:
                set(world, x, y, z, orElse(NEBlocks.crumblingSkystone, Blocks.gravel), 0);
                break;
            case VENT:
                set(world, x, y, z, orElse(NEBlocks.galeVent, Blocks.stonebrick), meta);
                break;
            case ALCOVE:
                set(world, x, y, z, orElse(NEBlocks.sealedAlcove, Blocks.stonebrick), meta);
                break;
            case SEAL:
                set(world, x, y, z, orElse(NEBlocks.wardenSeal, Blocks.stonebrick), 0);
                break;
            case TNT:
                set(world, x, y, z, Blocks.tnt, 0);
                break;
            case LAVA:
                set(world, x, y, z, Blocks.lava, 0);
                break;
            case WATER:
                set(world, x, y, z, Blocks.water, 0);
                break;
            case GATE:
                set(world, x, y, z, orElse(NEBlocks.vaultGate, Blocks.iron_block), meta);
                break;
            case BOOKSHELF:
                set(world, x, y, z, Blocks.bookshelf, 0);
                break;
            case COBWEB:
                set(world, x, y, z, Blocks.web, 0);
                break;
            case GRAVEL:
                set(world, x, y, z, Blocks.gravel, 0);
                break;
            case TILES:
                if (NEBlocks.skystoneDecor != null) set(world, x, y, z, NEBlocks.skystoneDecor, meta & 1);
                else set(world, x, y, z, Blocks.stonebrick, (meta & 1) != 0 ? 2 : 0);
                break;
            case POLISHED:
                if (NEBlocks.skystoneDecor != null) set(
                    world,
                    x,
                    y,
                    z,
                    NEBlocks.skystoneDecor,
                    naturalexceptions.minecraft.block.BlockSkystoneDecor.POLISHED);
                else set(world, x, y, z, Blocks.double_stone_slab, 8);
                break;
            case CHISELED:
                if (NEBlocks.skystoneDecor != null) set(
                    world,
                    x,
                    y,
                    z,
                    NEBlocks.skystoneDecor,
                    naturalexceptions.minecraft.block.BlockSkystoneDecor.CHISELED);
                else set(world, x, y, z, Blocks.stonebrick, 3);
                break;
            case PILLAR:
                if (NEBlocks.skystonePillar != null) set(world, x, y, z, NEBlocks.skystonePillar, meta & 12);
                else set(world, x, y, z, Blocks.quartz_block, 2);
                break;
            case SKY_STAIRS:
                set(world, x, y, z, orElse(NEBlocks.skystoneStairs, Blocks.stone_brick_stairs), meta & 7);
                break;
            case SKY_SLAB:
                if (NEBlocks.skystoneSlab != null) set(world, x, y, z, NEBlocks.skystoneSlab, meta & 15);
                else set(world, x, y, z, Blocks.stone_slab, 5 | (meta & 8));
                break;
            case BED: {
                // Both halves: the partner may lie in the next chunk, and vanilla drops a half whose partner is missing
                // when a neighbour changes, so it is set now too if that chunk is already there (never generated here).
                int facing = VANILLA_FACING[meta & 3];
                set(world, x, y, z, Blocks.bed, facing | (meta & 8));
                int[] step = BED_STEP[meta & 3];
                int sign = (meta & 8) != 0 ? -1 : 1;
                int px = x + sign * step[0], pz = z + sign * step[1];
                if (world.blockExists(px, y, pz) && world.getBlock(px, y, pz) != Blocks.bed)
                    set(world, px, y, pz, Blocks.bed, facing | (~meta & 8));
                break;
            }
            case CARPET:
                set(world, x, y, z, Blocks.carpet, meta & 15);
                break;
            case TORCH:
                set(world, x, y, z, Blocks.torch, TORCH_META[Math.min(meta, 4)]);
                break;
            case FLOWER_POT: {
                set(world, x, y, z, Blocks.flower_pot, 0);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.tileentity.TileEntityFlowerPot) {
                    Object[] plant = POT_PLANTS[meta % POT_PLANTS.length];
                    ((net.minecraft.tileentity.TileEntityFlowerPot) tile)
                        .func_145964_a(net.minecraft.item.Item.getItemFromBlock((Block) plant[0]), (Integer) plant[1]);
                    tile.markDirty();
                }
                break;
            }
            case TRAPDOOR:
                set(world, x, y, z, Blocks.trapdoor, TRAPDOOR_META[meta & 3] | 8);
                break;
            case FENCE_GATE:
                set(world, x, y, z, Blocks.fence_gate, VANILLA_FACING[meta & 3]);
                break;
            case PRESSURE_PLATE:
                set(world, x, y, z, Blocks.wooden_pressure_plate, 0);
                break;
            case BREWING_STAND:
                set(world, x, y, z, Blocks.brewing_stand, 0);
                break;
            case ENCHANTING_TABLE:
                set(world, x, y, z, Blocks.enchanting_table, 0);
                break;
            case JUKEBOX:
                set(world, x, y, z, Blocks.jukebox, 0);
                break;
            case STAINED_GLASS_PANE:
                set(world, x, y, z, Blocks.stained_glass_pane, meta & 15);
                break;
            case IRON_BARS:
                set(world, x, y, z, Blocks.iron_bars, 0);
                break;
            case GRASS:
                set(world, x, y, z, Blocks.grass, 0);
                break;
            case TALL_GRASS:
                set(world, x, y, z, Blocks.tallgrass, 1);
                break;
            case FLOWER:
                set(world, x, y, z, meta == 1 ? Blocks.yellow_flower : Blocks.red_flower, 0);
                break;
            case WINDBLOOM:
                set(world, x, y, z, orElse(NEBlocks.windbloom, Blocks.yellow_flower), 0);
                break;
            case CAULDRON:
                set(world, x, y, z, Blocks.cauldron, 3);
                break;
            case FURNACE: {
                // A hearth's fire is a furnace set alight with no fuel: it glows and never goes out or spreads.
                Block furnace = (meta & 8) != 0 ? Blocks.lit_furnace : Blocks.furnace;
                set(world, x, y, z, furnace, meta & 7);
                world.setBlockMetadataWithNotify(x, y, z, meta & 7, 2);
                if ((meta & naturalexceptions.core.worldgen.SkyDungeon.MID_WORK) != 0
                    && world.getTileEntity(x, y, z) instanceof net.minecraft.inventory.IInventory)
                    Wares.leftMidWork(
                        (net.minecraft.inventory.IInventory) world.getTileEntity(x, y, z),
                        wareSeed(x, y, z));
                break;
            }
            case CRAFTING_TABLE:
                set(world, x, y, z, Blocks.crafting_table, 0);
                break;
            case ANVIL:
                set(world, x, y, z, Blocks.anvil, meta & 3);
                break;
            case FENCE:
                set(world, x, y, z, Blocks.fence, 0);
                break;
            case QUARTZ:
                set(world, x, y, z, Blocks.quartz_block, meta == 1 ? 1 : meta == 2 ? 0 : 2);
                break;
            case STAIRS: {
                int wood = (meta >> 3) & 3;
                Block stairs = wood == 1 ? Blocks.oak_stairs
                    : wood == 2 ? Blocks.dark_oak_stairs : Blocks.spruce_stairs;
                set(world, x, y, z, stairs, meta & 7);
                break;
            }
            case SLAB:
                set(world, x, y, z, Blocks.wooden_slab, meta & 15);
                break;
            case SKYSLATE:
                set(
                    world,
                    x,
                    y,
                    z,
                    orElse(NEBlocks.skyslate, Blocks.stonebrick),
                    NEBlocks.skyslate != null ? meta & 1 : 0);
                break;
            case SHINGLE_STAIRS:
                set(world, x, y, z, orElse(NEBlocks.skyslateShingleStairs, Blocks.stone_brick_stairs), meta & 7);
                break;
            case SKYSLATE_SLAB:
                if (NEBlocks.skyslateSlab != null) set(world, x, y, z, NEBlocks.skyslateSlab, meta & 15);
                else set(world, x, y, z, Blocks.stone_slab, 5 | (meta & 8));
                break;
            case DIRT:
                set(world, x, y, z, Blocks.dirt, 0);
                break;
            case FARMLAND:
                set(world, x, y, z, Blocks.farmland, 7);
                break;
            case CROP:
                set(world, x, y, z, meta == 1 ? Blocks.carrots : meta == 2 ? Blocks.potatoes : Blocks.wheat, 7);
                break;
            case CRATE: {
                set(world, x, y, z, orElse(NEBlocks.crate, Blocks.chest), 0);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.inventory.IInventory)
                    Wares.stockCrate((net.minecraft.inventory.IInventory) tile, wareSeed(x, y, z));
                break;
            }
            case BARREL: {
                set(world, x, y, z, orElse(NEBlocks.barrel, Blocks.cauldron), 0);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof naturalexceptions.minecraft.furniture.TileEntityBarrel) {
                    naturalexceptions.minecraft.furniture.TileEntityBarrel barrel = (naturalexceptions.minecraft.furniture.TileEntityBarrel) tile;
                    if (meta == 1) while (barrel.pourWater()) {}
                    else barrel.insert(Wares.barrelStore(wareSeed(x, y, z)));
                }
                break;
            }
            case PANTRY_SHELF: {
                set(world, x, y, z, orElse(NEBlocks.pantryShelf, Blocks.bookshelf), SIDE_FACING[meta & 3]);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.inventory.IInventory)
                    Wares.stockPantry((net.minecraft.inventory.IInventory) tile, wareSeed(x, y, z));
                break;
            }
            case MARKET_COUNTER: {
                set(world, x, y, z, orElse(NEBlocks.marketCounter, Blocks.crafting_table), meta & 3);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.inventory.IInventory)
                    Wares.stockCounter((net.minecraft.inventory.IInventory) tile, meta & 3, wareSeed(x, y, z));
                break;
            }
            case WALL_SHELF:
            case WEAPON_RACK: {
                boolean shelf = piece.block == naturalexceptions.core.worldgen.SkyDungeon.Block.WALL_SHELF;
                Block furniture = shelf ? NEBlocks.wallShelf : NEBlocks.weaponRack;
                if (furniture == null) break;
                set(world, x, y, z, furniture, SIDE_FACING[meta & 3]);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.inventory.IInventory) {
                    if (shelf) Wares.stockShelf((net.minecraft.inventory.IInventory) tile, wareSeed(x, y, z));
                    else Wares.stockRack((net.minecraft.inventory.IInventory) tile, wareSeed(x, y, z));
                }
                break;
            }
            case WOODEN_TABLE:
                if (NEBlocks.woodenTable != null) set(world, x, y, z, NEBlocks.woodenTable, meta & 3);
                else set(world, x, y, z, Blocks.fence, 0);
                break;
            case AWNING:
                if (NEBlocks.marketAwning != null)
                    set(world, x, y, z, NEBlocks.marketAwning, (meta & 12) | AWNING_WALL[meta & 3]);
                else set(world, x, y, z, Blocks.wool, 14);
                break;
            case DOOR: {
                // Both halves at once (same column, same chunk); the upper half also has its own piece, so that
                // whichever is written last, the door stands whole.
                if ((meta & naturalexceptions.core.worldgen.SkyDungeon.DOOR_TOP) != 0) {
                    set(world, x, y, z, Blocks.wooden_door, 8);
                    break;
                }
                set(world, x, y, z, Blocks.wooden_door, meta & 3);
                set(world, x, y + 1, z, Blocks.wooden_door, 8);
                break;
            }
            case PLANKS:
                set(world, x, y, z, Blocks.planks, meta);
                break;
            case WOOL:
                set(world, x, y, z, Blocks.wool, meta);
                break;
            case HAY:
                set(world, x, y, z, Blocks.hay_block, 0);
                break;
            case STORMGLASS_CLUSTER:
                set(world, x, y, z, orElse(NEBlocks.stormglassCluster, Blocks.air), 0);
                break;
            case STORMGLASS_VEIN:
                set(world, x, y, z, orElse(NEBlocks.stormglassVein, Blocks.stone), 0);
                break;
            case VINE:
                set(world, x, y, z, Blocks.vine, VINE_META[meta & 3]);
                break;
            case CANDELABRA:
                set(world, x, y, z, orElse(NEBlocks.candelabra, Blocks.torch), meta & 1);
                break;
            case BRAZIER:
                set(world, x, y, z, orElse(NEBlocks.brazier, Blocks.glowstone), 0);
                break;
            case BENCH:
                set(world, x, y, z, orElse(NEBlocks.stoneBench, Blocks.stone_slab), SIDE_FACING[meta & 3]);
                break;
            case PEW:
                set(world, x, y, z, orElse(NEBlocks.pew, Blocks.oak_stairs), SIDE_FACING[meta & 3]);
                break;
            case SEAM:
                set(world, x, y, z, orElse(NEBlocks.skystoneSeam, Blocks.stone), meta & 1);
                break;
            case PALLET:
                set(world, x, y, z, orElse(NEBlocks.stonePallet, Blocks.planks), Math.min(meta, 8));
                break;
            case CUTTING_BENCH: {
                Block[] benches = { NEBlocks.masonsBench, NEBlocks.sawmill, NEBlocks.loom, NEBlocks.pottersWheel,
                    NEBlocks.glassFurnace };
                set(
                    world,
                    x,
                    y,
                    z,
                    orElse(benches[Math.min(meta & 7, benches.length - 1)], Blocks.crafting_table),
                    SIDE_FACING[(meta >> 3) & 3]);
                break;
            }
            case TANNING_RACK:
                set(world, x, y, z, orElse(NEBlocks.tanningRack, Blocks.fence), SIDE_FACING[meta & 3]);
                break;
            case ARCHERY_BUTT:
                set(world, x, y, z, orElse(NEBlocks.archeryButt, Blocks.hay_block), SIDE_FACING[meta & 3]);
                break;
            case BROKEN_AUTOMATON:
                if (NEBlocks.brokenAutomaton != null) set(
                    world,
                    x,
                    y,
                    z,
                    NEBlocks.brokenAutomaton,
                    BROKEN_FACING[meta & 3] | meta & naturalexceptions.minecraft.block.BlockBrokenAutomaton.KNEELING);
                break;
            case GUARD: {
                // A Citadel Automaton at its post, dormant: no block, only it.
                naturalexceptions.minecraft.entity.EntityCitadelAutomaton guard = new naturalexceptions.minecraft.entity.EntityCitadelAutomaton(
                    world);
                guard.setLocationAndAngles(x + 0.5, y, z + 0.5, SIDE_YAW[meta & 3], 0.0F);
                guard.rotationYawHead = guard.renderYawOffset = SIDE_YAW[meta & 3];
                guard.post(x, y, z);
                world.spawnEntityInWorld(guard);
                break;
            }
            case WELL_WINCH:
                set(world, x, y, z, orElse(NEBlocks.wellWinch, Blocks.fence), meta & 1);
                break;
            case TRAINING_DUMMY:
                set(world, x, y, z, orElse(NEBlocks.trainingDummy, Blocks.hay_block), SIDE_FACING[meta & 3]);
                break;
            case HOIST:
                set(
                    world,
                    x,
                    y,
                    z,
                    orElse(meta == 1 ? NEBlocks.hoistBasket : NEBlocks.hoistWindlass, Blocks.planks),
                    meta >= 2 && meta <= 5 ? meta : 2);
                if (meta != 1
                    && world.getTileEntity(x, y, z) instanceof naturalexceptions.minecraft.furniture.TileEntityHoist)
                    ((naturalexceptions.minecraft.furniture.TileEntityHoist) world.getTileEntity(x, y, z)).fitCoils(3);
                break;
            case SHEAVE:
                set(world, x, y, z, orElse(NEBlocks.sheave, Blocks.planks), meta & 1);
                break;
            case DERRICK_PIVOT:
                set(world, x, y, z, orElse(NEBlocks.derrickPivot, Blocks.cobblestone), 0);
                break;
            case COUNTERWEIGHT:
                set(world, x, y, z, orElse(NEBlocks.counterweightBox, Blocks.planks), 0);
                // Its stone goes to the windlass in front of it, which hangs it.
                for (int[] o : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
                    net.minecraft.tileentity.TileEntity t = world.getTileEntity(x + o[0], y + 1, z + o[1]);
                    if (t instanceof naturalexceptions.minecraft.furniture.TileEntityHoist)
                        ((naturalexceptions.minecraft.furniture.TileEntityHoist) t).weightCounterBlocks(meta);
                }
                break;
            case CONDENSER:
                set(world, x, y, z, orElse(NEBlocks.cloudCondenser, Blocks.glass), 0);
                break;
            case STORMCRYSTAL:
                set(world, x, y, z, orElse(NEBlocks.stormcrystal, Blocks.glowstone), meta & 1);
                break;
            case LIGHTHOUSE_LAMP:
                set(world, x, y, z, orElse(NEBlocks.lighthouseLamp, Blocks.glowstone), 0);
                break;
            case ROCK:
                if (meta == 1) set(world, x, y, z, Blocks.cobblestone, 0);
                else set(world, x, y, z, orElse(NEBlocks.skystoneRock, Blocks.stone), 0);
                break;
            case CHAIR:
                if (NEBlocks.chair != null) set(world, x, y, z, NEBlocks.chair, meta & 15);
                else set(world, x, y, z, Blocks.oak_stairs, 0);
                break;
            case FOUNTAIN:
                set(world, x, y, z, orElse(NEBlocks.fountain, Blocks.cauldron), 0);
                break;
            case ORRERY:
                set(world, x, y, z, orElse(NEBlocks.orrery, Blocks.gold_block), 0);
                break;
            case BEACON:
                set(world, x, y, z, orElse(NEBlocks.stormglassLantern, Blocks.glowstone), 1);
                break;
            case CHART_TABLE:
                set(world, x, y, z, orElse(NEBlocks.chartTable, Blocks.crafting_table), 0);
                break;
            case GLASS:
                set(world, x, y, z, Blocks.glass, 0);
                break;
            case LOG: {
                // Meta: the wood (0 oak, 1 spruce, 5 dark oak), plus 16 lying along x or 32 along z.
                int axis = (meta & 16) != 0 ? 4 : (meta & 32) != 0 ? 8 : 0, wood = meta & 15;
                if (wood == 5) set(world, x, y, z, Blocks.log2, 1 | axis); // dark oak
                else set(world, x, y, z, Blocks.log, (wood & 3) | axis);
                break;
            }
            case LEAVES:
                set(world, x, y, z, Blocks.leaves, 4); // placed, not grown: never decays
                break;
            case AIR:
            default:
                set(world, x, y, z, Blocks.air, 0);
                break;
        }
    }

    /** Vanilla side for a dungeon direction (north, east, south, west): what a shelf's front faces. */
    private static final int[] SIDE_FACING = { 2, 5, 3, 4 };
    /** A broken Automaton's facing bits for the way it faces (north, east, south, west). */
    private static final int[] BROKEN_FACING = { 1, 2, 0, 3 };
    /** An entity's yaw facing north, east, south, west. */
    private static final float[] SIDE_YAW = { 180.0F, 270.0F, 0.0F, 90.0F };
    /** Vine meta clinging to the wall on the north, east, south, west. */
    private static final int[] VINE_META = { 4, 8, 1, 2 };
    /** An awning's wall, as its block counts them (0 north, 1 south, 2 west, 3 east), by dungeon direction. */
    private static final int[] AWNING_WALL = { 0, 3, 1, 2 };

    private static long wareSeed(int x, int y, int z) {
        return x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
    }

    /** From a bed's foot to its head, by dungeon direction (north, east, south, west). */
    private static final int[][] BED_STEP = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    /** Vanilla facing for a dungeon direction (north, east, south, west): beds and fence gates. */
    private static final int[] VANILLA_FACING = { 2, 3, 0, 1 };
    /** Torch meta hanging on the wall to the north, east, south, west, or standing. */
    private static final int[] TORCH_META = { 3, 2, 4, 1, 5 };
    /** Trapdoor meta hinged on the wall to the north, east, south, west. */
    private static final int[] TRAPDOOR_META = { 1, 2, 0, 3 };
    /** A flower pot's plant by meta: poppy, dandelion, fern, oak sapling, blue orchid, allium. */
    private static final Object[][] POT_PLANTS = { { Blocks.red_flower, 0 }, { Blocks.yellow_flower, 0 },
        { Blocks.tallgrass, 2 }, { Blocks.sapling, 0 }, { Blocks.red_flower, 1 }, { Blocks.red_flower, 2 } };

    private static void set(World world, int x, int y, int z, Block block, int meta) {
        Chunk chunk = batchChunk;
        if (chunk != null && x >> 4 == chunk.xPosition && z >> 4 == chunk.zPosition && plain(block, meta)) {
            place(chunk, x, y, z, block, meta, batchGlowing);
        } else world.setBlock(x, y, z, block, meta, 2);
    }

    /**
     * A spawner for a creature by its vanilla name, set slow and sparse; "Zombie+" (and the like) spawns it in iron
     * armour with a weapon, through the spawner's own spawn data.
     */
    /** The entity name a dungeon's mob list means: vanilla's names as they are, and the sky's own creatures. */
    static String entityName(String mob) {
        String id = naturalexceptions.NaturalExceptions.MOD_ID;
        if (mob.equals("Stormwisp")) return id + ".stormwisp";
        if (mob.equals("Sentinel")) return id + ".skystone_sentinel";
        // The sky's own dead in place of vanilla's.
        if (mob.equals("Zombie")) return id + ".gale_husk";
        if (mob.equals("Skeleton")) return id + ".stormbone_archer";
        if (mob.equals("CitadelGuard")) return id + ".citadel_guard";
        if (mob.equals("Spider") || mob.equals("CaveSpider")) return id + ".cloudweaver";
        return mob;
    }

    private static void spawner(World world, int x, int y, int z, String mob) {
        set(world, x, y, z, Blocks.mob_spawner, 0);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof net.minecraft.tileentity.TileEntityMobSpawner) || mob == null) return;
        net.minecraft.tileentity.MobSpawnerBaseLogic logic = ((net.minecraft.tileentity.TileEntityMobSpawner) tile)
            .func_145881_a();
        boolean armed = mob.endsWith("+");
        String name = armed ? mob.substring(0, mob.length() - 1) : mob;
        logic.setEntityName(entityName(name));
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        logic.writeToNBT(tag);
        // Slow and sparse: a pair every 30 to 60 seconds, never more than three about, only for a player close by.
        tag.setShort("Delay", (short) 200);
        tag.setShort("MinSpawnDelay", (short) 600);
        tag.setShort("MaxSpawnDelay", (short) 1200);
        tag.setShort("SpawnCount", (short) 2);
        tag.setShort("MaxNearbyEntities", (short) 3);
        tag.setShort("RequiredPlayerRange", (short) 10);
        tag.setShort("SpawnRange", (short) 3);
        if (!armed) {
            logic.readFromNBT(tag);
            return;
        }
        net.minecraft.nbt.NBTTagList equipment = new net.minecraft.nbt.NBTTagList();
        net.minecraft.item.Item[] gear = {
            name.equals("Skeleton") ? net.minecraft.init.Items.bow : net.minecraft.init.Items.iron_sword,
            net.minecraft.init.Items.iron_boots, null, net.minecraft.init.Items.iron_chestplate,
            net.minecraft.init.Items.iron_helmet };
        for (net.minecraft.item.Item item : gear) {
            net.minecraft.nbt.NBTTagCompound slot = new net.minecraft.nbt.NBTTagCompound();
            if (item != null) new net.minecraft.item.ItemStack(item).writeToNBT(slot);
            equipment.appendTag(slot);
        }
        net.minecraft.nbt.NBTTagCompound data = new net.minecraft.nbt.NBTTagCompound();
        data.setTag("Equipment", equipment);
        data.setBoolean("PersistenceRequired", true);
        tag.setTag("SpawnData", data);
        logic.readFromNBT(tag);
    }

    private static Block orElse(Block block, Block fallback) {
        return block != null ? block : fallback;
    }

    private static SkyCacheLoot.Tier tierOf(naturalexceptions.core.worldgen.SkyDungeon.Loot loot) {
        if (loot == null) return SkyCacheLoot.Tier.STORE;
        switch (loot) {
            case KEY:
                return SkyCacheLoot.Tier.KEY;
            case CHART:
                return SkyCacheLoot.Tier.CHART;
            case QUARRY:
                return SkyCacheLoot.Tier.QUARRY;
            case HOUSEHOLD:
                return SkyCacheLoot.Tier.HOUSEHOLD;
            case VAULT_TREASURE:
                return SkyCacheLoot.Tier.VAULT_TREASURE;
            case VAULT_RICH:
                return SkyCacheLoot.Tier.VAULT_RICH;
            case STORE:
            default:
                return SkyCacheLoot.Tier.STORE;
        }
    }

    /** A chest with its loot, seeded from where it stands, so it is the same whichever chunk writes it. */
    private static void chest(World world, int x, int y, int z, SkyCacheLoot.Tier tier, SkyIslands.Island island) {
        world.setBlock(x, y, z, Blocks.chest, 0, 2);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityChest) {
            long seed = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ island.cellX();
            SkyCacheLoot.fill((TileEntityChest) tile, seed, tier);
        }
    }

    // --- Giant terrain features ------------------------------------------------------------------------------

    /**
     * The overworld's landmarks through this chunk. Like the islands, carved straight into the chunk and lit once
     * at the end: a chasm or sinkhole through world.setBlock relit its column and neighbours at every block.
     */
    static int landmarks(final World world, final int chunkX, final int chunkZ, final Landmarks field) {
        return batched(world, chunkX, chunkZ, new java.util.function.IntSupplier() {

            @Override
            public int getAsInt() {
                return carveLandmarks(world, chunkX, chunkZ, field);
            }
        });
    }

    private static int carveLandmarks(World world, int chunkX, int chunkZ, Landmarks field) {
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
                            changed += archColumn(world, field, landmark, x, z);
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
                set(world, x, y, z, Blocks.air, 0);
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
                    if (ore != null) set(world, x, y, z, ore, 0);
                }
        }
        if (Landmarks.chasmOpen(here, Landmarks.CHASM_FLOOR_Y, surface)) {
            set(world, x, Landmarks.CHASM_FLOOR_Y - 1, z, Blocks.lava, 0);
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
            set(world, x, y, z, block, 0);
            changed++;
        }
        Landmarks.Pillar lookout = naturalexceptions.core.worldgen.HermitLookout.pillar(spires);
        if (lookout != null) changed += lookoutColumn(world, spires, field, lookout, x, z, ground);
        return changed;
    }

    /**
     * A Hermit's lookout in this column (see {@link naturalexceptions.core.worldgen.HermitLookout}): the post on
     * top, and the ladder shaft down the pillar's heart with its passage out to the south at the foot.
     */
    private static int lookoutColumn(World world, Landmarks.Landmark spires, Landmarks field, Landmarks.Pillar pillar,
        int x, int z, int ground) {
        int dx = x - pillar.x, dz = z - pillar.z;
        int floor = naturalexceptions.core.worldgen.HermitLookout.floorY(pillar);
        int changed = 0;
        if (dx == 0 && dz == 0) {
            // The ladder hangs on the shaft's north wall, which the pillar always keeps.
            for (int y = ground + 1; y < floor; y++) set(world, x, y, z, Blocks.ladder, 3);
            changed += floor - ground - 1;
        } else if (dx == 0 && dz > 0 && dz <= naturalexceptions.core.worldgen.HermitLookout.passageLength(pillar)) {
            for (int y = ground + 1; y <= ground + 2; y++) {
                if (!field.spireAt(spires, x, y, z, ground)) continue;
                set(world, x, y, z, Blocks.air, 0);
                changed++;
            }
        }
        double columnRoll = fieldRoll(x, floor, z);
        for (int dy = -1; dy <= naturalexceptions.core.worldgen.HermitLookout.ROOF_HEIGHT; dy++) {
            int y = floor + dy;
            naturalexceptions.core.worldgen.HermitLookout.Part part = naturalexceptions.core.worldgen.HermitLookout
                .partAt(dx, dy, dz, columnRoll, fieldRoll(x, y, z));
            switch (part) {
                case NONE:
                    continue;
                case FOOTING:
                    if (world.getBlock(x, y, z)
                        .getMaterial()
                        .isSolid()) continue;
                    set(world, x, y, z, Blocks.stonebrick, 0);
                    break;
                case FLOOR:
                    set(world, x, y, z, Blocks.stonebrick, 0);
                    break;
                case FLOOR_MOSSY:
                    set(world, x, y, z, Blocks.stonebrick, 1);
                    break;
                case FLOOR_CRACKED:
                    set(world, x, y, z, Blocks.stonebrick, 2);
                    break;
                case PARAPET:
                    set(world, x, y, z, Blocks.cobblestone_wall, fieldRoll(z, y, x) < 0.5 ? 1 : 0);
                    break;
                case POST:
                    set(world, x, y, z, Blocks.log, 1);
                    break;
                case ROOF:
                    set(world, x, y, z, Blocks.wooden_slab, 1);
                    break;
                case HATCH:
                    set(world, x, y, z, Blocks.trapdoor, 8); // shut, flush with the floor
                    break;
                case BED_FOOT:
                    set(world, x, y, z, Blocks.bed, 0); // its head to the south
                    break;
                case BED_HEAD:
                    set(world, x, y, z, Blocks.bed, 8);
                    break;
                case CHEST:
                    lookoutChest(world, x, y, z);
                    break;
                case CLEAR:
                default:
                    set(world, x, y, z, Blocks.air, 0);
                    break;
            }
            changed++;
        }
        return changed;
    }

    /** The lookout's chest: what the last watch left, and the log of bearings to the sky-halls. */
    private static void lookoutChest(World world, int x, int y, int z) {
        world.setBlock(x, y, z, Blocks.chest, 0, 2);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest) tile;
        SkyCacheLoot
            .fill(chest, x * 0x9E3779B97F4A7C15L ^ y * 31L ^ z * 0x165667B19E3779F9L, SkyCacheLoot.Tier.HOUSEHOLD);
        net.minecraft.item.ItemStack log = LookoutLog.write(world, x, z);
        for (int slot = 0; slot < chest.getSizeInventory(); slot++) {
            if (chest.getStackInSlot(slot) != null) continue;
            chest.setInventorySlotContents(slot, log);
            return;
        }
        chest.setInventorySlotContents(0, log);
    }

    private static int sinkholeColumn(World world, Landmarks field, Landmarks.Landmark sinkhole, int x, int z) {
        double dx = x + 0.5 - sinkhole.x(), dz = z + 0.5 - sinkhole.z();
        if (dx * dx + dz * dz > sinkhole.reach() * sinkhole.reach() || underWater(world, x, z)) return 0;
        int surface = world.getHeightValue(x, z) - 1;
        int changed = 0;
        for (int y = Landmarks.CAVERN_FLOOR_Y; y <= surface; y++) {
            if (!field.sinkholeOpen(sinkhole, x, y, z, surface)) continue;
            set(world, x, y, z, y <= Landmarks.CAVERN_WATER_TOP_Y ? Blocks.water : Blocks.air, 0);
            changed++;
        }
        return changed;
    }

    private static int archColumn(World world, Landmarks field, Landmarks.Landmark arch, int x, int z) {
        double dx = x + 0.5 - arch.x(), dz = z + 0.5 - arch.z();
        if (dx * dx + dz * dz > arch.reach() * arch.reach()) return 0;
        int ground = groundY(world, x, z);
        int top = Landmarks.ARCH_BASE_Y + (int) Math.ceil(arch.height() + arch.width() * 1.6) + 1;
        int[] middle = Landmarks.archMiddle(arch);
        // Some arches keep a Gale Core sealed in their keystone, set in the underside of the span's middle.
        boolean keystone = x == middle[0] && z == middle[1]
            && field.hasGaleKeystone(arch)
            && NEBlocks.galeKeystone != null;
        int changed = 0;
        for (int y = ground + 1; y <= top; y++) {
            if (!Landmarks.archAt(arch, x, y, z, ground)) continue;
            boolean cap = !Landmarks.archAt(arch, x, y + 1, z, ground);
            boolean underside = keystone && !Landmarks.archAt(arch, x, y - 1, z, ground);
            if (underside) keystone = false;
            set(world, x, y, z, underside ? NEBlocks.galeKeystone : cap ? Blocks.grass : Blocks.stone, 0);
            changed++;
        }
        return changed;
    }

    // --- Deep layers ----------------------------------------------------------------------------------------

    /** The deep pockets (Lumenite grottos, magma) through this chunk, carved and lit as {@link #landmarks} are. */
    static int deepLayers(final World world, final int chunkX, final int chunkZ, final DeepLayers field) {
        return batched(world, chunkX, chunkZ, new java.util.function.IntSupplier() {

            @Override
            public int getAsInt() {
                return carveDeepLayers(world, chunkX, chunkZ, field);
            }
        });
    }

    /**
     * Runs {@code work} with this chunk open for {@link #set} to write straight into, then lights it once if
     * anything changed. Blocks outside the chunk, and any but plain ones, still go through world.setBlock.
     */
    private static int batched(World world, int chunkX, int chunkZ, java.util.function.IntSupplier work) {
        if (batchChunk != null) return work.getAsInt(); // already inside a batch (an island's): its relight covers it
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        List<int[]> glowing = new ArrayList<>();
        batchChunk = chunk;
        batchGlowing = glowing;
        int changed;
        try {
            changed = work.getAsInt();
        } finally {
            batchChunk = null;
            batchGlowing = null;
        }
        if (changed > 0) relight(world, chunk, glowing);
        return changed;
    }

    private static int carveDeepLayers(World world, int chunkX, int chunkZ, DeepLayers field) {
        int x0 = chunkX * 16;
        int z0 = chunkZ * 16;
        int changed = 0;
        for (DeepLayers.Pocket pocket : field.near(x0, z0, x0 + 15, z0 + 15)) {
            int reachX = (int) Math.ceil(pocket.rx() * 1.6) + 1;
            int reachZ = (int) Math.ceil(pocket.rz() * 1.6) + 1;
            for (int x = Math.max(x0, pocket.x() - reachX); x <= Math.min(x0 + 15, pocket.x() + reachX); x++) {
                for (int z = Math.max(z0, pocket.z() - reachZ); z <= Math.min(z0 + 15, pocket.z() + reachZ); z++) {
                    for (int y = pocket.minY(); y <= pocket.maxY(); y++) {
                        Block block = deepBlock(world, field.partAt(pocket, x, y, z), x, y, z);
                        if (block == null) continue;
                        set(world, x, y, z, block, 0);
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    /** The block a pocket part becomes here, or null to leave the world as it is. Bedrock is never touched. */
    private static Block deepBlock(World world, DeepLayers.Part part, int x, int y, int z) {
        if (part == DeepLayers.Part.NONE) return null;
        Block existing = world.getBlock(x, y, z);
        if (existing == Blocks.bedrock) return null;
        boolean rock = existing == Blocks.stone || existing == Blocks.dirt
            || existing == Blocks.gravel
            || existing == Blocks.coal_ore
            || existing == Blocks.iron_ore;
        switch (part) {
            case OPEN:
                return Blocks.air;
            case LAVA:
                return Blocks.lava;
            case CLUSTER:
                return NEBlocks.lumeniteCluster != null ? NEBlocks.lumeniteCluster : Blocks.air;
            case ORE:
                return rock && NEBlocks.lumeniteOre != null ? NEBlocks.lumeniteOre : null;
            case MAGMA_ROCK:
                return rock && NEBlocks.magmaRock != null ? NEBlocks.magmaRock : null;
            default:
                return null;
        }
    }

    private static double fieldRoll(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        return ((h ^ (h >>> 29)) >>> 11) * 0x1.0p-53;
    }
}
