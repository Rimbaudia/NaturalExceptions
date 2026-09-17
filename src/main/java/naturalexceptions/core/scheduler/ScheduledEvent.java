package naturalexceptions.core.scheduler;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

public final class ScheduledEvent implements Comparable<ScheduledEvent> {

    private final DeterministicId id;
    private final ResourceId eventType;
    private final long dueTick;
    private final String payload;

    public ScheduledEvent(DeterministicId id, ResourceId eventType, long dueTick, String payload) {
        if (id == null) {
            throw new IllegalArgumentException("Event id cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("EventType cannot be null");
        }
        if (dueTick < 0) {
            throw new IllegalArgumentException("Due tick cannot be negative: " + dueTick);
        }
        this.id = id;
        this.eventType = eventType;
        this.dueTick = dueTick;
        this.payload = payload != null ? payload : "";
    }

    public DeterministicId id() {
        return id;
    }

    public ResourceId eventType() {
        return eventType;
    }

    public long dueTick() {
        return dueTick;
    }

    public String payload() {
        return payload;
    }

    @Override
    public int compareTo(ScheduledEvent other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare ScheduledEvent to null");
        }
        int cmp = Long.compare(this.dueTick, other.dueTick);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.id.compareTo(other.id);
        if (cmp != 0) {
            return cmp;
        }
        return this.eventType.compareTo(other.eventType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledEvent)) return false;
        ScheduledEvent that = (ScheduledEvent) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ScheduledEvent{" + "id="
            + id
            + ", eventType="
            + eventType
            + ", dueTick="
            + dueTick
            + ", payload='"
            + payload
            + '\''
            + '}';
    }
}
