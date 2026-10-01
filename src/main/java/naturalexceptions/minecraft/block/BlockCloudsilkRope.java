package naturalexceptions.minecraft.block;

import java.util.function.IntPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Cloudsilk Rope: the way down from a sky island. Set it under a block (or hang it from the rope above) and it unrolls
 * straight down as far as there is air, up to {@link #MAX_DROP} blocks, as far as the rope you hold will go; use more
 * rope on a hanging one to let it further down. Climb it like a ladder; sneak to hang still. Each length hangs from
 * the one above, so cutting it brings all of it below down with it, and back to you.
 */
public final class BlockCloudsilkRope extends Block {

    public static final int MAX_DROP = 32;

    public BlockCloudsilkRope() {
        super(Material.cloth);
        setBlockName("cloudsilkRope");
        setBlockTextureName("naturalexceptions:cloudsilk_rope");
        setHardness(0.3F);
        setStepSound(soundTypeCloth);
        setBlockBounds(0.4F, 0.0F, 0.4F, 0.6F, 1.0F, 0.6F);
    }

    /**
     * How many lengths of rope unroll from the first place down: as many places as are {@code clear} one below the
     * next, no more than {@code have} or {@link #MAX_DROP}.
     */
    public static int unroll(int have, IntPredicate clear) {
        int n = 0;
        while (n < Math.min(have, MAX_DROP) && clear.test(n)) n++;
        return n;
    }

    /** Lets rope down from (x, y, z) (the first free place below its support), using up to {@code have}; how many. */
    public int letDown(World world, int x, int y, int z, int have) {
        int n = unroll(have, k -> world.isAirBlock(x, y - k, z) && y - k > 0);
        for (int k = 0; k < n; k++) world.setBlock(x, y - k, z, this, 0, 3);
        return n;
    }

    /** Where rope let down from the column at (x, y, z) would start: below its lowest length. */
    public int bottom(World world, int x, int y, int z) {
        while (world.getBlock(x, y - 1, z) == this) y--;
        return y - 1;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        Block above = world.getBlock(x, y + 1, z);
        return above == this || above.isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, 0, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** More rope on a hanging rope lets it further down. */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (held == null || held.getItem() != net.minecraft.item.Item.getItemFromBlock(this)) return false;
        if (world.isRemote) return true;
        int used = letDown(world, x, bottom(world, x, y, z), z, player.capabilities.isCreativeMode ? MAX_DROP : held.stackSize);
        if (!player.capabilities.isCreativeMode) held.stackSize -= used;
        if (used > 0) world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "step.cloth", 1.0F, 0.8F);
        return true;
    }

    @Override
    public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** Drawn as crossed quads, a twisted strand down the middle. */
    @Override
    public int getRenderType() {
        return 1;
    }
}
