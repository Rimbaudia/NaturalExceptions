package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/** Skyslate as an item: bricks or shingles by metadata. */
public final class ItemBlockSkyslate extends ItemBlockWithMetadata {

    private static final String[] SUFFIXES = { ".bricks", ".shingles" };

    public ItemBlockSkyslate(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        return super.getUnlocalizedName() + (meta >= 0 && meta < SUFFIXES.length ? SUFFIXES[meta] : "");
    }
}
