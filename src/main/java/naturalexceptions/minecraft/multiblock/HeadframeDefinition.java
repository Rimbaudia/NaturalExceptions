package naturalexceptions.minecraft.multiblock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

import naturalexceptions.minecraft.furniture.BlockSheave;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A headframe: a hoist over a shaft. Two posts of logs stand on the ground either side of the shaft's mouth, a beam
 * across their tops holds a Sheave over the shaft's middle, and a Windlass stands on the ground within
 * {@link #REACH} blocks of the shaft, its rope running up over the sheave and down the shaft to the basket.
 * <p>
 * Its size is the builder's: the posts {@link #MIN_HEIGHT} to {@link #MAX_HEIGHT} blocks high, the shaft as deep as it
 * is dug. {@link #find} discovers it around a windlass, and is what both a player's build and a generated one are held
 * to. The patterns are its smallest layout, for the Knowledge Browser: the windlass at the origin, facing the shaft
 * three blocks ahead (local +z), posts four high.
 */
public final class HeadframeDefinition extends MultiblockDefinition {

    public static final HeadframeDefinition INSTANCE = new HeadframeDefinition();

    public static final int MIN_HEIGHT = 3, MAX_HEIGHT = 12;
    /** How far from its windlass the shaft may be, either way. */
    public static final int REACH = 5;

    private HeadframeDefinition() {
        super("headframe", "Headframe", MultiblockMaturity.STABLE, null);
        Block log = Blocks.log, sheave = NEBlocks.sheave != null ? NEBlocks.sheave : Blocks.planks;
        Block windlass = NEBlocks.hoistWindlass != null ? NEBlocks.hoistWindlass : Blocks.planks;
        addPattern(
            MultiblockPattern
                .ofBlock(0, 0, 0, MultiblockRole.REQUIRED_CONTROLLER, windlass, "Windlass", new ItemStack(windlass)));
        for (int side : new int[] { -1, 1 }) for (int dy = 0; dy <= 4; dy++) addPattern(
            MultiblockPattern.ofBlock(side, dy, 3, MultiblockRole.REQUIRED_BLOCK, log, "Log", new ItemStack(log)));
        addPattern(
            MultiblockPattern.ofBlock(0, 4, 3, MultiblockRole.REQUIRED_BLOCK, sheave, "Sheave", new ItemStack(sheave)));
        for (int dy = -3; dy <= 3; dy++) addPattern(MultiblockPattern.ofAir(0, dy, 3));
        if (NEBlocks.hoistBasket != null) addPattern(
            MultiblockPattern.ofBlock(
                0,
                -3,
                3,
                MultiblockRole.OPTIONAL_ATTACHMENT,
                NEBlocks.hoistBasket,
                "Basket",
                new ItemStack(NEBlocks.hoistBasket)));
    }

    /**
     * The headframe a windlass at (x, y, z) works: a sheave within reach whose frame stands on the ground the windlass
     * stands on; null if there is none.
     */
    public HoistFrame find(IBlockAccess world, int x, int y, int z) {
        HoistFrame best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dz = -REACH; dz <= REACH; dz++) for (int dy = MIN_HEIGHT; dy <= MAX_HEIGHT + 1; dy++) {
                int sx = x + dx, sy = y + dy, sz = z + dz;
                if (!(world.getBlock(sx, sy, sz) instanceof BlockSheave)) continue;
                HoistFrame frame = frame(world, sx, sy, sz);
                if (frame == null || frame.groundY != y - 1) continue;
                int distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    best = frame;
                    bestDistance = distance;
                }
            }
        return best;
    }

    /** The headframe round the sheave at (sx, sy, sz), if it is whole; null if not. */
    public HoistFrame frame(IBlockAccess world, int sx, int sy, int sz) {
        for (boolean alongX : new boolean[] { true, false }) {
            int ax = alongX ? 1 : 0, az = alongX ? 0 : 1;
            int ground = post(world, sx - ax, sy, sz - az);
            if (ground == Integer.MIN_VALUE || ground != post(world, sx + ax, sy, sz + az)) continue;
            int height = sy - ground - 1;
            if (height < MIN_HEIGHT || height > MAX_HEIGHT) continue;
            boolean open = true;
            for (int y = sy - 1; y >= ground && open; y--) open = clear(world, sx, y, sz);
            if (open) return HoistFrame.headframe(sx, sy, sz, ground);
        }
        return null;
    }

    /** The ground under a post whose top (a beam end) is at (x, top, z); {@link Integer#MIN_VALUE} if it is no post. */
    private static int post(IBlockAccess world, int x, int top, int z) {
        if (!(world.getBlock(x, top, z) instanceof BlockLog)) return Integer.MIN_VALUE;
        int y = top - 1;
        while (y > 0 && isPost(world.getBlock(x, y, z))) y--;
        Block ground = world.getBlock(x, y, z);
        return y < top - 1 && ground.getMaterial()
            .isSolid() ? y : Integer.MIN_VALUE;
    }

    private static boolean isPost(Block block) {
        return block instanceof BlockLog || block instanceof BlockFence;
    }

    /** Open for the basket: air, or the basket itself waiting there. */
    public static boolean clear(IBlockAccess world, int x, int y, int z) {
        return naturalexceptions.minecraft.furniture.HoistGeometry.clearFor(world, x, y, z);
    }

    /** Validation for a windlass at (x, y, z): whole if it has a headframe to work. */
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
                + " blocks: a Sheave in a log beam on two log posts "
                + MIN_HEIGHT
                + "-"
                + MAX_HEIGHT
                + " high, standing on the ground this windlass stands on, over an open shaft");
    }
}
