package naturalexceptions.minecraft.biology;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Converts sustained contact with hot matter into bounded exposure. Collision callbacks fire every tick; a body
 * touching hot matter for a second must receive about one second of heat, delivered as one interaction, rather
 * than twenty separate injuries (each of which also stresses armor).
 */
public final class ThermalContactLimiter {

    /** Minimum ticks between resolved contact interactions for one body. */
    public static final long INTERVAL_TICKS = 20L;
    /** Longest gap credited as continuous exposure; a returning body does not receive a stored-up burst. */
    public static final long MAX_CREDIT_TICKS = 40L;

    private final Map<Object, Long> lastAdmitted = new WeakHashMap<>();

    /**
     * Returns the joules to apply now, or zero if this body was already resolved within the interval. The first
     * contact after a break is credited as one interval of exposure.
     */
    public synchronized double admit(Object body, long worldTick, double joulesPerSecond) {
        if (body == null || !Double.isFinite(joulesPerSecond) || joulesPerSecond <= 0.0) return 0.0;
        Long last = lastAdmitted.get(body);
        long elapsed;
        if (last == null || worldTick < last || worldTick - last > MAX_CREDIT_TICKS) {
            elapsed = INTERVAL_TICKS;
        } else if (worldTick - last < INTERVAL_TICKS) {
            return 0.0;
        } else {
            elapsed = worldTick - last;
        }
        lastAdmitted.put(body, worldTick);
        return joulesPerSecond * elapsed / 20.0;
    }
}
