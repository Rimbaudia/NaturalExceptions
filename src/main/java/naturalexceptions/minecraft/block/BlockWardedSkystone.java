package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * The shell of a sky dungeon's vault: Skystone bound with wards, which no pick, blast or star can break. The only
 * way in is through its Sealed Vault Gate, with the dungeon's Sky Key.
 */
public final class BlockWardedSkystone extends Block {

    public BlockWardedSkystone() {
        super(Material.rock);
        setBlockName("wardedSkystone");
        setBlockTextureName("naturalexceptions:warded_skystone");
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setStepSound(soundTypePiston);
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {}
}
