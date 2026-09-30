package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Three candles on a brass stand: a steady light (14) that stands on a table, a counter or the floor, and needs no
 * fuel. The flames flicker.
 */
public final class BlockCandelabra extends BlockParts {

    /** The candles' middles along x, and how tall they burn. */
    static final double[] CANDLES = { 0.22, 0.5, 0.78 };
    static final double TOP = 0.78;

    /** Foot, stem, arm, three cups, three candles. */
    private static final double[][] PARTS = { { 0.34, 0.0, 0.34, 0.66, 0.0625, 0.66 },
        { 0.4625, 0.0625, 0.4625, 0.5375, 0.5, 0.5375 }, { 0.16, 0.5, 0.4625, 0.84, 0.5625, 0.5375 },
        { 0.17, 0.5625, 0.45, 0.27, 0.5938, 0.55 }, { 0.45, 0.5625, 0.45, 0.55, 0.5938, 0.55 },
        { 0.73, 0.5625, 0.45, 0.83, 0.5938, 0.55 }, { 0.19, 0.5938, 0.47, 0.25, TOP, 0.53 },
        { 0.47, 0.5938, 0.47, 0.53, TOP + 0.04, 0.53 }, { 0.75, 0.5938, 0.47, 0.81, TOP, 0.53 } };

    public BlockCandelabra() {
        super(Material.iron);
        setBlockName("candelabra");
        setBlockTextureName("gold_block");
        setHardness(0.8F);
        setLightLevel(0.9F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        boolean turned = (metadata & 1) != 0;
        for (int i = 0; i < PARTS.length; i++) {
            double[] b = PARTS[i];
            // Turned a quarter: the arm runs along z.
            out[i] = turned ? new double[] { b[2], b[1], b[0], b[5], b[4], b[3] } : b.clone();
        }
        return out;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
        net.minecraft.item.ItemStack stack) {
        int facing = BlockPantryShelf.facingTowards(placer);
        world.setBlockMetadataWithNotify(x, y, z, facing == 4 || facing == 5 ? 1 : 0, 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.gold_block.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index >= 6 ? Blocks.wool.getIcon(1, 0) : Blocks.gold_block.getIcon(1, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        boolean turned = (world.getBlockMetadata(x, y, z) & 1) != 0;
        for (int i = 0; i < CANDLES.length; i++) {
            double top = y + TOP + (i == 1 ? 0.06 : 0.02), along = CANDLES[i];
            double px = turned ? x + 0.5 : x + along, pz = turned ? z + along : z + 0.5;
            world.spawnParticle("flame", px, top, pz, 0.0, 0.0, 0.0);
            if (random.nextInt(4) == 0) world.spawnParticle("smoke", px, top + 0.05, pz, 0.0, 0.0, 0.0);
        }
    }
}
