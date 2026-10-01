package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;

import com.google.common.collect.Multimap;

/**
 * A Citadel Halberd, taken from a Citadel Guard: an axe blade and a spike on a long iron-shod pole. It strikes harder
 * than a sword ({@link #DAMAGE}), and each blow cleaves on through whatever else stands within {@link #CLEAVE} blocks
 * of what it struck, for half as much.
 */
public final class ItemCitadelHalberd extends ItemSword {

    /** Its blow (a bare hand's 1 added): an iron sword's is 6. */
    public static final double DAMAGE = 8.0;
    public static final double CLEAVE = 1.5;

    public ItemCitadelHalberd() {
        super(ToolMaterial.IRON);
        setUnlocalizedName("citadelHalberd");
        setTextureName("naturalexceptions:citadel_halberd");
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Multimap getItemAttributeModifiers() {
        Multimap map = super.getItemAttributeModifiers();
        String key = SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName();
        map.removeAll(key);
        map.put(key, new AttributeModifier(field_111210_e, "Weapon modifier", DAMAGE - 1.0, 0));
        return map;
    }

    /** The others a blow on {@code target} cleaves through: within reach of it, not the one who swung. */
    @SuppressWarnings("unchecked")
    public static List<EntityLivingBase> cleaved(EntityLivingBase target, EntityLivingBase attacker) {
        List<EntityLivingBase> near = target.worldObj
            .getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(CLEAVE, 0.5, CLEAVE));
        near.remove(target);
        near.remove(attacker);
        return near;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!target.worldObj.isRemote) {
            DamageSource source = attacker instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) attacker)
                : DamageSource.causeMobDamage(attacker);
            for (EntityLivingBase other : cleaved(target, attacker)) other.attackEntityFrom(source, (float) (DAMAGE / 2));
            if (target.worldObj instanceof WorldServer) ((WorldServer) target.worldObj)
                .func_147487_a("crit", target.posX, target.posY + target.height * 0.6, target.posZ, 10, 0.6, 0.3, 0.6, 0.2);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Cleaves on through all within " + CLEAVE + " blocks");
        lines.add("§7of what it strikes, for half as much.");
    }
}
