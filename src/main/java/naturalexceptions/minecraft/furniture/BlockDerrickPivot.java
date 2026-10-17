package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A derrick's pivot: a turntable on a footing, the mast standing in its hub. Right-click it to swing the boom a
 * quarter-turn clockwise (seen from above); sneak to swing it the other way ({@link TileEntityDerrickPivot}).
 */
public final class BlockDerrickPivot extends BlockParts {

    private static final double[][] PARTS = { { 0.0, 0.0, 0.0, 1.0, 0.25, 1.0 }, { 0.1, 0.25, 0.1, 0.9, 0.45, 0.9 },
        { 0.25, 0.45, 0.25, 0.75, 1.0, 0.75 } };

    public BlockDerrickPivot() {
        super(Material.wood);
        setBlockName("derrickPivot");
        setBlockTextureName("planks_spruce");
        setHardness(2.5F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index == 0) return Blocks.cobblestone.getIcon(1, 0);
        return index == 1 ? Blocks.iron_block.getIcon(1, 0) : Blocks.log.getIcon(2, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityDerrickPivot();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityDerrickPivot) ((TileEntityDerrickPivot) tile).slew(!player.isSneaking(), player);
        return true;
    }
}
