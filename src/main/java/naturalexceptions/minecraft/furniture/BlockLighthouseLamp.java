package naturalexceptions.minecraft.furniture;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * A Lighthouse Lamp: a great Stormglass light in a Skystone cage, whose two beams sweep round the sky day and night
 * and are seen from far off (they are drawn out to {@link TileEntityLighthouseLamp#BEAM} blocks). Set one high on a
 * rim and it shows the way home across the sky. A redstone signal puts it out.
 */
public final class BlockLighthouseLamp extends BlockContainer {

    public BlockLighthouseLamp() {
        super(Material.glass);
        setBlockName("lighthouseLamp");
        setBlockTextureName("naturalexceptions:stormglass_lantern");
        setHardness(1.5F);
        setLightLevel(1.0F);
        setStepSound(soundTypeGlass);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityLighthouseLamp();
    }

    /** Whether the lamp at (x, y, z) is lit: always, unless a redstone signal reaches it. */
    public static boolean lit(World world, int x, int y, int z) {
        return !world.isBlockIndirectlyGettingPowered(x, y, z);
    }
}
