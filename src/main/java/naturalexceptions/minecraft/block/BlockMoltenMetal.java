package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Molten metal poured across a floor by the Foundry Colossus: a thin, glowing sheet that burns whatever wades into it,
 * and cools away to nothing a few seconds later. It only ever lies in open air on solid ground.
 */
public final class BlockMoltenMetal extends Block {

    /** How long a pour stays molten, at least and at most, in ticks. */
    public static final int COOLS_AFTER = 140, COOLS_SPREAD = 60;
    public static final float BURN = 3.0F;

    public BlockMoltenMetal() {
        super(new MaterialLogic(MapColor.tntColor));
        setBlockName("moltenMetal");
        setBlockTextureName("naturalexceptions:molten_metal");
        setLightLevel(12.0F / 15.0F);
        setLightOpacity(0);
        setHardness(-1.0F);
        setResistance(6000.0F);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        disableStats();
    }

    /** Whether a pour can lie at (x, y, z): open air there, solid ground under it. */
    public static boolean canLie(World world, int x, int y, int z) {
        return world.isAirBlock(x, y, z) && world.getBlock(x, y - 1, z)
            .isSideSolid(world, x, y - 1, z, net.minecraftforge.common.util.ForgeDirection.UP);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) world.scheduleBlockUpdate(x, y, z, this, COOLS_AFTER + world.rand.nextInt(COOLS_SPREAD));
    }

    /** It cools away, with a hiss. */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        world.setBlockToAir(x, y, z);
        world.playSoundEffect(x + 0.5, y + 0.1, z + 0.5, "random.fizz", 0.3F, 1.6F);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!world.getBlock(x, y - 1, z)
            .isSideSolid(world, x, y - 1, z, net.minecraftforge.common.util.ForgeDirection.UP))
            world.setBlockToAir(x, y, z);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || entity.isImmuneToFire()) return;
        entity.setFire(4);
        entity.attackEntityFrom(DamageSource.lava, BURN);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(3) == 0)
            world.spawnParticle("lava", x + random.nextDouble(), y + 0.15, z + random.nextDouble(), 0.0, 0.0, 0.0);
        if (random.nextInt(2) == 0)
            world.spawnParticle("smoke", x + random.nextDouble(), y + 0.2, z + random.nextDouble(), 0.0, 0.03, 0.0);
    }
}
