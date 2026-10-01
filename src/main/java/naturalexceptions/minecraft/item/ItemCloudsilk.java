package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Cloudsilk, taken from a Cloudweaver: pale silk with vapour in it, light as air and stronger than it looks. It is
 * twisted into Cloudsilk Rope, woven at a Loom into dressings, sewn into a Glider Cloak's sail, and spun by a
 * Silkspinner.
 */
public final class ItemCloudsilk extends Item {

    public ItemCloudsilk() {
        setUnlocalizedName("cloudsilk");
        setTextureName("naturalexceptions:cloudsilk");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Rope, dressings, a glider's sail.");
    }
}
