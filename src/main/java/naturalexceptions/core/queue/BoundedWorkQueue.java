package naturalexceptions.core.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public final class BoundedWorkQueue<T> {

    private final Deque<T> queue = new ArrayDeque<>();

    public synchronized void enqueue(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null item");
        }
        queue.addLast(item);
    }

    public synchronized void enqueueAll(Collection<? extends T> items) {
        if (items != null) {
            for (T item : items) {
                enqueue(item);
            }
        }
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int drain(int unitBudget, Consumer<? super T> processor) {
        if (unitBudget <= 0 || queue.isEmpty()) {
            return 0;
        }
        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }
        int processed = 0;
        while (processed < unitBudget && !queue.isEmpty()) {
            T item = queue.pollFirst();
            processor.accept(item);
            processed++;
        }
        return processed;
    }

    public synchronized List<T> drain(int unitBudget) {
        List<T> list = new ArrayList<>();
        drain(unitBudget, list::add);
        return list;
    }

    public synchronized List<T> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    public synchronized void clear() {
        queue.clear();
    }
}
