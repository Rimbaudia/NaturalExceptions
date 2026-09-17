package naturalexceptions.minecraft.worldgen;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import naturalexceptions.minecraft.registry.NEBlocks;

public final class BootstrapCopperGenerator implements IWorldGenerator {

    private static final int VEIN_SIZE = 7;
    private static final int ATTEMPTS_PER_CHUNK = 10;
    private static final int MIN_Y = 16;
    private static final int MAX_Y_EXCLUSIVE = 65;

    public static void register() {
        GameRegistry.registerWorldGenerator(new BootstrapCopperGenerator(), 0);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
        IChunkProvider chunkProvider) {
        if (world.provider.dimensionId != 0) return;

        for (int attempt = 0; attempt < ATTEMPTS_PER_CHUNK; attempt++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_Y_EXCLUSIVE - MIN_Y);
            int z = chunkZ * 16 + random.nextInt(16);
            new WorldGenMinable(NEBlocks.copperOre, VEIN_SIZE).generate(world, random, x, y, z);
        }
    }
}
