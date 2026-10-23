package naturalexceptions.minecraft.worldgen;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.worldgen.CopperDeposit;
import naturalexceptions.core.worldgen.CopperDepositField;
import naturalexceptions.core.worldgen.OverworldGeneration;
import naturalexceptions.core.worldgen.OverworldSample;
import naturalexceptions.core.worldgen.SoilContext;
import naturalexceptions.core.worldgen.WorldgenSite;
import naturalexceptions.core.worldgen.WorldgenSites;
import naturalexceptions.core.worldgen.WorldgenStage;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;

/** One deterministic, chunk-bounded materializer for the P6 Overworld stage pipeline. */
public final class NEOverworldGenerator implements IWorldGenerator {

    public static final ResourceId SUBSYSTEM = ResourceId.of("naturalexceptions", "subsystem/worldgen");
    public static final SubsystemProfiler PROFILER = new SubsystemProfiler();

    private static final long ECOLOGY_GRASS_CHANNEL = 0x45434f5f47524153L;
    private static final int SEA_LEVEL = 63;
    /** Relief-field value above which a meadow is lifted by one block. */
    private static final double RISE_THRESHOLD = 0.8;
    private static final long BOULDER_CHANNEL = 0x424f554c44455253L;
    private static final long SCAR_CHANNEL = 0x5155494554534352L;

    public static void register() {
        GameRegistry.registerWorldGenerator(new NEOverworldGenerator(), 0);
    }

    /** The seed of a world's landmarks (sky islands, their dungeons, giant features, deep layers). */
    public static long landmarkSeed(long worldSeed, WorldgenEpoch epoch) {
        return worldSeed ^ ((long) epoch.value() << 40);
    }

    /** The seed the landmarks of this (overworld) world were laid out with. */
    public static long landmarkSeed(World world) {
        NEWorldSavedData savedData = NEWorldSavedData.get(world);
        WorldgenEpoch epoch = savedData != null && savedData.metadata() != null ? savedData.metadata()
            .worldgenEpoch() : WorldgenEpoch.CURRENT;
        return landmarkSeed(world.getSeed(), epoch);
    }

    @Override
    public void generate(Random ignoredForgeRandom, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
        IChunkProvider chunkProvider) {
        if (world == null || world.isRemote || world.provider.dimensionId != 0 || NEBlocks.copperOre == null) return;

        long startNanos = System.nanoTime();
        NEWorldSavedData savedData = NEWorldSavedData.get(world);
        WorldgenEpoch epoch = savedData != null && savedData.metadata() != null ? savedData.metadata()
            .worldgenEpoch() : WorldgenEpoch.CURRENT;
        OverworldGeneration generation = new OverworldGeneration(world.getSeed(), epoch);
        OverworldSample[][] samples = sampleChunk(generation, chunkX, chunkZ);
        ChunkGenerationRecordBuilder history = new ChunkGenerationRecordBuilder(world.getSeed(), epoch, chunkX, chunkZ);

        long stage = System.nanoTime();
        adjustBaseTerrain(world, chunkX, chunkZ, samples);
        history.mark(WorldgenStage.BASE_TERRAIN);

        // Geology places no blocks of its own (a band of sandstone or cobblestone under the land looked wrong); its
        // provinces still decide where copper lies (CopperDepositField).
        history.mark(WorldgenStage.GEOLOGY);
        // Hydrology is a coarse drainage/moisture context consumed by the surface derivation below.
        history.mark(WorldgenStage.HYDROLOGY);
        applySoilAndSurface(world, chunkX, chunkZ, samples);
        history.mark(WorldgenStage.SOIL_SURFACE);

        stage = time("stage_terrain_geology_surface", stage);
        seedEcology(world, chunkX, chunkZ, generation, samples);
        history.mark(WorldgenStage.ECOLOGY);

        int copperBlocks = generateCopper(world, chunkX, chunkZ, generation);
        history.mark(WorldgenStage.RESOURCES);
        stage = time("stage_ecology_copper", stage);

        WorldgenSites sites = new WorldgenSites(generation);
        generateOutcrops(world, chunkX, chunkZ, sites);
        // Rare, dramatic landmarks are structures too; they share this stage so older chunk records stay valid.
        long landmarkSeed = landmarkSeed(world.getSeed(), epoch);
        stage = time("stage_outcrops", stage);
        int islandBlocks = LandmarkMaterializer
            .skyIslands(world, chunkX, chunkZ, new naturalexceptions.core.worldgen.SkyIslands(landmarkSeed));
        stage = time("stage_sky_islands", stage);
        int landmarkBlocks = LandmarkMaterializer
            .landmarks(world, chunkX, chunkZ, new naturalexceptions.core.worldgen.Landmarks(landmarkSeed));
        stage = time("stage_landmarks", stage);
        int deepBlocks = LandmarkMaterializer
            .deepLayers(world, chunkX, chunkZ, new naturalexceptions.core.worldgen.DeepLayers(landmarkSeed));
        history.mark(WorldgenStage.STRUCTURES);
        stage = time("stage_deep_layers", stage);
        generateInitialAnomalies(world, chunkX, chunkZ, sites);
        history.mark(WorldgenStage.INITIAL_ANOMALIES);
        time("stage_anomalies", stage);

        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        NEChunkGenerationHistory.mark(chunk, history.build());
        chunk.setChunkModified();
        PROFILER.incrementCounter(SUBSYSTEM, "chunks_materialized", 1L);
        PROFILER.incrementCounter(SUBSYSTEM, "columns_sampled", 256L);
        PROFILER.incrementCounter(SUBSYSTEM, "copper_blocks_placed", copperBlocks);
        PROFILER.incrementCounter(SUBSYSTEM, "sky_island_blocks_placed", islandBlocks);
        PROFILER.incrementCounter(SUBSYSTEM, "landmark_blocks_changed", landmarkBlocks);
        PROFILER.incrementCounter(SUBSYSTEM, "deep_layer_blocks_changed", deepBlocks);
        PROFILER.recordElapsedNanos(SUBSYSTEM, "chunk_materialization", System.nanoTime() - startNanos);
    }

    /** Records the time since {@code since} under one stage's name, and returns now for the next stage. */
    private static long time(String stage, long since) {
        long now = System.nanoTime();
        PROFILER.recordElapsedNanos(SUBSYSTEM, stage, now - since);
        return now;
    }

    private OverworldSample[][] sampleChunk(OverworldGeneration generation, int chunkX, int chunkZ) {
        OverworldSample[][] result = new OverworldSample[16][16];
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                result[localX][localZ] = generation.sample(originX + localX, originZ + localZ);
            }
        }
        return result;
    }

    /**
     * Retains vanilla's bulk terrain. The NE relief field only lifts high-tendency meadows by one block, so it forms
     * broad gentle rises; it never digs single-column gouges or stacks dirt onto rock.
     */
    private void adjustBaseTerrain(World world, int chunkX, int chunkZ, OverworldSample[][] samples) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (samples[localX][localZ].elevationTendency() < RISE_THRESHOLD) continue;
                int x = originX + localX;
                int z = originZ + localZ;
                int surfaceY = world.getHeightValue(x, z) - 1;
                if (surfaceY < SEA_LEVEL || surfaceY >= 250) continue;
                if (world.getBlock(x, surfaceY, z) != Blocks.grass || !world.isAirBlock(x, surfaceY + 1, z)) continue;
                world.setBlock(x, surfaceY, z, Blocks.dirt, 0, 2);
                world.setBlock(x, surfaceY + 1, z, Blocks.grass, 0, 2);
            }
        }
    }

    private boolean isNaturalSurface(Block block) {
        return block == Blocks.grass || block == Blocks.dirt
            || block == Blocks.stone
            || block == Blocks.gravel
            || block == Blocks.sand;
    }

    /** Materializes a thin province bed as recognizable vanilla stone-family blocks. */
    private void applySoilAndSurface(World world, int chunkX, int chunkZ, OverworldSample[][] samples) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = originX + localX;
                int z = originZ + localZ;
                int y = world.getHeightValue(x, z) - 1;
                if (y < SEA_LEVEL || y >= 250) continue;
                Block top = world.getBlock(x, y, z);
                if (top != Blocks.grass) continue;
                OverworldSample sample = samples[localX][localZ];
                SoilContext soil = sample.soil();
                // Surface cues stay local: gravel only on low wet-basin banks, bare rock only on the steepest
                // rocky ground, coarse dry earth only in the driest core. Elsewhere the grass is left alone.
                if (soil == SoilContext.ALLUVIAL && y <= SEA_LEVEL + 1) {
                    world.setBlock(x, y, z, Blocks.gravel, 0, 2);
                } else if (soil == SoilContext.THIN_ROCKY && sample.slope() > 0.40) {
                    world.setBlock(x, y, z, Blocks.stone, 0, 2);
                } else if (soil == SoilContext.DRY_SAND && sample.moisture() < 0.20) {
                    world.setBlock(x, y, z, Blocks.dirt, 1, 2);
                }
            }
        }
    }

    private void seedEcology(World world, int chunkX, int chunkZ, OverworldGeneration generation,
        OverworldSample[][] samples) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                OverworldSample sample = samples[localX][localZ];
                if (sample.ecologySuitability() < 0.72
                    || sample.soil() != SoilContext.LOAM && sample.soil() != SoilContext.ALLUVIAL) continue;
                int x = originX + localX;
                int z = originZ + localZ;
                if (!generation.passesChanceAt(x, z, ECOLOGY_GRASS_CHANNEL, 0.012)) continue;
                int y = world.getHeightValue(x, z);
                if (y < 1 || y >= 255
                    || world.getBlock(x, y, z) != Blocks.air
                    || world.getBlock(x, y - 1, z) != Blocks.grass) continue;
                world.setBlock(x, y, z, Blocks.tallgrass, 1, 2);
            }
        }
    }

    private int generateCopper(World world, int chunkX, int chunkZ, OverworldGeneration generation) {
        CopperDepositField deposits = new CopperDepositField(generation);
        List<CopperDeposit> candidates = deposits.depositsForChunk(chunkX, chunkZ);
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        int placed = 0;
        for (CopperDeposit deposit : candidates) {
            int extent = deposit.horizontalExtent();
            int minX = Math.max(originX, deposit.centerX() - extent);
            int maxX = Math.min(originX + 15, deposit.centerX() + extent);
            int minZ = Math.max(originZ, deposit.centerZ() - extent);
            int maxZ = Math.min(originZ + 15, deposit.centerZ() + extent);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = Math.max(1, deposit.centerY() - deposit.verticalExtent()); y
                        <= deposit.centerY() + deposit.verticalExtent(); y++) {
                        if (deposit.contains(x, y, z) && world.getBlock(x, y, z) == Blocks.stone
                            && world.setBlock(x, y, z, NEBlocks.copperOre, 0, 2)) placed++;
                    }
                }
            }
        }
        return placed;
    }

    private void generateOutcrops(World world, int chunkX, int chunkZ, WorldgenSites sites) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        int minCellX = Math.floorDiv(originX - 3, WorldgenSites.STRUCTURE_CELL_SIZE);
        int maxCellX = Math.floorDiv(originX + 18, WorldgenSites.STRUCTURE_CELL_SIZE);
        int minCellZ = Math.floorDiv(originZ - 3, WorldgenSites.STRUCTURE_CELL_SIZE);
        int maxCellZ = Math.floorDiv(originZ + 18, WorldgenSites.STRUCTURE_CELL_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                WorldgenSite site = sites.structureAtCell(cellX, cellZ);
                if (site == null) continue;
                // A small weathered boulder: a core block, a capstone, and a few deterministic neighbours.
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int x = site.x() + dx;
                        int z = site.z() + dz;
                        if (x < originX || x >= originX + 16 || z < originZ || z >= originZ + 16) continue;
                        boolean core = dx == 0 && dz == 0;
                        if (!core && (Math.abs(dx) + Math.abs(dz) > 1 || !sites.generation()
                            .passesChanceAt(x, z, BOULDER_CHANNEL, 0.5))) continue;
                        int y = world.getHeightValue(x, z) - 1;
                        if (y < SEA_LEVEL || y >= 249 || !isNaturalSurface(world.getBlock(x, y, z))) continue;
                        Block rock = sites.generation()
                            .passesChanceAt(x, z, BOULDER_CHANNEL ^ 1L, 0.45) ? Blocks.mossy_cobblestone
                                : Blocks.cobblestone;
                        world.setBlock(x, y + 1, z, rock, 0, 2);
                        if (core && world.isAirBlock(x, y + 2, z)) world.setBlock(x, y + 2, z, Blocks.stone, 0, 2);
                    }
                }
            }
        }
    }

    private void generateInitialAnomalies(World world, int chunkX, int chunkZ, WorldgenSites sites) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        int minCellX = Math.floorDiv(originX - 2, WorldgenSites.ANOMALY_CELL_SIZE);
        int maxCellX = Math.floorDiv(originX + 17, WorldgenSites.ANOMALY_CELL_SIZE);
        int minCellZ = Math.floorDiv(originZ - 2, WorldgenSites.ANOMALY_CELL_SIZE);
        int maxCellZ = Math.floorDiv(originZ + 17, WorldgenSites.ANOMALY_CELL_SIZE);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                WorldgenSite site = sites.anomalyAtCell(cellX, cellZ);
                if (site == null) continue;
                int x = site.x();
                int z = site.z();
                // A quiet scar reads as withered ground: grass gives way to coarse earth and a few dead bushes.
                witherPatch(world, sites, originX, originZ, x, z);
            }
        }
    }

    private void witherPatch(World world, WorldgenSites sites, int originX, int originZ, int centerX, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if (x < originX || x >= originX + 16 || z < originZ || z >= originZ + 16) continue;
                if (dx * dx + dz * dz > 5) continue;
                boolean edge = dx * dx + dz * dz > 2;
                if (edge && !sites.generation()
                    .passesChanceAt(x, z, SCAR_CHANNEL, 0.55)) continue;
                int y = world.getHeightValue(x, z) - 1;
                if (y < SEA_LEVEL || y >= 250 || world.getBlock(x, y, z) != Blocks.grass) continue;
                world.setBlock(x, y, z, Blocks.dirt, 1, 2);
                if (world.isAirBlock(x, y + 1, z) && sites.generation()
                    .passesChanceAt(x, z, SCAR_CHANNEL ^ 1L, 0.2)) {
                    world.setBlock(x, y + 1, z, Blocks.deadbush, 0, 2);
                }
            }
        }
    }

    private static final class ChunkGenerationRecordBuilder {

        private naturalexceptions.core.worldgen.ChunkGenerationRecord record;

        ChunkGenerationRecordBuilder(long seed, WorldgenEpoch epoch, int chunkX, int chunkZ) {
            record = new naturalexceptions.core.worldgen.ChunkGenerationRecord(seed, epoch, chunkX, chunkZ);
        }

        void mark(WorldgenStage stage) {
            record = record.withStage(stage);
        }

        naturalexceptions.core.worldgen.ChunkGenerationRecord build() {
            return record;
        }
    }
}
