package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A market counter: a stall's front and board, holding nine stacks of goods, the first three set out on top. Its
 * front shows what the stall sells (meta 0 produce, 1 fish, 2 pottery, 3 cloth).
 */
public final class BlockMarketCounter extends BlockContainer {

    public static final String[] GOODS = { "produce", "fish", "pottery", "cloth" };

    @SideOnly(Side.CLIENT)
    private IIcon[] fronts;
    @SideOnly(Side.CLIENT)
    private IIcon top;

    public BlockMarketCounter() {
        super(Material.wood);
        setBlockName("marketCounter");
        setBlockTextureName("naturalexceptions:market_counter_top");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        fronts = new IIcon[GOODS.length];
        String[] names = { "naturalexceptions:market_counter_produce", "naturalexceptions:market_counter_fish",
            "naturalexceptions:market_counter_pottery", "naturalexceptions:market_counter_cloth" };
        for (int i = 0; i < GOODS.length; i++) fronts[i] = register.registerIcon(names[i]);
        top = register.registerIcon("naturalexceptions:market_counter_top");
        blockIcon = top;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return side < 2 ? top : fronts[meta & 3];
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < GOODS.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityMarketCounter();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(x, y, z) instanceof TileEntityMarketCounter)
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
