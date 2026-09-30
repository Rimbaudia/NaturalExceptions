package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.item.ItemSlab;

import naturalexceptions.minecraft.registry.NEBlocks;

/** The Skyslate slab as an item: placed as a half, or stacked on another into the double slab. */
public final class ItemSkyslateSlab extends ItemSlab {

    public ItemSkyslateSlab(Block block) {
        super(
            block,
            (BlockSlab) NEBlocks.skyslateSlab,
            (BlockSlab) NEBlocks.skyslateDoubleSlab,
            block == NEBlocks.skyslateDoubleSlab);
    }
}
