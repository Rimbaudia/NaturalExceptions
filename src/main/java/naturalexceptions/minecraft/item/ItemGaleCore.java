package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** The captive wind at a Sky Warden's heart. Builds Gale Vents, and bottles or sews the wind into sky gear. */
public final class ItemGaleCore extends Item {

    public ItemGaleCore() {
        setUnlocalizedName("galeCore");
        setTextureName("naturalexceptions:gale_core");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("\u00a77Wind that will not stay still.");
        lines.add("\u00a78Crafts Gale Vents, Cloud Flasks and Glider Cloaks.");
    }
}
