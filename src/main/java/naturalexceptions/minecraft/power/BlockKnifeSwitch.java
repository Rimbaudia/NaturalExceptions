package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A knife switch on a slate base: a copper blade on a hinge, closed down into its jaws or thrown open. Closed, it
 * carries current like a length of cable; open, the circuit is broken there (and a spark jumps if it was carrying).
 * Meta: bit 0 closed, bit 1 its blade along z.
 */
public final class BlockKnifeSwitch extends Block implements PowerBlock, PartModel {

    public static final int CLOSED = 1, ALONG_Z = 2;
    private static final double P = 1.0 / 16.0;
    /** Base, two jaws, hinge, blade, handle: along x, closed. */
    private static final double[][] CLOSED_PARTS = { { 2, 0, 5, 14, 1, 11 }, { 11, 1, 7, 13, 4, 9 },
        { 3, 1, 7, 5, 4, 9 }, { 3.5, 3, 7.5, 13, 4, 8.5 }, { 12.5, 2.5, 7, 15, 4.5, 9 } };
    private static final double[][] OPEN_PARTS = { { 2, 0, 5, 14, 1, 11 }, { 11, 1, 7, 13, 4, 9 }, { 3, 1, 7, 5, 4, 9 },
        { 3.5, 3, 7.5, 4.5, 13, 8.5 }, { 3, 12.5, 7, 5, 15, 9 } };
    private static final int BASE = 0, BLADE = 3, HANDLE = 4;

    @SideOnly(Side.CLIENT)
    private IIcon copper;

    public BlockKnifeSwitch() {
        super(Material.circuits);
        setBlockName("knifeSwitch");
        setBlockTextureName("naturalexceptions:cable_copper");
        setHardness(0.5F);
        setStepSound(soundTypeStone);
    }

    public static boolean closed(int meta) {
        return (meta & CLOSED) != 0;
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return closed(world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        if (world.isRemote) return true;
        int meta = world.getBlockMetadata(x, y, z);
        boolean opening = closed(meta);
        double volts = Math.abs(
            PowerNetworks.of(world)
                .voltage(x, y, z));
        world.setBlockMetadataWithNotify(x, y, z, meta ^ CLOSED, 3);
        world.playSoundEffect(x + 0.5, y + 0.3, z + 0.5, "random.click", 0.6F, opening ? 0.6F : 0.8F);
        if (opening && volts > 1.0 && world instanceof WorldServer) {
            ((WorldServer) world).func_147487_a(
                "fireworksSpark",
                x + 0.5,
                y + 0.3,
                z + 0.5,
                (int) Math.min(20, 3 + volts / 4),
                0.1,
                0.1,
                0.1,
                0.05);
            world.playSoundEffect(x + 0.5, y + 0.3, z + 0.5, "random.fizz", 0.4F, 2.0F);
        }
        PowerNetworks.dirty(world, x, y, z);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int quarter = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, quarter % 2 == 0 ? ALONG_Z : 0, 2);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        PowerNetworks.dirty(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        PowerNetworks.dirty(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    /** It is screwed down to what is under it: with that gone, it comes away. */
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote || World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)) return;
        dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
        world.setBlockToAir(x, y, z);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    // --- Shape ----------------------------------------------------------------------------------------------

    @Override
    public double[][] parts(int meta) {
        double[][] authored = closed(meta) ? CLOSED_PARTS : OPEN_PARTS;
        double[][] out = new double[authored.length][];
        boolean z = (meta & ALONG_Z) != 0;
        for (int i = 0; i < authored.length; i++) {
            double[] b = authored[i];
            out[i] = z ? new double[] { b[2] * P, b[1] * P, b[0] * P, b[5] * P, b[4] * P, b[3] * P }
                : new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int meta) {
        if (index == BASE) return Blocks.stone_slab.getIcon(1, 0);
        if (index == HANDLE) return Blocks.hardened_clay.getIcon(0, 0);
        return copper;
    }

    @Override
    public int inventoryMetadata() {
        return CLOSED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        copper = blockIcon;
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
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {}
}
