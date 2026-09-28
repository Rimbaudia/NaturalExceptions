package naturalexceptions.core.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;

/** Queries deterministic host-rock-gated copper seams without retaining any world-scale state. */
public final class CopperDepositField {

    public static final int CELL_SIZE = 32;
    public static final int MAX_HORIZONTAL_REACH = 10;

    private static final long DEPOSIT_SALT = 0x4355505045525f31L;
    private final OverworldGeneration generation;

    public CopperDepositField(OverworldGeneration generation) {
        this.generation = Objects.requireNonNull(generation, "generation");
    }

    /** Returns this cell's deposit when its substrate and authored occurrence field permit one. */
    public CopperDeposit depositAtCell(long cellX, long cellZ) {
        int candidateX = (int) (cellX * CELL_SIZE + (int) (unit(cellX, cellZ, 1) * CELL_SIZE));
        int candidateZ = (int) (cellZ * CELL_SIZE + (int) (unit(cellX, cellZ, 2) * CELL_SIZE));
        OverworldSample host = generation.sample(candidateX, candidateZ);
        if (host.geology() != GeologicalProvince.BASALTIC || unit(cellX, cellZ, 3) >= 0.74) return null;

        int y = 18 + (int) (unit(cellX, cellZ, 4) * 39.0);
        double angle = unit(cellX, cellZ, 5) * Math.PI;
        double length = 3.0 + unit(cellX, cellZ, 6) * 3.0;
        double radius = 1.4 + unit(cellX, cellZ, 7) * 0.8;
        DeterministicId id = DeterministicId.derive(
            "naturalexceptions:worldgen/copper_deposit",
            Long.toString(generation.worldSeed()),
            Integer.toString(
                generation.epoch()
                    .value()),
            Long.toString(cellX),
            Long.toString(cellZ));
        return new CopperDeposit(
            id,
            host.geology(),
            candidateX,
            y,
            candidateZ,
            Math.cos(angle),
            Math.sin(angle),
            length,
            radius);
    }

    /** Deposits whose bounded seams can intersect a 16 by 16 chunk, including negative coordinates. */
    public List<CopperDeposit> depositsForChunk(int chunkX, int chunkZ) {
        int minX = chunkX * 16 - MAX_HORIZONTAL_REACH;
        int maxX = chunkX * 16 + 15 + MAX_HORIZONTAL_REACH;
        int minZ = chunkZ * 16 - MAX_HORIZONTAL_REACH;
        int maxZ = chunkZ * 16 + 15 + MAX_HORIZONTAL_REACH;
        long minCellX = Math.floorDiv(minX, CELL_SIZE);
        long maxCellX = Math.floorDiv(maxX, CELL_SIZE);
        long minCellZ = Math.floorDiv(minZ, CELL_SIZE);
        long maxCellZ = Math.floorDiv(maxZ, CELL_SIZE);
        List<CopperDeposit> result = new ArrayList<>();
        for (long cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (long cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                CopperDeposit deposit = depositAtCell(cellX, cellZ);
                if (deposit != null) result.add(deposit);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private double unit(long cellX, long cellZ, long lane) {
        long value = generation.worldSeed() ^ DEPOSIT_SALT
            ^ ((long) generation.epoch()
                .value() << 48);
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
