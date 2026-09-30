package naturalexceptions.minecraft.furniture;

import net.minecraft.item.ItemStack;

/** A cutting bench's one slot: what is waiting to be cut. Which bench it is comes from its block. */
public final class TileEntityCuttingBench extends TileEntityStorage {

    public TileEntityCuttingBench() {
        super(1, "container.naturalexceptions.cutting");
    }

    public CuttingRecipes.Kind kind() {
        return worldObj != null && worldObj.getBlock(xCoord, yCoord, zCoord) instanceof BlockCuttingBench
            ? ((BlockCuttingBench) worldObj.getBlock(xCoord, yCoord, zCoord)).kind
            : CuttingRecipes.Kind.MASON;
    }

    @Override
    public String getInventoryName() {
        return kind() == CuttingRecipes.Kind.SAW ? "tile.sawmill.name" : "tile.masonsBench.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return CuttingRecipes.accepts(kind(), stack);
    }
}
