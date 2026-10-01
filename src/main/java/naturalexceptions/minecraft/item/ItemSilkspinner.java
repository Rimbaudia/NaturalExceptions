package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import naturalexceptions.minecraft.entity.EntityCloudsilk;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The Silkspinner: a Cloudweaver's spinneret, set in silver on a wrist-guard. Each use shoots a strand of Cloudsilk
 * that binds what it strikes, as a Cloudweaver's do, and spins up one Cloudsilk from your pack to do it. Only a
 * Cloudweaver ever leaves one.
 */
public final class ItemSilkspinner extends Item {

    public static final int USES = 250, COOLDOWN = 15;
    private static final String KEY_NEXT = "NextShot";

    public ItemSilkspinner() {
        setMaxStackSize(1);
        setMaxDamage(USES);
        setUnlocalizedName("silkspinner");
        setTextureName("naturalexceptions:silkspinner");
        setFull3D();
    }

    /** Whether {@code player} can shoot one now: Cloudsilk to spin (or creative). */
    public static boolean loaded(EntityPlayer player) {
        return player.capabilities.isCreativeMode || NEItems.cloudsilk != null
            && player.inventory.hasItem(NEItems.cloudsilk);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        long now = world.getTotalWorldTime();
        if (now < stack.getTagCompound()
            .getLong(KEY_NEXT) || !loaded(player)) return stack;
        if (!player.capabilities.isCreativeMode) {
            player.inventory.consumeInventoryItem(NEItems.cloudsilk);
            stack.damageItem(1, player);
        }
        stack.getTagCompound()
            .setLong(KEY_NEXT, now + COOLDOWN);
        world.spawnEntityInWorld(new EntityCloudsilk(world, player));
        world.playSoundAtEntity(player, "mob.spider.say", 0.5F, 1.8F);
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Shoots a strand of Cloudsilk that");
        lines.add("§7binds what it strikes. Spins one");
        lines.add("§7Cloudsilk from your pack a shot.");
    }
}
