package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A keystone with a Gale Core sealed in it, set in the underside of some of the great stone arches (Wind Gates). The
 * wind leaks from it in wisps; broken with a pickaxe, it gives up the core.
 */
public final class BlockGaleKeystone extends Block {

    public BlockGaleKeystone() {
        super(Material.rock);
        setBlockName("galeKeystone");
        setBlockTextureName("naturalexceptions:gale_keystone");
        setHardness(3.0F);
        setResistance(15.0F);
        setLightLevel(4.0F / 15.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return NEItems.galeCore != null ? NEItems.galeCore : Item.getItemFromBlock(this);
    }

    /** Wisps of wind drift down from it. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(3) != 0) return;
        world.spawnParticle(
            "cloud",
            x + random.nextDouble(),
            y - 0.05,
            z + random.nextDouble(),
            (random.nextDouble() - 0.5) * 0.04,
            -0.04,
            (random.nextDouble() - 0.5) * 0.04);
    }
}
