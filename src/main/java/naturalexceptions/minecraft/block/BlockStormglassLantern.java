package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Stormglass in a skystone frame: the light of the sky dungeons, flameless and full. Set at the foot of a shaft
 * between levels (meta {@link #BEACON}) it sends a column of sparks up the shaft, seen rising out of the hole above.
 */
public final class BlockStormglassLantern extends Block {

    public static final int BEACON = 1;

    public BlockStormglassLantern() {
        super(Material.glass);
        setBlockName("stormglassLantern");
        setBlockTextureName("naturalexceptions:stormglass_lantern");
        setHardness(0.8F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (world.getBlockMetadata(x, y, z) != BEACON) return;
        for (int i = 0; i < 3; i++) world.spawnParticle(
            "fireworksSpark",
            x + 0.3 + random.nextDouble() * 0.4,
            y + 1.1,
            z + 0.3 + random.nextDouble() * 0.4,
            (random.nextDouble() - 0.5) * 0.02,
            0.45 + random.nextDouble() * 0.25,
            (random.nextDouble() - 0.5) * 0.02);
        world.spawnParticle("witchMagic", x + 0.5, y + 1.2 + random.nextDouble() * 3.0, z + 0.5, 0.0, 0.3, 0.0);
    }
}
