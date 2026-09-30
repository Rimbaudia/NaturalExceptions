package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.biology.BodyTreatment;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Skystone basin fed by a spout that never runs dry: fill a bottle or a bucket at it, or cup your hands and drink
 * (an empty hand; a quarter litre, once a second).
 */
public final class BlockFountain extends BlockParts {

    /** Water a handful gives the body. */
    public static final double HANDFUL_MILLILITRES = 250.0;
    private static final String KEY_LAST_DRINK = "naturalexceptions.fountainDrink";

    /** Basin floor, four rim walls, the water, the spout's column and its bowl. */
    private static final double[][] PARTS = { { 0.0, 0.0, 0.0, 1.0, 0.125, 1.0 }, { 0.0, 0.125, 0.0, 1.0, 0.5, 0.125 },
        { 0.0, 0.125, 0.875, 1.0, 0.5, 1.0 }, { 0.0, 0.125, 0.125, 0.125, 0.5, 0.875 },
        { 0.875, 0.125, 0.125, 1.0, 0.5, 0.875 }, { 0.125, 0.125, 0.125, 0.875, 0.4375, 0.875 },
        { 0.4, 0.4375, 0.4, 0.6, 0.85, 0.6 }, { 0.3, 0.85, 0.3, 0.7, 0.95, 0.7 } };
    static final int WATER = 5;

    public BlockFountain() {
        super(Material.rock);
        setBlockName("fountain");
        setBlockTextureName("stonebrick");
        setHardness(2.0F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    /** What a fountain makes of {@code held}: a water bottle for a glass bottle, a water bucket for a bucket. */
    public static ItemStack fill(ItemStack held) {
        if (held == null) return null;
        if (held.getItem() == Items.glass_bottle) return new ItemStack(Items.potionitem, 1, 0);
        if (held.getItem() == Items.bucket) return new ItemStack(Items.water_bucket);
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        ItemStack held = player.getHeldItem();
        ItemStack filled = fill(held);
        if (held != null && filled == null) return false;
        if (world.isRemote) return true;
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.splash", 0.3F, 1.4F);
        if (filled != null) {
            if (player.capabilities.isCreativeMode) return true;
            if (--held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, filled);
            else if (!player.inventory.addItemStackToInventory(filled))
                player.dropPlayerItemWithRandomChoice(filled, false);
            return true;
        }
        long now = world.getTotalWorldTime();
        if (now - player.getEntityData()
            .getLong(KEY_LAST_DRINK) < 20 || !(player instanceof EntityPlayerMP)) return true;
        player.getEntityData()
            .setLong(KEY_LAST_DRINK, now);
        BodyTreatment.drink((EntityPlayerMP) player, HANDFUL_MILLILITRES);
        world.playSoundAtEntity(player, "random.drink", 0.5F, 1.1F);
        return true;
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
        if (index == WATER) return Blocks.water.getIcon(1, 0);
        if (index >= 6 && NEBlocks.skystoneDecor != null) return NEBlocks.skystoneDecor.getIcon(2, 3);
        return getIcon(1, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        for (int i = 0; i < 3; i++) world.spawnParticle(
            "splash",
            x + 0.5 + (random.nextDouble() - 0.5) * 0.3,
            y + 0.95,
            z + 0.5 + (random.nextDouble() - 0.5) * 0.3,
            0.0,
            0.0,
            0.0);
        if (random.nextInt(8) == 0)
            world.playSound(x + 0.5, y + 0.5, z + 0.5, "liquid.water", 0.15F, 1.2F + random.nextFloat() * 0.3F, false);
    }
}
