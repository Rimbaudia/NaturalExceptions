package naturalexceptions.minecraft.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Skystone slabs, half a block of brick (meta 0), tile (1) or polished Skystone (2); meta 8 is the top half. The
 * double form is two slabs stacked, and drops them.
 */
public final class BlockSkystoneSlab extends BlockSlab {

    public static final String[] VARIANTS = { "brick", "tiles", "polished" };
    public static final int BRICK = 0, TILES = 1, POLISHED = 2;

    public BlockSkystoneSlab(boolean full) {
        super(full, Material.rock);
        setBlockName(full ? "skystoneDoubleSlab" : "skystoneSlab");
        setHardness(3.0F);
        setResistance(6000.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 1);
        if (!full) useNeighborBrightness = true;
    }

    @Override
    public String func_150002_b(int meta) {
        int kind = meta & 7;
        return super.getUnlocalizedName() + "." + VARIANTS[kind < VARIANTS.length ? kind : 0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        // Borrowed from the full blocks each half is cut from.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        int kind = meta & 7;
        if (kind == TILES && NEBlocks.skystoneDecor != null)
            return NEBlocks.skystoneDecor.getIcon(side, BlockSkystoneDecor.TILES);
        if (kind == POLISHED && NEBlocks.skystoneDecor != null)
            return NEBlocks.skystoneDecor.getIcon(side, BlockSkystoneDecor.POLISHED);
        return NEBlocks.skystoneBricks.getIcon(side, 0);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(NEBlocks.skystoneSlab);
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(NEBlocks.skystoneSlab, field_150004_a ? 2 : 1, meta & 7);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(NEBlocks.skystoneSlab);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        if (field_150004_a) return;
        for (int meta = 0; meta < VARIANTS.length; meta++) list.add(new ItemStack(item, 1, meta));
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public boolean canDropFromExplosion(net.minecraft.world.Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {
        // Skystone shrugs off blasts.
    }
}
