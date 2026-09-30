package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** A crate as an item: when it holds things, it says so, and puts them back when set down. */
public final class ItemBlockCrate extends ItemBlock {

    public ItemBlockCrate(Block block) {
        super(block);
    }

    /** A crate item holding what {@code crate} holds (or a plain crate if it is empty). */
    public static ItemStack packed(TileEntityCrate crate) {
        ItemStack stack = new ItemStack(naturalexceptions.minecraft.registry.NEBlocks.crate);
        if (crate.lastFilled() >= 0) BlockCrate.write(crate, stack);
        return stack;
    }

    /** The items packed in a crate item's tag, or an empty list. */
    public static NBTTagList contents(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getTagList("Items", 10) : new NBTTagList();
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return stack.hasTagCompound() ? 1 : super.getItemStackLimit(stack); // full crates do not stack
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityCrate && stack.hasTagCompound())
            ((TileEntityCrate) tile).readSlots(contents(stack));
        return true;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        NBTTagList items = contents(stack);
        if (items.tagCount() == 0) {
            lines.add("§7Empty. Keeps what it holds when picked up.");
            return;
        }
        int shown = 0;
        for (int i = 0; i < items.tagCount() && shown < 5; i++, shown++) {
            ItemStack item = ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i));
            if (item != null) lines.add("§7" + item.stackSize + " × " + item.getDisplayName());
        }
        if (items.tagCount() > shown) lines.add("§8and " + (items.tagCount() - shown) + " more stacks");
    }
}
