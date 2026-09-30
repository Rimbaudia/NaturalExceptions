package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.worldgen.DungeonBeacons;

/**
 * The key to a sky dungeon's Warden, hidden in one of its rooms (and now and then in a storeroom, or carried by its
 * guards). Used on the Sealed Vault Gate, it opens it. Held, it pulls toward the nearest such gate: a thin stream of
 * Stormglass light runs from it that way, and used, it says how far and how deep.
 */
public final class ItemSkyKey extends Item {

    /** How far along the pull its light runs, in blocks. */
    static final int STREAM = 6;

    public ItemSkyKey() {
        setMaxStackSize(1);
        setUnlocalizedName("skyKey");
        setTextureName("naturalexceptions:sky_key");
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.uncommon;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (!held || world.isRemote || !(entity instanceof EntityPlayer) || world.getTotalWorldTime() % 20 != 0) return;
        DungeonBeacons.Beacon beacon = DungeonBeacons.nearest(world, entity.posX, entity.posZ);
        if (beacon != null) stream((EntityPlayer) entity, beacon.gate, STREAM);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        DungeonBeacons.Beacon beacon = DungeonBeacons.nearest(world, player.posX, player.posZ);
        if (beacon == null) {
            player.addChatComponentMessage(new ChatComponentText("§7The key lies still: no sealed gate near."));
            return stack;
        }
        player.addChatComponentMessage(
            new ChatComponentText(
                "§dThe key pulls "
                    + DungeonBeacons
                        .describe(player.posX, player.posY + player.getEyeHeight() - 1.0, player.posZ, beacon.gate)
                    + "."));
        stream(player, beacon.gate, STREAM * 2);
        world.playSoundAtEntity(player, "note.pling", 0.6F, 1.7F);
        return stack;
    }

    /** Stormglass light from the key toward {@code gate}, brighter and humming when it is close. */
    private static void stream(EntityPlayer player, int[] gate, int length) {
        if (!(player.worldObj instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.worldObj;
        double x = player.posX, y = player.posY + player.getEyeHeight() - 0.5, z = player.posZ;
        double dx = gate[0] + 0.5 - x, dy = gate[1] + 1.5 - y, dz = gate[2] + 0.5 - z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0) return;
        boolean close = distance < 10.0;
        for (int i = 1; i <= Math.min(length, (int) distance); i++) {
            double t = i / distance;
            world.func_147487_a("witchMagic", x + dx * t, y + dy * t, z + dz * t, close ? 3 : 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (close) world.playSoundEffect(x, y, z, "note.harp", 0.3F, 2.0F);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Opens a sky dungeon's Sealed Vault Gate.");
        lines.add("§7Used up when the gate opens.");
        lines.add("§7Held, it pulls toward the nearest");
        lines.add("§7sealed gate; use it to feel how far.");
    }
}
