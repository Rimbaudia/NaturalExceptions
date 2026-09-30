package naturalexceptions.minecraft.block;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A working face of a Skystone quarry. A seam (meta 0), its bedding marked out by the quarrymen's chisels, splits
 * off two blocks of Skystone at a pick's blow and is left worked (meta 1): the fresh-cut face, which cannot be cut
 * again until the seam has opened afresh. On random ticks a worked face opens again (about twenty minutes on
 * average, while someone is near enough for the land to be awake), so a quarry stays a quarry.
 */
public final class BlockSkystoneSeam extends Block {

    public static final int SEAM = 0, WORKED = 1;
    /** One random tick in this many opens a worked face: about twenty minutes, at a tick every 68 seconds. */
    public static final int REGROWTH = 18;
    /** How much Skystone a seam gives. */
    public static final int YIELD = 2;

    @SideOnly(Side.CLIENT)
    private IIcon seam, worked;

    public BlockSkystoneSeam() {
        super(Material.rock);
        setBlockName("skystoneSeam");
        setBlockTextureName("naturalexceptions:skystone_seam");
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 0);
        setTickRandomly(true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        seam = register.registerIcon("naturalexceptions:skystone_seam");
        worked = register.registerIcon("naturalexceptions:skystone_seam_worked");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == WORKED ? worked : seam;
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) == WORKED ? -1.0F : super.getBlockHardness(world, x, y, z);
    }

    /** Cutting a seam leaves the face worked, where it was, rather than a hole. */
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        if (world.getBlockMetadata(x, y, z) != SEAM) return false;
        if (player != null && player.capabilities.isCreativeMode && player.isSneaking())
            return super.removedByPlayer(world, player, x, y, z, willHarvest);
        world.setBlockMetadataWithNotify(x, y, z, WORKED, 3);
        return true;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        Block rock = NEBlocks.skystoneRock;
        if (metadata == SEAM && rock != null) drops.add(new ItemStack(Item.getItemFromBlock(rock), YIELD));
        return drops;
    }

    /** A seam belongs to its quarry: it cannot be lifted whole and set down elsewhere. */
    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || world.getBlockMetadata(x, y, z) != WORKED) return;
        if (random.nextInt(REGROWTH) == 0) regrow(world, x, y, z);
    }

    /** A worked face opens into a fresh seam. */
    public static void regrow(World world, int x, int y, int z) {
        world.setBlockMetadataWithNotify(x, y, z, SEAM, 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) != SEAM || random.nextInt(6) != 0) return;
        // A little grit trickling from the seam's bedding: it is ready to be cut.
        for (int side = 0; side < 4; side++) {
            int dx = side == 0 ? 1 : side == 1 ? -1 : 0, dz = side == 2 ? 1 : side == 3 ? -1 : 0;
            if (!world.isAirBlock(x + dx, y, z + dz)) continue;
            world.spawnParticle(
                "snowshovel",
                x + 0.5 + dx * 0.55,
                y + 0.2 + random.nextDouble() * 0.6,
                z + 0.5 + dz * 0.55,
                0.0,
                -0.02,
                0.0);
            return;
        }
    }
}
