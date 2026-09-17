package naturalexceptions.core.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import naturalexceptions.core.id.DeterministicId;

public final class DueTimeScheduler {

    private final PriorityQueue<ScheduledEvent> queue = new PriorityQueue<>();
    private final Map<DeterministicId, ScheduledEvent> indexedById = new HashMap<>();

    public synchronized void schedule(ScheduledEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot schedule null event");
        }
        if (indexedById.containsKey(event.id())) {
            throw new IllegalArgumentException("Event already scheduled: " + event.id());
        }
        queue.add(event);
        indexedById.put(event.id(), event);
    }

    public synchronized boolean cancel(DeterministicId id) {
        if (id == null) {
            return false;
        }
        ScheduledEvent existing = indexedById.remove(id);
        if (existing != null) {
            queue.remove(existing);
            return true;
        }
        return false;
    }

    public synchronized boolean contains(DeterministicId id) {
        return id != null && indexedById.containsKey(id);
    }

    public synchronized int pendingCount() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized long peekNextDueTick() {
        ScheduledEvent next = queue.peek();
        return next != null ? next.dueTick() : -1L;
    }

    public synchronized List<ScheduledEvent> pollDueEvents(long currentTick) {
        List<ScheduledEvent> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek()
            .dueTick() <= currentTick) {
            ScheduledEvent event = queue.poll();
            indexedById.remove(event.id());
            due.add(event);
        }
        return due;
    }

    public synchronized List<ScheduledEvent> snapshot() {
        List<ScheduledEvent> list = new ArrayList<>(queue);
        Collections.sort(list);
        return Collections.unmodifiableList(list);
    }

    public synchronized void restore(Collection<ScheduledEvent> events) {
        clear();
        if (events != null) {
            for (ScheduledEvent event : events) {
                schedule(event);
            }
        }
    }

    public synchronized void clear() {
        queue.clear();
        indexedById.clear();
    }
}
