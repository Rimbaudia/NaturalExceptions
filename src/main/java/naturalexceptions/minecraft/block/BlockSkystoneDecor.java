package naturalexceptions.minecraft.block;

import java.util.List;

import net.minecraft.block.Block;
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

/**
 * The dressed Skystone of the sky dungeons, as blast-proof as the bricks: floor tiles (meta 0, cracked meta 1),
 * polished Skystone for ceilings and base courses (meta 2), and chiseled Skystone carved with a sky glyph (meta 3).
 */
public final class BlockSkystoneDecor extends Block {

    public static final String[] VARIANTS = { "skystone_tiles", "skystone_tiles_cracked", "polished_skystone",
        "chiseled_skystone" };
    public static final int TILES = 0, TILES_CRACKED = 1, POLISHED = 2, CHISELED = 3;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockSkystoneDecor() {
        super(Material.rock);
        setBlockName("skystoneDecor");
        setBlockTextureName("naturalexceptions:skystone_tiles");
        setHardness(3.0F);
        setResistance(6000.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[VARIANTS.length];
        for (int i = 0; i < VARIANTS.length; i++) icons[i] = register.registerIcon("naturalexceptions:" + VARIANTS[i]);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[Math.max(0, Math.min(icons.length - 1, meta))];
    }

    @Override
    public int damageDropped(int meta) {
        return Math.max(0, Math.min(VARIANTS.length - 1, meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
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
