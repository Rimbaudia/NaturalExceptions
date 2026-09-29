package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * Old, cracked Skystone floor over a drop. It holds until someone steps on it: then it shudders and falls away half
 * a second later, taking the crumbling tiles joined to it along one after another.
 */
public final class BlockCrumblingSkystone extends Block {

    /** Meta 0: whole. 1: giving way. */
    static final int GIVING_WAY = 1;
    static final int DELAY_TICKS = 10;
    static final int CHAIN_TICKS = 3;

    public BlockCrumblingSkystone() {
        super(Material.rock);
        setBlockName("crumblingSkystone");
        setBlockTextureName("naturalexceptions:crumbling_skystone");
        setHardness(0.8F);
        setResistance(3.0F);
        setStepSound(soundTypeStone);
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (entity instanceof EntityPlayer) giveWay(world, x, y, z, DELAY_TICKS);
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
        if (entity instanceof EntityPlayer) giveWay(world, x, y, z, DELAY_TICKS);
    }

    private void giveWay(World world, int x, int y, int z, int delay) {
        if (world.isRemote || world.getBlockMetadata(x, y, z) == GIVING_WAY) return;
        world.setBlockMetadataWithNotify(x, y, z, GIVING_WAY, 2);
        world.scheduleBlockUpdate(x, y, z, this, delay);
        world.playSoundEffect(
            x + 0.5,
            y + 0.5,
            z + 0.5,
            "naturalexceptions:trap.crumble",
            0.7F,
            0.9F + world.rand.nextFloat() * 0.2F);
        if (world instanceof net.minecraft.world.WorldServer) ((net.minecraft.world.WorldServer) world)
            .func_147487_a("blockcrack_4_0", x + 0.5, y + 1.0, z + 0.5, 10, 0.3, 0.05, 0.3, 0.05);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || world.getBlockMetadata(x, y, z) != GIVING_WAY) return;
        world.func_147480_a(x, y, z, false); // breaks with particles and sound, drops nothing
        int[][] around = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : around)
            if (world.getBlock(x + d[0], y, z + d[1]) == this) giveWay(world, x + d[0], y, z + d[1], CHAIN_TICKS);
    }

    /** While giving way it sheds grit, so there is a moment to jump. */
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) != GIVING_WAY) return;
        for (int i = 0; i < 4; i++)
            world.spawnParticle("blockcrack_4_0", x + random.nextDouble(), y, z + random.nextDouble(), 0.0, -0.1, 0.0);
    }
}
