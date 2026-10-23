package naturalexceptions.minecraft.network;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Bounded handoff from Forge 1.7 SimpleImpl's Netty handlers to the server tick thread. */
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
