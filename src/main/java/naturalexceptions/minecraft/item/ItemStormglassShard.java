package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A splinter of Stormglass, the crystal that grows under the sky islands where storms charge their rock: the charge
 * of a Shardcaster, and what sky treasures are made with.
 */
public final class ItemStormglassShard extends Item {

    public ItemStormglassShard() {
        setUnlocalizedName("stormglassShard");
        setTextureName("naturalexceptions:stormglass_shard");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Grows hanging under the sky islands.");
        lines.add("§7A Shardcaster's charge; sky treasures");
        lines.add("§7are made with it.");
    }
}
