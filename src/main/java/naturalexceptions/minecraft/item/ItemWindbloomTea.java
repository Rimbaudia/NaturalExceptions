package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Windbloom steeped in water. For a while after drinking it, falls do not hurt. */
public final class ItemWindbloomTea extends Item {

    /** How long the tea lasts, in ticks (30 s). */
    public static final int DURATION_TICKS = 600;
    public static final String KEY_UNTIL = "naturalexceptions.windbloomUntil";

    public ItemWindbloomTea() {
        setMaxStackSize(16);
        setUnlocalizedName("windbloomTea");
        setTextureName("naturalexceptions:windbloom_tea");
    }

    /** Whether the tea is still working for this entity at world time {@code now}. */
    public static boolean active(net.minecraft.entity.Entity entity, long now) {
        return entity != null && entity.getEntityData()
            .getLong(KEY_UNTIL) > now;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            player.getEntityData()
                .setLong(KEY_UNTIL, world.getTotalWorldTime() + DURATION_TICKS);
            world.playSoundAtEntity(player, "naturalexceptions:gear.glide", 0.6F, 1.3F);
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
        lines.add("§7For 30 seconds after drinking,");
        lines.add("§7falls do not hurt.");
    }
}
