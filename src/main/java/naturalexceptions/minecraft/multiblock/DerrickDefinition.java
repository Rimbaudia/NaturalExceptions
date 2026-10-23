package naturalexceptions.minecraft.multiblock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

import naturalexceptions.minecraft.furniture.BlockDerrickPivot;
import naturalexceptions.minecraft.furniture.BlockSheave;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A derrick: a hoist whose rope hangs from a boom that swings. A Derrick Pivot stands on the ground, a mast of logs
 * stands on it ({@link #MIN_MAST} to {@link #MAX_MAST} high), and a boom of logs runs out level from the mast's top
 * ({@link #MIN_BOOM} to {@link #MAX_BOOM} long) with a Sheave at its tip. A Windlass on the same ground within
 * {@link #REACH} blocks of the pivot works it: the rope runs up the mast, out along the boom, over the sheave and down.
 * Right-click the pivot to swing the boom a quarter-turn (sneak for the other way) when its way round is clear.
 * <p>
 * The patterns are its smallest layout, for the Knowledge Browser: the windlass at the origin, the pivot two blocks
 * ahead (local +z), a mast of four and a boom of four running on ahead.
 */
public final class DerrickDefinition extends MultiblockDefinition {

    public static final DerrickDefinition INSTANCE = new DerrickDefinition();

    public static final int MIN_MAST = 3, MAX_MAST = 12, MIN_BOOM = 3, MAX_BOOM = 12, REACH = 5;

    private DerrickDefinition() {
        super("derrick", "Derrick", MultiblockMaturity.STABLE, null);
        Block log = Blocks.log, sheave = NEBlocks.sheave != null ? NEBlocks.sheave : Blocks.planks;
        Block windlass = NEBlocks.hoistWindlass != null ? NEBlocks.hoistWindlass : Blocks.planks;
        Block pivot = NEBlocks.derrickPivot != null ? NEBlocks.derrickPivot : Blocks.planks;
        addPattern(
            MultiblockPattern
                .ofBlock(0, 0, 0, MultiblockRole.REQUIRED_CONTROLLER, windlass, "Windlass", new ItemStack(windlass)));
        addPattern(
            MultiblockPattern
                .ofBlock(0, 0, 2, MultiblockRole.REQUIRED_BLOCK, pivot, "Derrick Pivot", new ItemStack(pivot)));
        for (int dy = 1; dy <= 4; dy++) addPattern(
            MultiblockPattern.ofBlock(0, dy, 2, MultiblockRole.REQUIRED_BLOCK, log, "Log", new ItemStack(log)));
        for (int k = 1; k <= 3; k++) addPattern(
            MultiblockPattern.ofBlock(0, 4, 2 + k, MultiblockRole.REQUIRED_BLOCK, log, "Log", new ItemStack(log)));
        addPattern(
            MultiblockPattern.ofBlock(0, 4, 6, MultiblockRole.REQUIRED_BLOCK, sheave, "Sheave", new ItemStack(sheave)));
        for (int dy = -2; dy <= 3; dy++) addPattern(MultiblockPattern.ofAir(0, dy, 6));
    }

    /** The derrick a windlass at (x, y, z) works: a pivot on its ground, within reach, with mast and boom whole. */
    public HoistFrame find(IBlockAccess world, int x, int y, int z) {
        HoistFrame best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
            if (!(world.getBlock(x + dx, y, z + dz) instanceof BlockDerrickPivot)) continue;
            HoistFrame frame = frame(world, x + dx, y, z + dz);
            int distance = dx * dx + dz * dz;
            if (frame != null && distance < bestDistance) {
                best = frame;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The derrick standing on the pivot at (px, py, pz), if it is whole; null if not. */
    public HoistFrame frame(IBlockAccess world, int px, int py, int pz) {
        int top = py;
        while (top < py + MAX_MAST && world.getBlock(px, top + 1, pz) instanceof BlockLog) top++;
        int mast = top - py;
        if (mast < MIN_MAST) return null;
        for (int facing = 2; facing <= 5; facing++) {
            int[] d = step(facing);
            int k = 1;
            while (k <= MAX_BOOM && world.getBlock(px + d[0] * k, top, pz + d[1] * k) instanceof BlockLog) k++;
            if (k < MIN_BOOM || k > MAX_BOOM) continue;
            int sx = px + d[0] * k, sz = pz + d[1] * k;
            if (!(world.getBlock(sx, top, sz) instanceof BlockSheave)) continue;
            if (!HeadframeDefinition.clear(world, sx, top - 1, sz)) continue;
            return new HoistFrame(HoistFrame.Kind.DERRICK, sx, top, sz, py - 1, px, py, pz, top, facing, k);
        }
        return null;
    }

    /** A vanilla side (2..5) as a step {dx, dz}. */
    public static int[] step(int facing) {
        switch (facing) {
            case 2:
                return new int[] { 0, -1 };
            case 3:
                return new int[] { 0, 1 };
            case 4:
                return new int[] { -1, 0 };
            default:
                return new int[] { 1, 0 };
        }
    }

    /** The next side round, clockwise seen from above (north, east, south, west), or the other way. */
    public static int turn(int facing, boolean clockwise) {
        int[] round = { 2, 5, 3, 4 };
        int i = 0;
        while (round[i] != facing) i++;
        return round[(i + (clockwise ? 1 : 3)) % 4];
    }

    /** Validation for a windlass at (x, y, z): whole if it has a derrick to work. */
    @Override
    public MultiblockValidationResult validate(IBlockAccess world, int x, int y, int z, int facing) {
        if (world == null) return MultiblockValidationResult.fail(x, y, z, null, "World is null");
        if (find(world, x, y, z) != null) return MultiblockValidationResult.ok();
        return MultiblockValidationResult.fail(
            x,
            y,
            z,
            null,
            "none within " + REACH
                + " blocks: a Derrick Pivot on this ground, a log mast "
                + MIN_MAST
                + "-"
                + MAX_MAST
                + " high on it, a log boom "
                + MIN_BOOM
                + "-"
                + MAX_BOOM
                + " long out from its top with a Sheave at the tip, over open air");
    }
}
