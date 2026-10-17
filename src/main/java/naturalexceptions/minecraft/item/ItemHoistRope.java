package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A coil of Hoist Rope, laid up at the Loom from string: {@link #BLOCKS_PER_COIL} blocks of it. Fitted to a windlass
 * (right-click the windlass with it), the coils are the hoist's reach.
 */
public final class ItemHoistRope extends Item {

    public static final int BLOCKS_PER_COIL = 8;

    public ItemHoistRope() {
        setUnlocalizedName("hoistRope");
        setTextureName("naturalexceptions:hoist_rope");
        setMaxStackSize(16);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7" + BLOCKS_PER_COIL + " blocks of rope; fit it to a windlass");
    }
}
