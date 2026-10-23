package naturalexceptions.minecraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cpw.mods.fml.common.gameevent.TickEvent;

final class MainThreadTasksTest {

    private final MainThreadTasks ticker = new MainThreadTasks();

    /** Runs whatever an earlier test left queued, so each test starts empty. */
    @BeforeEach
    void drain() {
        for (int i = 0; i < 10; i++) tick();
    }

    private void tick() {
        ticker.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }

    @Test
    void workRunsOnTheServerTickNotWhenSubmitted() {
        List<String> ran = new ArrayList<>();
        assertTrue(MainThreadTasks.submit(() -> ran.add("a")));
        assertTrue(ran.isEmpty(), "a packet handler's work must wait for the main thread");
        tick();
        assertEquals(1, ran.size());
    }

    @Test
    void workRunsInTheOrderItCame() {
        List<Integer> ran = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int n = i;
            MainThreadTasks.submit(() -> ran.add(n));
        }
        tick();
        assertEquals(java.util.Arrays.asList(0, 1, 2, 3, 4), ran);
    }

    @Test
    void theStartOfATickRunsNothing() {
        List<String> ran = new ArrayList<>();
        MainThreadTasks.submit(() -> ran.add("a"));
        ticker.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertTrue(ran.isEmpty());
        tick();
        assertEquals(1, ran.size());
    }

    @Test
    void aFloodIsTurnedAwayRatherThanQueuedWithoutEnd() {
        int accepted = 0;
        for (int i = 0; i < 1000; i++) if (MainThreadTasks.submit(() -> {})) accepted++;
        assertEquals(256, accepted);
        assertFalse(MainThreadTasks.submit(() -> {}));
    }

    @Test
    void oneFailingTaskDoesNotStopTheRest() {
        List<String> ran = new ArrayList<>();
        MainThreadTasks.submit(() -> { throw new IllegalStateException("boom"); });
        MainThreadTasks.submit(() -> ran.add("after"));
        tick();
        assertEquals(1, ran.size());
    }
}
