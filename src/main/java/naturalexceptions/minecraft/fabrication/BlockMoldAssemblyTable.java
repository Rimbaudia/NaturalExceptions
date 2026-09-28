package naturalexceptions.minecraft.fabrication;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/** Simple hand-shaping station for selecting one authored casting-mold geometry. */
public final class BlockMoldAssemblyTable extends BlockContainer {

    public BlockMoldAssemblyTable() {
        super(Material.wood);
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setBlockName("moldAssemblyTable");
        setBlockTextureName("naturalexceptions:mold_assembly_table_top");
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon iconTop;
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon iconSide;
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon iconBottom;

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister reg) {
        iconTop = reg.registerIcon("naturalexceptions:mold_assembly_table_top");
        iconSide = reg.registerIcon("naturalexceptions:mold_assembly_table_side");
        iconBottom = reg.registerIcon("naturalexceptions:assembly_table_bottom");
        blockIcon = iconTop;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getIcon(int side, int meta) {
        return side == 1 ? iconTop : side == 0 ? iconBottom : iconSide;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityMoldAssemblyTable();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_MOLD_ASSEMBLY, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof TileEntityMoldAssemblyTable) {
            TileEntityMoldAssemblyTable table = (TileEntityMoldAssemblyTable) tile;
            for (int slot = 0; slot < table.getSizeInventory(); slot++) {
                ItemStack stack = table.getStackInSlot(slot);
                if (stack != null) {
                    world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stack.copy()));
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
