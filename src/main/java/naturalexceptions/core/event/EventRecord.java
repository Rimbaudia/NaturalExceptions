package naturalexceptions.core.event;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Immutable persisted semantic state for a scheduled event instance. */
public final class EventRecord {

    public static final ResourceId PROTECTED_STAR_RAIN = ResourceId.of("naturalexceptions", "star_rain");

    private final DeterministicId id;
    private final ResourceId type;
    private final EventRegion targetRegion;
    private final boolean protectedOpportunity;
    private final long scheduledAtTick;
    private final long endsAtTick;
    private final EventPhase phase;
    private final long phaseChangedAtTick;
    private final long parameterSeed;
    private final long cooldownUntilTick;
    private final ResourceId temporalInfluence;
    private final int temporalInfluenceStrength;

    /** Full semantic constructor for persistence codecs and deterministic factories. */
    public EventRecord(DeterministicId id, ResourceId type, EventRegion targetRegion,
        boolean protectedOpportunity, long scheduledAtTick, long endsAtTick, EventPhase phase,
        long phaseChangedAtTick, long parameterSeed, long cooldownUntilTick,
        ResourceId temporalInfluence, int temporalInfluenceStrength) {
        if (id == null || type == null || targetRegion == null || phase == null) {
            throw new IllegalArgumentException("Event identity, type, target, and phase are required");
        }
        if (scheduledAtTick < 0 || endsAtTick <= scheduledAtTick || phaseChangedAtTick < scheduledAtTick
            || cooldownUntilTick < endsAtTick) {
            throw new IllegalArgumentException("Invalid event timeline");
        }
        if (protectedOpportunity && !PROTECTED_STAR_RAIN.equals(type)) {
            throw new IllegalArgumentException("Only the first Star Rain may be a protected opportunity");
        }
        if ((temporalInfluence == null) != (temporalInfluenceStrength == 0)) {
            throw new IllegalArgumentException("Temporal influence and nonzero strength must be specified together");
        }
        if (phase == EventPhase.SCHEDULED && phaseChangedAtTick != scheduledAtTick) {
            throw new IllegalArgumentException("Scheduled event transition time must equal its schedule time");
        }
        if (phase == EventPhase.ACTIVE && (phaseChangedAtTick < scheduledAtTick || phaseChangedAtTick >= endsAtTick)) {
            throw new IllegalArgumentException("Active event transition must be inside its active interval");
        }
        if (phase == EventPhase.COMPLETED && phaseChangedAtTick < endsAtTick) {
            throw new IllegalArgumentException("Completed event transition cannot precede its end");
        }
        this.id = id;
        this.type = type;
        this.targetRegion = targetRegion;
        this.protectedOpportunity = protectedOpportunity;
        this.scheduledAtTick = scheduledAtTick;
        this.endsAtTick = endsAtTick;
        this.phase = phase;
        this.phaseChangedAtTick = phaseChangedAtTick;
        this.parameterSeed = parameterSeed;
        this.cooldownUntilTick = cooldownUntilTick;
        this.temporalInfluence = temporalInfluence;
        this.temporalInfluenceStrength = temporalInfluenceStrength;
    }

    public DeterministicId id() { return id; }
    public ResourceId type() { return type; }
    public EventRegion targetRegion() { return targetRegion; }
    public boolean protectedOpportunity() { return protectedOpportunity; }
    public long scheduledAtTick() { return scheduledAtTick; }
    public long endsAtTick() { return endsAtTick; }
    public EventPhase phase() { return phase; }
    public long phaseChangedAtTick() { return phaseChangedAtTick; }
    public long parameterSeed() { return parameterSeed; }
    public long cooldownUntilTick() { return cooldownUntilTick; }
    public ResourceId temporalInfluence() { return temporalInfluence; }
    public int temporalInfluenceStrength() { return temporalInfluenceStrength; }
    public boolean hasTemporalInfluence() { return temporalInfluence != null; }

    EventRecord transitionTo(EventPhase next, long atTick) {
        if (next == phase) return this;
        if (phase == EventPhase.COMPLETED || next.ordinal() < phase.ordinal()) return this;
        return new EventRecord(id, type, targetRegion, protectedOpportunity, scheduledAtTick, endsAtTick, next,
            atTick, parameterSeed, cooldownUntilTick, temporalInfluence, temporalInfluenceStrength);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EventRecord)) return false;
        EventRecord that = (EventRecord) other;
        return id.equals(that.id) && type.equals(that.type) && targetRegion.equals(that.targetRegion)
            && protectedOpportunity == that.protectedOpportunity && scheduledAtTick == that.scheduledAtTick
            && endsAtTick == that.endsAtTick && phase == that.phase
            && phaseChangedAtTick == that.phaseChangedAtTick && parameterSeed == that.parameterSeed
            && cooldownUntilTick == that.cooldownUntilTick
            && Objects.equals(temporalInfluence, that.temporalInfluence)
            && temporalInfluenceStrength == that.temporalInfluenceStrength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, targetRegion, protectedOpportunity, scheduledAtTick, endsAtTick, phase,
            phaseChangedAtTick, parameterSeed, cooldownUntilTick, temporalInfluence, temporalInfluenceStrength);
    }
}
