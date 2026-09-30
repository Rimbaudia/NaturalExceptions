package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A tanning rack: a frame of poles to stretch hides on. Hang rotten flesh on it (right-click with it, one at a time,
 * three at most) and each cures into leather in two minutes; right-click with an empty hand to take down what has
 * cured. Meta is the side it faces (2..5).
 */
public final class BlockTanningRack extends BlockParts {

    private static final double[][] PARTS = { { 0.0, 0.0, 0.4, 0.1, 1.3, 0.6 }, { 0.9, 0.0, 0.4, 1.0, 1.3, 0.6 },
        { 0.0, 1.2, 0.42, 1.0, 1.3, 0.58 }, { 0.1, 0.1, 0.44, 0.9, 0.18, 0.56 } };

    public BlockTanningRack() {
        super(Material.wood);
        setBlockName("tanningRack");
        setBlockTextureName("planks_spruce");
        setHardness(1.5F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        int facing = metadata < 2 ? 2 : metadata;
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], facing);
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 2;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(x, y, z, BlockPantryShelf.facingTowards(placer), 2);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityTanningRack();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityTanningRack)) return false;
        if (world.isRemote) return true;
        TileEntityTanningRack rack = (TileEntityTanningRack) tile;
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == Items.rotten_flesh) {
            if (rack.hang(held) && !player.capabilities.isCreativeMode) held.stackSize--;
            if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "mob.slime.small", 0.5F, 0.8F);
            return true;
        }
        if (held != null) return false;
        ItemStack taken = rack.takeDown();
        if (taken != null && !player.inventory.addItemStackToInventory(taken))
            world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, taken));
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.log.getIcon(2, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index >= 2 ? Blocks.planks.getIcon(1, 1) : getIcon(2, 0);
    }
}
