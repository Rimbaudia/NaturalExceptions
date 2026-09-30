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

/** Skyslate slabs, half a block of brick (meta 0) or shingles (1); meta 8 is the top half. */
public final class BlockSkyslateSlab extends BlockSlab {

    public static final String[] VARIANTS = { "brick", "shingles" };
    public static final int BRICK = 0, SHINGLES = 1;

    public BlockSkyslateSlab(boolean full) {
        super(full, Material.rock);
        setBlockName(full ? "skyslateDoubleSlab" : "skyslateSlab");
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
        return NEBlocks.skyslate.getIcon(side, (meta & 7) == SHINGLES ? BlockSkyslate.SHINGLES : BlockSkyslate.BRICKS);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(NEBlocks.skyslateSlab);
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(NEBlocks.skyslateSlab, field_150004_a ? 2 : 1, meta & 7);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(NEBlocks.skyslateSlab);
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
