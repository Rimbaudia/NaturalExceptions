package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.power.Cable;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A fuse: a thin copper link in a glass tube between two brass clips on a porcelain base. It carries current like a
 * length of cable until more than its rating flows for a moment; then the link melts, the line opens there, and the
 * rest of it is spared. A length of copper wire links it again. Meta: bits 0-1 its rating, bit 2 blown, bit 3 along z.
 */
public final class BlockFuse extends Block implements PowerBlock, PartModel {

    /** What each holder is rated to carry (amperes), and what it is called. */
    public static final double[] RATED_AMPS = { 15.0, 40.0, 100.0 };
    static final String[] NAMES = { "fuseLight", "fuseStandard", "fuseHeavy" };
    public static final int BLOWN = 4, ALONG_Z = 8;
    /** Solves over its rating before the link goes: a moment (0.4 s), so a switch closing doesn't blow it. */
    public static final int BLOW_SOLVES = 2;

    private static final double P = 1.0 / 16.0;
    /** Base, two clips, the tube, the link inside it: along x. */
    private static final double[][] PARTS = { { 3, 0, 5, 13, 1.5, 11 }, { 3.5, 1.5, 6.5, 5.5, 5, 9.5 },
        { 10.5, 1.5, 6.5, 12.5, 5, 9.5 }, { 5.5, 2.5, 7, 10.5, 4.5, 9 }, { 5.6, 3.3, 7.8, 10.4, 3.7, 8.2 } };
    private static final int BASE = 0, TUBE = 3, LINK = 4;

    @SideOnly(Side.CLIENT)
    private IIcon copper;

    public BlockFuse() {
        super(Material.circuits);
        setBlockName("fuse");
        setBlockTextureName("naturalexceptions:cable_copper");
        setHardness(0.5F);
        setStepSound(soundTypeGlass);
    }

    public static boolean blown(int meta) {
        return (meta & BLOWN) != 0;
    }

    public static double ratedAmps(int meta) {
        return RATED_AMPS[Math.min(RATED_AMPS.length - 1, meta & 3)];
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return !blown(world.getBlockMetadata(x, y, z));
    }

    /** The link melts: a pop, a puff, and the line is open. */
    static void blow(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, meta | BLOWN, 3);
        world.playSoundEffect(x + 0.5, y + 0.2, z + 0.5, "naturalexceptions:power.fuse", 0.9F, 1.0F);
        if (world instanceof WorldServer) ((WorldServer) world)
            .func_147487_a("smoke", x + 0.5, y + 0.25, z + 0.5, 6, 0.1, 0.05, 0.1, 0.01);
    }

    /** A blown fuse takes a length of bare copper wire as its new link. */
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        int meta = world.getBlockMetadata(x, y, z);
        ItemStack held = player.getCurrentEquippedItem();
        if (!blown(meta) || held == null
            || held.getItem() != Item.getItemFromBlock(naturalexceptions.minecraft.registry.NEBlocks.cable)
            || held.getItemDamage() != Cable.LIGHT_BARE.ordinal()) return false;
        if (world.isRemote) return true;
        if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        world.setBlockMetadataWithNotify(x, y, z, meta & ~BLOWN, 3);
        world.playSoundEffect(x + 0.5, y + 0.2, z + 0.5, "random.click", 0.5F, 1.4F);
        PowerNetworks.dirty(world, x, y, z);
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int quarter = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, (stack.getItemDamage() & 3) | (quarter % 2 == 0 ? ALONG_Z : 0), 2);
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

    /** It sits on what is under it: with that gone, it comes away. */
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

    /** The holder comes away with its rating; a link is cheap, so a blown one comes away linked again. */
    @Override
    public int damageDropped(int meta) {
        return meta & 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < RATED_AMPS.length; i++) list.add(new ItemStack(item, 1, i));
    }

    // --- Shape ----------------------------------------------------------------------------------------------

    @Override
    public double[][] parts(int meta) {
        boolean z = (meta & ALONG_Z) != 0;
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) {
            double[] b = PARTS[i];
            if (i == LINK && blown(meta)) {
                out[i] = new double[] { 0, 0, 0, 0, 0, 0 };
                continue;
            }
            out[i] = z ? new double[] { b[2] * P, b[1] * P, b[0] * P, b[5] * P, b[4] * P, b[3] * P }
                : new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int meta) {
        if (index == BASE) return Blocks.quartz_block.getIcon(1, 0);
        if (index == TUBE) return blown(meta) ? Blocks.coal_block.getIcon(0, 0) : Blocks.glass.getIcon(0, 0);
        if (index == LINK) return copper;
        return Blocks.gold_block.getIcon(0, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int inventoryMetadata(int itemDamage) {
        return itemDamage & 3;
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
    public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    /** Its item: one per rating. */
    public static final class ItemFuse extends ItemBlock {

        public ItemFuse(Block block) {
            super(block);
            setHasSubtypes(true);
            setMaxDamage(0);
        }

        @Override
        public int getMetadata(int damage) {
            return damage & 3;
        }

        @Override
        public String getUnlocalizedName(ItemStack stack) {
            return "tile." + NAMES[Math.min(NAMES.length - 1, stack.getItemDamage() & 3)];
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            lines.add(String.format("Opens the line past %.0f A.", ratedAmps(stack.getItemDamage())));
            lines.add("Blown: link it again with a length of copper wire.");
        }
    }
}
