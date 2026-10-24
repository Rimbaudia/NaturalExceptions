package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.entity.EntityGatheredShade;

/**
 * A relic of the Gathered: the little bell the mourners rang over the dead. Rung, the undead round you stand still and
 * forget you for a while, and the shades of the Gathered are struck; it must rest between ringings.
 */
public final class ItemBellOfRest extends Item {

    public static final double REACH = 10.0;
    public static final int STILL_TICKS = 100, REST_TICKS = 600;
    public static final float SHADE_HURT = 4.0F;

    public ItemBellOfRest() {
        setUnlocalizedName("bellOfRest");
        setTextureName("naturalexceptions:bell_of_rest");
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    /** Ticks until it can be rung again (0 if it can). */
    public static long resting(ItemStack stack, long now) {
        if (!stack.hasTagCompound()) return 0;
        long rung = stack.getTagCompound()
            .getLong("Rung");
        return Math.max(0, rung + REST_TICKS - now);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        long now = world.getTotalWorldTime();
        if (resting(stack, now) > 0) return stack;
        if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stack.getTagCompound()
            .setLong("Rung", now);
        for (float pitch : new float[] { 0.5F, 0.63F, 0.75F })
            world.playSoundAtEntity(player, "note.pling", 1.5F, pitch);
        player.swingItem();
        if (world.isRemote) return stack;
        if (world instanceof WorldServer) ((WorldServer) world)
            .func_147487_a("note", player.posX, player.posY + 2.2, player.posZ, 6, 0.6, 0.2, 0.6, 1.0);
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> near = world
            .getEntitiesWithinAABB(EntityLivingBase.class, player.boundingBox.expand(REACH, REACH / 2, REACH));
        for (EntityLivingBase living : near) {
            if (living == player || living.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD) continue;
            if (living instanceof EntityGatheredShade)
                living.attackEntityFrom(DamageSource.causePlayerDamage(player), SHADE_HURT);
            if (living instanceof IBossDisplayData) continue; // the great dead do not sleep for a bell
            living.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, STILL_TICKS, 4));
            living.addPotionEffect(new PotionEffect(Potion.weakness.id, STILL_TICKS, 1));
            if (living instanceof EntityLiving) ((EntityLiving) living).setAttackTarget(null);
            if (living instanceof net.minecraft.entity.EntityCreature)
                ((net.minecraft.entity.EntityCreature) living).setTarget(null);
        }
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§5Relic of the Gathered");
        lines.add("§7Ring it: the undead round you stand still");
        lines.add("§7and forget you; shades are struck.");
        long rest = player == null ? 0 : resting(stack, player.worldObj.getTotalWorldTime());
        lines.add(rest > 0 ? "§8Resting: " + (rest / 20 + 1) + " s" : "§8Ready to ring.");
    }
}
