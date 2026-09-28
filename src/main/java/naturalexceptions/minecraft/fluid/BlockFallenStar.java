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
 * Minecraft-facing, phase-aware host for the canonical parcel at a Fallen Star impact site. Molten matter can be
 * waded into (and burns); once it solidifies it is solid ground that still burns whoever stands on it while hot.
 * It is not water: it neither extinguishes nor lets anything swim.
 */
public final class BlockFallenStar extends BlockFluidCell {

    /** Heat delivered per second of immersion in molten matter. */
    static final double MOLTEN_CONTACT_JOULES_PER_SECOND = 60.0;
    /** Heat delivered per second of standing on hot solid matter (boot contact only). */
    static final double SOLID_CONTACT_JOULES_PER_SECOND = 25.0;
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId LEFT_LEG = ResourceId.of("naturalexceptions", "left_leg");

    @SideOnly(Side.CLIENT)
    private IIcon moltenIcon;
    @SideOnly(Side.CLIENT)
    private IIcon solidIcon;

    public BlockFallenStar() {
        super(Material.rock);
        setBlockName("fallenStarSite");
        setBlockTextureName("naturalexceptions:fallen_star_solid");
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F);
        setHardness(1.5F);
        setResistance(12.0F);
        setStepSound(soundTypeStone);
        setLightOpacity(0);
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
        return isSolidMetadata(metadata) ? solidIcon : moltenIcon;
    }

    /** The stellar textures are authored in their own color; no material tint is applied on top. */
    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess access, int x, int y, int z) {
        return 0xFFFFFF;
    }

    @Override
    public int getLightValue(IBlockAccess access, int x, int y, int z) {
        int kind = kindOf(access.getBlockMetadata(x, y, z));
        if (kind == KIND_LIQUID) return 14;
        if (kind == KIND_GLOWING_SOLID) return 9;
        // Cold stellar matter keeps a faint glassy luminescence so the site remains findable at night.
        return 4;
    }

    /** Hot sites shed cyan sparks and a heat-shimmer of smoke; a cooled site only glints occasionally. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        int metadata = world.getBlockMetadata(x, y, z);
        int kind = kindOf(metadata);
        double top = y + heightOf(metadata);
        if (kind == KIND_LIQUID || kind == KIND_GLOWING_SOLID) {
            int sparks = kind == KIND_LIQUID ? 3 : 1;
            for (int i = 0; i < sparks; i++) {
                // "reddust" takes its velocity arguments as RGB; a near-zero red channel gives stellar cyan.
                world.spawnParticle(
                    "reddust",
                    x + random.nextDouble(),
                    top + random.nextDouble() * 0.4,
                    z + random.nextDouble(),
                    1.0E-4,
                    0.85,
                    1.0);
            }
            if (random.nextInt(kind == KIND_LIQUID ? 2 : 4) == 0) {
                world.spawnParticle(
                    "largesmoke",
                    x + 0.2 + random.nextDouble() * 0.6,
                    top + 0.1,
                    z + 0.2 + random.nextDouble() * 0.6,
                    0.0,
                    0.06,
                    0.0);
            }
            if (kind == KIND_LIQUID && random.nextInt(6) == 0) {
                world.spawnParticle("lava", x + 0.5, top, z + 0.5, 0.0, 0.0, 0.0);
            }
        } else if (random.nextInt(8) == 0) {
            world.spawnParticle(
                "reddust",
                x + random.nextDouble(),
                top + 0.05,
                z + random.nextDouble(),
                1.0E-4,
                0.7,
                0.9);
        }
    }

    /** Wading into molten matter: slowed, and burned at a bounded rate. */
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world == null || world.isRemote) return;
        if (!isSolidMetadata(world.getBlockMetadata(x, y, z))) {
            entity.motionX *= 0.4;
            entity.motionZ *= 0.4;
        }
        if (!(entity instanceof EntityLivingBase)) return;
        FluidParcel parcel = parcelAt(world, x, y, z);
        if (parcel == null) return;
        double kelvin = parcel.thermalState()
            .temperatureKelvin();
        if (kelvin < 373.15) return;
        naturalexceptions.minecraft.biology.BiologicalEvents
            .applySustainedThermalContact((EntityLivingBase) entity, TORSO, kelvin, MOLTEN_CONTACT_JOULES_PER_SECOND);
    }

    /** Standing on hot solid matter burns through the soles at a lower, bounded rate. */
    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (world == null || world.isRemote || !(entity instanceof EntityLivingBase)) return;
        FluidParcel parcel = parcelAt(world, x, y, z);
        if (parcel == null) return;
        double kelvin = parcel.thermalState()
            .temperatureKelvin();
        if (kelvin < 373.15) return;
        naturalexceptions.minecraft.biology.BiologicalEvents
            .applySustainedThermalContact((EntityLivingBase) entity, LEFT_LEG, kelvin, SOLID_CONTACT_JOULES_PER_SECOND);
    }

    private static FluidParcel parcelAt(World world, int x, int y, int z) {
        if (world == null || world.isRemote) return null;
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        return saved == null ? null
            : saved.fluids()
                .parcelAt(new FluidCellKey(world.provider.dimensionId, x, y, z));
    }
}
