package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.biology.BodyTreatment;

/** A dark red iron draught: the body makes back {@link #BLOOD_MILLILITRES} of lost blood over the next minute. */
public final class ItemIronTonic extends Item {

    public static final double BLOOD_MILLILITRES = 600.0;
    public static final double WATER_MILLILITRES = 250.0;

    public ItemIronTonic() {
        setMaxStackSize(16);
        setUnlocalizedName("ironTonic");
        setTextureName("naturalexceptions:iron_tonic");
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            BodyTreatment.fortify((EntityPlayerMP) player, BLOOD_MILLILITRES, WATER_MILLILITRES);
            world.playSoundAtEntity(player, "random.burp", 0.5F, 0.8F);
        }
        if (player.capabilities.isCreativeMode) return stack;
        stack.stackSize--;
        if (stack.stackSize <= 0) return new ItemStack(Items.glass_bottle);
        player.inventory.addItemStackToInventory(new ItemStack(Items.glass_bottle));
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.drink;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Drink to make back 600 mL of");
        lines.add("§7lost blood over the next minute.");
    }
}
