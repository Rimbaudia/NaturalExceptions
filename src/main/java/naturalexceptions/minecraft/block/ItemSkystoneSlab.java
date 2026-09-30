package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.item.ItemSlab;

import naturalexceptions.minecraft.registry.NEBlocks;

/** The Skystone slab as an item: placed as a half, or stacked on another into the double slab. */
public final class ItemSkystoneSlab extends ItemSlab {

    public ItemSkystoneSlab(Block block) {
        super(
            block,
            (BlockSlab) NEBlocks.skystoneSlab,
            (BlockSlab) NEBlocks.skystoneDoubleSlab,
            block == NEBlocks.skystoneDoubleSlab);
    }
}
