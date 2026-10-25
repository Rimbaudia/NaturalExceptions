package naturalexceptions.minecraft.power;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.power.Cable;

/**
 * Copper cable, a metre to a block: light wire, standard cable or heavy cable, each bare or wrapped in cloth (meta: a
 * {@link Cable}). It joins whatever carries current on its six faces, and is walked through, not on. Bare copper
 * carrying more than {@link Cable#SHOCK_VOLTS} bites whoever touches it.
 */
public final class BlockCable extends Block implements PowerBlock {

    public static final DamageSource SHOCK = new DamageSource("naturalexceptions.shock").setDamageBypassesArmor();
    public static int renderId = -1;

    @SideOnly(Side.CLIENT)
    private IIcon copper, cloth;

    public BlockCable() {
        super(Material.circuits);
        setBlockName("cable");
        setBlockTextureName("naturalexceptions:cable_copper");
        setHardness(0.3F);
        setStepSound(soundTypeCloth);
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public double ohms(IBlockAccess world, int x, int y, int z) {
        return Cable.of(world.getBlockMetadata(x, y, z))
            .ohmsPerMetre();
    }

    /** Whether it joins what is on (x, y, z): anything that carries current, open switch or not. */
    public static boolean joins(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block instanceof PowerBlock || world.getTileEntity(x, y, z) instanceof PowerDevice;
    }

    /** Its thickness, by gauge (pixels). */
    public static double thickness(int meta) {
        switch (Cable.of(meta).gauge) {
            case HEAVY:
                return 5.0 / 16.0;
            case STANDARD:
                return 3.0 / 16.0;
            default:
                return 2.0 / 16.0;
        }
    }

    // --- The world ------------------------------------------------------------------------------------------

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        PowerNetworks.dirty(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        PowerNetworks.dirty(world, x, y, z);
        PowerNetworks.cooled(world, x, y, z); // a new length laid here starts cold
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        PowerNetworks.dirty(world, x, y, z);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    /** What bare copper at {@code volts} does to whoever touches it: nothing at 24 V, four hearts at 130 V. */
    public static float shockDamage(double volts) {
        if (Math.abs(volts) <= Cable.SHOCK_VOLTS) return 0.0F;
        return (float) Math.min(8.0, 1.0 + (Math.abs(volts) - Cable.SHOCK_VOLTS) / 14.0);
    }

    /** Cut while it carries high tension, it arcs at whoever cut it. */
    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int meta, net.minecraft.entity.player.EntityPlayer player) {
        super.onBlockHarvested(world, x, y, z, meta, player);
        if (world.isRemote || player.capabilities.isCreativeMode) return;
        PowerNetworks networks = PowerNetworks.of(world);
        Arc.at(world, x, y, z, networks.voltage(x, y, z), networks.current(x, y, z), player);
    }

    /** Bare copper under voltage bites. */
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || !(entity instanceof EntityLivingBase)
            || Cable.of(world.getBlockMetadata(x, y, z)).insulated) return;
        double volts = Math.abs(
            PowerNetworks.of(world)
                .voltage(x, y, z));
        if (volts <= Cable.SHOCK_VOLTS) return;
        if (entity.attackEntityFrom(SHOCK, shockDamage(volts)))
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.fizz", 0.4F, 2.0F);
    }

    // --- Shape ----------------------------------------------------------------------------------------------

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        double t = thickness(world.getBlockMetadata(x, y, z)), lo = 0.5 - t / 2, hi = 0.5 + t / 2;
        setBlockBounds(
            (float) (joins(world, x - 1, y, z) ? 0 : lo),
            (float) (joins(world, x, y - 1, z) ? 0 : lo),
            (float) (joins(world, x, y, z - 1) ? 0 : lo),
            (float) (joins(world, x + 1, y, z) ? 1 : hi),
            (float) (joins(world, x, y + 1, z) ? 1 : hi),
            (float) (joins(world, x, y, z + 1) ? 1 : hi));
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.4F, 0.4F, 1.0F, 0.6F, 0.6F);
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
    public int getRenderType() {
        return renderId;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        copper = register.registerIcon("naturalexceptions:cable_copper");
        cloth = register.registerIcon("naturalexceptions:cable_cloth");
        blockIcon = copper;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Cable.of(meta).insulated ? cloth : copper;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (Cable cable : Cable.values()) list.add(new ItemStack(item, 1, cable.ordinal()));
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    /** Its item: one per kind, each with what it carries. */
    public static final class ItemCable extends ItemBlock {

        public ItemCable(Block block) {
            super(block);
            setHasSubtypes(true);
            setMaxDamage(0);
        }

        @Override
        public int getMetadata(int damage) {
            return damage;
        }

        @Override
        public String getUnlocalizedName(ItemStack stack) {
            return "tile." + Cable.of(stack.getItemDamage()).unlocalizedName;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void addInformation(ItemStack stack, net.minecraft.entity.player.EntityPlayer player, List lines,
            boolean advanced) {
            Cable cable = Cable.of(stack.getItemDamage());
            lines.add(
                String.format(
                    "§7%.1f mm² copper%s",
                    cable.gauge.crossSection * 1.0e6,
                    cable.insulated ? ", in rubbered braid" : ", bare"));
            lines.add(
                String.format("§8%.4f Ω a block; carries %.0f A steadily", cable.ohmsPerMetre(), cable.ratedAmps()));
            if (!cable.insulated) lines.add("§cBare: it bites above 30 V, and hard at high tension.");
        }
    }
}
