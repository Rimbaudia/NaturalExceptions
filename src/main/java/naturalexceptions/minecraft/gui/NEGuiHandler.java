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
    public static final int GUI_ASSEMBLY_TABLE = 1;
    public static final int GUI_MATERIAL_NAMING = 2;
    public static final int GUI_MOLD_ASSEMBLY = 3;
    public static final int GUI_ALLOY_NAMING = GUI_MATERIAL_NAMING;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_STARTER_SMELTER) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityStarterSmelter) {
                return new ContainerStarterSmelter(player.inventory, (TileEntityStarterSmelter) te);
            }
        }
        if (ID == GUI_ASSEMBLY_TABLE) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable) {
                return new naturalexceptions.minecraft.fabrication.ContainerAssemblyTable(
                    player.inventory,
                    (naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable) te);
            }
        }
        if (ID == GUI_MATERIAL_NAMING) {
            return new ContainerMaterialNaming(player, player.getHeldItem());
        }
        if (ID == GUI_MOLD_ASSEMBLY) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable)
                return new naturalexceptions.minecraft.fabrication.ContainerMoldAssemblyTable(
                    player.inventory,
                    (naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable) te);
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
        if (ID == GUI_ASSEMBLY_TABLE) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable) {
                return new naturalexceptions.minecraft.fabrication.client.GuiAssemblyTable(
                    player.inventory,
                    (naturalexceptions.minecraft.fabrication.TileEntityAssemblyTable) te);
            }
        }
        if (ID == GUI_MATERIAL_NAMING) {
            return new naturalexceptions.minecraft.gui.client.GuiMaterialNaming(player, player.getHeldItem());
        }
        if (ID == GUI_MOLD_ASSEMBLY) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable)
                return new naturalexceptions.minecraft.fabrication.client.GuiMoldAssemblyTable(
                    player.inventory,
                    (naturalexceptions.minecraft.fabrication.TileEntityMoldAssemblyTable) te);
        }
        return null;
    }
}
