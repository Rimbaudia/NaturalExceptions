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
    private static final int TERRAIN_ADJUSTMENT_MAX = 2;

    public static void register() {
        GameRegistry.registerWorldGenerator(new NEOverworldGenerator(), 0);
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

        adjustBaseTerrain(world, chunkX, chunkZ, samples);
        history.mark(WorldgenStage.BASE_TERRAIN);

        applyGeology(world, chunkX, chunkZ, samples);
        history.mark(WorldgenStage.GEOLOGY);
        // Hydrology is a coarse drainage/moisture context consumed by the surface derivation below.
        history.mark(WorldgenStage.HYDROLOGY);
        applySoilAndSurface(world, chunkX, chunkZ, samples);
        history.mark(WorldgenStage.SOIL_SURFACE);

        seedEcology(world, chunkX, chunkZ, generation, samples);
        history.mark(WorldgenStage.ECOLOGY);

        int copperBlocks = generateCopper(world, chunkX, chunkZ, generation);
        history.mark(WorldgenStage.RESOURCES);

        WorldgenSites sites = new WorldgenSites(generation);
        generateOutcrops(world, chunkX, chunkZ, sites);
        history.mark(WorldgenStage.STRUCTURES);
        generateInitialAnomalies(world, chunkX, chunkZ, sites);
        history.mark(WorldgenStage.INITIAL_ANOMALIES);

        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        NEChunkGenerationHistory.mark(chunk, history.build());
        chunk.setChunkModified();
        PROFILER.incrementCounter(SUBSYSTEM, "chunks_materialized", 1L);
        PROFILER.incrementCounter(SUBSYSTEM, "columns_sampled", 256L);
        PROFILER.incrementCounter(SUBSYSTEM, "copper_blocks_placed", copperBlocks);
        PROFILER.recordElapsedNanos(SUBSYSTEM, "chunk_materialization", System.nanoTime() - startNanos);
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

    /** Retains vanilla's recognizable bulk terrain while applying a small NE-owned coarse relief field. */
    private void adjustBaseTerrain(World world, int chunkX, int chunkZ, OverworldSample[][] samples) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = originX + localX;
                int z = originZ + localZ;
                int surfaceY = world.getHeightValue(x, z) - 1;
                if (surfaceY < SEA_LEVEL || surfaceY >= 250) continue;
                Block surface = world.getBlock(x, surfaceY, z);
                if (!isNaturalSurface(surface)) continue;
                double elevation = samples[localX][localZ].elevationTendency();
                int adjustment = (int) Math.round((elevation - 0.5) * (TERRAIN_ADJUSTMENT_MAX * 2.0));
                adjustment = Math.max(-TERRAIN_ADJUSTMENT_MAX, Math.min(TERRAIN_ADJUSTMENT_MAX, adjustment));
                if (adjustment > 0) {
                    for (int y = 1; y <= adjustment; y++) {
                        world.setBlock(x, surfaceY + y, z, Blocks.dirt, 0, 2);
                    }
                    world.setBlock(x, surfaceY, z, Blocks.dirt, 0, 2);
                    world.setBlock(x, surfaceY + adjustment, z, Blocks.grass, 0, 2);
                } else if (adjustment < 0) {
                    for (int y = 0; y < -adjustment; y++) {
                        world.setBlockToAir(x, surfaceY - y, z);
                    }
                    int loweredSurface = surfaceY + adjustment;
                    if (world.getBlock(x, loweredSurface, z) == Blocks.dirt) {
                        world.setBlock(x, loweredSurface, z, Blocks.grass, 0, 2);
                    }
                }
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
    private void applyGeology(World world, int chunkX, int chunkZ, OverworldSample[][] samples) {
        int originX = chunkX * 16;
        int originZ = chunkZ * 16;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                OverworldSample sample = samples[localX][localZ];
                Block host = sample.geology() == naturalexceptions.core.worldgen.GeologicalProvince.SEDIMENTARY
                    ? Blocks.sandstone
                    : sample.geology() == naturalexceptions.core.worldgen.GeologicalProvince.GRANITIC
                        ? Blocks.cobblestone
                        : Blocks.stone;
                int firstLayer = 32 + (int) Math.round(sample.elevationTendency() * 6.0);
                for (int depth = 0; depth < 4; depth++) {
                    int y = firstLayer + depth;
                    if (world.getBlock(originX + localX, y, originZ + localZ) == Blocks.stone && host != Blocks.stone) {
                        world.setBlock(originX + localX, y, originZ + localZ, host, 0, 2);
                    }
                }
            }
        }
    }

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
                SoilContext soil = samples[localX][localZ].soil();
                if ((soil == SoilContext.ALLUVIAL || soil == SoilContext.THIN_ROCKY)
                    && (top == Blocks.grass || top == Blocks.dirt || top == Blocks.stone)) {
                    world.setBlock(x, y, z, Blocks.gravel, 0, 2);
                } else if (soil == SoilContext.DRY_SAND && (top == Blocks.grass || top == Blocks.dirt)) {
                    world.setBlock(x, y, z, Blocks.sand, 0, 2);
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
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                        int x = site.x() + dx;
                        int z = site.z() + dz;
                        if (x < originX || x >= originX + 16 || z < originZ || z >= originZ + 16) continue;
                        int y = world.getHeightValue(x, z) - 1;
                        if (y >= SEA_LEVEL && y < 250 && isNaturalSurface(world.getBlock(x, y, z))) {
                            world.setBlock(x, y, z, Blocks.cobblestone, 0, 2);
                        }
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
                if (x < originX || x >= originX + 16 || z < originZ || z >= originZ + 16) continue;
                int y = world.getHeightValue(x, z) - 1;
                if (y < SEA_LEVEL || y >= 250 || !isNaturalSurface(world.getBlock(x, y, z))) continue;
                world.setBlock(x, y, z, Blocks.obsidian, 0, 2);
                world.setBlock(x, y + 1, z, Blocks.glowstone, 0, 2);
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
