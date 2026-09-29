package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Cloud, gathered from the rims of sky islands: soft enough that landing on it takes no fall damage (see
 * {@code GearMovement.onFall}), light enough to break by hand. Bottled, it makes a Cloud Flask.
 */
public final class BlockCloud extends Block {

    public BlockCloud() {
        super(Material.cloth);
        setBlockName("cloud");
        setBlockTextureName("naturalexceptions:cloud");
        setHardness(0.2F);
        setLightOpacity(1);
        setStepSound(soundTypeCloth);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return 1;
    }

    /** Faces between two cloud blocks are not drawn, so a cloud reads as one soft mass. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
    }
}
