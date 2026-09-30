package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.biology.BodyTreatment;

/**
 * A jar of Windbloom paste, rubbed into open wounds: every bleeding cut slows to a trickle at once and clots four
 * times as fast after. It does not close the wound; a dressing still does that.
 */
public final class ItemStypticSalve extends Item {

    public ItemStypticSalve() {
        setMaxStackSize(16);
        setUnlocalizedName("stypticSalve");
        setTextureName("naturalexceptions:styptic_salve");
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)) return stack;
        if (BodyTreatment.stanch((EntityPlayerMP) player) == 0) {
            player.addChatComponentMessage(new ChatComponentText("§7Nothing is bleeding."));
            return stack;
        }
        world.playSoundAtEntity(player, "mob.slime.small", 0.7F, 1.2F);
        if (world instanceof WorldServer) ((WorldServer) world)
            .func_147487_a("slime", player.posX, player.posY + 0.9, player.posZ, 6, 0.25, 0.3, 0.25, 0.0);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.block;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Hold use to rub it in:");
        lines.add("§7every bleeding cut slows at once");
        lines.add("§7and clots fast. A dressing still");
        lines.add("§7closes the wound.");
    }
}
