package naturalexceptions.minecraft.fabrication;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * Assembly Table block used for assembling tools and equipment from cast components.
 */
public final class BlockAssemblyTable extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon iconTop;
    @SideOnly(Side.CLIENT)
    private IIcon iconSide;
    @SideOnly(Side.CLIENT)
    private IIcon iconBottom;

    public BlockAssemblyTable() {
        super(Material.wood);
        setHardness(2.5F);
        setResistance(5.0F);
        setStepSound(Block.soundTypeWood);
        setBlockName("assemblyTable");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        iconTop = reg.registerIcon("naturalexceptions:assembly_table_top");
        iconSide = reg.registerIcon("naturalexceptions:assembly_table_side");
        iconBottom = reg.registerIcon("naturalexceptions:refractory_brick_block");
        this.blockIcon = iconTop;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (side == 1) return iconTop;
        if (side == 0) return iconBottom;
        return iconSide;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAssemblyTable();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityAssemblyTable)) {
            return false;
        }
        TileEntityAssemblyTable table = (TileEntityAssemblyTable) te;
        ItemStack held = player.getHeldItem();

        // 1. Sneak right-click: retrieve hosted parts if present, or open GUI
        if (player.isSneaking()) {
            if (table.getStackInSlot(0) != null || table.getStackInSlot(1) != null) {
                return table.retrieveInWorld(player);
            }
            player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_ASSEMBLY_TABLE, world, x, y, z);
            return true;
        }

        // 2. In-world item placement
        if (held != null) {
            if (table.placeComponentInWorld(player, held)) {
                return true;
            }
            // If held item wasn't placed, open the Assembly GUI
            player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_ASSEMBLY_TABLE, world, x, y, z);
            return true;
        }

        // 3. Empty hand: if assembly is complete, assemble and retrieve; otherwise open GUI
        if (table.computeResult() != null) {
            return table.assembleInWorld(player);
        }

        player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_ASSEMBLY_TABLE, world, x, y, z);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityAssemblyTable) {
            TileEntityAssemblyTable table = (TileEntityAssemblyTable) te;
            for (int i = 0; i < 2; i++) {
                ItemStack stack = table.getStackInSlot(i);
                if (stack != null) {
                    float rx = world.rand.nextFloat() * 0.8F + 0.1F;
                    float ry = world.rand.nextFloat() * 0.8F + 0.1F;
                    float rz = world.rand.nextFloat() * 0.8F + 0.1F;
                    EntityItem entityItem = new EntityItem(world, x + rx, y + ry, z + rz, stack.copy());
                    world.spawnEntityInWorld(entityItem);
                }
            }
        }
        super.breakBlock(world, x, y, z, block, meta);
    }
}
