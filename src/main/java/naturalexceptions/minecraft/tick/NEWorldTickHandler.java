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

public final class NEWorldTickHandler {

    private static final Logger LOGGER = LogManager.getLogger("NaturalExceptions");
    public static final ResourceId SUBSYSTEM_SCHEDULER = ResourceId.of("naturalexceptions", "subsystem/scheduler");

    private final SubsystemProfiler profiler;
    private final BoundedWorkQueue<ScheduledEvent> workQueue = new BoundedWorkQueue<>();
    private final int perTickBudget;
    private final Consumer<ScheduledEvent> eventConsumer;

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
        tickWorld(data, worldTime);
    }

    public void tickWorld(NEWorldSavedData data, long worldTime) {
        long startNanos = System.nanoTime();

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
