package naturalexceptions.core.event;

import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

/** Compact last-occurrence/cooldown summary retained after old completed instances are pruned. */
public final class EventCooldownHistory implements Comparable<EventCooldownHistory> {
    private final ResourceId type;
    private final EventRegion region;
    private final long lastOccurrenceTick;
    private final long cooldownUntilTick;

    public EventCooldownHistory(ResourceId type, EventRegion region, long lastOccurrenceTick, long cooldownUntilTick) {
        if (type == null || region == null || lastOccurrenceTick < 0 || cooldownUntilTick < lastOccurrenceTick) {
            throw new IllegalArgumentException("Invalid event cooldown history");
        }
        this.type = type;
        this.region = region;
        this.lastOccurrenceTick = lastOccurrenceTick;
        this.cooldownUntilTick = cooldownUntilTick;
    }

    public ResourceId type() { return type; }
    public EventRegion region() { return region; }
    public long lastOccurrenceTick() { return lastOccurrenceTick; }
    public long cooldownUntilTick() { return cooldownUntilTick; }

    @Override public int compareTo(EventCooldownHistory other) {
        int c = type.compareTo(other.type);
        if (c != 0) return c;
        return region.compareTo(other.region);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EventCooldownHistory)) return false;
        EventCooldownHistory that = (EventCooldownHistory) other;
        return lastOccurrenceTick == that.lastOccurrenceTick && cooldownUntilTick == that.cooldownUntilTick
            && type.equals(that.type) && region.equals(that.region);
    }

    @Override public int hashCode() { return Objects.hash(type, region, lastOccurrenceTick, cooldownUntilTick); }
}
