package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The Stormglass heart of a Citadel Automaton, still faintly charged: what kept the dead city's machines walking.
 * Cracked open, it is good Stormglass.
 */
public final class ItemStormglassHeart extends Item {

    public ItemStormglassHeart() {
        setUnlocalizedName("stormglassHeart");
        setTextureName("naturalexceptions:stormglass_heart");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7What kept a Citadel Automaton walking.");
        lines.add("§7It still hums, faintly.");
        lines.add("§8Crack it open for four Stormglass Shards.");
    }
}
