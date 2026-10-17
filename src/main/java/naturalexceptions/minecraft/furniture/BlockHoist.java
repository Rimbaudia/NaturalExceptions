package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.core.hoisting.HoistDynamics;
import naturalexceptions.minecraft.gui.NEGuiHandler;
import naturalexceptions.minecraft.multiblock.HeadframeDefinition;

/**
 * The two ends of a hoist. The Windlass ({@link #top}): a rope drum between two posts on a footing, a crank at one end
 * and the brake lever at the other, facing (metadata 2..5) the shaft its rope runs to. Right-click and hold the crank
 * to wind up; sneak and hold to let down (easing the brake); right-click the lever to set or release the brake; right-
 * click with Hoist Rope to fit a coil; with anything else, open the store the basket carries. It works only by a whole
 * headframe ({@link HeadframeDefinition}) or derrick, and says what is missing if it has none.
 * <p>
 * The Basket: set it in the shaft, and the windlass hooks it onto its rope.
 */
public final class BlockHoist extends BlockParts {

    /** The windlass as authored, its rope leaving towards -z: a footing, two posts, the lever's quadrant. */
    private static final double[][] WINDLASS = { { 0.0, 0.0, 0.2, 1.0, 0.125, 0.8 },
        { 0.05, 0.125, 0.35, 0.2, 0.85, 0.65 }, { 0.8, 0.125, 0.35, 0.95, 0.85, 0.65 },
        { 0.0, 0.125, 0.42, 0.05, 0.45, 0.58 } };
    /** The basket: its floor, four sides, and the ring its slings meet in. */
    static final double[][] BASKET = { { 0.1, 0.0, 0.1, 0.9, 0.1, 0.9 }, { 0.1, 0.1, 0.1, 0.9, 0.6, 0.18 },
        { 0.1, 0.1, 0.82, 0.9, 0.6, 0.9 }, { 0.1, 0.1, 0.18, 0.18, 0.6, 0.82 }, { 0.82, 0.1, 0.18, 0.9, 0.6, 0.82 },
        { 0.46, 0.6, 0.46, 0.54, 1.0, 0.54 } };

    /** The windlass (true) or the basket. */
    public final boolean top;

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

    /** The way a windlass faces (2..5): its rope runs off the drum that way. */
    public static int facing(int metadata) {
        return metadata < 2 || metadata > 5 ? 2 : metadata;
    }

    @Override
    public double[][] parts(int metadata) {
        if (!top) {
            double[][] out = new double[BASKET.length][];
            for (int i = 0; i < BASKET.length; i++) out[i] = BASKET[i].clone();
            return out;
        }
        double[][] out = new double[WINDLASS.length][];
        for (int i = 0; i < WINDLASS.length; i++)
            out[i] = naturalexceptions.minecraft.block.PartModel.rotate(WINDLASS[i], facing(metadata));
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 2;
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
        if (top)
            return index == 3 ? Blocks.iron_block.getIcon(2, 0) : index == 0 ? Blocks.log.getIcon(2, 1) : getIcon(1, 0);
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

    /** A windlass faces away from whoever places it: its rope runs off towards where they look. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (top) world.setBlockMetadataWithNotify(x, y, z, BlockPantryShelf.facingTowards(placer) ^ 1, 2);
    }

    /** Whether a click at (hx, hz) on a windlass facing {@code facing} is on its brake lever, at the drum's -x end. */
    static boolean onLever(int facing, float hx, float hz) {
        float along = facing == 4 || facing == 5 ? hz : hx;
        return along < 0.25F;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (!top) return false;
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityHoist)) return true;
        TileEntityHoist hoist = (TileEntityHoist) tile;
        ItemStack held = player.getHeldItem();
        if (held != null) {
            if (hoist.fitRope(held)) {
                player.addChatMessage(
                    new ChatComponentText("The windlass has " + hoist.ropeLength() + " blocks of rope on its drum."));
                if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            } else player.openGui(NaturalExceptions.instance, NEGuiHandler.GUI_STORAGE, world, x, y, z);
            return true;
        }
        if (naturalexceptions.minecraft.multiblock.HoistFrames.find(world, x, y, z) == null) {
            player.addChatMessage(
                new ChatComponentText(naturalexceptions.minecraft.multiblock.HoistFrames.missing(world, x, y, z)));
            return true;
        }
        if (onLever(facing(world.getBlockMetadata(x, y, z)), hitX, hitZ)) {
            hoist.toggleBrake();
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.wood_click", 0.6F, hoist.braked() ? 0.8F : 1.2F);
            player.addChatMessage(
                new ChatComponentText(hoist.braked() ? "Brake set." : "Brake off: the heavier side runs down."));
            return true;
        }
        if (!hoist.hooked()) {
            player.addChatMessage(
                new ChatComponentText(
                    hoist.ropeLength() < 3 ? "Fit rope to the windlass (right-click it with Hoist Rope)."
                        : "Set a basket in the shaft within reach of the rope (" + hoist.ropeLength() + " blocks)."));
            return true;
        }
        hoist.work(HoistDynamics.Crank.WINDLASS, player.isSneaking() ? 1 : -1);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityHoist && !world.isRemote) ((TileEntityHoist) tile).taken();
        if (tile instanceof TileEntityStorage && !world.isRemote)
            ((TileEntityStorage) tile).dropContents(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }
}
