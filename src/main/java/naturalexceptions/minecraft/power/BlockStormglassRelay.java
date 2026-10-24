package naturalexceptions.minecraft.power;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.power.PowerTopology;
import naturalexceptions.minecraft.block.PartModel;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Stormglass relay: a crystal on an iron post. It joins whatever touches it, and every other relay within
 * {@link PowerTopology#RELAY_REACH} blocks, through the air, losing a little more the farther the beam reaches.
 */
public final class BlockStormglassRelay extends BlockContainer implements PowerBlock, PartModel {

    private static final double P = 1.0 / 16.0;
    private static final double[][] PARTS = { { 5, 0, 5, 11, 1, 11 }, { 7, 1, 7, 9, 10, 9 }, { 6, 10, 6, 10, 14, 10 } };

    public BlockStormglassRelay() {
        super(Material.iron);
        setBlockName("stormglassRelay");
        setBlockTextureName("naturalexceptions:stormglass_lantern_core");
        setHardness(1.5F);
        setLightLevel(0.4F);
        setStepSound(soundTypeGlass);
    }

    @Override
    public boolean conducts(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean relay() {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new Tile();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        PowerNetworks.relay(world, x, y, z, false);
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (random.nextInt(4) == 0)
            world.spawnParticle("witchMagic", x + 0.5, y + 0.8 + random.nextDouble() * 0.2, z + 0.5, 0.0, 0.05, 0.0);
    }

    @Override
    public double[][] parts(int meta) {
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) {
            double[] b = PARTS[i];
            out[i] = new double[] { b[0] * P, b[1] * P, b[2] * P, b[3] * P, b[4] * P, b[5] * P };
        }
        return out;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int meta) {
        return index == 2 ? blockIcon : Blocks.iron_block.getIcon(2, 0);
    }

    @Override
    public int inventoryMetadata() {
        return 0;
    }

    @Override
    public int getRenderType() {
        return PartModel.RenderType.id;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBlockBounds(5 * (float) P, 0.0F, 5 * (float) P, 11 * (float) P, 14 * (float) P, 11 * (float) P);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + 5 * P, y, z + 5 * P, x + 11 * P, y + 14 * P, z + 11 * P);
        if (mask.intersectsWith(box)) boxes.add(box);
    }

    /** Keeps its relay known while it is loaded: the beams are not in the blocks, so they are told. */
    public static final class Tile extends TileEntity {

        private boolean known;

        @Override
        public boolean canUpdate() {
            return true;
        }

        @Override
        public void updateEntity() {
            if (known || worldObj == null || worldObj.isRemote) return;
            known = true;
            PowerNetworks.relay(
                worldObj,
                xCoord,
                yCoord,
                zCoord,
                worldObj.getBlock(xCoord, yCoord, zCoord) == NEBlocks.stormglassRelay);
        }

        @Override
        public void onChunkUnload() {
            if (worldObj != null && !worldObj.isRemote) PowerNetworks.relay(worldObj, xCoord, yCoord, zCoord, false);
        }
    }
}
