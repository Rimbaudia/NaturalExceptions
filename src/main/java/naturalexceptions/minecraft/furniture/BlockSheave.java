package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A sheave: a grooved pulley wheel between two cheeks, set in the beam of a headframe (or at a derrick's boom tip)
 * for a hoist's rope to run over. Its block is the cheeks, the axle and the bar that joins them; the wheel turns with
 * the rope and is drawn by the windlass that works it. Metadata: 0 if its wheel stands across x (axle along z), 1 if
 * across z (axle along x).
 */
public final class BlockSheave extends BlockParts {

    private static final double P = 1.0 / 16.0;
    /**
     * Authored with the axle along z: two open cheeks (each two upright straps, a bar across their feet and a boss
     * round the axle, so the wheel shows through), the bar over the wheel, and last the axle.
     */
    private static final double[][] PARTS = { { 1, 2, 0, 3, 16, 2 }, { 13, 2, 0, 15, 16, 2 }, { 3, 2, 0, 13, 4, 2 },
        { 6, 6, 0, 10, 10, 2 }, { 1, 2, 14, 3, 16, 16 }, { 13, 2, 14, 15, 16, 16 }, { 3, 2, 14, 13, 4, 16 },
        { 6, 6, 14, 10, 10, 16 }, { 0, 14, 2, 16, 16, 14 }, { 7, 7, 2, 9, 9, 14 } };

    public BlockSheave() {
        super(Material.wood);
        setBlockName("sheave");
        setBlockTextureName("planks_spruce");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    /** Whether a sheave's axle runs along x (its wheel turns across z). */
    public static boolean axleAlongX(int metadata) {
        return (metadata & 1) == 1;
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) {
            double[] b = PARTS[i];
            out[i] = axleAlongX(metadata) ? new double[] { b[2] * P, b[1] * P, b[0] * P, b[5] * P, b[4] * P, b[3] * P }
                : new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index == PARTS.length - 1 ? Blocks.iron_block.getIcon(2, 0) : Blocks.planks.getIcon(1, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    /** Its wheel turns the way the player faces: the rope runs off it towards them. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
        net.minecraft.item.ItemStack stack) {
        int facing = BlockPantryShelf.facingTowards(placer);
        world.setBlockMetadataWithNotify(x, y, z, facing == 2 || facing == 3 ? 1 : 0, 2);
    }
}
