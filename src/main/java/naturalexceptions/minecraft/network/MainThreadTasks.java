package naturalexceptions.minecraft.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Work handed from a packet handler to the server's main thread. In 1.7.10, SimpleNetworkWrapper runs server-side
 * handlers on the network (Netty) thread, so a handler that changes the world, an entity, an inventory or a tile entity
 * must not do it there: it races the game tick (lost or duplicated items, ConcurrentModificationException). Handlers
 * read their message, then {@link #submit} the change, which runs at the end of the next server tick.
 */
public final class MainThreadTasks {

    private static final int MAX_PENDING = 256;
    private static final int MAX_PER_TICK = 64;
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger PENDING = new AtomicInteger();
    /** How much one player may have waiting at once, so a flooding client can't crowd everyone else out. */
    static final int MAX_PER_PLAYER = 8;
    private static final java.util.concurrent.ConcurrentHashMap<Object, AtomicInteger> BY_PLAYER = new java.util.concurrent.ConcurrentHashMap<>();

    /** Work for {@code player} (any key standing for them), turned away while they already have enough waiting. */
    public static boolean submit(Object player, final Runnable task) {
        if (player == null) return submit(task);
        if (task == null) return false;
        final AtomicInteger mine = BY_PLAYER.computeIfAbsent(player, p -> new AtomicInteger());
        if (mine.incrementAndGet() > MAX_PER_PLAYER) {
            mine.decrementAndGet();
            return false;
        }
        boolean queued = submit(() -> {
            mine.decrementAndGet();
            task.run();
        });
        if (!queued) mine.decrementAndGet();
        return queued;
    }

    public static boolean submit(Runnable task) {
        if (task == null) return false;
        while (true) {
            int count = PENDING.get();
            if (count >= MAX_PENDING) return false;
            if (PENDING.compareAndSet(count, count + 1)) break;
        }
        TASKS.add(task);
        return true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < MAX_PER_TICK; i++) {
            Runnable task = TASKS.poll();
            if (task == null) break;
            PENDING.decrementAndGet();
            try {
                task.run();
            } catch (RuntimeException e) {
                cpw.mods.fml.common.FMLLog.warning("Natural Exceptions main-thread task failed: %s", e.toString());
            }
        }
    }

    public MainThreadTasks() {}
}
