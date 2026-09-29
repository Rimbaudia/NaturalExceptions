package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import naturalexceptions.minecraft.registry.NEItems;

/** A glowing growth of Lumenite on a grotto floor. Brittle: breaks by hand into a shard or two. */
public final class BlockLumeniteCluster extends Block {

    public BlockLumeniteCluster() {
        super(Material.glass);
        setBlockName("lumeniteCluster");
        setBlockTextureName("naturalexceptions:lumenite_cluster");
        setHardness(0.6F);
        setLightLevel(0.8F);
        setStepSound(soundTypeGlass);
        setBlockBounds(0.2F, 0.0F, 0.2F, 0.8F, 0.75F, 0.8F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** Drawn as crossed quads, like a flower. */
    @Override
    public int getRenderType() {
        return 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return world.getBlock(x, y - 1, z)
            .isSideSolid(world, x, y - 1, z, net.minecraftforge.common.util.ForgeDirection.UP);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return NEItems.lumeniteShard != null ? NEItems.lumeniteShard : Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1 + random.nextInt(2);
    }
}
