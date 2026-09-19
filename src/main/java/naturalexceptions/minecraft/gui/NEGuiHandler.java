package naturalexceptions.minecraft.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import naturalexceptions.minecraft.metallurgy.ContainerStarterSmelter;
import naturalexceptions.minecraft.metallurgy.GuiStarterSmelter;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;

public final class NEGuiHandler implements IGuiHandler {

    public static final int GUI_STARTER_SMELTER = 0;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_STARTER_SMELTER) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityStarterSmelter) {
                return new ContainerStarterSmelter(player.inventory, (TileEntityStarterSmelter) te);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_STARTER_SMELTER) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityStarterSmelter) {
                return new GuiStarterSmelter(player.inventory, (TileEntityStarterSmelter) te);
            }
        }
        return null;
    }
}
