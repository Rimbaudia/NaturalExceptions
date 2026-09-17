package naturalexceptions.core.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

final class BoundedWorkQueueTest {

    @Test
    void processes100ItemsInBatchesOf7Deterministically() {
        BoundedWorkQueue<Integer> queue = new BoundedWorkQueue<>();
        for (int i = 0; i < 100; i++) {
            queue.enqueue(i);
        }

        assertEquals(100, queue.size());
        assertFalse(queue.isEmpty());

        List<Integer> processed = new ArrayList<>();
        int drainCalls = 0;

        while (!queue.isEmpty()) {
            drainCalls++;
            int drainedCount = queue.drain(7, processed::add);
            if (!queue.isEmpty()) {
                assertEquals(7, drainedCount, "Each intermediate batch must drain exactly the budget");
            } else {
                assertEquals(2, drainedCount, "Final batch must drain remaining items (100 % 7 = 2)");
            }
        }

        // 100 / 7 = 14 full batches + 1 remainder batch = 15 batches total
        assertEquals(15, drainCalls);
        assertEquals(100, processed.size());
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());

        // Verify exact FIFO ordering with no loss and no duplication
        for (int i = 0; i < 100; i++) {
            assertEquals(Integer.valueOf(i), processed.get(i));
        }
    }

    @Test
    void zeroOrNegativeBudgetDoesZeroWork() {
        BoundedWorkQueue<String> queue = new BoundedWorkQueue<>();
        queue.enqueue("task-1");
        queue.enqueue("task-2");

        List<String> processed = new ArrayList<>();
        int drainedZero = queue.drain(0, processed::add);
        int drainedNeg = queue.drain(-5, processed::add);

        assertEquals(0, drainedZero);
        assertEquals(0, drainedNeg);
        assertEquals(2, queue.size());
        assertTrue(processed.isEmpty());
    }

    @Test
    void emptyQueueDoesZeroWork() {
        BoundedWorkQueue<String> queue = new BoundedWorkQueue<>();
        List<String> processed = new ArrayList<>();
        int drained = queue.drain(10, processed::add);

        assertEquals(0, drained);
        assertTrue(queue.isEmpty());
        assertTrue(processed.isEmpty());
    }

    @Test
    void rejectsNullElementsAndNullProcessor() {
        BoundedWorkQueue<String> queue = new BoundedWorkQueue<>();
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));

        queue.enqueue("valid");
        assertThrows(IllegalArgumentException.class, () -> queue.drain(5, null));
    }
}
