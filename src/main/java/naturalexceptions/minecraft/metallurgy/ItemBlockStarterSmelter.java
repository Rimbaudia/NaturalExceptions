package naturalexceptions.minecraft.metallurgy;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class ItemBlockStarterSmelter extends ItemBlock {

    public ItemBlockStarterSmelter(Block block) {
        super(block);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add("\u00a76Structure: 3x3x3 Multiblock Smelter");
        list.add("  \u00a77Base (Y-1): 3x3 solid Refractory Bricks (9 blocks)");
        list.add("  \u00a77Middle (Y=0): Controller front-center, 7 Refractory Bricks, 1 center air");
        list.add("  \u00a77Top (Y+1): 8 Refractory Bricks ring, 1 center chimney air");
        list.add("  \u00a7eUse \u00a7f/ne_smelter structure\u00a7e for construction guide.");
    }
}
