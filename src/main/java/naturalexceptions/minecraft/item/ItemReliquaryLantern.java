package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * A relic of the Gathered: the lantern carried before the biers. Held, its pale light lights your way and burns the
 * undead that come into it.
 */
public final class ItemReliquaryLantern extends Item {

    public static final double REACH = 6.0;
    public static final float BURN = 2.0F;

    public ItemReliquaryLantern() {
        setUnlocalizedName("reliquaryLantern");
        setTextureName("naturalexceptions:reliquary_lantern");
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    /** Held: once a second, the undead in its light burn. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity holder, int slot, boolean held) {
        if (!held || world.isRemote || !(holder instanceof EntityPlayer) || holder.ticksExisted % 20 != 0) return;
        EntityPlayer player = (EntityPlayer) holder;
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> near = world
            .getEntitiesWithinAABB(EntityLivingBase.class, player.boundingBox.expand(REACH, REACH / 2, REACH));
        for (EntityLivingBase living : near) {
            if (living == player || living.getCreatureAttribute() != EnumCreatureAttribute.UNDEAD
                || living.getDistanceToEntity(player) > REACH) continue;
            living.attackEntityFrom(
                DamageSource.causePlayerDamage(player)
                    .setMagicDamage(),
                BURN);
            living.setFire(2);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§5Relic of the Gathered");
        lines.add("§7Held, its pale light lights your way");
        lines.add("§7and burns the undead that come into it.");
    }
}
