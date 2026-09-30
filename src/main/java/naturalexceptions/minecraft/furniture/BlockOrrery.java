package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A brass model of the sky: a golden sun on a stand with the moon and a wandering star on arms turning round it,
 * in step with the real sky. Right-click it to read the hour, the moon's phase and how many nights until it is full.
 */
public final class BlockOrrery extends BlockParts {

    /** Plinth, stem, the sun. The arms and their bodies turn, drawn by {@code RenderOrrery}. */
    private static final double[][] PARTS = { { 0.15, 0.0, 0.15, 0.85, 0.125, 0.85 },
        { 0.46, 0.125, 0.46, 0.54, 0.5, 0.54 }, { 0.4, 0.5, 0.4, 0.6, 0.7, 0.6 } };

    static final String[] PHASES = { "full", "waning gibbous", "at its last quarter", "a waning crescent", "new",
        "a waxing crescent", "at its first quarter", "waxing gibbous" };

    public BlockOrrery() {
        super(Material.iron);
        setBlockName("orrery");
        setBlockTextureName("gold_block");
        setHardness(1.5F);
        setResistance(5.0F);
        setStepSound(soundTypeMetal);
    }

    @Override
    public double[][] parts(int metadata) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PARTS[i].clone();
        return out;
    }

    /** "It is 16:40, late afternoon. The moon is waxing gibbous: full in 1 night." */
    public static String reading(long worldTime, int moonPhase) {
        long t = (worldTime + 6000L) % 24000L;
        int hour = (int) (t / 1000L), minute = (int) (t % 1000L * 60L / 1000L);
        String part = hour < 5 ? "deep night"
            : hour < 7 ? "dawn"
                : hour < 12 ? "morning" : hour < 14 ? "midday" : hour < 18 ? "afternoon" : hour < 20 ? "dusk" : "night";
        int phase = Math.floorMod(moonPhase, 8);
        int nights = (8 - phase) % 8;
        String moon = "The moon is " + PHASES[phase]
            + (nights == 0 ? " tonight." : ": full in " + nights + (nights == 1 ? " night." : " nights."));
        return String.format("It is %02d:%02d, %s. %s", hour, minute, part, moon);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        player.addChatComponentMessage(
            new ChatComponentText(
                "§6" + reading(world.getWorldTime(), world.provider.getMoonPhase(world.getWorldTime()))));
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "random.click", 0.4F, 1.6F);
        return true;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityOrrery();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.gold_block.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        return index == 0 ? Blocks.planks.getIcon(1, 5) : Blocks.gold_block.getIcon(1, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }
}
