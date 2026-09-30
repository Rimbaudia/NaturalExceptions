package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * Wooden furniture hung on a wall and holding a few things in view: meta is the side its wall is on (2 north,
 * 3 south, 4 west, 5 east). Right-click with something to set it there; with an empty hand to take the last back.
 */
abstract class BlockWallFurniture extends BlockParts {

    BlockWallFurniture(String name) {
        super(Material.wood);
        setBlockName(name);
        setBlockTextureName("planks_spruce");
        setHardness(1.5F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    /** The boxes with the wall to the north (-z). */
    abstract double[][] authored();

    /** How much of a held stack one use sets down: one, or all of it. */
    abstract int setsDown(ItemStack held);

    @Override
    public double[][] parts(int metadata) {
        double[][] boxes = authored();
        double[][] out = new double[boxes.length][];
        int wall = metadata < 2 || metadata > 5 ? 2 : metadata;
        for (int i = 0; i < boxes.length; i++) out[i] = PartModel.rotate(boxes[i], wall);
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return getIcon(1, 1);
    }

    @Override
    public int inventoryMetadata() {
        return 2;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    /** Hung on the face that was clicked; set on a floor or ceiling, it goes on the wall away from the placer. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        return side >= 2 ? side ^ 1 : 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (world.getBlockMetadata(x, y, z) >= 2) return;
        int towards = BlockPantryShelf.facingTowards(placer); // the side towards the placer; the wall is opposite
        world.setBlockMetadataWithNotify(x, y, z, towards ^ 1, 2);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityStorage)) return true;
        TileEntityStorage storage = (TileEntityStorage) tile;
        ItemStack held = player.getHeldItem();
        if (held != null) {
            int count = Math.min(setsDown(held), held.stackSize);
            ItemStack part = held.copy();
            part.stackSize = count;
            if (storage.addToFreeSlot(part) && !player.capabilities.isCreativeMode) {
                held.stackSize -= count;
                if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            return true;
        }
        int last = storage.lastFilled();
        if (last >= 0) {
            ItemStack taken = storage.getStackInSlotOnClosing(last);
            storage.markDirty();
            if (!player.inventory.addItemStackToInventory(taken))
                world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, taken));
        }
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
