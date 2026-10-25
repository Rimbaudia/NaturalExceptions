package naturalexceptions.minecraft.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.event.StarRainCraterPlan;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.fluid.BlockFluidCell;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * Turns a resolved Star Rain impact into a crater with a half-buried ball of fused crust around a stellar core.
 * Works only when every chunk the crater touches is loaded; otherwise the impact stays awaiting and is retried.
 */
public final class StarRainWorldMaterializer {

    /** Crust shell parcels are one block (1 L) of fused rock, just under the cell limit. */
    static final double CRUST_FILL = 0.999;
    /** Fresh crust is set but still glowing; the core arrives molten. */
    static final double CRUST_TEMPERATURE_K = 1150.0;
    static final double CORE_TEMPERATURE_K = 1780.0;

    /** Chunks that loaded with an impact perhaps waiting in them, each as {chunkX, chunkZ}: looked at next tick. */
    private final java.util.ArrayDeque<int[]> loaded = new java.util.ArrayDeque<>();
    private World loadedWorld;

    /**
     * A chunk loaded: a crater waiting there is carved on the next tick, not now. Carving while a chunk loads would
     * tell the blocks round its edge, and those can load (or generate) further chunks from inside the load.
     */
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event == null || event.getChunk() == null
            || event.getChunk().worldObj == null
            || event.getChunk().worldObj.isRemote
            || event.getChunk().worldObj.provider.dimensionId != 0) return;
        if (loadedWorld != event.getChunk().worldObj) loaded.clear();
        loadedWorld = event.getChunk().worldObj;
        loaded.add(new int[] { event.getChunk().xPosition, event.getChunk().zPosition });
    }

    /** Carves what waited in the chunks loaded since, a few chunks a tick. */
    public final class Ticker {

        @SubscribeEvent
        public void onWorldTick(cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent event) {
            if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END || event.world != loadedWorld
                || loaded.isEmpty()) return;
            NEWorldSavedData saved = NEWorldSavedData.get(event.world);
            for (int i = 0; i < 16 && !loaded.isEmpty(); i++) {
                int[] c = loaded.poll();
                if (saved != null && event.world.getChunkProvider()
                    .chunkExists(c[0], c[1])) materializePending(event.world, saved, c[0], c[1], 4);
            }
        }
    }

    public int materializePending(World world, NEWorldSavedData saved, int chunkX, int chunkZ, int budget) {
        if (world == null || saved == null || budget <= 0 || world.isRemote) return 0;
        int work = 0;
        for (StarRainImpact impact : saved.p11()
            .awaitingMaterializationInChunk(chunkX, chunkZ, budget)) {
            if (work >= budget) break;
            if (materialize(world, saved, impact, false)) work++;
        }
        return work;
    }

    public boolean materialize(World world, NEWorldSavedData saved, StarRainImpact impact) {
        return materialize(world, saved, impact, true);
    }

    /** True if the host may carve or replace this block: never tile entities, bedrock, parcel hosts or unbreakables. */
    public static boolean carvable(Block block, boolean hasTileEntity, float hardness) {
        if (block == null || hasTileEntity || hardness < 0.0F) return false;
        return block != Blocks.bedrock && !(block instanceof BlockFluidCell)
            && !(block instanceof naturalexceptions.minecraft.block.BlockSkystoneBricks); // built to survive stars
    }

    private static boolean carvable(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return carvable(block, world.getTileEntity(x, y, z) != null, block.getBlockHardness(world, x, y, z));
    }

    private static final class Placement {

        final FluidCellKey key;
        final FluidParcel parcel;
        final Block host;

        Placement(FluidCellKey key, FluidParcel parcel, Block host) {
            this.key = key;
            this.parcel = parcel;
            this.host = host;
        }
    }

    private boolean materialize(World world, NEWorldSavedData saved, StarRainImpact impact,
        boolean applyImmediateEntityConsequence) {
        if (world == null || saved == null
            || impact == null
            || world.isRemote
            || !impact.isResolved()
            || impact.isMaterialized()) return false;
        StarRainCraterPlan plan = StarRainCraterPlan.of(impact.severity(), StarRainCraterPlan.seedOf(impact.id()));
        int x = impact.x();
        int z = impact.z();
        int reach = plan.horizontalReach();
        // One block past the crater too: the blocks there are told of the change.
        for (int cx = (x - reach - 1) >> 4; cx <= (x + reach + 1) >> 4; cx++) {
            for (int cz = (z - reach - 1) >> 4; cz <= (z + reach + 1) >> 4; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) return false;
            }
        }
        int surfaceY = Math.max(plan.depth() + 8, Math.min(240, world.getTopSolidOrLiquidBlock(x, z) - 1));
        FluidWorldState fluids = saved.fluids();

        List<Placement> placements = planBall(world, fluids, impact, plan, x, surfaceY, z);
        if (placements == null) return false;
        List<Placement> placed = new ArrayList<>();
        try {
            for (Placement placement : placements) {
                fluids.put(placement.key, placement.parcel);
                placed.add(placement);
            }
        } catch (RuntimeException refused) {
            for (Placement placement : placed) fluids.remove(placement.key);
            return false;
        }
        saved.markDirty();

        carve(world, plan, x, surfaceY, z);
        for (Placement placement : placements) {
            world.setBlock(
                placement.key.x(),
                placement.key.y(),
                placement.key.z(),
                placement.host,
                BlockFluidCell.metadataFor(placement.parcel),
                3);
        }
        if (saved.p11()
            .transferToSpatialSite(impact.id())
            .isZero()) return false;
        saved.markDirty();

        double centerY = surfaceY + plan.ballCenterDy() + 0.5;
        // A visual and physical blast; it does not break blocks (the crater is already carved).
        world.newExplosion(null, x + 0.5, surfaceY + 1.0, z + 0.5, 2.0F + impact.severity(), false, false);
        world.playSoundEffect(x + 0.5, centerY, z + 0.5, "random.explode", 4.0F, 0.55F);
        notifyNearbyPlayers(world, x, surfaceY, z, impact);
        if (applyImmediateEntityConsequence) scorchNearbyLiving(world, x, surfaceY, z, plan.radius());
        return true;
    }

    /**
     * Crust and core parcels for every ball cell that may be occupied. The core mass is split exactly over the core
     * cells that can be used; if the planned core is blocked, the nearest usable ball cells carry it instead.
     */
    private List<Placement> planBall(World world, FluidWorldState fluids, StarRainImpact impact,
        StarRainCraterPlan plan, int x, int surfaceY, int z) {
        List<FluidCellKey> core = new ArrayList<>();
        List<FluidCellKey> crust = new ArrayList<>();
        int dimension = world.provider.dimensionId;
        for (StarRainCraterPlan.Cell cell : plan.cells()) {
            if (cell.role() != StarRainCraterPlan.Role.CORE && cell.role() != StarRainCraterPlan.Role.CRUST) continue;
            int bx = x + cell.dx();
            int by = surfaceY + cell.dy();
            int bz = z + cell.dz();
            if (by < 1 || by > 254) continue;
            FluidCellKey key = new FluidCellKey(dimension, bx, by, bz);
            if (fluids.parcelAt(key) != null || !world.isAirBlock(bx, by, bz) && !carvable(world, bx, by, bz)) continue;
            (cell.role() == StarRainCraterPlan.Role.CORE ? core : crust).add(key);
        }
        if (core.isEmpty()) {
            if (crust.isEmpty()) return null;
            core.add(crust.remove(0));
        }
        List<Placement> placements = new ArrayList<>();
        MaterialQuantity total = impact.stellarMass();
        long[] shares = StarRainCraterPlan.splitMilligrams(total.milligrams(), core.size());
        for (int i = 0; i < core.size(); i++) {
            if (shares[i] <= 0L) continue;
            placements.add(
                new Placement(
                    core.get(i),
                    coreParcel(impact, MaterialQuantity.ofMilligrams(shares[i])),
                    NEBlocks.fallenStarCell));
        }
        for (FluidCellKey key : crust) {
            placements.add(new Placement(key, crustParcel(impact), NEBlocks.impactCrust));
        }
        return placements;
    }

    private static FluidParcel coreParcel(StarRainImpact impact, MaterialQuantity mass) {
        MaterialRef ref = BuiltinMaterials.STELLAR_MATTER.ref();
        CanonicalMaterialKey key = CanonicalMaterialKey
            .of(BuiltinMaterials.STELLAR_MATTER_COMPOSITION, MaterialPhase.LIQUID, null, null);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(CORE_TEMPERATURE_K, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        return new FluidParcel(ref, key, BuiltinMaterials.STELLAR_MATTER_COMPOSITION, mass, thermal, impact.id());
    }

    static FluidParcel crustParcel(StarRainImpact impact) {
        MaterialQuantity mass = MaterialQuantity
            .ofKilograms(BuiltinMaterials.IMPACT_CRUST_PROPERTIES.density() * CRUST_FILL);
        CanonicalMaterialKey key = CanonicalMaterialKey
            .of(BuiltinMaterials.IMPACT_CRUST_COMPOSITION, MaterialPhase.SOLID, null, null);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(CRUST_TEMPERATURE_K, 0.0, mass.toKilograms(), BuiltinMaterials.IMPACT_CRUST_PROPERTIES);
        return new FluidParcel(
            BuiltinMaterials.IMPACT_CRUST.ref(),
            key,
            BuiltinMaterials.IMPACT_CRUST_COMPOSITION,
            mass,
            thermal,
            impact.id());
    }

    /** Clears the bowl, scorches its floor and throws ejecta onto the rim. Tile entities are never touched. */
    private static void carve(World world, StarRainCraterPlan plan, int x, int surfaceY, int z) {
        for (StarRainCraterPlan.Cell cell : plan.cells()) {
            int bx = x + cell.dx();
            int by = surfaceY + cell.dy();
            int bz = z + cell.dz();
            if (by < 1 || by > 254) continue;
            if (cell.role() == StarRainCraterPlan.Role.CLEAR) {
                if (!world.isAirBlock(bx, by, bz) && carvable(world, bx, by, bz)) world.setBlockToAir(bx, by, bz);
            } else if (cell.role() == StarRainCraterPlan.Role.LINING) {
                if (!world.isAirBlock(bx, by, bz) && carvable(world, bx, by, bz)) {
                    setLining(world, bx, by, bz, cell.variant());
                }
            }
        }
        for (StarRainCraterPlan.RimColumn column : plan.rim()) {
            int bx = x + column.dx();
            int bz = z + column.dz();
            int top = world.getHeightValue(bx, bz);
            if (top < 2 || top > 250 || !world.isAirBlock(bx, top, bz)) continue;
            Block ejecta = column.variant() == 0 ? Blocks.gravel
                : column.variant() == 1 ? Blocks.cobblestone : Blocks.dirt;
            world.setBlock(bx, top, bz, ejecta, column.variant() == 2 ? 1 : 0, 3);
            if (column.variant() == 2 && Math.floorMod(column.dx() * 7 + column.dz() * 13, 6) == 0
                && world.isAirBlock(bx, top + 1, bz)) {
                // Smouldering ejecta: the block just placed below is solid earth, so fire can sit on it.
                world.setBlock(bx, top + 1, bz, Blocks.fire, 0, 3);
            }
        }
    }

    private static void setLining(World world, int x, int y, int z, int variant) {
        switch (variant) {
            case 1:
                world.setBlock(x, y, z, Blocks.gravel, 0, 3);
                break;
            case 2:
                world.setBlock(x, y, z, Blocks.cobblestone, 0, 3);
                break;
            case 3:
                world.setBlock(x, y, z, Blocks.obsidian, 0, 3);
                break;
            default:
                world.setBlock(x, y, z, Blocks.dirt, 1, 3);
                break;
        }
    }

    /** Anything caught near the ball is set alight briefly on top of the blast's physical hit. */
    private static void scorchNearbyLiving(World world, int x, int y, int z, int radius) {
        double reach = radius + 2.0;
        net.minecraft.util.AxisAlignedBB bounds = net.minecraft.util.AxisAlignedBB
            .getBoundingBox(x - reach, y - reach, z - reach, x + 1 + reach, y + 1 + reach, z + 1 + reach);
        for (Object object : world.getEntitiesWithinAABB(EntityLivingBase.class, bounds)) {
            EntityLivingBase living = (EntityLivingBase) object;
            double distance = Math.sqrt(living.getDistanceSq(x + 0.5, y + 0.5, z + 0.5));
            if (distance > reach) continue;
            living.setFire((int) Math.max(1, Math.round(6.0 * (1.0 - distance / reach))));
        }
    }

    private static void notifyNearbyPlayers(World world, int x, int y, int z, StarRainImpact impact) {
        double radius = 48.0D;
        net.minecraft.util.AxisAlignedBB bounds = net.minecraft.util.AxisAlignedBB
            .getBoundingBox(x - radius, y - radius, z - radius, x + 1 + radius, y + 1 + radius, z + 1 + radius);
        for (Object object : world.getEntitiesWithinAABB(net.minecraft.entity.player.EntityPlayer.class, bounds)) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) object;
            if (player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= radius * radius) {
                player.addChatMessage(
                    new net.minecraft.util.ChatComponentText(StarRainPresentation.impactMessage(impact)));
            }
        }
    }
}
