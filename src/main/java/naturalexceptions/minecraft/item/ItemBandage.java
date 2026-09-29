package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * A field dressing. It is used from the Body panel: pick it up and drop it on a wound, and it dresses a cut or burn
 * or splints a broken bone there. Treatment helps the body heal; it does not undo the injury.
 */
public final class ItemBandage extends Item {

    public ItemBandage() {
        setMaxStackSize(16);
        setUnlocalizedName("bandage");
        setTextureName("naturalexceptions:field_dressing");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("\u00a77Drop it on a wound in the Body panel");
        lines.add("\u00a77(inventory, Body tab).");
    }
}
