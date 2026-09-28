package naturalexceptions.core.event;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** A time-bounded influence contributed by an active event to shared context queries. */
public final class EventInfluence {
    private final DeterministicId eventId;
    private final ResourceId eventType;
    private final ResourceId influence;
    private final EventRegion targetRegion;
    private final int strength;
    private final long startsAtTick;
    private final long endsAtTick;

    EventInfluence(EventRecord record) {
        this.eventId = record.id();
        this.eventType = record.type();
        this.influence = record.temporalInfluence();
        this.targetRegion = record.targetRegion();
        this.strength = record.temporalInfluenceStrength();
        this.startsAtTick = record.scheduledAtTick();
        this.endsAtTick = record.endsAtTick();
    }

    public DeterministicId eventId() { return eventId; }
    public ResourceId eventType() { return eventType; }
    public ResourceId influence() { return influence; }
    public EventRegion targetRegion() { return targetRegion; }
    public int strength() { return strength; }
    public long startsAtTick() { return startsAtTick; }
    public long endsAtTick() { return endsAtTick; }
}
