package naturalexceptions.minecraft.weapon;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

/** A sword of star-iron from the sky caches. Every hit calls a small star down onto what it struck. */
public final class ItemSkyfall extends ItemSword {

    /** Star-iron holds an edge longer than iron; it hits like iron, the star does the rest. */
    static final int DURABILITY = 700;
    /** One star per swing, however many things a sweep touches. */
    static final long CALL_GAP_TICKS = 8L;
    private static final Map<EntityLivingBase, Long> LAST_CALL = new WeakHashMap<>();

    public ItemSkyfall() {
        super(ToolMaterial.IRON);
        setMaxDamage(DURABILITY);
        setUnlocalizedName("skyfall");
        setTextureName("naturalexceptions:skyfall");
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(stack, target, attacker);
        if (attacker != null && !attacker.worldObj.isRemote && callDue(attacker)) {
            attacker.worldObj.spawnEntityInWorld(
                EntitySkyfallStar
                    .call(attacker.worldObj, attacker, target.posX, target.posY + target.height * 0.5, target.posZ));
            attacker.worldObj.playSoundAtEntity(attacker, "naturalexceptions:weapon.skyfall_call", 0.8F, 1.0F);
        }
        return result;
    }

    private static boolean callDue(EntityLivingBase attacker) {
        long now = attacker.worldObj.getTotalWorldTime();
        synchronized (LAST_CALL) {
            Long last = LAST_CALL.get(attacker);
            if (last != null && now - last < CALL_GAP_TICKS) return false;
            LAST_CALL.put(attacker, now);
            return true;
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Every hit calls a small star down");
        lines.add("§7onto what it struck.");
    }
}
