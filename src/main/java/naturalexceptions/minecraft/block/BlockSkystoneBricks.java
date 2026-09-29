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
 * The pale carved stone of the sky dungeons, whole, cracked (meta 1) or mossy (meta 2). It does not break under a
 * Fallen Star's impact or any explosion, so it is what you build with where stars fall. Meta 3 is a loose patch
 * walling off a dungeon's secret room: it looks nearly cracked, gives way to a few pickaxe (or fist) blows, and
 * drops as cracked bricks.
 */
public final class BlockSkystoneBricks extends Block {

    public static final String[] VARIANTS = { "skystone_bricks", "skystone_bricks_cracked", "skystone_bricks_mossy" };
    public static final int LOOSE = 3;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockSkystoneBricks() {
        super(Material.rock);
        setBlockName("skystoneBricks");
        setBlockTextureName("naturalexceptions:skystone_bricks");
        setHardness(3.0F);
        setResistance(6000.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[VARIANTS.length + 1];
        icons[0] = register.registerIcon("naturalexceptions:skystone_bricks");
        icons[1] = register.registerIcon("naturalexceptions:skystone_bricks_cracked");
        icons[2] = register.registerIcon("naturalexceptions:skystone_bricks_mossy");
        icons[LOOSE] = register.registerIcon("naturalexceptions:skystone_bricks_loose");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[Math.max(0, Math.min(icons.length - 1, meta))];
    }

    @Override
    public int damageDropped(int meta) {
        return meta == LOOSE ? 1 : Math.max(0, Math.min(VARIANTS.length - 1, meta));
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) == LOOSE ? 0.6F : super.getBlockHardness(world, x, y, z);
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
