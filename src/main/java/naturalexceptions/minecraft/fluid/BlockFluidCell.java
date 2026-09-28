package naturalexceptions.minecraft.fluid;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

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

    @SideOnly(Side.CLIENT)
    private IIcon liquidCellIcon;
    @SideOnly(Side.CLIENT)
    private IIcon solidCellIcon;

    public BlockFluidCell() {
        this(Material.water);
        setBlockName("fluidCell");
        setBlockTextureName("minecraft:water_still");
        setLightOpacity(2);
    }

    protected BlockFluidCell(Material material) {
        super(material);
        setBlockBounds(0, 0, 0, 1, 0.875F, 1);
        setHardness(0.1F);
    }

    // Block metadata is four bits: two for the host kind and two for the fill level in quarters.
    public static final int KIND_LIQUID = 0;
    public static final int KIND_WATER = 1;
    public static final int KIND_SOLID = 2;
    public static final int KIND_GLOWING_SOLID = 3;
    /** Draper point: solid matter this hot visibly glows. */
    public static final double GLOW_TEMPERATURE_K = 798.0;

    public static int metadataFor(FluidParcel parcel) {
        if (parcel == null) return 0;
        boolean water = parcel.composition()
            .contains(naturalexceptions.core.id.ResourceId.of("minecraft", "water"));
        boolean solid = parcel.phase() != naturalexceptions.core.material.MaterialPhase.LIQUID;
        long volume = parcel.volumeMicrolitres();
        int quarters = (int) Math.max(
            1L,
            Math.min(
                4L,
                (volume * 4L + naturalexceptions.core.fluid.FluidWorldState.MAX_CELL_VOLUME_MICROLITRES - 1L)
                    / naturalexceptions.core.fluid.FluidWorldState.MAX_CELL_VOLUME_MICROLITRES));
        int kind;
        if (solid) kind = parcel.thermalState()
            .temperatureKelvin() >= GLOW_TEMPERATURE_K ? KIND_GLOWING_SOLID : KIND_SOLID;
        else kind = water ? KIND_WATER : KIND_LIQUID;
        return (kind << 2) | (quarters - 1);
    }

    public static int kindOf(int metadata) {
        return (metadata >> 2) & 3;
    }

    public static boolean isSolidMetadata(int metadata) {
        return kindOf(metadata) >= KIND_SOLID;
    }

    public static float heightOf(int metadata) {
        return ((metadata & 3) + 1) / 4.0F;
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
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean isReplaceable(IBlockAccess access, int x, int y, int z) {
        if (access instanceof World) {
            World world = (World) access;
            NEWorldSavedData saved = NEWorldSavedData.get(world);
            if (saved != null && saved.fluids()
                .parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z)) != null) return false;
        }
        return true;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, heightOf(access.getBlockMetadata(x, y, z)), 1.0F);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
    }

    /** Liquid parcels can be waded into; solidified parcels are solid ground. */
    @Override
    public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (!isSolidMetadata(metadata)) return null;
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + heightOf(metadata), z + 1);
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityFluidCell();
    }

    /** Material tint, supplied by the server from canonical appearance so different liquids read differently. */
    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess access, int x, int y, int z) {
        if (kindOf(access.getBlockMetadata(x, y, z)) == KIND_WATER) return 0xFFFFFF;
        net.minecraft.tileentity.TileEntity tile = access.getTileEntity(x, y, z);
        return tile instanceof TileEntityFluidCell ? ((TileEntityFluidCell) tile).color() : 0xFFFFFF;
    }

    /** Hot liquids and glowing solids light their surroundings. */
    @Override
    public int getLightValue(IBlockAccess access, int x, int y, int z) {
        int kind = kindOf(access.getBlockMetadata(x, y, z));
        if (kind == KIND_LIQUID || kind == KIND_GLOWING_SOLID) {
            net.minecraft.tileentity.TileEntity tile = access.getTileEntity(x, y, z);
            if (tile instanceof TileEntityFluidCell) return ((TileEntityFluidCell) tile).lightLevel();
        }
        return 0;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int kind = kindOf(metadata);
        if (kind >= KIND_SOLID) return solidCellIcon;
        if (kind == KIND_WATER) return net.minecraft.init.Blocks.water.getIcon(0, 0);
        return liquidCellIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        liquidCellIcon = register.registerIcon("naturalexceptions:fluid_liquid");
        solidCellIcon = register.registerIcon("naturalexceptions:fluid_solid");
    }

    public static void refreshHost(World world, FluidCellKey key, FluidParcel parcel) {
        if (world == null || key == null
            || parcel == null
            || world.isRemote
            || world.provider.dimensionId != key.dimension()
            || !world.getChunkProvider()
                .chunkExists(key.x() >> 4, key.z() >> 4))
            return;
        Block host = world.getBlock(key.x(), key.y(), key.z());
        if (!(host instanceof BlockFluidCell)) return;
        int metadata = metadataFor(parcel);
        if (world.getBlockMetadata(key.x(), key.y(), key.z()) != metadata) {
            world.setBlockMetadataWithNotify(key.x(), key.y(), key.z(), metadata, 3);
        }
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(key.x(), key.y(), key.z());
        if (tile instanceof TileEntityFluidCell) ((TileEntityFluidCell) tile).refreshFromWorld();
        // Tint and glow travel in the host tile's description packet; resend after any visible change.
        world.markBlockForUpdate(key.x(), key.y(), key.z());
        world.updateLightByType(net.minecraft.world.EnumSkyBlock.Block, key.x(), key.y(), key.z());
    }

    public ItemStack recoveryStack(FluidParcel parcel) {
        if (parcel == null) return null;
        if (parcel.phase() == naturalexceptions.core.material.MaterialPhase.LIQUID) {
            ItemStack vessel = new ItemStack(NEItems.fluidVessel != null ? NEItems.fluidVessel : new ItemFluidVessel());
            ItemFluidVessel.setContents(vessel, parcel);
            return vessel;
        }
        ItemStack sample = naturalexceptions.minecraft.item.ItemMaterialSample
            .createStack(parcel.materialRef(), parcel.stateKey(), parcel.mass(), parcel.thermalState());
        naturalexceptions.minecraft.item.ItemMaterialSample.setProvenance(sample, parcel.provenanceId());
        return sample;
    }

    /** Survival collection of any liquid host requires a vessel; mining cannot silently become a bucket action. */
    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        FluidParcel parcel = saved == null ? null
            : saved.fluids()
                .parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z));
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
