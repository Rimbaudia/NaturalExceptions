package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.core.metallurgy.VesselState;

/** A shard of Lumenite: light without fire, a smelting flux, and the charge of a Shardcaster. */
public final class ItemLumeniteShard extends Item {

    public ItemLumeniteShard() {
        setUnlocalizedName("lumeniteShard");
        setTextureName("naturalexceptions:lumenite_shard");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Put in a smelter's feed: the next " + VesselState.FLUX_PER_SHARD + " ores");
        lines.add("§7give more metal and less slag.");
        lines.add("§7Four around a copper ingot make a Lumenite Lamp.");
    }
}
