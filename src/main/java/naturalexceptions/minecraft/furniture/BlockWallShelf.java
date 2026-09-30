package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A wall shelf: a spruce board on two brackets, holding three stacks of anything, the front of each on show. */
public final class BlockWallShelf extends BlockWallFurniture {

    private static final double[][] PARTS = { { 0.0, 0.5, 0.0, 1.0, 0.5625, 0.5 },
        { 0.125, 0.25, 0.0, 0.1875, 0.5, 0.375 }, { 0.8125, 0.25, 0.0, 0.875, 0.5, 0.375 } };

    public BlockWallShelf() {
        super("wallShelf");
    }

    @Override
    double[][] authored() {
        return PARTS;
    }

    @Override
    int setsDown(ItemStack held) {
        return held.stackSize; // the whole stack
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityWallShelf();
    }
}
