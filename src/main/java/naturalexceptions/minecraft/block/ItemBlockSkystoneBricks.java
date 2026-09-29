package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/** Skystone Bricks as an item: whole, cracked or mossy by metadata, each with its own name. */
public final class ItemBlockSkystoneBricks extends ItemBlockWithMetadata {

    private static final String[] SUFFIXES = { "", ".cracked", ".mossy" };

    public ItemBlockSkystoneBricks(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        return super.getUnlocalizedName() + (meta >= 0 && meta < SUFFIXES.length ? SUFFIXES[meta] : "");
    }
}
