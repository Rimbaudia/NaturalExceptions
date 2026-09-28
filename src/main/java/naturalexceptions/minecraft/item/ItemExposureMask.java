package naturalexceptions.minecraft.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** Simple reusable inhalation barrier; its finite attenuation is applied from its worn state. */
public final class ItemExposureMask extends Item {
    private static final String KEY_WORN = "Worn";
    public ItemExposureMask() { setMaxStackSize(1); setUnlocalizedName("exposureMask"); setTextureName("naturalexceptions:filter_mask"); }
    public static boolean isWorn(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && stack.getTagCompound().getBoolean(KEY_WORN);
    }
    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || player == null) return stack;
        if (stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        boolean worn = !stack.getTagCompound().getBoolean(KEY_WORN);
        stack.getTagCompound().setBoolean(KEY_WORN, worn);
        player.addChatMessage(new ChatComponentText(worn
            ? "You tie on the filter mask. Breathing smoke is easier, not safe."
            : "You remove the filter mask."));
        return stack;
    }
}
