package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Cloud Condenser: an open frame on a Skystone plinth, a Stormglass point at its heart that draws the sky's vapour
 * together. High in the sky (from {@link #MIN_Y} up) and open to it, it gathers a block of Cloud now and then (about
 * every five minutes, twice as fast in rain) until it holds {@link #MAX}; right-click to take them. Lower down there is
 * vapour enough only while rain falls on it, and then it gathers at the slow pace. Meta is how many it holds, and you
 * can see them fill it.
 */
public final class BlockCloudCondenser extends BlockParts {

    public static final int MAX = 8, MIN_Y = 128;
    /** One random tick in this many adds a cloud (about every five minutes, at a tick every 68 seconds). */
    public static final int GATHER = 4;

    private static final double[][] FRAME = { { 0.0, 0.0, 0.0, 1.0, 0.1875, 1.0 },
        { 0.0, 0.1875, 0.0, 0.125, 1.0, 0.125 }, { 0.875, 0.1875, 0.0, 1.0, 1.0, 0.125 },
        { 0.0, 0.1875, 0.875, 0.125, 1.0, 1.0 }, { 0.875, 0.1875, 0.875, 1.0, 1.0, 1.0 },
        { 0.125, 0.875, 0.0, 0.875, 1.0, 0.125 }, { 0.125, 0.875, 0.875, 0.875, 1.0, 1.0 },
        { 0.44, 0.1875, 0.44, 0.56, 0.5, 0.56 } };

    public BlockCloudCondenser() {
        super(Material.rock);
        setBlockName("cloudCondenser");
        setBlockTextureName("stonebrick");
        setHardness(2.0F);
        setResistance(8.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
        setTickRandomly(true);
    }

    @Override
    public double[][] parts(int metadata) {
        int held = Math.max(0, Math.min(MAX, metadata));
        double[][] out = new double[FRAME.length + (held > 0 ? 1 : 0)][];
        for (int i = 0; i < FRAME.length; i++) out[i] = FRAME[i].clone();
        if (held > 0) out[FRAME.length] = new double[] { 0.15, 0.1875, 0.15, 0.85, 0.1875 + 0.08 * held, 0.85 };
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 3;
    }

    /** Whether a condenser at (x, y, z) gathers: open to the sky, and high in it or rained on. */
    public static boolean gathers(World world, int x, int y, int z) {
        if (!world.canBlockSeeTheSky(x, y + 1, z)) return false;
        return y >= MIN_Y || world.canLightningStrikeAt(x, y + 1, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int held = world.getBlockMetadata(x, y, z);
        if (held >= MAX || !gathers(world, x, y, z)) return;
        int chance = world.isRaining() && y >= MIN_Y ? GATHER / 2 : GATHER;
        if (random.nextInt(chance) == 0) world.setBlockMetadataWithNotify(x, y, z, held + 1, 3);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        int held = world.getBlockMetadata(x, y, z);
        if (held <= 0 || NEBlocks.cloud == null) return false;
        if (world.isRemote) return true;
        ItemStack clouds = new ItemStack(NEBlocks.cloud, held);
        if (!player.inventory.addItemStackToInventory(clouds))
            world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 1.1, z + 0.5, clouds));
        world.setBlockMetadataWithNotify(x, y, z, 0, 3);
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "step.cloth", 1.0F, 1.2F);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) >= MAX || y < MIN_Y) return;
        // Wisps of vapour drawn in towards the point.
        double a = random.nextDouble() * Math.PI * 2;
        world.spawnParticle(
            "cloud",
            x + 0.5 + Math.cos(a) * 0.9,
            y + 0.3 + random.nextDouble() * 0.6,
            z + 0.5 + Math.sin(a) * 0.9,
            -Math.cos(a) * 0.03,
            0.01,
            -Math.sin(a) * 0.03);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return NEBlocks.skystoneDecor != null ? NEBlocks.skystoneDecor.getIcon(side, 2)
            : Blocks.stonebrick.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index == 0) return getIcon(1, 0);
        if (index == FRAME.length - 1)
            return NEBlocks.stormglassLantern != null ? NEBlocks.stormglassLantern.getIcon(2, 0)
                : Blocks.glowstone.getIcon(2, 0);
        if (index >= FRAME.length) return NEBlocks.cloud != null ? NEBlocks.cloud.getIcon(1, 0) : Blocks.wool.getIcon(1, 0);
        return Blocks.iron_block.getIcon(2, 0);
    }
}
