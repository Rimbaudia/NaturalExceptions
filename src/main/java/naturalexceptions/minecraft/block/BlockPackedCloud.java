package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Packed Cloud: cloud pressed hard at the Loom, two to one, until it holds. Unlike loose cloud it bears weight, and it
 * is so light it holds itself out over nothing: used toward open air, it sets itself in the air touching another
 * Packed Cloud, up to {@link #REACH} blocks out from anything solid. It breaks at a touch. Meta: how far it is from
 * something solid (0 against it).
 */
public final class BlockPackedCloud extends Block {

    /** How many blocks of it can hold out over nothing from something solid. */
    public static final int REACH = 4;

    public BlockPackedCloud() {
        super(Material.cloth);
        setBlockName("packedCloud");
        setBlockTextureName("naturalexceptions:packed_cloud");
        setHardness(0.1F);
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

    /** Faces between two packed cloud blocks are not drawn. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
    }

    /** It always drops whole, whatever it was held out from. */
    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    /** How far from something solid a Packed Cloud at (x, y, z) would be: one past its nearest-held neighbour. */
    public static int reachAt(World world, int x, int y, int z) {
        int best = Integer.MAX_VALUE;
        for (int[] f : new int[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } }) {
            int nx = x + f[0], ny = y + f[1], nz = z + f[2];
            Block block = world.getBlock(nx, ny, nz);
            if (block == NEBlocks.packedCloud) best = Math.min(best, world.getBlockMetadata(nx, ny, nz) + 1);
            else if (block.getMaterial()
                .isSolid() && block != NEBlocks.cloud) best = 0;
        }
        return best;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int reach = reachAt(world, x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, Math.min(15, reach == Integer.MAX_VALUE ? REACH : reach), 2);
    }

    /** Its item: used toward open air, it sets itself there, held out from the Packed Cloud it touches. */
    public static final class ItemPackedCloud extends ItemBlock {

        public ItemPackedCloud(Block block) {
            super(block);
        }

        @Override
        public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
            if (world.isRemote || stack.stackSize <= 0) return stack;
            Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            Vec3 look = player.getLookVec();
            for (double d = 1.5; d <= 4.0; d += 0.25) {
                int x = MathHelper.floor_double(eye.xCoord + look.xCoord * d);
                int y = MathHelper.floor_double(eye.yCoord + look.yCoord * d);
                int z = MathHelper.floor_double(eye.zCoord + look.zCoord * d);
                if (!world.isAirBlock(x, y, z)) return stack;
                int reach = reachAt(world, x, y, z);
                if (reach > REACH || !player.canPlayerEdit(x, y, z, 1, stack)
                    || !world.checkNoEntityCollision(field_150939_a.getCollisionBoundingBoxFromPool(world, x, y, z)))
                    continue;
                world.setBlock(x, y, z, field_150939_a, reach, 3);
                world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "step.cloth", 0.8F, 1.2F);
                if (!player.capabilities.isCreativeMode) stack.stackSize--;
                return stack;
            }
            return stack;
        }
    }
}
