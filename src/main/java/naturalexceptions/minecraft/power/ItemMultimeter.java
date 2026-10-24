package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.power.Cable;

/** A multimeter: touch its probes to a cable or a machine to read what it carries. */
public final class ItemMultimeter extends Item {

    public ItemMultimeter() {
        setUnlocalizedName("multimeter");
        setTextureName("naturalexceptions:multimeter");
        setMaxStackSize(1);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hx,
        float hy, float hz) {
        if (world.isRemote) return true;
        Block block = world.getBlock(x, y, z);
        TileEntity tile = world.getTileEntity(x, y, z);
        PowerNetworks networks = PowerNetworks.of(world);
        String line;
        if (tile instanceof PowerReadout) line = ((PowerReadout) tile).readout();
        else if (block instanceof BlockCable) {
            Cable cable = Cable.of(world.getBlockMetadata(x, y, z));
            double t = networks.temperature(x, y, z);
            line = String.format(
                "%s: %.1f V, %.0f °C (fails at %.0f °C)",
                net.minecraft.util.StatCollector.translateToLocal("tile." + cable.unlocalizedName + ".name"),
                networks.voltage(x, y, z),
                t - 273.15,
                cable.limitKelvin() - 273.15);
        } else if (block instanceof PowerBlock) line = String.format("%.1f V", networks.voltage(x, y, z));
        else line = "No circuit here.";
        int size = networks.networkSize(x, y, z);
        player.addChatMessage(new ChatComponentText("§e" + line + (size > 0 ? " §8[" + size + " blocks]" : "")));
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Use it on a cable or a machine:");
        lines.add("§7its volts, amps, watts and heat.");
    }
}
