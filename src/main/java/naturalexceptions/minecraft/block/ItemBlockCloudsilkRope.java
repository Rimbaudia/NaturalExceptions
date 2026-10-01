package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Cloudsilk Rope in hand: set under a block (or on hanging rope), it unrolls down as far as it can. */
public final class ItemBlockCloudsilkRope extends ItemBlock {

    public ItemBlockCloudsilkRope(Block block) {
        super(block);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        BlockCloudsilkRope rope = (BlockCloudsilkRope) field_150939_a;
        if (world.getBlock(x, y, z) == rope) return false; // the rope's own use lets it further down
        int[][] offsets = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        int px = x + offsets[side][0], py = y + offsets[side][1], pz = z + offsets[side][2];
        if (!world.isAirBlock(px, py, pz) || !rope.canBlockStay(world, px, py, pz)) return false;
        if (world.isRemote) return true;
        int used = rope.letDown(world, px, py, pz, player.capabilities.isCreativeMode ? BlockCloudsilkRope.MAX_DROP : stack.stackSize);
        if (!player.capabilities.isCreativeMode) stack.stackSize -= used;
        if (used > 0) world.playSoundEffect(px + 0.5, py + 0.5, pz + 0.5, "step.cloth", 1.0F, 0.8F);
        return used > 0;
    }
}
