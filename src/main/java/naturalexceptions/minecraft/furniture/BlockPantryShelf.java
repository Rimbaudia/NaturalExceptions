package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.block.PartModel;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A pantry shelf: a spruce cabinet open at the front, three tiers of food inside (meta: the side its front faces,
 * vanilla 2..5; its back is to the wall opposite). It holds food only. With an empty hand, a hungry player eats from
 * it, a proper meal ({@link Meals}); otherwise it opens.
 */
public final class BlockPantryShelf extends BlockParts {

    /** Heights of the three tiers' floors, where the goods stand. */
    public static final double[] TIERS = { 0.0625, 0.375, 0.6875 };

    /** Authored with the wall to the north (-z): back, sides, bottom, two shelves, top. */
    private static final double[][] PARTS = { { 0.0, 0.0, 0.0, 1.0, 1.0, 0.0625 }, { 0.0, 0.0, 0.0, 0.0625, 1.0, 1.0 },
        { 0.9375, 0.0, 0.0, 1.0, 1.0, 1.0 }, { 0.0, 0.0, 0.0, 1.0, 0.0625, 1.0 }, { 0.0, 0.3125, 0.0, 1.0, 0.375, 1.0 },
        { 0.0, 0.625, 0.0, 1.0, 0.6875, 1.0 }, { 0.0, 0.9375, 0.0, 1.0, 1.0, 1.0 } };

    public BlockPantryShelf() {
        super(Material.wood);
        setBlockName("pantryShelf");
        setBlockTextureName("planks_spruce");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        int front = metadata < 2 || metadata > 5 ? 3 : metadata;
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], front ^ 1);
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
        return 3;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(x, y, z, facingTowards(placer), 2);
    }

    /** The vanilla side facing the placer. */
    static int facingTowards(EntityLivingBase placer) {
        int quarter = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        return new int[] { 2, 5, 3, 4 }[quarter];
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityPantryShelf();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityPantryShelf)) return true;
        TileEntityPantryShelf shelf = (TileEntityPantryShelf) tile;
        int food = shelf.firstFood();
        if (player.getHeldItem() == null && !player.isSneaking()
            && food >= 0
            && Meals.wouldEat(player, shelf.getStackInSlot(food))) {
            ItemStack stack = shelf.getStackInSlot(food);
            Meals.eat(world, player, stack);
            shelf.setInventorySlotContents(food, stack.stackSize > 0 ? stack : null);
            return true;
        }
        player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_STORAGE, world, x, y, z);
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
