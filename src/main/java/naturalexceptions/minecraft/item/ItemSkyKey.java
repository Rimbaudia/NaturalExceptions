package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** The key to a sky dungeon's vault, hidden in one of its rooms. Used on the Sealed Vault Gate, it opens it. */
public final class ItemSkyKey extends Item {

    public ItemSkyKey() {
        setMaxStackSize(1);
        setUnlocalizedName("skyKey");
        setTextureName("naturalexceptions:sky_key");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("\u00a77Opens a sky dungeon's Sealed Vault Gate.");
        lines.add("\u00a77Used up when the gate opens.");
    }
}
