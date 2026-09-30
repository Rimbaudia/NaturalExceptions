package naturalexceptions.minecraft.furniture;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A wooden crate: a chest's worth of room that can be picked up full. Broken by a player, it drops as a crate with
 * everything still inside ({@link ItemBlockCrate} puts it back when placed); broken any other way, it spills.
 */
public final class BlockCrate extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockCrate() {
        super(Material.wood);
        setBlockName("crate");
        setBlockTextureName("naturalexceptions:crate_side");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("naturalexceptions:crate_side");
        top = register.registerIcon("naturalexceptions:crate_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side < 2 ? top : blockIcon;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCrate();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityCrate)
            player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_STORAGE, world, x, y, z);
        return true;
    }

    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!world.isRemote && tile instanceof TileEntityCrate) {
            TileEntityCrate crate = (TileEntityCrate) tile;
            crate.harvested = true;
            if (!player.capabilities.isCreativeMode) {
                EntityItem item = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, ItemBlockCrate.packed(crate));
                item.delayBeforeCanPickup = 10;
                world.spawnEntityInWorld(item);
            }
        }
        super.onBlockHarvested(world, x, y, z, meta, player);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        return new ArrayList<>(); // a player's break drops the packed crate; anything else, see breakBlock
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        // Not broken by a player (an explosion, say): it spills, and the crate itself drops empty.
        if (tile instanceof TileEntityCrate && !((TileEntityCrate) tile).harvested && !world.isRemote) {
            ((TileEntityCrate) tile).dropContents(world, x, y, z);
            world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, new ItemStack(this)));
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public Item getItemDropped(int meta, java.util.Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    /** Copies the crate's contents into an item's tag. */
    static void write(TileEntityCrate crate, ItemStack stack) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Items", crate.writeSlots());
        stack.setTagCompound(tag);
    }
}
