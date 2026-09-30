package naturalexceptions.minecraft.furniture;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A striped canvas awning sloping down from the wall it hangs on. Canvas gives: whatever lands on it takes no fall
 * damage, so a stall's awning is a safe place to come down on. Meta: colour (0 red, 1 blue,
 * 2 green, 3 yellow) times four, plus the side its wall is on (0 north, 1 south, 2 west, 3 east).
 */
public final class BlockMarketAwning extends BlockParts {

    public static final String[] COLOURS = { "red", "blue", "green", "yellow" };
    /** Authored with the wall to the north (-z): three steps down to the south. */
    private static final double[][] PARTS = { { 0.0, 0.875, 0.0, 1.0, 1.0, 0.34 },
        { 0.0, 0.75, 0.33, 1.0, 0.875, 0.67 }, { 0.0, 0.625, 0.66, 1.0, 0.75, 1.0 } };
    private static final int[] FACING = { 2, 3, 4, 5 };

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockMarketAwning() {
        super(Material.cloth);
        setBlockName("marketAwning");
        setBlockTextureName("naturalexceptions:awning_red");
        setHardness(0.8F);
        setStepSound(soundTypeCloth);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], FACING[metadata & 3]);
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[COLOURS.length];
        String[] names = { "naturalexceptions:awning_red", "naturalexceptions:awning_blue",
            "naturalexceptions:awning_green", "naturalexceptions:awning_yellow" };
        for (int i = 0; i < COLOURS.length; i++) icons[i] = register.registerIcon(names[i]);
        blockIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[(meta >> 2) & 3];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return getIcon(1, metadata);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 12;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < COLOURS.length; i++) list.add(new ItemStack(item, 1, i << 2));
    }

    /** Hung on the face that was clicked; set on top of something, it hangs from the side away from the placer. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int wall;
        switch (side) {
            case 2:
                wall = 1; // clicked a wall's north face: that wall is to the south
                break;
            case 3:
                wall = 0;
                break;
            case 4:
                wall = 3;
                break;
            case 5:
                wall = 2;
                break;
            default:
                wall = -1;
        }
        placedOnTop.set(wall < 0);
        return (meta & 12) | (wall < 0 ? 0 : wall);
    }

    /** Whether the awning being placed was set on top of a block (then the placer decides which way it hangs). */
    private static final ThreadLocal<Boolean> placedOnTop = ThreadLocal.withInitial(() -> false);

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int meta = world.getBlockMetadata(x, y, z);
        if (!placedOnTop.get()) return;
        placedOnTop.set(false);
        int facing = BlockPantryShelf.facingTowards(placer); // the side towards the placer: slope down to them
        int wall = facing == 2 ? 1 : facing == 3 ? 0 : facing == 4 ? 3 : 2;
        world.setBlockMetadataWithNotify(x, y, z, (meta & 12) | wall, 2);
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float distance) {
        entity.fallDistance = 0.0F; // canvas gives
    }
}
