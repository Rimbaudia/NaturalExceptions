package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/** Dressed Skystone as an item: tiles, cracked tiles, polished or chiseled by metadata, each with its own name. */
public final class ItemBlockSkystoneDecor extends ItemBlockWithMetadata {

    private static final String[] SUFFIXES = { ".tiles", ".tilesCracked", ".polished", ".chiseled" };

    public ItemBlockSkystoneDecor(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        return super.getUnlocalizedName() + (meta >= 0 && meta < SUFFIXES.length ? SUFFIXES[meta] : "");
    }
}
