package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A wooden pew: a seat, a tall back and two end boards. Sit on it to rest. */
public final class BlockPew extends BlockSeat {

    private static final double[][] PARTS = { { 0.0, 0.375, 0.1, 1.0, 0.5, 0.8 }, { 0.0, 0.5, 0.0, 1.0, 1.0, 0.12 },
        { 0.0, 0.0, 0.0, 0.1, 0.75, 0.8 }, { 0.9, 0.0, 0.0, 1.0, 0.75, 0.8 } };

    public BlockPew() {
        super(Material.wood);
        setBlockName("pew");
        setBlockTextureName("planks_spruce");
        setHardness(1.5F);
        setResistance(4.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
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
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index >= 2 ? Blocks.planks.getIcon(1, 5) : getIcon(1, 0);
    }
}
