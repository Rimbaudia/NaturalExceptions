package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A quarry hoist: the Hoist Windlass at the top (a rope drum on two posts, with its crank) and the Hoist Basket at
 * the foot, anywhere within {@link #REACH} across and {@link #DEPTH} down. Right-click either with an empty hand to
 * ride the hook to the other; with something in hand, or sneaking, open the hoist's store, which both ends share.
 */
public final class BlockHoist extends BlockParts {

    /** How far across and down a windlass reaches its basket. */
    public static final int REACH = 12, DEPTH = 40;

    private static final double[][] WINDLASS = { { 0.05, 0.0, 0.3, 0.2, 0.9, 0.7 }, { 0.8, 0.0, 0.3, 0.95, 0.9, 0.7 },
        { 0.2, 0.45, 0.32, 0.8, 0.77, 0.68 }, { 0.95, 0.56, 0.46, 1.0, 0.66, 0.54 },
        { 0.95, 0.3, 0.46, 1.0, 0.66, 0.54 } };
    static final double[][] BASKET = { { 0.1, 0.0, 0.1, 0.9, 0.1, 0.9 }, { 0.1, 0.1, 0.1, 0.9, 0.6, 0.18 },
        { 0.1, 0.1, 0.82, 0.9, 0.6, 0.9 }, { 0.1, 0.1, 0.18, 0.18, 0.6, 0.82 }, { 0.82, 0.1, 0.18, 0.9, 0.6, 0.82 },
        { 0.46, 0.6, 0.46, 0.54, 1.0, 0.54 } };

    /** The windlass (top) or the basket (foot). */
    final boolean top;

    public BlockHoist(boolean top) {
        super(Material.wood);
        this.top = top;
        setBlockName(top ? "hoistWindlass" : "hoistBasket");
        setBlockTextureName("planks_spruce");
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] src = top ? WINDLASS : BASKET;
        boolean away = !top && (metadata & 1) != 0; // the basket is up on its rope, with a rider
        double[][] out = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i].clone();
            if (away) out[i][4] = out[i][1];
        }
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (top) {
            if (index == 2) return Blocks.log.getIcon(2, 1);
            if (index >= 3) return Blocks.iron_block.getIcon(2, 0);
            return getIcon(1, 0);
        }
        if (index == 5) return Blocks.iron_block.getIcon(2, 0);
        return getIcon(1, 0);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return top;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return top ? new TileEntityHoist() : null;
    }

    /** The other end of the hoist at (x, y, z): {x, y, z}, or null if there is none in reach. */
    public int[] partner(World world, int x, int y, int z) {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        int dyFrom = top ? -DEPTH : 3, dyTo = top ? -3 : DEPTH;
        for (int dy = dyFrom; dy <= dyTo; dy++)
            for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++) {
                Block b = world.getBlock(x + dx, y + dy, z + dz);
                if (!(b instanceof BlockHoist) || ((BlockHoist) b).top == top) continue;
                int distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new int[] { x + dx, y + dy, z + dz };
                }
            }
        return best;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        int[] other = partner(world, x, y, z);
        int[] windlass = top ? new int[] { x, y, z } : other;
        int[] basket = top ? other : new int[] { x, y, z };
        if (player.getHeldItem() != null || player.isSneaking()) {
            if (windlass == null) return true;
            TileEntity tile = world.getTileEntity(windlass[0], windlass[1], windlass[2]);
            if (tile instanceof TileEntityHoist && basket != null)
                ((TileEntityHoist) tile).link(basket[0], basket[1], basket[2]);
            player.openGui(
                NaturalExceptions.instance,
                NEGuiHandler.GUI_STORAGE,
                world,
                windlass[0],
                windlass[1],
                windlass[2]);
            return true;
        }
        if (other == null || player.isRiding()) return true;
        @SuppressWarnings("unchecked")
        List<EntityHoistHook> busy = world.getEntitiesWithinAABB(
            EntityHoistHook.class,
            AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 3, z + 2));
        if (!busy.isEmpty()) return true;
        EntityHoistHook hook = EntityHoistHook.between(world, windlass, basket, top, ropeTop(world, basket, windlass));
        world.spawnEntityInWorld(hook);
        player.mountEntity(hook);
        return true;
    }

    /** The height the basket's rope hangs from: the underside of what is over the basket (the derrick's boom). */
    static int ropeTop(World world, int[] basket, int[] windlass) {
        for (int y = basket[1] + 2; y <= basket[1] + 64 && y < 256; y++) if (world.getBlock(basket[0], y, basket[2])
            .getMaterial()
            .isSolid()) return y;
        return windlass[1] + 10;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
