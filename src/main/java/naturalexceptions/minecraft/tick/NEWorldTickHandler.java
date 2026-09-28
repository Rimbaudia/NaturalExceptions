package naturalexceptions.minecraft.tick;

import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.queue.BoundedWorkQueue;
import naturalexceptions.core.scheduler.ScheduledEvent;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.event.StarRainWorldMaterializer;
import naturalexceptions.core.event.StarRainImpact;

public final class NEWorldTickHandler {

    private static final Logger LOGGER = LogManager.getLogger("NaturalExceptions");
    public static final ResourceId SUBSYSTEM_SCHEDULER = ResourceId.of("naturalexceptions", "subsystem/scheduler");
    public static final ResourceId SUBSYSTEM_FLUIDS = ResourceId.of("naturalexceptions", "subsystem/fluids");
    public static final ResourceId SUBSYSTEM_ELECTRICAL = ResourceId.of("naturalexceptions", "subsystem/electrical");
    public static final ResourceId SUBSYSTEM_STAR_RAIN = ResourceId.of("naturalexceptions", "subsystem/star_rain");

    private final SubsystemProfiler profiler;
    private final BoundedWorkQueue<ScheduledEvent> workQueue = new BoundedWorkQueue<>();
    private final int perTickBudget;
    private final Consumer<ScheduledEvent> eventConsumer;
    private long lastElectricalSolves;
    private long lastElectricalThermalWork;
    private final StarRainWorldMaterializer starRainMaterializer = new StarRainWorldMaterializer();

    public NEWorldTickHandler(SubsystemProfiler profiler, int perTickBudget, Consumer<ScheduledEvent> eventConsumer) {
        this.profiler = profiler != null ? profiler : new SubsystemProfiler();
        this.perTickBudget = perTickBudget > 0 ? perTickBudget : 64;
        this.eventConsumer = eventConsumer != null ? eventConsumer : this::defaultHandleEvent;
    }

    public NEWorldTickHandler(SubsystemProfiler profiler, int perTickBudget) {
        this(profiler, perTickBudget, null);
    }

    public NEWorldTickHandler() {
        this(new SubsystemProfiler(), 64, null);
    }

    public SubsystemProfiler profiler() {
        return profiler;
    }

    public BoundedWorkQueue<ScheduledEvent> workQueue() {
        return workQueue;
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.world == null || event.world.isRemote || event.world.provider.dimensionId != 0) {
            return;
        }

        NEWorldSavedData data = NEWorldSavedData.get(event.world);
        if (data == null) {
            return;
        }

        long worldTime = event.world.getTotalWorldTime();
        tickWorld(data, worldTime, event.world);
    }

    public void tickWorld(NEWorldSavedData data, long worldTime) {
        tickWorld(data, worldTime, null);
    }

    /** Runs the ordinary server-side domain path at a simulated time for operator-only acceptance tooling. */
    public void advanceForDevelopment(NEWorldSavedData data, net.minecraft.world.World world, long simulatedTime) {
        if (data == null || world == null || world.isRemote || world.provider.dimensionId != 0
            || simulatedTime < 0L) return;
        tickWorld(data, simulatedTime, world);
    }

    private void tickWorld(NEWorldSavedData data, long worldTime, net.minecraft.world.World world) {
        long startNanos = System.nanoTime();

        long fluidStartNanos = System.nanoTime();
        int fluidWork = 0;
        if (data.fluids().dirtyCellCount() > 0) {
            fluidWork = data.fluids().processDirty(perTickBudget);
            if (fluidWork > 0) data.markDirty();
            profiler.incrementCounter(SUBSYSTEM_FLUIDS, "region_cells_rebuilt", fluidWork);
        }
        java.util.function.Predicate<naturalexceptions.core.fluid.FluidCellKey> loadedFluidCell = key -> world == null
            || world.getChunkProvider().chunkExists(key.x() >> 4, key.z() >> 4);
        int thermalFluidWork = data.fluids().activeThermalCellCount() > 0
            ? data.fluids().processThermalWork(0.05, perTickBudget, loadedFluidCell) : 0;
        if (thermalFluidWork > 0) data.markDirty();
        int fluidHostsRefreshed = 0;
        if (world != null) {
            for (naturalexceptions.core.fluid.FluidCellKey key : data.fluids()
                .drainHostPhaseChanges(perTickBudget, loadedFluidCell)) {
                naturalexceptions.core.fluid.FluidParcel parcel = data.fluids().parcelAt(key);
                if (parcel != null) {
                    naturalexceptions.minecraft.fluid.BlockFluidCell.refreshHost(world, key, parcel);
                    fluidHostsRefreshed++;
                }
            }
        }
        profiler.incrementCounter(SUBSYSTEM_FLUIDS, "thermal_coupling_work", thermalFluidWork);
        profiler.incrementCounter(SUBSYSTEM_FLUIDS, "host_phase_updates", fluidHostsRefreshed);
        profiler.setActiveCount(SUBSYSTEM_FLUIDS, data.fluids().dirtyCellCount()
            + data.fluids().activeThermalCellCount());
        profiler.setQueuedWork(SUBSYSTEM_FLUIDS, data.fluids().dirtyCellCount());
        profiler.setSleepingCount(SUBSYSTEM_FLUIDS, data.fluids().sleepingRegionCount());
        profiler.recordElapsedNanos(SUBSYSTEM_FLUIDS, "region_and_thermal_work", System.nanoTime() - fluidStartNanos);

        long electricalStartNanos = System.nanoTime();
        int electricalWork = data.electrical().processWork(0.05, perTickBudget);
        if (electricalWork > 0) data.markDirty();
        profiler.incrementCounter(SUBSYSTEM_ELECTRICAL, "solver_and_thermal_work", electricalWork);
        long solves = data.electrical().solveWork();
        long thermal = data.electrical().thermalWork();
        profiler.incrementCounter(SUBSYSTEM_ELECTRICAL, "solves", Math.max(0L, solves - lastElectricalSolves));
        profiler.incrementCounter(SUBSYSTEM_ELECTRICAL, "dirty_topologies_solved",
            Math.max(0L, solves - lastElectricalSolves));
        profiler.incrementCounter(SUBSYSTEM_ELECTRICAL, "thermal_coupling", Math.max(0L, thermal - lastElectricalThermalWork));
        lastElectricalSolves = solves;
        lastElectricalThermalWork = thermal;
        profiler.setActiveCount(SUBSYSTEM_ELECTRICAL, data.electrical().activeNetworkCount());
        profiler.setSleepingCount(SUBSYSTEM_ELECTRICAL, data.electrical().sleepingNetworkCount());
        profiler.setQueuedWork(SUBSYSTEM_ELECTRICAL, data.electrical().queuedWorkCount());
        profiler.recordElapsedNanos(SUBSYSTEM_ELECTRICAL, "network_work", System.nanoTime() - electricalStartNanos);

        java.util.Set<naturalexceptions.core.id.DeterministicId> starRainStarts = new java.util.LinkedHashSet<>();
        if (data.p8() != null) {
            for (naturalexceptions.core.event.EventRecord record : data.p8().events().snapshot().events()) {
                if (naturalexceptions.core.event.EventRecord.PROTECTED_STAR_RAIN.equals(record.type())
                    && record.phase() == naturalexceptions.core.event.EventPhase.SCHEDULED) starRainStarts.add(record.id());
            }
        }
        boolean p8Changed = data.p8() != null && data.p8().advance(worldTime);
        if (p8Changed) {
            data.markDirty();
            profiler.incrementCounter(SUBSYSTEM_SCHEDULER, "p8_state_transitions", 1L);
        }
        if (world != null && !starRainStarts.isEmpty()) {
            for (naturalexceptions.core.event.EventRecord record : data.p8().events().snapshot().events()) {
                if (!starRainStarts.contains(record.id()) || record.phase() != naturalexceptions.core.event.EventPhase.ACTIVE)
                    continue;
                String cue = naturalexceptions.minecraft.event.StarRainPresentation.eventStartMessage(record);
                if (cue != null) for (net.minecraft.entity.player.EntityPlayer player : world.playerEntities) {
                    player.addChatMessage(new net.minecraft.util.ChatComponentText(cue));
                }
            }
        }

        if (data.p8() != null) {
            net.minecraft.util.ChunkCoordinates spawn = world != null ? world.getSpawnPoint() : null;
            int centerX = spawn != null ? spawn.posX : 0;
            int centerZ = spawn != null ? spawn.posZ : 0;
            if (data.p11().synchronize(data.p8().events().snapshot(), centerX, centerZ)) data.markDirty();
        }

        long starRainStartNanos = System.nanoTime();
        int impactsResolved = 0;
        int sitesMaterialized = 0;
        if (data.p11().nextDueTick() != -1L && data.p11().nextDueTick() <= worldTime) {
            for (StarRainImpact impact : data.p11().dueImpacts(worldTime, 2)) {
                if (data.p11().resolve(impact.id())) {
                    impactsResolved++;
                    if (world != null && starRainMaterializer.materialize(world, data, impact)) sitesMaterialized++;
                }
            }
            if (impactsResolved > 0) data.markDirty();
        }
        profiler.incrementCounter(SUBSYSTEM_STAR_RAIN, "impacts_resolved", impactsResolved);
        profiler.incrementCounter(SUBSYSTEM_STAR_RAIN, "sites_materialized", sitesMaterialized);
        profiler.setQueuedWork(SUBSYSTEM_STAR_RAIN, data.p11().pendingCount()
            + data.p11().awaitingMaterializationCount());
        profiler.setActiveCount(SUBSYSTEM_STAR_RAIN, data.p11().pendingCount());
        profiler.setSleepingCount(SUBSYSTEM_STAR_RAIN, data.p11().impactCount()
            - data.p11().pendingCount() - data.p11().awaitingMaterializationCount());
        profiler.recordElapsedNanos(SUBSYSTEM_STAR_RAIN, "due_impact_work", System.nanoTime() - starRainStartNanos);

        // 1. Check if scheduler has any events due
        long nextDue = data.scheduler()
            .peekNextDueTick();
        if (nextDue != -1L && nextDue <= worldTime) {
            List<ScheduledEvent> dueEvents = data.scheduler()
                .pollDueEvents(worldTime);
            workQueue.enqueueAll(dueEvents);
            data.markDirty();
        }

        // 2. Drain bounded work queue with per-tick budget
        if (!workQueue.isEmpty()) {
            int drained = workQueue.drain(perTickBudget, eventConsumer);
            profiler.incrementCounter(SUBSYSTEM_SCHEDULER, "events_processed", drained);
            data.markDirty();
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        profiler.recordElapsedNanos(SUBSYSTEM_SCHEDULER, "tick", elapsedNanos);
        profiler.setSleepingCount(
            SUBSYSTEM_SCHEDULER,
            data.scheduler()
                .pendingCount());
        profiler.setQueuedWork(SUBSYSTEM_SCHEDULER, workQueue.size());
    }

    private void defaultHandleEvent(ScheduledEvent event) {
        LOGGER.info(
            "Natural Exceptions dispatched scheduled event: id={}, type={}, dueTick={}",
            event.id(),
            event.eventType(),
            event.dueTick());
    }
}
