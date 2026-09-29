package naturalexceptions.minecraft.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The seal on the floor of a Warden's hall. When a player comes within eight blocks it breaks, and the Sky Warden
 * wakes above it. Cannot be mined or blown up while it holds.
 */
public final class BlockWardenSeal extends BlockContainer {

    public BlockWardenSeal() {
        super(Material.rock);
        setBlockName("wardenSeal");
        setBlockTextureName("naturalexceptions:warden_seal");
        setBlockUnbreakable();
        setResistance(6000000.0F);
        setLightLevel(0.5F);
        setStepSound(soundTypeStone);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityWardenSeal();
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean canEntityDestroy(net.minecraft.world.IBlockAccess world, int x, int y, int z, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {}
}
