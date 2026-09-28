package naturalexceptions.core.worldgen;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;

/** Sparse, lazy deterministic locations for P6 surface structures and initial anomalies. */
public final class WorldgenSites {

    public static final int STRUCTURE_CELL_SIZE = 48;
    public static final int ANOMALY_CELL_SIZE = 128;

    private final OverworldGeneration generation;

    public WorldgenSites(OverworldGeneration generation) {
        this.generation = Objects.requireNonNull(generation, "generation");
    }

    public WorldgenSite structureAtCell(long cellX, long cellZ) {
        int x = candidate(cellX, cellZ, STRUCTURE_CELL_SIZE, 11);
        int z = candidate(cellX, cellZ, STRUCTURE_CELL_SIZE, 12);
        OverworldSample sample = generation.sample(x, z);
        if (sample.elevationTendency() < 0.55 || sample.slope() < 0.025
            || sample.geology() == GeologicalProvince.SEDIMENTARY
            || unit(cellX, cellZ, 13) >= 0.60) return null;
        return site(WorldgenSite.Kind.ROCK_OUTCROP, cellX, cellZ, x, z, sample);
    }

    public WorldgenSite anomalyAtCell(long cellX, long cellZ) {
        int x = candidate(cellX, cellZ, ANOMALY_CELL_SIZE, 21);
        int z = candidate(cellX, cellZ, ANOMALY_CELL_SIZE, 22);
        OverworldSample sample = generation.sample(x, z);
        if (sample.geology() != GeologicalProvince.GRANITIC || sample.elevationTendency() < 0.56
            || sample.moisture() > 0.66
            || unit(cellX, cellZ, 23) >= 0.48) return null;
        return site(WorldgenSite.Kind.INITIAL_ANOMALY, cellX, cellZ, x, z, sample);
    }

    private WorldgenSite site(WorldgenSite.Kind kind, long cellX, long cellZ, int x, int z, OverworldSample sample) {
        DeterministicId id = DeterministicId.derive(
            "naturalexceptions:worldgen/" + kind.name()
                .toLowerCase(java.util.Locale.ROOT),
            Long.toString(generation.worldSeed()),
            Integer.toString(
                generation.epoch()
                    .value()),
            Long.toString(cellX),
            Long.toString(cellZ));
        return new WorldgenSite(id, kind, x, z, sample);
    }

    private int candidate(long cellX, long cellZ, int size, long lane) {
        return (int) (cellX * size + (int) (unit(cellX, cellZ, lane) * size));
    }

    private double unit(long cellX, long cellZ, long lane) {
        long value = generation.worldSeed() ^ 0x4e45536974657331L
            ^ ((long) generation.epoch()
                .value() << 43);
        value ^= mix64(cellX * 0x9e3779b97f4a7c15L);
        value ^= mix64(cellZ * 0xc2b2ae3d27d4eb4fL);
        value ^= mix64(lane * 0x165667b19e3779f9L);
        return (mix64(value) >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
