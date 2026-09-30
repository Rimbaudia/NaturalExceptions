package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
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
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A pantry shelf, facing out from the wall it stands against (meta: vanilla side 2..5). It holds food only. With an
 * empty hand, a hungry player eats from it, a proper meal ({@link Meals}); otherwise it opens.
 */
public final class BlockPantryShelf extends BlockContainer {

    @SideOnly(Side.CLIENT)
    private IIcon front;

    public BlockPantryShelf() {
        super(Material.wood);
        setBlockName("pantryShelf");
        setBlockTextureName("naturalexceptions:pantry_shelf_front");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        front = register.registerIcon("naturalexceptions:pantry_shelf_front");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        int facing = meta < 2 ? 3 : meta;
        return side == facing ? front : Blocks.planks.getIcon(side, 1);
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
    public TileEntity createNewTileEntity(World world, int meta) {
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

    @Override
    public boolean isOpaqueCube() {
        return true;
    }
}
