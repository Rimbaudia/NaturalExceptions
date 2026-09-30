package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A barrel: bulk storage for one kind of thing ({@link TileEntityBarrel}). Right-click with an item to pour in the
 * stack you hold; with an empty hand to take a stack out (sneaking, just one); with a bucket or bottle to pour or
 * draw water. Each use says what it holds.
 */
public final class BlockBarrel extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockBarrel() {
        super(Material.wood);
        setBlockName("barrel");
        setBlockTextureName("naturalexceptions:barrel_side");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("naturalexceptions:barrel_side");
        top = register.registerIcon("naturalexceptions:barrel_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side < 2 ? top : blockIcon;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBarrel();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityBarrel)) return true;
        TileEntityBarrel barrel = (TileEntityBarrel) tile;
        ItemStack held = player.getHeldItem();
        boolean creative = player.capabilities.isCreativeMode;
        if (held != null && held.getItem() == Items.water_bucket) {
            if (barrel.pourWater()) {
                if (!creative) replaceHeld(player, new ItemStack(Items.bucket));
                world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "liquid.swim", 0.4F, 1.2F);
            }
        } else if (held != null && held.getItem() == Items.bucket && barrel.water() > 0) {
            if (barrel.drawWater()) takeOne(player, held, new ItemStack(Items.water_bucket), creative);
        } else if (held != null && held.getItem() == Items.glass_bottle && barrel.water() > 0) {
            if (barrel.drawBottle()) takeOne(player, held, new ItemStack(Items.potionitem, 1, 0), creative);
        } else if (held != null && barrel.accepts(held)) {
            int moved = barrel.insert(held);
            if (!creative) {
                held.stackSize -= moved;
                if (held.stackSize <= 0) replaceHeld(player, null);
            }
            if (moved > 0) world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.wood", 0.5F, 1.4F);
        } else if (held == null) {
            ItemStack out = barrel.extract(player.isSneaking() ? 1 : 64);
            if (out != null && !player.inventory.addItemStackToInventory(out))
                world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 1.1, z + 0.5, out));
        }
        player.inventoryContainer.detectAndSendChanges();
        player.addChatMessage(new ChatComponentText(describe(barrel)));
        return true;
    }

    /** "Barrel: 312 × Wheat (of 1024)", "Barrel: 2 buckets of water", "Barrel: empty". */
    static String describe(TileEntityBarrel barrel) {
        if (barrel.water() > 0) {
            int buckets = barrel.water() / TileEntityBarrel.BOTTLES_PER_BUCKET,
                bottles = barrel.water() % TileEntityBarrel.BOTTLES_PER_BUCKET;
            return "§7Barrel: " + (buckets > 0 ? buckets + (buckets == 1 ? " bucket" : " buckets") : "")
                + (buckets > 0 && bottles > 0 ? " and " : "")
                + (bottles > 0 ? bottles + (bottles == 1 ? " bottle" : " bottles") : "")
                + " of water";
        }
        if (barrel.kind() == null) return "§7Barrel: empty";
        return "§7Barrel: " + barrel.count()
            + " × "
            + barrel.kind()
                .getDisplayName()
            + " (of "
            + barrel.capacity()
            + ")";
    }

    private static void takeOne(EntityPlayer player, ItemStack held, ItemStack result, boolean creative) {
        if (creative) {
            if (!player.inventory.hasItemStack(result)) player.inventory.addItemStackToInventory(result);
            return;
        }
        if (--held.stackSize <= 0) replaceHeld(player, result);
        else if (!player.inventory.addItemStackToInventory(result))
            player.dropPlayerItemWithRandomChoice(result, false);
    }

    private static void replaceHeld(EntityPlayer player, ItemStack stack) {
        player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityBarrel && !world.isRemote)
            for (ItemStack stack : ((TileEntityBarrel) tile).spill())
                world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityBarrel)) return 0;
        TileEntityBarrel barrel = (TileEntityBarrel) tile;
        if (barrel.water() > 0) return Math
            .max(1, 15 * barrel.water() / (TileEntityBarrel.WATER_BUCKETS * TileEntityBarrel.BOTTLES_PER_BUCKET));
        return barrel.count() == 0 ? 0 : Math.max(1, 15 * barrel.count() / barrel.capacity());
    }
}
