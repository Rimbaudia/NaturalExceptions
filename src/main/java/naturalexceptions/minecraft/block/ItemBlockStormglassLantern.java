package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** The Stormglass Lamp (a whole block) and the small Stormglass Lantern, as items. */
public final class ItemBlockStormglassLantern extends ItemBlock {

    public ItemBlockStormglassLantern(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return BlockStormglassLantern.small(stack.getItemDamage()) ? super.getUnlocalizedName() + ".small"
            : super.getUnlocalizedName();
    }
}
