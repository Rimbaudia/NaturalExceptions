package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A wooden table (meta 0 oak, 1 spruce, 2 dark oak): a board on a turned post. Set things on it and take them up by
 * hand, four at most; a hungry player with an empty hand eats what food is on it, a proper meal ({@link Meals}).
 */
public final class BlockWoodenTable extends BlockParts {

    /** Planks metadata for each table wood. */
    public static final int[] PLANKS = { 0, 1, 5 };
    public static final String[] WOODS = { "oak", "spruce", "darkOak" };

    private static final double[][] PARTS = { { 0.0, 0.875, 0.0, 1.0, 1.0, 1.0 },
        { 0.375, 0.125, 0.375, 0.625, 0.875, 0.625 }, { 0.1875, 0.0, 0.1875, 0.8125, 0.125, 0.8125 } };

    public BlockWoodenTable() {
        super(Material.wood);
        setBlockName("woodenTable");
        setBlockTextureName("planks_oak");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] copy = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) copy[i] = PARTS[i].clone();
        return copy;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, PLANKS[Math.min(meta & 3, PLANKS.length - 1)]);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index == 1 ? Blocks.log.getIcon(2, metadata == 1 ? 1 : 0) : getIcon(1, metadata);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int damageDropped(int meta) {
        return Math.min(meta & 3, PLANKS.length - 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < PLANKS.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityWoodenTable();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityWoodenTable)) return true;
        TileEntityWoodenTable table = (TileEntityWoodenTable) tile;
        ItemStack held = player.getHeldItem();
        if (held != null) {
            ItemStack one = held.copy();
            one.stackSize = 1;
            if (table.addToFreeSlot(one) && !player.capabilities.isCreativeMode) {
                if (--held.stackSize <= 0)
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            return true;
        }
        for (int i = 0; i < table.getSizeInventory(); i++) {
            ItemStack food = table.getStackInSlot(i);
            if (Meals.wouldEat(player, food)) {
                Meals.eat(world, player, food);
                table.setInventorySlotContents(i, food.stackSize > 0 ? food : null);
                return true;
            }
        }
        int last = table.lastFilled();
        if (last >= 0) {
            ItemStack taken = table.getStackInSlotOnClosing(last);
            table.markDirty();
            if (!player.inventory.addItemStackToInventory(taken))
                world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 1.1, z + 0.5, taken));
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
