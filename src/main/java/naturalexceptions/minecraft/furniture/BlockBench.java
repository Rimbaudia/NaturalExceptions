package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/** A Skystone bench: a polished slab on two carved supports. Sit on it to rest. */
public final class BlockBench extends BlockSeat {

    private static final double[][] PARTS = { { 0.0, 0.375, 0.15, 1.0, 0.5, 0.85 },
        { 0.08, 0.0, 0.25, 0.28, 0.375, 0.75 }, { 0.72, 0.0, 0.25, 0.92, 0.375, 0.75 } };

    public BlockBench() {
        super(Material.rock);
        setBlockName("stoneBench");
        setBlockTextureName("stonebrick");
        setHardness(1.5F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    double[][] authored() {
        return PARTS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return NEBlocks.skystoneDecor != null ? NEBlocks.skystoneDecor.getIcon(side, 2)
            : Blocks.stonebrick.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index > 0 && NEBlocks.skystoneDecor != null) return NEBlocks.skystoneDecor.getIcon(2, 3);
        return getIcon(1, 0);
    }
}
