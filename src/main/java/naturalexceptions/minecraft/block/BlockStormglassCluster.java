package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Stormglass hanging in points from the underside of a sky island. Each block hangs from rock or from the point
 * above it, so breaking a point's top brings the rest of it down. Brittle: breaks by hand into shards. Now and then
 * a spark runs down it.
 */
public final class BlockStormglassCluster extends Block {

    public BlockStormglassCluster() {
        super(Material.glass);
        setBlockName("stormglassCluster");
        setBlockTextureName("naturalexceptions:stormglass_cluster");
        setHardness(0.5F);
        setLightLevel(0.6F);
        setStepSound(soundTypeGlass);
        setBlockBounds(0.15F, 0.1F, 0.15F, 0.85F, 1.0F, 0.85F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** Drawn as crossed quads, like a flower, but hanging. */
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
        Block above = world.getBlock(x, y + 1, z);
        return above == this
            || above.isSideSolid(world, x, y + 1, z, net.minecraftforge.common.util.ForgeDirection.DOWN);
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
        return NEItems.stormglassShard != null ? NEItems.stormglassShard : Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1 + random.nextInt(3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(4) != 0) return;
        world.spawnParticle(
            "fireworksSpark",
            x + 0.3 + random.nextDouble() * 0.4,
            y + random.nextDouble() * 0.6,
            z + 0.3 + random.nextDouble() * 0.4,
            0.0,
            -0.02,
            0.0);
        if (random.nextInt(6) == 0) world.playSound(
            x + 0.5,
            y + 0.5,
            z + 0.5,
            "fireworks.twinkle_far",
            0.15F,
            1.6F + random.nextFloat() * 0.3F,
            false);
    }
}
