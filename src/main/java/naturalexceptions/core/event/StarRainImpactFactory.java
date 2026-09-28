package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/** Deterministic bounded impacts derived from the already-persisted P8 parent event. */
public final class StarRainImpactFactory {
    public static final int IMPACTS_PER_EVENT = 4;
    private static final String ID_DOMAIN = "naturalexceptions:p11:star_rain_impact:v1";
    private static final long MASS_PER_SEVERITY_MG = 400_000L;

    private StarRainImpactFactory() { }

    public static List<StarRainImpact> create(EventRecord event, int centerX, int centerZ) {
        if (event == null || !EventRecord.PROTECTED_STAR_RAIN.equals(event.type())) {
            throw new IllegalArgumentException("A persisted Star Rain event is required");
        }
        List<StarRainImpact> impacts = new ArrayList<>(IMPACTS_PER_EVENT);
        long duration = event.endsAtTick() - event.scheduledAtTick();
        for (int ordinal = 0; ordinal < IMPACTS_PER_EVENT; ordinal++) {
            DeterministicId id = DeterministicId.derive(ID_DOMAIN, event.id().hex(), Integer.toString(ordinal));
            long entropy = Long.parseUnsignedLong(DeterministicId.derive(ID_DOMAIN, event.id().hex(), "parameters",
                Integer.toString(ordinal)).hex().substring(0, 16), 16);
            int dx = (int) Long.remainderUnsigned(entropy, 193L) - 96;
            int dz = (int) Long.remainderUnsigned(mix(entropy), 193L) - 96;
            int severity = 1 + (int) Long.remainderUnsigned(mix(entropy ^ 0x535441525241494eL), 3L);
            long parts = IMPACTS_PER_EVENT + 1L;
            long dueOffset = (duration / parts) * (ordinal + 1L)
                + (duration % parts) * (ordinal + 1L) / parts;
            dueOffset = Math.max(1L, dueOffset);
            long dueTick = Math.addExact(event.scheduledAtTick(), dueOffset);
            impacts.add(new StarRainImpact(event.id(), id, ordinal, dueTick, centerX + dx, centerZ + dz,
                severity, MaterialQuantity.ofMilligrams(MASS_PER_SEVERITY_MG * severity), false, false));
        }
        return Collections.unmodifiableList(impacts);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
