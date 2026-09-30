package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.block.BlockSkystoneBricks;
import naturalexceptions.minecraft.block.BlockSkystoneDecor;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A mason's chisel: set to a placed Skystone block, it dresses it afresh where it stands. Bricks weather to cracked,
 * then mossy, then back to whole; tiles crack and are made good; polished stone is carved, and smoothed again.
 */
public final class ItemMasonsChisel extends Item {

    public ItemMasonsChisel() {
        setMaxStackSize(1);
        setMaxDamage(128);
        setUnlocalizedName("masonsChisel");
        setTextureName("naturalexceptions:masons_chisel");
        setFull3D();
    }

    /** What the block of {@code block} with {@code meta} becomes under the chisel, or -1 if it does not take it. */
    public static int dressed(Block block, int meta) {
        if (block != null && block == NEBlocks.skystoneBricks) {
            if (meta == BlockSkystoneBricks.LOOSE) return -1;
            return (meta + 1) % BlockSkystoneBricks.VARIANTS.length;
        }
        if (block != null && block == NEBlocks.skystoneDecor) {
            switch (meta) {
                case BlockSkystoneDecor.TILES:
                    return BlockSkystoneDecor.TILES_CRACKED;
                case BlockSkystoneDecor.TILES_CRACKED:
                    return BlockSkystoneDecor.TILES;
                case BlockSkystoneDecor.POLISHED:
                    return BlockSkystoneDecor.CHISELED;
                case BlockSkystoneDecor.CHISELED:
                    return BlockSkystoneDecor.POLISHED;
                default:
                    return -1;
            }
        }
        return -1;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        int next = dressed(block, meta);
        if (next < 0) return false;
        if (world.isRemote) return true;
        world.setBlockMetadataWithNotify(x, y, z, next, 3);
        world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block) + (meta << 12)); // chips and the sound of stone
        stack.damageItem(1, player);
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Use on placed Skystone bricks, tiles or");
        lines.add("§7polished stone to dress them afresh.");
    }
}
