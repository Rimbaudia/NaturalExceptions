package naturalexceptions.minecraft.fluid;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * Host for one block of the fused-rock shell around a fallen star. Each block is a full one-litre parcel of
 * {@code impact_crust}: it glows while hot, dims as it cools, burns whoever stands on it while hot, and mines into
 * an ordinary material sample carrying its mass, heat and impact provenance.
 */
public final class BlockImpactCrust extends BlockFluidCell {

    static final double CONTACT_JOULES_PER_SECOND = 25.0;
    private static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");

    @SideOnly(Side.CLIENT)
    private IIcon hotIcon;
    @SideOnly(Side.CLIENT)
    private IIcon coldIcon;

    public BlockImpactCrust() {
        super(Material.rock);
        setBlockName("impactCrust");
        setBlockTextureName("naturalexceptions:impact_crust");
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        setHardness(3.0F);
        setResistance(20.0F);
        setStepSound(soundTypeStone);
        setLightOpacity(255);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public int getRenderBlockPass() {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        hotIcon = register.registerIcon("naturalexceptions:impact_crust_hot");
        coldIcon = register.registerIcon("naturalexceptions:impact_crust");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return kindOf(metadata) == KIND_SOLID ? coldIcon : hotIcon;
    }

    /** Authored in its own colours; no material tint. */
    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess access, int x, int y, int z) {
        return 0xFFFFFF;
    }

    /** The shell is always a whole block, whatever the parcel's exact volume. */
    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z) {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
    }

    /** Hot crust can be mined, unlike a molten pool. */
    @Override
    public float getPlayerRelativeBlockHardness(net.minecraft.entity.player.EntityPlayer player, World world, int x,
        int y, int z) {
        return net.minecraft.init.Blocks.stone.getPlayerRelativeBlockHardness(player, world, x, y, z) * 0.5F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (kindOf(world.getBlockMetadata(x, y, z)) == KIND_SOLID || !world.isAirBlock(x, y + 1, z)) return;
        if (random.nextInt(3) == 0) {
            world.spawnParticle("smoke", x + random.nextDouble(), y + 1.05, z + random.nextDouble(), 0.0, 0.04, 0.0);
        }
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (world == null || world.isRemote || !(entity instanceof EntityLivingBase)) return;
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        FluidParcel parcel = saved == null ? null
            : saved.fluids()
                .parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z));
        if (parcel == null) return;
        double kelvin = parcel.thermalState()
            .temperatureKelvin();
        if (kelvin < 373.15) return;
        naturalexceptions.minecraft.biology.BiologicalEvents
            .applySustainedThermalContact((EntityLivingBase) entity, LEFT_LEG, kelvin, CONTACT_JOULES_PER_SECOND);
    }
}
