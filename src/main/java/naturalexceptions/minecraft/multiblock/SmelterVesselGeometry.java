package naturalexceptions.minecraft.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import naturalexceptions.minecraft.registry.NEBlocks;

/** Discovered bounded cavity geometry shared by smelter validation and vessel consumers. */
public final class SmelterVesselGeometry {

    /** Gameplay scale: one clear cavity block represents 25 litres of usable vessel volume. */
    public static final double LITRES_PER_CAVITY_CELL = 25.0;
    public static final int MIN_OUTER_WIDTH = 3;
    public static final int MAX_OUTER_WIDTH = 7;
    public static final int MIN_INTERIOR_HEIGHT = 1;
    public static final int MAX_INTERIOR_HEIGHT = 3;

    private final int outerWidth;
    private final int outerDepth;
    private final int interiorHeight;
    private final int facing;
    private final int controllerX;
    private final int controllerY;
    private final int controllerZ;
    /** Colossus Crucibles set into the shell in place of bricks. */
    private int crucibles;

    private SmelterVesselGeometry(int width, int depth, int height, int facing, int x, int y, int z) {
        outerWidth = width;
        outerDepth = depth;
        interiorHeight = height;
        this.facing = facing;
        controllerX = x;
        controllerY = y;
        controllerZ = z;
    }

    public int interiorWidth() {
        return outerWidth - 2;
    }

    public int interiorDepth() {
        return outerDepth - 2;
    }

    public int interiorHeight() {
        return interiorHeight;
    }

    /** Whether (x, y, z) is in its shell (floor or walls, the controller aside). */
    public boolean inShell(int x, int y, int z) {
        int half = outerWidth / 2;
        for (int dy = -1; dy <= interiorHeight; dy++)
            for (int dx = -half; dx <= half; dx++) for (int dz = 0; dz < outerDepth; dz++) {
                boolean floor = dy == -1, perimeter = dx == -half || dx == half || dz == 0 || dz == outerDepth - 1;
                if (!floor && !perimeter) continue;
                int[] pos = world(dx, dy, dz);
                if (pos[0] == x && pos[1] == y && pos[2] == z) return true;
            }
        return false;
    }

    /** How many Colossus Crucibles are set into its shell (each in place of a refractory brick). */
    public int crucibles() {
        return crucibles;
    }

    /** A shell block: refractory brick, or a Colossus Crucible in its place. */
    private boolean shell(Block actual, Block brick) {
        if (actual == brick) return true;
        if (actual != null && actual == NEBlocks.colossusCrucible) {
            crucibles++;
            return true;
        }
        return actual != null && actual == NEBlocks.electricHeater;
    }

    public int cavityCellCount() {
        return interiorWidth() * interiorDepth() * interiorHeight;
    }

    public double capacityLitres() {
        return cavityCellCount() * LITRES_PER_CAVITY_CELL;
    }

    public double fillFractionForVolume(double occupiedLitres) {
        if (!Double.isFinite(occupiedLitres) || occupiedLitres <= 0.0) return 0.0;
        return Math.min(1.0, occupiedLitres / capacityLitres());
    }

    public double heightForVolume(double occupiedLitres) {
        return interiorHeight * fillFractionForVolume(occupiedLitres);
    }

    public List<int[]> cavityCells() {
        List<int[]> cells = new ArrayList<>();
        int half = outerWidth / 2;
        for (int y = 0; y < interiorHeight; y++) {
            for (int x = -half + 1; x < half; x++) {
                for (int z = 1; z < outerDepth - 1; z++) cells.add(world(x, y, z));
            }
        }
        return Collections.unmodifiableList(cells);
    }

    public int[] world(int dx, int dy, int dz) {
        return MultiblockDefinition.localToWorld(controllerX, controllerY, controllerZ, facing, dx, dy, dz);
    }

    /** Finds the largest valid bounded rectangular shell around the controller. */
    public static SmelterVesselGeometry discover(IBlockAccess world, int cx, int cy, int cz, int facing) {
        if (world == null || facing < 2 || facing > 5) return null;
        Block brick = NEBlocks.refractoryBrick;
        Block controller = NEBlocks.starterSmelter;
        if (brick == null || controller == null || world.getBlock(cx, cy, cz) != controller) return null;
        for (int width = MAX_OUTER_WIDTH; width >= MIN_OUTER_WIDTH; width -= 2) {
            for (int depth = MAX_OUTER_WIDTH; depth >= MIN_OUTER_WIDTH; depth -= 2) {
                for (int height = MAX_INTERIOR_HEIGHT; height >= MIN_INTERIOR_HEIGHT; height--) {
                    SmelterVesselGeometry candidate = new SmelterVesselGeometry(
                        width,
                        depth,
                        height,
                        facing,
                        cx,
                        cy,
                        cz);
                    if (candidate.matches(world, brick, controller)) return candidate;
                }
            }
        }
        return null;
    }

    private boolean matches(IBlockAccess world, Block brick, Block controller) {
        crucibles = 0;
        int half = outerWidth / 2;
        for (int x = -half; x <= half; x++) {
            for (int z = 0; z < outerDepth; z++) {
                int[] floor = world(x, -1, z);
                if (!shell(world.getBlock(floor[0], floor[1], floor[2]), brick)) return false;
            }
        }
        for (int y = 0; y <= interiorHeight; y++) {
            for (int x = -half; x <= half; x++) {
                for (int z = 0; z < outerDepth; z++) {
                    boolean perimeter = x == -half || x == half || z == 0 || z == outerDepth - 1;
                    boolean isController = x == 0 && z == 0 && y == 0;
                    int[] pos = world(x, y, z);
                    Block actual = world.getBlock(pos[0], pos[1], pos[2]);
                    if (isController) {
                        if (actual != controller) return false;
                    } else if (perimeter) {
                        if (!shell(actual, brick)) return false;
                    } else if (!world.isAirBlock(pos[0], pos[1], pos[2])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
