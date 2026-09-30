package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A wooden chair: a seat on four legs and a tall back. Sit on it to rest. Meta is the wood (as the table's: 0 oak,
 * 1 spruce, 2 dark oak) times four, plus the side its back is on (0 north, 1 east, 2 south, 3 west). The item's
 * damage is the wood times four too, so it draws in its own wood.
 */
public final class BlockChair extends BlockSeat {

    /** The side (2..5) for each back: north, east, south, west. */
    private static final int[] SIDE = { 2, 5, 3, 4 };

    private static final double[][] PARTS = { { 0.125, 0.5, 0.125, 0.875, 0.625, 0.875 },
        { 0.125, 0.625, 0.125, 0.875, 1.2, 0.25 }, { 0.15, 0.0, 0.15, 0.275, 0.5, 0.275 },
        { 0.725, 0.0, 0.15, 0.85, 0.5, 0.275 }, { 0.15, 0.0, 0.725, 0.275, 0.5, 0.85 },
        { 0.725, 0.0, 0.725, 0.85, 0.5, 0.85 } };

    public BlockChair() {
        super(Material.wood);
        setBlockName("woodenChair");
        setBlockTextureName("planks_oak");
        setHardness(1.5F);
        setResistance(4.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
    }

    @Override
    double[][] authored() {
        return PARTS;
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], SIDE[metadata & 3]);
        return out;
    }

    /** The wood of a chair's metadata. */
    static int wood(int meta) {
        return Math.min(meta >> 2 & 3, BlockWoodenTable.PLANKS.length - 1);
    }

    @Override
    public int inventoryMetadata() {
        return 2;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        // Placed facing you: its back away from you.
        int side = BlockPantryShelf.facingTowards(placer) ^ 1;
        int back = side == 2 ? 0 : side == 5 ? 1 : side == 3 ? 2 : 3;
        world.setBlockMetadataWithNotify(x, y, z, wood(stack.getItemDamage()) << 2 | back, 2);
    }

    @Override
    public int damageDropped(int meta) {
        return wood(meta) << 2;
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return wood(world.getBlockMetadata(x, y, z)) << 2;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < BlockWoodenTable.PLANKS.length; i++) list.add(new ItemStack(item, 1, i << 2));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.planks.getIcon(side, BlockWoodenTable.PLANKS[wood(meta)]);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return getIcon(1, metadata);
    }
}
