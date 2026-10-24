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
 * fuel. The flames flicker. One can stand unlit (meta {@link #UNLIT}), as round the hall of the Gathered: light it
 * with flint and steel, a torch or a fire charge.
 */
public final class BlockCandelabra extends BlockParts {

    /** Meta bit: its candles are out. */
    public static final int UNLIT = 2;

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

    public static boolean lit(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return (world.getBlockMetadata(x, y, z) & UNLIT) == 0;
    }

    @Override
    public int getLightValue(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return world.getBlock(x, y, z) == this && !lit(world, x, y, z) ? 0 : super.getLightValue(world, x, y, z);
    }

    /** Lit with flint and steel, a torch or a fire charge. */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, net.minecraft.entity.player.EntityPlayer player,
        int side, float hitX, float hitY, float hitZ) {
        net.minecraft.item.ItemStack held = player.getCurrentEquippedItem();
        if (lit(world, x, y, z) || held == null) return false;
        net.minecraft.item.Item item = held.getItem();
        if (item != net.minecraft.init.Items.flint_and_steel && item != net.minecraft.init.Items.fire_charge
            && item != net.minecraft.item.Item.getItemFromBlock(Blocks.torch)) return false;
        if (world.isRemote) return true;
        light(world, x, y, z, true);
        if (item == net.minecraft.init.Items.flint_and_steel) held.damageItem(1, player);
        else if (item == net.minecraft.init.Items.fire_charge && !player.capabilities.isCreativeMode) held.stackSize--;
        return true;
    }

    /** Lights its candles or puts them out (with a hiss and a breath of smoke). */
    public static void light(World world, int x, int y, int z, boolean on) {
        int meta = world.getBlockMetadata(x, y, z);
        if (on == ((meta & UNLIT) == 0)) return;
        world.setBlockMetadataWithNotify(x, y, z, on ? meta & ~UNLIT : meta | UNLIT, 3);
        world.func_147451_t(x, y, z); // the light changes with it
        world.playSoundEffect(x + 0.5, y + 0.8, z + 0.5, on ? "fire.ignite" : "random.fizz", 0.8F, on ? 1.0F : 1.4F);
        if (!on && world instanceof net.minecraft.world.WorldServer) ((net.minecraft.world.WorldServer) world)
            .func_147487_a("smoke", x + 0.5, y + 0.9, z + 0.5, 8, 0.2, 0.05, 0.2, 0.01);
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
        if (!lit(world, x, y, z)) return;
        for (int i = 0; i < CANDLES.length; i++) {
            double top = y + TOP + (i == 1 ? 0.06 : 0.02), along = CANDLES[i];
            double px = turned ? x + 0.5 : x + along, pz = turned ? z + along : z + 0.5;
            world.spawnParticle("flame", px, top, pz, 0.0, 0.0, 0.0);
            if (random.nextInt(4) == 0) world.spawnParticle("smoke", px, top + 0.05, pz, 0.0, 0.0, 0.0);
        }
    }
}
