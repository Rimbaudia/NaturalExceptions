package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Detached persistence-ready event and cooldown state. */
public final class EventScheduleSnapshot {
    private final List<EventRecord> events;
    private final List<EventCooldownHistory> cooldownHistory;
    private final long lastAdvancedTick;

    public EventScheduleSnapshot(List<EventRecord> events, List<EventCooldownHistory> cooldownHistory,
        long lastAdvancedTick) {
        if (events == null || cooldownHistory == null || lastAdvancedTick < 0) {
            throw new IllegalArgumentException("Snapshot fields are required and time must be nonnegative");
        }
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.cooldownHistory = Collections.unmodifiableList(new ArrayList<>(cooldownHistory));
        this.lastAdvancedTick = lastAdvancedTick;
    }

    public List<EventRecord> events() { return events; }
    public List<EventCooldownHistory> cooldownHistory() { return cooldownHistory; }
    public long lastAdvancedTick() { return lastAdvancedTick; }
}
