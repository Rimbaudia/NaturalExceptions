package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.block.PartModel;

/**
 * Something to sit on. Right-click with an empty hand to sit (sneak to stand); while seated you heal as though
 * resting in bed, and wounds mend faster, without the night passing. Meta is the side its back is on (2..5).
 */
abstract class BlockSeat extends BlockParts {

    BlockSeat(Material material) {
        super(material);
    }

    /** The boxes with the back (if any) to the north (-z). */
    abstract double[][] authored();

    @Override
    public double[][] parts(int metadata) {
        double[][] boxes = authored();
        double[][] out = new double[boxes.length][];
        int back = metadata < 2 ? 2 : metadata;
        for (int i = 0; i < boxes.length; i++) out[i] = PartModel.rotate(boxes[i], back);
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 3;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        // Placed facing you: its back away from you.
        world.setBlockMetadataWithNotify(x, y, z, BlockPantryShelf.facingTowards(placer) ^ 1, 2);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (player.getHeldItem() != null || player.isRiding() || player.isSneaking()) return false;
        if (world.isRemote) return true;
        @SuppressWarnings("unchecked")
        List<EntitySeat> taken = world.getEntitiesWithinAABB(
            EntitySeat.class,
            net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
        if (!taken.isEmpty()) return true;
        EntitySeat seat = new EntitySeat(world, x, y, z);
        world.spawnEntityInWorld(seat);
        player.mountEntity(seat);
        return true;
    }
}
