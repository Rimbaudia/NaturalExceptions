package naturalexceptions.minecraft.multiblock;

import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

/**
 * Defines a single relative block expectation within a multiblock structure definition.
 */
public final class MultiblockPattern {

    @FunctionalInterface
    public interface BlockPredicate {

        boolean test(IBlockAccess world, int x, int y, int z);
    }

    private final int dx;
    private final int dy;
    private final int dz;
    private final MultiblockRole role;
    private final String blockName;
    private final ItemStack displayStack;
    private final BlockPredicate predicate;

    public MultiblockPattern(int dx, int dy, int dz, MultiblockRole role, String blockName, ItemStack displayStack,
        BlockPredicate predicate) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.role = Objects.requireNonNull(role, "role");
        this.blockName = Objects.requireNonNull(blockName, "blockName");
        this.displayStack = displayStack;
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static MultiblockPattern ofBlock(int dx, int dy, int dz, MultiblockRole role, Block block, String name,
        ItemStack stack) {
        return new MultiblockPattern(
            dx,
            dy,
            dz,
            role,
            name,
            stack,
            (world, x, y, z) -> world != null && world.getBlock(x, y, z) == block);
    }

    public static MultiblockPattern ofAir(int dx, int dy, int dz) {
        return new MultiblockPattern(
            dx,
            dy,
            dz,
            MultiblockRole.REQUIRED_AIR,
            "Air Cavity",
            null,
            (world, x, y, z) -> world != null && world.getBlock(x, y, z)
                .isAir(world, x, y, z));
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public MultiblockRole role() {
        return role;
    }

    public String blockName() {
        return blockName;
    }

    public ItemStack displayStack() {
        return displayStack != null ? displayStack.copy() : null;
    }

    public boolean matches(IBlockAccess world, int x, int y, int z) {
        return predicate.test(world, x, y, z);
    }
}
