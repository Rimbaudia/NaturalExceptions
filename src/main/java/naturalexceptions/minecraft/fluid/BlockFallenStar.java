package naturalexceptions.minecraft.fluid;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/** Minecraft-facing, phase-aware host for the canonical parcel at a Fallen Star impact site. */
public final class BlockFallenStar extends BlockFluidCell {
    @SideOnly(Side.CLIENT) private IIcon moltenIcon;
    @SideOnly(Side.CLIENT) private IIcon solidIcon;

    public BlockFallenStar() {
        super();
        setBlockName("fallenStarSite");
        setBlockTextureName("naturalexceptions:fallen_star_solid");
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.55F, 1.0F);
        setHardness(1.2F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        moltenIcon = register.registerIcon("naturalexceptions:fallen_star_molten");
        solidIcon = register.registerIcon("naturalexceptions:fallen_star_solid");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return (metadata & 16) != 0 ? solidIcon : moltenIcon;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world == null || world.isRemote || !(entity instanceof EntityLivingBase)) return;
        FluidParcel parcel = parcelAt(world, x, y, z);
        if (parcel == null || parcel.thermalState().temperatureKelvin() < 373.15) return;
        naturalexceptions.minecraft.biology.BiologicalEvents.applyThermalContact((EntityLivingBase) entity,
            naturalexceptions.core.id.ResourceId.of("naturalexceptions", "torso"),
            parcel.thermalState().temperatureKelvin(), 35.0);
    }

    private static FluidParcel parcelAt(World world, int x, int y, int z) {
        if (world == null || world.isRemote) return null;
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        return saved == null ? null
            : saved.fluids().parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z));
    }
}
