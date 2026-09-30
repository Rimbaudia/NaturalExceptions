package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import naturalexceptions.minecraft.biology.BodyTreatment;

/**
 * A potter's water jug: thrown on a Potter's Wheel, it holds {@link #DRINKS} good drinks. Dip it in water (or a
 * cauldron) to fill it; drink from it as from a bottle, a quarter of a litre at a time. Its damage counts the drinks
 * gone, so it is empty at {@link #DRINKS}, never broken.
 */
public final class ItemWaterJug extends Item {

    public static final int DRINKS = 4;
    public static final double SIP_MILLILITRES = 250.0;

    public ItemWaterJug() {
        setMaxStackSize(1);
        setMaxDamage(DRINKS);
        setNoRepair();
        setUnlocalizedName("waterJug");
        setTextureName("naturalexceptions:water_jug");
    }

    public static int drinksLeft(ItemStack stack) {
        return DRINKS - Math.min(DRINKS, stack.getItemDamage());
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && world.getBlock(hit.blockX, hit.blockY, hit.blockZ)
                .getMaterial() == Material.water) {
            if (drinksLeft(stack) < DRINKS) {
                stack.setItemDamage(0);
                world.playSoundAtEntity(player, "game.neutral.swim", 0.4F, 1.4F);
            }
            return stack;
        }
        if (drinksLeft(stack) > 0) player.setItemInUse(stack, getMaxItemUseDuration(stack));
        return stack;
    }

    /** Filled from a cauldron, which gives up a third of its water. */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.getBlock(x, y, z) != Blocks.cauldron || drinksLeft(stack) >= DRINKS) return false;
        int level = world.getBlockMetadata(x, y, z);
        if (level <= 0) return false;
        if (!world.isRemote) {
            Blocks.cauldron.func_150024_a(world, x, y, z, level - 1);
            stack.setItemDamage(0);
            world.playSoundAtEntity(player, "game.neutral.swim", 0.4F, 1.4F);
        }
        return true;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (drinksLeft(stack) <= 0) return stack;
        if (!world.isRemote && player instanceof EntityPlayerMP)
            BodyTreatment.drink((EntityPlayerMP) player, SIP_MILLILITRES);
        if (!player.capabilities.isCreativeMode) stack.setItemDamage(stack.getItemDamage() + 1);
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

    /** A jug is never worn out: its bar shows the water in it. */
    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - drinksLeft(stack) / (double) DRINKS;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        int left = drinksLeft(stack);
        lines.add(left > 0 ? "§7" + left + " of " + DRINKS + " drinks of water" : "§7Empty: dip it in water to fill it");
    }
}
