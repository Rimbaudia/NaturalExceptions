package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
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
        for (SkyIslands.Island island : field.near(x0, z0, x0 + 15, z0 + 15)) {
            // 1. Rock, soil and the kind's surface; the pond and the waterfall's channel.
            for (int x = x0; x < x0 + 16; x++) {
                for (int z = z0; z < z0 + 16; z++) {
                    int[] column = field.column(island, x, z);
                    if (column == null) continue;
                    boolean water = field.pondAt(island, x, z) || field.waterfallAt(island, x, z);
                    SkyIslands.Surface surface = field.surface(island, x, z);
                    for (int y = column[0]; y <= column[1]; y++) {
                        world.setBlock(x, y, z, islandBlock(field, x, y, z, column, water, surface), 0, 2);
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
                        world.setBlock(x, y, z, NEBlocks.cloud, 0, 2);
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
                    placed += placeDecor(world, field, decor, x, column[1] + 1, z);
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

    private static Block islandBlock(SkyIslands field, int x, int y, int z, int[] column, boolean water,
        SkyIslands.Surface surface) {
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
        double roll = field.blockRoll(x, y, z, 11);
        if (roll < COAL) return Blocks.coal_ore;
        if (roll < COAL + IRON) return Blocks.iron_ore;
        if (roll < COAL + IRON + COPPER && NEBlocks.copperOre != null) return NEBlocks.copperOre;
        return Blocks.stone;
    }

    private static int placeDecor(World world, SkyIslands field, SkyIslands.Decor decor, int x, int y, int z) {
        switch (decor) {
            case TALL_GRASS:
                world.setBlock(x, y, z, Blocks.tallgrass, 1, 2);
                return 1;
            case FERN:
                world.setBlock(x, y, z, Blocks.tallgrass, 2, 2);
                return 1;
            case RED_FLOWER:
                world.setBlock(x, y, z, Blocks.red_flower, (int) (field.blockRoll(x, y, z, 12) * 9), 2);
                return 1;
            case YELLOW_FLOWER:
                world.setBlock(x, y, z, Blocks.yellow_flower, 0, 2);
                return 1;
            case WINDBLOOM:
                if (NEBlocks.windbloom == null) return 0;
                world.setBlock(x, y, z, NEBlocks.windbloom, 0, 2);
                return 1;
            case BUSH:
                world.setBlock(x, y, z, Blocks.leaves, 4, 2); // oak leaves that never decay
                return 1;
            case DEAD_BUSH:
                world.setBlock(x, y, z, Blocks.deadbush, 0, 2);
                return 1;
            case TALL_BOULDER:
                world.setBlock(x, y, z, Blocks.mossy_cobblestone, 0, 2);
                world.setBlock(x, y + 1, z, Blocks.cobblestone, 0, 2);
                return 2;
            case BOULDER:
                world.setBlock(
                    x,
                    y,
                    z,
                    field.blockRoll(x, y, z, 13) < 0.5 ? Blocks.mossy_cobblestone : Blocks.cobblestone,
                    0,
                    2);
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
        for (naturalexceptions.core.worldgen.SkyDungeon.Piece piece : dungeon.piecesInChunk(x0 >> 4, z0 >> 4)) {
            if (piece.y < 1 || piece.y > 254) continue;
            writePiece(world, piece, island);
            placed++;
        }
        return placed;
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
                set(world, x, y, z, orElse(NEBlocks.lumeniteLamp, Blocks.glowstone), 0);
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
                // Each half on its own (the two may fall in different chunks); vanilla faces 0 south .. 3 east.
                int facing = VANILLA_FACING[meta & 3];
                set(world, x, y, z, Blocks.bed, facing | (meta & 8));
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
            case FURNACE:
                set(world, x, y, z, Blocks.furnace, meta);
                world.setBlockMetadataWithNotify(x, y, z, meta, 2);
                break;
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
            case STAIRS:
                set(world, x, y, z, Blocks.spruce_stairs, meta);
                break;
            case SLAB:
                set(world, x, y, z, Blocks.wooden_slab, 1 | (meta & 8));
                break;
            case DOOR: {
                // Both halves at once (same column, same chunk); the layout keeps the block above clear for it.
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
            case GLASS:
                set(world, x, y, z, Blocks.glass, 0);
                break;
            case LOG:
                set(world, x, y, z, Blocks.log, 0);
                break;
            case LEAVES:
                set(world, x, y, z, Blocks.leaves, 4); // placed, not grown: never decays
                break;
            case AIR:
            default:
                set(world, x, y, z, Blocks.air, 0);
                break;
        }
    }

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
        world.setBlock(x, y, z, block, meta, 2);
    }

    /**
     * A spawner for a creature by its vanilla name, set slow and sparse; "Zombie+" (and the like) spawns it in iron
     * armour with a weapon, through the spawner's own spawn data.
     */
    private static void spawner(World world, int x, int y, int z, String mob) {
        set(world, x, y, z, Blocks.mob_spawner, 0);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof net.minecraft.tileentity.TileEntityMobSpawner) || mob == null) return;
        net.minecraft.tileentity.MobSpawnerBaseLogic logic = ((net.minecraft.tileentity.TileEntityMobSpawner) tile)
            .func_145881_a();
        boolean armed = mob.endsWith("+");
        String name = armed ? mob.substring(0, mob.length() - 1) : mob;
        logic.setEntityName(name);
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

    // --- Deep layers ----------------------------------------------------------------------------------------

    static int deepLayers(World world, int chunkX, int chunkZ, DeepLayers field) {
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
                        world.setBlock(x, y, z, block, 0, 2);
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
