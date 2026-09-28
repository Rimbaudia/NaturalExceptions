package naturalexceptions.minecraft.fluid;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.IIcon;
import net.minecraft.client.renderer.texture.IIconRegister;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.minecraft.item.ItemFluidVessel;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEItems;

/** Static visual host for a semantic parcel stored by FluidWorldState; it performs no per-cell ticking. */
public class BlockFluidCell extends Block {
    @SideOnly(Side.CLIENT) private IIcon liquidCellIcon;
    @SideOnly(Side.CLIENT) private IIcon solidCellIcon;
    public BlockFluidCell() {
        super(Material.water); setBlockName("fluidCell"); setBlockTextureName("minecraft:water_still");
        setBlockBounds(0, 0, 0, 1, 0.875F, 1); setHardness(0.1F); setLightOpacity(2);
    }
    public static int metadataFor(FluidParcel parcel) {
        if (parcel == null) return 0;
        boolean water = parcel.composition().contains(naturalexceptions.core.id.ResourceId.of("minecraft", "water"));
        boolean solid = parcel.phase() != naturalexceptions.core.material.MaterialPhase.LIQUID;
        long volume = parcel.volumeMicrolitres();
        int level = (int) Math.max(1L, Math.min(8L, (volume * 8L
            + naturalexceptions.core.fluid.FluidWorldState.MAX_CELL_VOLUME_MICROLITRES - 1L)
            / naturalexceptions.core.fluid.FluidWorldState.MAX_CELL_VOLUME_MICROLITRES));
        return (water ? 8 : 0) | (solid ? 16 : 0) | (level - 1);
    }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public int getRenderBlockPass() { return 1; }
    @Override public boolean isReplaceable(IBlockAccess access, int x, int y, int z) {
        if (access instanceof World) {
            World world = (World) access;
            NEWorldSavedData saved = NEWorldSavedData.get(world);
            if (saved != null && saved.fluids().parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z)) != null)
                return false;
        }
        return true;
    }
    @Override public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z) {
        int metadata = access.getBlockMetadata(x, y, z);
        int eighths = (metadata & 7) + 1;
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, eighths / 8.0F, 1.0F);
    }
    @Override public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }
    @Override public int quantityDropped(Random random) { return 0; }
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if ((metadata & 16) != 0) return solidCellIcon;
        if ((metadata & 8) != 0) return net.minecraft.init.Blocks.water.getIcon(0, 0);
        return liquidCellIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        liquidCellIcon = register.registerIcon("naturalexceptions:fluid_liquid");
        solidCellIcon = register.registerIcon("naturalexceptions:fluid_solid");
    }

    public static void refreshHost(World world, FluidCellKey key, FluidParcel parcel) {
        if (world == null || key == null || parcel == null || world.isRemote
            || world.provider.dimensionId != key.dimension()
            || !world.getChunkProvider().chunkExists(key.x() >> 4, key.z() >> 4)) return;
        Block host = world.getBlock(key.x(), key.y(), key.z());
        if (!(host instanceof BlockFluidCell)) return;
        int metadata = metadataFor(parcel);
        if (world.getBlockMetadata(key.x(), key.y(), key.z()) != metadata) {
            world.setBlockMetadataWithNotify(key.x(), key.y(), key.z(), metadata, 3);
            world.markBlockForUpdate(key.x(), key.y(), key.z());
        }
    }

    public ItemStack recoveryStack(FluidParcel parcel) {
        if (parcel == null) return null;
        if (parcel.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID) {
            ItemStack vessel = new ItemStack(NEItems.fluidVessel != null
                ? NEItems.fluidVessel : new ItemFluidVessel());
            ItemFluidVessel.setContents(vessel, parcel);
            return vessel;
        }
        ItemStack sample = naturalexceptions.minecraft.item.ItemMaterialSample.createStack(parcel.materialRef(),
            parcel.stateKey(), parcel.mass(), parcel.thermalState());
        naturalexceptions.minecraft.item.ItemMaterialSample.setProvenance(sample, parcel.provenanceId());
        return sample;
    }

    /** Survival collection of any liquid host requires a vessel; mining cannot silently become a bucket action. */
    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        FluidParcel parcel = saved == null ? null
            : saved.fluids().parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z));
        if (parcel != null && parcel.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID) return 0.0F;
        return super.getPlayerRelativeBlockHardness(player, world, x, y, z);
    }

    private void recoverParcel(World world, int x, int y, int z) {
        if (world.isRemote) return;
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        if (saved == null) return;
        FluidCellKey key = new FluidCellKey(world.provider.dimensionId, x, y, z);
        ItemStack stack = takeParcelForRecovery(saved.fluids(), key, this);
        if (stack == null) return;
        world.spawnEntityInWorld(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack));
        saved.markDirty();
    }

    /** Atomically moves the parcel out of spatial truth and into its phase-appropriate carrier. */
    static ItemStack takeParcelForRecovery(FluidWorldState fluids, FluidCellKey key, BlockFluidCell host) {
        if (fluids == null || key == null || host == null) return null;
        FluidParcel parcel = fluids.remove(key);
        if (parcel == null) return null;
        ItemStack stack = host.recoveryStack(parcel);
        if (stack == null) fluids.put(key, parcel);
        return stack;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        recoverParcel(world, x, y, z);
        super.harvestBlock(world, player, x, y, z, metadata);
    }

    /** Replacements caused outside ordinary harvesting still transfer the parcel to a carrier. */
    @Override
    public void breakBlock(World world, int x, int y, int z, Block oldBlock, int oldMetadata) {
        recoverParcel(world, x, y, z);
        super.breakBlock(world, x, y, z, oldBlock, oldMetadata);
    }
}
