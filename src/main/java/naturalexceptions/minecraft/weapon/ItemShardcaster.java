package naturalexceptions.minecraft.weapon;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import naturalexceptions.minecraft.registry.NEItems;

/**
 * A copper wand tipped with Lumenite. Fires piercing shard bolts; one shard loads
 * {@value ShardcasterCharge#SHOTS_PER_SHARD}.
 */
public final class ItemShardcaster extends Item {

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_LAST_SHOT = "LastShot";
    static final long SHOT_GAP_TICKS = 6L;

    public ItemShardcaster() {
        setMaxStackSize(1);
        setUnlocalizedName("shardcaster");
        setTextureName("naturalexceptions:shardcaster");
        setFull3D();
    }

    public static int charge(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getInteger(KEY_CHARGE) : 0;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        long now = world.getTotalWorldTime();
        if (now - tag.getLong(KEY_LAST_SHOT) < SHOT_GAP_TICKS) return stack;
        boolean free = player.capabilities.isCreativeMode;
        boolean hasShard = free || NEItems.lumeniteShard != null && player.inventory.hasItem(NEItems.lumeniteShard);
        int[] result = ShardcasterCharge.fire(free ? Math.max(1, charge(stack)) : charge(stack), hasShard);
        if (result == null) {
            world.playSoundAtEntity(player, "random.click", 0.5F, 0.6F);
            return stack;
        }
        if (result[1] > 0 && !free) player.inventory.consumeInventoryItem(NEItems.lumeniteShard);
        tag.setInteger(KEY_CHARGE, free ? charge(stack) : result[0]);
        tag.setLong(KEY_LAST_SHOT, now);
        world.spawnEntityInWorld(new EntityLumeniteBolt(world, player));
        world.playSoundAtEntity(
            player,
            "naturalexceptions:weapon.shard_fire",
            0.8F,
            0.95F + itemRand.nextFloat() * 0.1F);
        return stack;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Fires Lumenite bolts that pierce up to three foes.");
        lines.add("§7One Lumenite Shard loads " + ShardcasterCharge.SHOTS_PER_SHARD + " bolts.");
        lines.add("§bLoaded: " + charge(stack));
    }
}
