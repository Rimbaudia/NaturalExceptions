package naturalexceptions.minecraft.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Stormglass in a skystone frame: the light of the sky dungeons, flameless. As a whole block (meta 0) it is a light
 * set into a ceiling or a wall; set at the foot of a shaft between levels (meta {@link #BEACON}) it sends a column of
 * sparks up the shaft, seen rising out of the hole above. As a lantern (meta {@link #SMALL}) it is half a block of
 * glass between four slim posts under a cap, that stands on a post or a table, or hangs from a ceiling by a short
 * stem (meta {@link #HANGING}: placed against the underside of a block).
 */
public final class BlockStormglassLantern extends Block implements PartModel {

    public static final int BEACON = 1, SMALL = 2, HANGING = 3;

    private static final double P = 1.0 / 16.0;
    private static final double[][] WHOLE = { { 0, 0, 0, 1, 1, 1 } };
    /** Base, four posts, the glass, the cap and its knob (or, hanging, its stem): in pixels, standing. */
    private static final double[][] LANTERN = { { 5, 0, 5, 11, 1, 11 }, { 5, 1, 5, 6, 8, 6 }, { 10, 1, 5, 11, 8, 6 },
        { 5, 1, 10, 6, 8, 11 }, { 10, 1, 10, 11, 8, 11 }, { 6, 1, 6, 10, 8, 10 }, { 4.5, 8, 4.5, 11.5, 9, 11.5 },
        { 7, 9, 7, 9, 10, 9 } };
    private static final int GLASS = 5, KNOB = 7;
    /** How far a hanging lantern is lifted, so its stem meets the block above. */
    private static final double LIFT = 6;

    @SideOnly(Side.CLIENT)
    private IIcon core;

    public BlockStormglassLantern() {
        super(Material.glass);
        setBlockName("stormglassLantern");
        setBlockTextureName("naturalexceptions:stormglass_lantern");
        setHardness(0.8F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }

    public static boolean small(int meta) {
        return meta == SMALL || meta == HANGING;
    }

    @Override
    public double[][] parts(int metadata) {
        if (!small(metadata)) return WHOLE;
        double lift = metadata == HANGING ? LIFT : 0;
        double[][] out = new double[LANTERN.length][];
        for (int i = 0; i < LANTERN.length; i++) {
            double[] b = LANTERN[i];
            double top = b[4] + lift;
            if (i == KNOB && metadata == HANGING) top = 16; // the stem, up into the block above
            out[i] = new double[] { b[0] * P, (b[1] + lift) * P, b[2] * P, b[3] * P, top * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (!small(metadata)) return blockIcon;
        if (index == GLASS) return core;
        if (index == KNOB && metadata == HANGING) return Blocks.iron_block.getIcon(2, 0);
        return NEBlocks.skystoneDecor != null
            ? NEBlocks.skystoneDecor.getIcon(2, naturalexceptions.minecraft.block.BlockSkystoneDecor.POLISHED)
            : Blocks.stone.getIcon(2, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int inventoryMetadata(int itemDamage) {
        return small(itemDamage) ? SMALL : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        core = register.registerIcon("naturalexceptions:stormglass_lantern_core");
    }

    @Override
    public int getRenderType() {
        return PartModel.RenderType.id;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        double[] b = PartModel.bounds(parts(world.getBlockMetadata(x, y, z)));
        setBlockBounds((float) b[0], (float) b[1], (float) b[2], (float) b[3], (float) b[4], (float) b[5]);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {
        for (double[] b : parts(world.getBlockMetadata(x, y, z))) {
            if (PartModel.empty(b)) continue;
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(x + b[0], y + b[1], z + b[2], x + b[3], y + b[4], z + b[5]);
            if (mask.intersectsWith(box)) boxes.add(box);
        }
    }

    /** A lantern placed against the underside of a block hangs from it; otherwise it stands. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        return meta == SMALL && side == 0 ? HANGING : meta;
    }

    @Override
    public int damageDropped(int meta) {
        return small(meta) ? SMALL : 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, SMALL));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) != BEACON) return;
        for (int i = 0; i < 3; i++) world.spawnParticle(
            "fireworksSpark",
            x + 0.3 + random.nextDouble() * 0.4,
            y + 1.1,
            z + 0.3 + random.nextDouble() * 0.4,
            (random.nextDouble() - 0.5) * 0.02,
            0.45 + random.nextDouble() * 0.25,
            (random.nextDouble() - 0.5) * 0.02);
        world.spawnParticle("witchMagic", x + 0.5, y + 1.2 + random.nextDouble() * 3.0, z + 0.5, 0.0, 0.3, 0.0);
    }
}
