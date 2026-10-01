package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A Lighthouse Lamp: a burning core on an iron pedestal, under a brass cap, inside a Fresnel lens of prism glass that
 * turns (drawn by its renderer), throwing two great beams round the sky from its bull's-eyes, day and night. They
 * reach {@link TileEntityLighthouseLamp#BEAM} blocks and show from as far as the lamp itself is seen: set one high on
 * a rim and it shows the way home. A redstone signal puts it out.
 */
public final class BlockLighthouseLamp extends BlockParts {

    /** Base plate, pedestal, core, cap, finial. The lens turns round the core, between pedestal and cap. */
    private static final double[][] PARTS = { { 0.05, 0.0, 0.05, 0.95, 0.1, 0.95 }, { 0.3, 0.1, 0.3, 0.7, 0.25, 0.7 },
        { 0.38, 0.35, 0.38, 0.62, 0.75, 0.62 }, { 0.12, 0.9, 0.12, 0.88, 1.0, 0.88 },
        { 0.42, 1.0, 0.42, 0.58, 1.15, 0.58 } };
    public static final int CORE = 2;
    /** The lens's half-width about the lamp's middle, and its foot and top. */
    public static final double LENS = 0.34, LENS_FOOT = 0.25, LENS_TOP = 0.9;

    @SideOnly(Side.CLIENT)
    private IIcon brass, core;

    public BlockLighthouseLamp() {
        super(Material.iron);
        setBlockName("lighthouseLamp");
        setBlockTextureName("naturalexceptions:lighthouse_brass");
        setHardness(2.0F);
        setResistance(6.0F);
        setLightLevel(1.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityLighthouseLamp();
    }

    /** Whether the lamp at (x, y, z) is lit: always, unless a redstone signal reaches it. */
    public static boolean lit(World world, int x, int y, int z) {
        return !world.isBlockIndirectlyGettingPowered(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        brass = register.registerIcon("naturalexceptions:lighthouse_brass");
        core = register.registerIcon("naturalexceptions:lighthouse_core");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return brass;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index == CORE) return core;
        if (index == 1) return Blocks.iron_block.getIcon(2, 0);
        return brass;
    }
}
