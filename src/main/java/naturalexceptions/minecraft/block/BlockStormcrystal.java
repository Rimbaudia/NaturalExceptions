package naturalexceptions.minecraft.block;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Stormcrystal: Stormglass grown into a spire out of a scorched crater on the great islands, humming and throwing
 * sparks. An iron pick breaks it into two to four Stormglass Shards (more with Fortune). The spire's crown (meta
 * {@link #CROWN}) calls the lightning down on itself while it thunders: stand clear of it in a storm.
 */
public final class BlockStormcrystal extends Block {

    public static final int BODY = 0, CROWN = 1;
    /** One random tick in this many brings the lightning down on a crown in a thunderstorm (about every two minutes). */
    public static final int STRIKE = 2;

    @SideOnly(Side.CLIENT)
    private IIcon body, crown;

    public BlockStormcrystal() {
        super(Material.glass);
        setBlockName("stormcrystal");
        setBlockTextureName("naturalexceptions:stormcrystal");
        setHardness(3.0F);
        setResistance(12.0F);
        setLightLevel(0.7F);
        setStepSound(soundTypeGlass);
        setHarvestLevel("pickaxe", 2);
        setTickRandomly(true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        body = register.registerIcon("naturalexceptions:stormcrystal");
        crown = register.registerIcon("naturalexceptions:stormcrystal_crown");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == CROWN ? crown : body;
    }

    /** Solid crystal: Stormglass points hang from its underside, as from rock. */
    @Override
    public boolean isSideSolid(net.minecraft.world.IBlockAccess world, int x, int y, int z,
        net.minecraftforge.common.util.ForgeDirection side) {
        return true;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        if (NEItems.stormglassShard == null) return drops;
        Random random = world != null ? world.rand : new Random();
        drops.add(new ItemStack(NEItems.stormglassShard, 2 + random.nextInt(3) + random.nextInt(fortune + 1)));
        return drops;
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return metadata == BODY;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || world.getBlockMetadata(x, y, z) != CROWN) return;
        if (world.isThundering() && world.canBlockSeeTheSky(x, y + 1, z) && random.nextInt(STRIKE) == 0)
            world.addWeatherEffect(new EntityLightningBolt(world, x + 0.5, y + 1.0, z + 0.5));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(world.getBlockMetadata(x, y, z) == CROWN ? 1 : 4) != 0) return;
        world.spawnParticle(
            "fireworksSpark",
            x + random.nextDouble(),
            y + random.nextDouble(),
            z + random.nextDouble(),
            (random.nextDouble() - 0.5) * 0.1,
            random.nextDouble() * 0.1,
            (random.nextDouble() - 0.5) * 0.1);
    }
}
