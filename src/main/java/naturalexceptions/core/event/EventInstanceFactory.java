package naturalexceptions.core.event;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Deterministic factories for event identity and authored scheduling guarantees. */
public final class EventInstanceFactory {

    private static final String ID_DOMAIN = "naturalexceptions:p8:event:v1";
    private static final String STAR_RAIN_SLOT = "protected:first-star-rain";

    private EventInstanceFactory() { }

    public static EventRecord regular(long worldSeed, int worldgenEpoch, ResourceId type, EventRegion targetRegion,
        long ordinal, long scheduledAtTick, long durationTicks, long cooldownTicks,
        ResourceId temporalInfluence, int temporalInfluenceStrength) {
        requireWorldAndType(worldgenEpoch, type, targetRegion);
        if (ordinal < 0 || scheduledAtTick < 0 || durationTicks <= 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("Invalid regular event schedule parameters");
        }
        long endsAt = addExact(scheduledAtTick, durationTicks, "event end");
        long cooldownUntil = addExact(endsAt, cooldownTicks, "event cooldown");
        DeterministicId id = DeterministicId.derive(ID_DOMAIN, Long.toString(worldSeed),
            Integer.toString(worldgenEpoch), type.toString(), targetRegion.toString(), Long.toString(ordinal));
        long parameterSeed = seedFrom(DeterministicId.derive(ID_DOMAIN, "parameters", id.hex()));
        return new EventRecord(id, type, targetRegion, false, scheduledAtTick, endsAt, EventPhase.SCHEDULED,
            scheduledAtTick, parameterSeed, cooldownUntil, temporalInfluence, temporalInfluenceStrength);
    }

    /**
     * Creates the protected first scheduling opportunity, placed deterministically inside an inclusive
     * early-world window. It has no physical effect or temporal influence in P8.
     */
    public static EventRecord protectedFirstStarRain(long worldSeed, int worldgenEpoch, long worldCreationTick,
        long windowStartOffset, long windowEndOffset, long durationTicks) {
        if (worldgenEpoch <= 0 || worldCreationTick < 0 || windowStartOffset < 0
            || windowEndOffset < windowStartOffset || durationTicks <= 0) {
            throw new IllegalArgumentException("Invalid protected Star Rain window");
        }
        DeterministicId id = DeterministicId.derive(ID_DOMAIN, Long.toString(worldSeed),
            Integer.toString(worldgenEpoch), STAR_RAIN_SLOT);
        long windowSize = windowEndOffset - windowStartOffset + 1L;
        if (windowSize <= 0) throw new IllegalArgumentException("Star Rain window is too large");
        long offset = Long.remainderUnsigned(seedFrom(DeterministicId.derive(ID_DOMAIN, id.hex(), "due")), windowSize);
        long dueTick = addExact(addExact(worldCreationTick, windowStartOffset, "Star Rain window start"), offset,
            "Star Rain due time");
        long endsAt = addExact(dueTick, durationTicks, "Star Rain opportunity end");
        return new EventRecord(id, EventRecord.PROTECTED_STAR_RAIN, EventRegion.global(), true, dueTick, endsAt,
            EventPhase.SCHEDULED, dueTick, seedFrom(DeterministicId.derive(ID_DOMAIN, id.hex(), "parameters")),
            endsAt, null, 0);
    }

    private static void requireWorldAndType(int worldgenEpoch, ResourceId type, EventRegion targetRegion) {
        if (worldgenEpoch <= 0 || type == null || targetRegion == null) {
            throw new IllegalArgumentException("Positive worldgen epoch, event type, and target are required");
        }
    }

    private static long seedFrom(DeterministicId id) {
        return Long.parseUnsignedLong(id.hex().substring(0, 16), 16);
    }

    private static long addExact(long a, long b, String name) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Overflow in " + name, e);
        }
    }
}
