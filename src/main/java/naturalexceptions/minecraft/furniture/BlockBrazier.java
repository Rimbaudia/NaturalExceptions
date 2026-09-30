package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Skystone bowl of embers on three legs: bright light (15) that never goes out, and a grill. Right-click with raw
 * meat or fish to lay one on the coals; a few seconds later it jumps off cooked. Standing in it burns.
 */
public final class BlockBrazier extends BlockParts {

    /** Legs, the bowl's floor and walls, and the bed of embers. */
    private static final double[][] PARTS = { { 0.2, 0.0, 0.2, 0.3, 0.45, 0.3 }, { 0.7, 0.0, 0.2, 0.8, 0.45, 0.3 },
        { 0.45, 0.0, 0.7, 0.55, 0.45, 0.8 }, { 0.1, 0.45, 0.1, 0.9, 0.55, 0.9 }, { 0.1, 0.55, 0.1, 0.9, 0.8, 0.18 },
        { 0.1, 0.55, 0.82, 0.9, 0.8, 0.9 }, { 0.1, 0.55, 0.18, 0.18, 0.8, 0.82 }, { 0.82, 0.55, 0.18, 0.9, 0.8, 0.82 },
        { 0.18, 0.55, 0.18, 0.82, 0.68, 0.82 } };

    public BlockBrazier() {
        super(Material.rock);
        setBlockName("brazier");
        setBlockTextureName("stonebrick");
        setHardness(1.5F);
        setResistance(6.0F);
        setLightLevel(1.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    /** What the brazier makes of {@code raw}: its cooked form if it is raw food, else null. */
    public static ItemStack cooked(ItemStack raw) {
        if (raw == null || !(raw.getItem() instanceof ItemFood)) return null;
        ItemStack result = FurnaceRecipes.smelting()
            .getSmeltingResult(raw);
        return result != null && result.getItem() instanceof ItemFood ? result.copy() : null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        if (cooked(held) == null) return false;
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityBrazier) || !((TileEntityBrazier) tile).lay(held)) return true;
        if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        world.playSoundEffect(x + 0.5, y + 0.8, z + 0.5, "random.fizz", 0.5F, 1.4F);
        return true;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && entity.posY > y + 0.5 && !entity.isImmuneToFire())
            entity.attackEntityFrom(DamageSource.inFire, 1.0F);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityBrazier();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityBrazier && !world.isRemote) ((TileEntityBrazier) tile).spill();
        super.breakBlock(world, x, y, z, block, meta);
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
        if (index == PARTS.length - 1)
            return NEBlocks.magmaRock != null ? NEBlocks.magmaRock.getIcon(1, 0) : Blocks.netherrack.getIcon(1, 0);
        return getIcon(1, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        for (int i = 0; i < 2; i++) world.spawnParticle(
            "flame",
            x + 0.25 + random.nextDouble() * 0.5,
            y + 0.72,
            z + 0.25 + random.nextDouble() * 0.5,
            0.0,
            0.01,
            0.0);
        world.spawnParticle("smoke", x + 0.5, y + 0.95, z + 0.5, 0.0, 0.02, 0.0);
        if (random.nextInt(10) == 0)
            world.playSound(x + 0.5, y + 0.6, z + 0.5, "fire.fire", 0.4F, 0.8F + random.nextFloat() * 0.3F, false);
    }
}
