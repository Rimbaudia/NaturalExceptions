package naturalexceptions.minecraft.metallurgy;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ItemBlockStarterSmelter extends ItemBlock {

    public ItemBlockStarterSmelter(Block block) {
        super(block);
    }

    /** True when this controller was broken while it still held fire, charge, melt, or items. */
    public static boolean carriesContents(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(TileEntityStarterSmelter.CONTENTS_TAG, 10);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        // Loaded controllers must not merge: each carries its own vessel.
        return carriesContents(stack) ? 1 : super.getItemStackLimit(stack);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (carriesContents(stack)) {
            NBTTagCompound contents = stack.getTagCompound()
                .getCompoundTag(TileEntityStarterSmelter.CONTENTS_TAG);
            list.add("\u00a76Still holds its contents");
            if (contents.getInteger("FuelRemaining") > 0) {
                list.add("  \u00a77Embers of its fire, smothered but not spent");
            }
            if (contents.getTagList("Layers", 10)
                .tagCount() > 0) {
                list.add("  \u00a77Charge, melt, or slag sealed inside");
            }
            if (contents.getTagList("Items", 10)
                .tagCount() > 0 || contents.hasKey("HearthWorkpiece")) {
                list.add("  \u00a77Fuel, feed, or hearth work inside");
            }
            list.add("  \u00a7ePlace it again to resume where it stopped.");
            return;
        }
        list.add("\u00a76Controller of a 3\u00d73\u00d73 refractory smelter");
        list.add("  \u00a77Floor: 3\u00d73 Refractory Bricks");
        list.add("  \u00a77Middle: this controller in front, 7 bricks, open cavity");
        list.add("  \u00a77Top: ring of 8 bricks around an open chimney");
        list.add("  \u00a7eSee the Knowledge Browser (K) for the full layout.");
    }
}
