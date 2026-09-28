package naturalexceptions.minecraft.biology;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * One physical hit stresses armor once. When P10 has already resolved a hit through the wearer's armor (and
 * committed its durability), it marks the wearer here; the host armor step for that same hit then passes the
 * damage through without reducing it or wearing the armor a second time.
 */
public final class ArmorPathGate {

    private static final Map<Object, Long> RESOLVED = new WeakHashMap<>();

    private ArmorPathGate() {}

    /** Marks that armor for the hit being processed on this world tick has been resolved by P10. */
    public static synchronized void markResolved(Object wearer, long worldTick) {
        if (wearer != null) RESOLVED.put(wearer, worldTick);
    }

    /** True exactly once for a mark made on this world tick; stale marks are discarded. */
    public static synchronized boolean consume(Object wearer, long worldTick) {
        if (wearer == null) return false;
        Long marked = RESOLVED.remove(wearer);
        return marked != null && marked == worldTick;
    }
}
