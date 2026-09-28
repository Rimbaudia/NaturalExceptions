package naturalexceptions.minecraft.event;

import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.StarRainScarPlan;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;

/** Materializes persisted P11 intent only into already-loaded Overworld chunks. */
public final class StarRainWorldMaterializer {
    private static final List<Block> NATURAL_SURFACE = Arrays.asList(
        Blocks.grass, Blocks.dirt, Blocks.stone, Blocks.gravel, Blocks.sand, Blocks.hardened_clay,
        Blocks.stained_hardened_clay);

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event == null || event.getChunk() == null || event.getChunk().worldObj == null
            || event.getChunk().worldObj.isRemote || event.getChunk().worldObj.provider.dimensionId != 0) return;
        World world = event.getChunk().worldObj;
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        if (saved != null) materializePending(world, saved, event.getChunk().xPosition,
            event.getChunk().zPosition, 4);
    }

    public int materializePending(World world, NEWorldSavedData saved, int chunkX, int chunkZ, int budget) {
        if (world == null || saved == null || budget <= 0 || world.isRemote) return 0;
        int work = 0;
        for (StarRainImpact impact : saved.p11().awaitingMaterializationInChunk(chunkX, chunkZ, budget)) {
            if (work >= budget) break;
            if (materialize(world, saved, impact, false)) work++;
        }
        return work;
    }

    public boolean materialize(World world, NEWorldSavedData saved, StarRainImpact impact) {
        return materialize(world, saved, impact, true);
    }

    private boolean materialize(World world, NEWorldSavedData saved, StarRainImpact impact,
        boolean applyImmediateEntityConsequence) {
        if (world == null || saved == null || impact == null || world.isRemote || !impact.isResolved()
            || impact.isMaterialized() || !world.getChunkProvider().chunkExists(impact.x() >> 4, impact.z() >> 4)) return false;
        int x = impact.x();
        int z = impact.z();
        int surfaceY = world.getTopSolidOrLiquidBlock(x, z);
        int y = Math.max(1, Math.min(254, surfaceY - 1));
        FluidCellKey key = new FluidCellKey(world.provider.dimensionId, x, y, z);
        MaterialQuantity mass = impact.stellarMass();
        FluidParcel existing = saved.fluids().parcelAt(key);
        FluidParcel parcel;
        if (existing != null) {
            if (!isThisImpactMatter(existing, impact, mass)) return false;
            parcel = existing;
        } else {
            if (mass.isZero() || !isNaturalSurface(world.getBlock(x, y, z))
                || world.getTileEntity(x, y, z) != null) return false;
            parcel = parcel(impact, mass);
            try { saved.fluids().put(key, parcel); }
            catch (RuntimeException invalid) { return false; }
            saved.markDirty();
        }

        if (world.getBlock(x, y, z) != NEBlocks.fallenStarCell) {
            if (world.getTileEntity(x, y, z) != null) return false;
            carveSmallScar(world, x, y, z, impact.severity());
            int metadata = naturalexceptions.minecraft.fluid.BlockFluidCell.metadataFor(parcel);
            // Upgrade a pre-stabilization generic fluid host without letting its replacement hook
            // transfer the parcel out of authoritative saved fluid state.
            boolean rehosting = existing != null
                && world.getBlock(x, y, z) instanceof naturalexceptions.minecraft.fluid.BlockFluidCell;
            if (rehosting) saved.fluids().remove(key);
            boolean placed = world.setBlock(x, y, z, NEBlocks.fallenStarCell, metadata, 3);
            if (rehosting) saved.fluids().put(key, parcel);
            if (!placed) return false;
            world.playSoundEffect(x + 0.5D, y + 0.4D, z + 0.5D, "random.explode", 0.7F, 1.35F);
            if (impact.severity() >= 3) igniteIfFlammable(world, x + 1, y, z);
            notifyNearbyPlayers(world, x, y, z, impact);
        }

        if (saved.p11().transferToSpatialSite(impact.id()).isZero()) return false;
        saved.markDirty();
        if (applyImmediateEntityConsequence) damageNearbyLiving(world, x, y, z, impact.severity());
        return true;
    }

    private static FluidParcel parcel(StarRainImpact impact, MaterialQuantity mass) {
        MaterialRef ref = BuiltinMaterials.STELLAR_MATTER.ref();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(BuiltinMaterials.STELLAR_MATTER_COMPOSITION,
            MaterialPhase.LIQUID, null, null);
        ActiveThermalState thermal = ActiveThermalState.atTemperature(1780.0, 1.0, mass.toKilograms(),
            BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        return new FluidParcel(ref, key, BuiltinMaterials.STELLAR_MATTER_COMPOSITION, mass, thermal,
            impact.id());
    }

    private static boolean isThisImpactMatter(FluidParcel parcel, StarRainImpact impact,
        MaterialQuantity expectedMass) {
        return parcel.materialRef().equals(BuiltinMaterials.STELLAR_MATTER.ref())
            && parcel.composition().equals(BuiltinMaterials.STELLAR_MATTER_COMPOSITION)
            && impact.id().equals(parcel.provenanceId())
            && parcel.mass().equals(expectedMass);
    }

    private static void carveSmallScar(World world, int x, int y, int z, int severity) {
        // Only already-loaded target/neighbor chunks are edited. Cleared blocks make retries harmless.
        for (StarRainScarPlan.Offset offset : StarRainScarPlan.offsets(severity)) {
            disturb(world, x + offset.x(), y, z + offset.z());
        }
    }

    private static void disturb(World world, int x, int y, int z) {
        if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return;
        Block block = world.getBlock(x, y, z);
        if (isNaturalSurface(block) && world.getTileEntity(x, y, z) == null) world.setBlockToAir(x, y, z);
    }

    private static boolean isNaturalSurface(Block block) { return NATURAL_SURFACE.contains(block); }

    private static void igniteIfFlammable(World world, int x, int y, int z) {
        if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4) || !world.isAirBlock(x, y, z)) return;
        if (Blocks.fire.canPlaceBlockAt(world, x, y, z)) world.setBlock(x, y, z, Blocks.fire, 0, 3);
    }

    private static void damageNearbyLiving(World world, int x, int y, int z, int severity) {
        double radius = 1.5D + severity;
        net.minecraft.util.AxisAlignedBB bounds = net.minecraft.util.AxisAlignedBB.getBoundingBox(
            x - radius, y - radius, z - radius, x + 1 + radius, y + 1 + radius, z + 1 + radius);
        for (Object object : world.getEntitiesWithinAABB(EntityLivingBase.class, bounds)) {
            EntityLivingBase living = (EntityLivingBase) object;
            if (living.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > radius * radius) continue;
            // P10's ordinary host-damage adapter converts fire damage into a thermal physical interaction.
            living.attackEntityFrom(net.minecraft.util.DamageSource.onFire, 0.5F * severity);
        }
    }

    private static void notifyNearbyPlayers(World world, int x, int y, int z, StarRainImpact impact) {
        double radius = 48.0D;
        net.minecraft.util.AxisAlignedBB bounds = net.minecraft.util.AxisAlignedBB.getBoundingBox(
            x - radius, y - radius, z - radius, x + 1 + radius, y + 1 + radius, z + 1 + radius);
        for (Object object : world.getEntitiesWithinAABB(net.minecraft.entity.player.EntityPlayer.class, bounds)) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) object;
            if (player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= radius * radius) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText(
                    StarRainPresentation.impactMessage(impact)));
            }
        }
    }
}
