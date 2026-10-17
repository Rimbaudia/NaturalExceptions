package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A Counterweight Box: a stout iron-banded crate on a bail, to be filled with stone. Set it in a well dug straight
 * down behind a windlass and the windlass hangs it from its drum, the other way round from the basket. Right-click the
 * hanging box with a block to weight it ({@link TileEntityHoist#COUNTER_CAPACITY} at most), with an empty hand to take
 * one back; hit it, still, to take it off the rope.
 */
public final class BlockCounterweightBox extends BlockParts {

    /** The box, two iron bands round it, and the bail over it. */
    static final double[][] PARTS = { { 0.15, 0.0, 0.15, 0.85, 0.7, 0.85 }, { 0.13, 0.15, 0.13, 0.87, 0.22, 0.87 },
        { 0.13, 0.5, 0.13, 0.87, 0.57, 0.87 }, { 0.46, 0.7, 0.2, 0.54, 0.95, 0.8 } };

    public BlockCounterweightBox() {
        super(Material.wood);
        setBlockName("counterweightBox");
        setBlockTextureName("planks_spruce");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index == 0 ? Blocks.planks.getIcon(1, 1) : Blocks.iron_block.getIcon(1, 0);
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
}
