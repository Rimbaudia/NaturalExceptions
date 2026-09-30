package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A bench that cuts one thing into many, one piece at a time, with none of the crafting grid's waste
 * ({@link CuttingRecipes}): the Mason's Bench, a stone workbench with a block on it waiting for the chisel, dresses
 * Skystone; the Sawmill, a trestle with its blade standing up through the bed, cuts logs.
 */
public final class BlockCuttingBench extends BlockParts {

    private static final double[][] MASON = { { 0.0, 0.75, 0.0, 1.0, 0.9375, 1.0 },
        { 0.0625, 0.0, 0.0625, 0.9375, 0.75, 0.9375 }, { 0.2, 0.9375, 0.25, 0.6, 1.25, 0.65 },
        { 0.7, 0.9375, 0.3, 0.78, 1.0, 0.8 } };
    private static final double[][] SAW = { { 0.0, 0.625, 0.125, 1.0, 0.8125, 0.875 },
        { 0.0625, 0.0, 0.1875, 0.25, 0.625, 0.375 }, { 0.75, 0.0, 0.1875, 0.9375, 0.625, 0.375 },
        { 0.0625, 0.0, 0.625, 0.25, 0.625, 0.8125 }, { 0.75, 0.0, 0.625, 0.9375, 0.625, 0.8125 },
        { 0.47, 0.5, 0.15, 0.53, 1.2, 0.85 }, { 0.0, 0.8125, 0.55, 0.4, 1.0625, 0.8 } };

    final CuttingRecipes.Kind kind;

    public BlockCuttingBench(CuttingRecipes.Kind kind) {
        super(kind == CuttingRecipes.Kind.SAW ? Material.wood : Material.rock);
        this.kind = kind;
        setBlockName(kind == CuttingRecipes.Kind.SAW ? "sawmill" : "masonsBench");
        setBlockTextureName(kind == CuttingRecipes.Kind.SAW ? "planks_spruce" : "stonebrick");
        setHardness(2.5F);
        setResistance(8.0F);
        setStepSound(kind == CuttingRecipes.Kind.SAW ? soundTypeWood : soundTypePiston);
        setHarvestLevel(kind == CuttingRecipes.Kind.SAW ? "axe" : "pickaxe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] src = kind == CuttingRecipes.Kind.SAW ? SAW : MASON;
        double[][] out = new double[src.length][];
        for (int i = 0; i < src.length; i++) out[i] = src[i].clone();
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        if (kind == CuttingRecipes.Kind.SAW) return Blocks.planks.getIcon(side, 1);
        return NEBlocks.skystoneDecor != null ? NEBlocks.skystoneDecor.getIcon(side, 2)
            : Blocks.stonebrick.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (kind == CuttingRecipes.Kind.SAW) {
            if (index == 0) return Blocks.planks.getIcon(1, 1);
            if (index == 5) return Blocks.iron_block.getIcon(2, 0);
            if (index == 6) return Blocks.log.getIcon(2, 0);
            return Blocks.log.getIcon(2, 1);
        }
        switch (index) {
            case 0:
                return getIcon(1, 0);
            case 1:
                return NEBlocks.skystoneBricks != null ? NEBlocks.skystoneBricks.getIcon(2, 0)
                    : Blocks.stonebrick.getIcon(2, 0);
            case 2:
                return NEBlocks.skystoneRock != null ? NEBlocks.skystoneRock.getIcon(2, 0) : Blocks.stone.getIcon(2, 0);
            default:
                return Blocks.iron_block.getIcon(2, 0);
        }
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityCuttingBench();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_CUTTING, world, x, y, z);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityCuttingBench) || ((TileEntityCuttingBench) tile).getStackInSlot(0) == null)
            return;
        // Dust from the work waiting on it: stone chips, or sawdust.
        world.spawnParticle(
            kind == CuttingRecipes.Kind.SAW ? "townaura" : "snowshovel",
            x + 0.3 + random.nextDouble() * 0.4,
            y + 1.05,
            z + 0.3 + random.nextDouble() * 0.4,
            0.0,
            0.02,
            0.0);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
