package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;
import net.minecraft.item.ItemStack;

/** Furniture that comes in kinds by metadata (counter goods, table wood, awning colour), each with its own name. */
public final class ItemBlockFurniture extends ItemBlockWithMetadata {

    public ItemBlockFurniture(Block block) {
        super(block, block);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        String[] kinds;
        int kind;
        if (field_150939_a instanceof BlockMarketCounter) {
            kinds = BlockMarketCounter.GOODS;
            kind = meta & 3;
        } else if (field_150939_a instanceof BlockWoodenTable) {
            kinds = BlockWoodenTable.WOODS;
            kind = meta & 3;
        } else if (field_150939_a instanceof BlockMarketAwning) {
            kinds = BlockMarketAwning.COLOURS;
            kind = (meta >> 2) & 3;
        } else return super.getUnlocalizedName(stack);
        return super.getUnlocalizedName() + "." + kinds[Math.min(kind, kinds.length - 1)];
    }
}
