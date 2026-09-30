package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A well's winch: a spruce drum right across between the well's two posts, its crank at one end, the rope wound on
 * it and the bucket hanging below. Use it as you would a fountain ({@link BlockFountain#serve}): it fills a bottle,
 * a bucket or a water jug, or gives you a drink. Meta: 0 its drum along x, 1 along z.
 */
public final class BlockWellWinch extends BlockParts {

    /** Drum, crank arm, handle, rope, bucket, the bucket's bail. Authored along x. */
    private static final double[][] PARTS = { { 0.0, 0.6, 0.35, 1.0, 0.9, 0.65 }, { 0.9, 0.65, 0.7, 0.98, 0.75, 0.9 },
        { 0.9, 0.35, 0.85, 0.98, 0.75, 0.93 }, { 0.48, 0.3, 0.48, 0.52, 0.6, 0.52 }, { 0.36, 0.0, 0.36, 0.64, 0.25, 0.64 },
        { 0.36, 0.25, 0.48, 0.64, 0.3, 0.52 } };

    public BlockWellWinch() {
        super(Material.wood);
        setBlockName("wellWinch");
        setBlockTextureName("planks_spruce");
        setHardness(1.5F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) {
            double[] b = PARTS[i];
            out[i] = (metadata & 1) == 0 ? b.clone() : new double[] { b[2], b[1], b[0], b[5], b[4], b[3] };
        }
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = BlockPantryShelf.facingTowards(placer);
        world.setBlockMetadataWithNotify(x, y, z, facing == 2 || facing == 3 ? 0 : 1, 2); // across your view
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.wood_click", 0.4F, 0.6F);
        return BlockFountain.serve(world, x, y, z, player);
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
        switch (index) {
            case 0:
                return getIcon(2, 0);
            case 1:
            case 2:
                return Blocks.iron_block.getIcon(2, 0);
            case 3:
                return Blocks.wool.getIcon(2, 12); // hempen rope
            default:
                return Blocks.planks.getIcon(1, 1);
        }
    }
}
