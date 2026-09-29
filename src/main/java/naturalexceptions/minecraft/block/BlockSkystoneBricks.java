package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * The pale carved stone of the sky shrines. It does not break under a Fallen Star's impact or any explosion,
 * so it is what you build with where stars fall.
 */
public final class BlockSkystoneBricks extends Block {

    public BlockSkystoneBricks() {
        super(Material.rock);
        setBlockName("skystoneBricks");
        setBlockTextureName("naturalexceptions:skystone_bricks");
        setHardness(3.0F);
        setResistance(6000.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public boolean canDropFromExplosion(net.minecraft.world.Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {
        // Skystone shrugs off blasts.
    }
}
