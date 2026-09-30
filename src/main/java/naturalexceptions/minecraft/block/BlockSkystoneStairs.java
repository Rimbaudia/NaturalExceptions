package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Skystone Brick Stairs: steps, seats, cornices and arches; as blast-proof as the bricks. */
public final class BlockSkystoneStairs extends BlockStairs {

    public BlockSkystoneStairs(Block bricks) {
        super(bricks, 0);
        setBlockName("skystoneStairs");
        setResistance(6000.0F);
        setHarvestLevel("pickaxe", 1);
        useNeighborBrightness = true;
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
