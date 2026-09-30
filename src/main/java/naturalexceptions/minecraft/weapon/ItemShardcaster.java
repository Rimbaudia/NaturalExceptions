package naturalexceptions.minecraft.weapon;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A copper wand with a Stormglass crystal clawed at its tip. Fires piercing shard bolts; one shard loads
 * {@value ShardcasterCharge#SHOTS_PER_SHARD}, and it holds up to {@value ShardcasterCharge#MAX_CHARGE}. Sneak and
 * right-click to load shards from the inventory; fired empty, it loads one by itself. The crystal glows while it is
 * loaded and flares as it fires.
 */
public final class ItemShardcaster extends Item {

    private static final String KEY_CHARGE = "Charge";
    private static final String KEY_LAST_SHOT = "LastShot";
    static final long SHOT_GAP_TICKS = 6L;
    static final long FLARE_TICKS = 4L;

    @SideOnly(Side.CLIENT)
    private IIcon empty, flare;

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
        boolean free = player.capabilities.isCreativeMode;
        if (player.isSneaking() && !free) {
            reload(stack, player);
            return stack;
        }
        if (now - tag.getLong(KEY_LAST_SHOT) < SHOT_GAP_TICKS) return stack;
        boolean hasShard = free || NEItems.stormglassShard != null && player.inventory.hasItem(NEItems.stormglassShard);
        int[] result = ShardcasterCharge.fire(free ? Math.max(1, charge(stack)) : charge(stack), hasShard);
        if (result == null) {
            world.playSoundAtEntity(player, "random.click", 0.5F, 0.6F);
            fizzle(world, player);
            player.addChatComponentMessage(new ChatComponentText("§7The crystal is dark: no Stormglass Shards."));
            return stack;
        }
        if (result[1] > 0 && !free) player.inventory.consumeInventoryItem(NEItems.stormglassShard);
        tag.setInteger(KEY_CHARGE, free ? charge(stack) : result[0]);
        tag.setLong(KEY_LAST_SHOT, now);
        EntityStormglassBolt bolt = new EntityStormglassBolt(world, player);
        world.spawnEntityInWorld(bolt);
        muzzle(world, player);
        world.playSoundAtEntity(
            player,
            "naturalexceptions:weapon.shard_fire",
            0.8F,
            0.95F + itemRand.nextFloat() * 0.1F);
        sync(player);
        return stack;
    }

    /** Loads shards from the inventory until it is full or they run out. */
    private static void reload(ItemStack stack, EntityPlayer player) {
        int shards = 0;
        for (ItemStack slot : player.inventory.mainInventory)
            if (slot != null && slot.getItem() == NEItems.stormglassShard) shards += slot.stackSize;
        int load = ShardcasterCharge.shardsToLoad(charge(stack), shards);
        if (load == 0) {
            player.addChatComponentMessage(
                new ChatComponentText(
                    charge(stack) >= ShardcasterCharge.MAX_CHARGE ? "§7The crystal is full."
                        : "§7No Stormglass Shards to load."));
            return;
        }
        for (int i = 0; i < load; i++) player.inventory.consumeInventoryItem(NEItems.stormglassShard);
        int charge = charge(stack) + load * ShardcasterCharge.SHOTS_PER_SHARD;
        stack.getTagCompound()
            .setInteger(KEY_CHARGE, charge);
        player.worldObj.playSoundAtEntity(player, "random.orb", 0.6F, 1.6F);
        player.addChatComponentMessage(new ChatComponentText("§bLoaded " + charge + " bolts."));
        sync(player);
    }

    /** The shard count and the charge show at once. */
    private static void sync(EntityPlayer player) {
        player.inventory.markDirty();
        if (player instanceof EntityPlayerMP) player.inventoryContainer.detectAndSendChanges();
    }

    /** A burst of pale sparks where the crystal is, as the bolt leaves it. */
    private static void muzzle(World world, EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        net.minecraft.util.Vec3 look = player.getLookVec();
        double x = player.posX + look.xCoord * 0.9, y = player.posY + player.getEyeHeight() - 0.15 + look.yCoord * 0.9,
            z = player.posZ + look.zCoord * 0.9;
        ((WorldServer) world).func_147487_a("fireworksSpark", x, y, z, 6, 0.08, 0.08, 0.08, 0.04);
        ((WorldServer) world).func_147487_a("instantSpell", x, y, z, 4, 0.1, 0.1, 0.1, 0.0);
    }

    /** The dark crystal flickers once and smokes: nothing to fire. */
    private static void fizzle(World world, EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        net.minecraft.util.Vec3 look = player.getLookVec();
        double x = player.posX + look.xCoord * 0.9, y = player.posY + player.getEyeHeight() - 0.15 + look.yCoord * 0.9,
            z = player.posZ + look.zCoord * 0.9;
        ((WorldServer) world).func_147487_a("fireworksSpark", x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        ((WorldServer) world).func_147487_a("smoke", x, y, z, 3, 0.04, 0.04, 0.04, 0.01);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        empty = register.registerIcon("naturalexceptions:shardcaster_empty");
        flare = register.registerIcon("naturalexceptions:shardcaster_flare");
    }

    /** The crystal: flaring just after a shot, glowing while loaded, dark when empty. */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        net.minecraft.client.multiplayer.WorldClient world = net.minecraft.client.Minecraft.getMinecraft().theWorld;
        if (world != null && stack.hasTagCompound()
            && world.getTotalWorldTime() - stack.getTagCompound()
                .getLong(KEY_LAST_SHOT) < FLARE_TICKS)
            return flare;
        return charge(stack) > 0 ? itemIcon : empty;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass) {
        return getIconIndex(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    /** The bar shows the charge: full when loaded to the brim, empty when dark. */
    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0 - Math.min(1.0, charge(stack) / (double) ShardcasterCharge.MAX_CHARGE);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Fires Stormglass bolts that pierce up to three foes.");
        lines.add("§7One Stormglass Shard loads " + ShardcasterCharge.SHOTS_PER_SHARD + " bolts.");
        lines.add("§7Sneak and use to load shards; empty, it loads one itself.");
        lines.add("§bLoaded: " + charge(stack) + " / " + ShardcasterCharge.MAX_CHARGE);
    }
}
