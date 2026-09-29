package naturalexceptions.core.biology;

import java.util.Objects;
import naturalexceptions.core.id.ResourceId;

/** Coalesced persistent quantitative dose for one material and route. */
public final class ExposureBurden {
    public static final long DOSE_MICRO_UNITS_PER_UNIT = 1_000_000L;
    public static final double CLEARANCE_HALF_LIFE_SECONDS = 2400.0;
    public static final long MAX_DOSE_MICRO_UNITS = 1_000_000_000_000_000_000L;
    private final ResourceId material;
    private final ExposureRoute route;
    private long doseMicrounits;
    private long lastResolvedTick;

    public ExposureBurden(ResourceId material, ExposureRoute route, double dose, long tick) {
        this.material = Objects.requireNonNull(material, "material");
        this.route = Objects.requireNonNull(route, "route");
        add(dose);
        if (tick < 0) throw new IllegalArgumentException("tick must be nonnegative");
        lastResolvedTick = tick;
    }
    public ResourceId material() { return material; }
    public ExposureRoute route() { return route; }
    public double dose() { return (double) doseMicrounits / DOSE_MICRO_UNITS_PER_UNIT; }
    public long doseMicrounits() { return doseMicrounits; }
    public long lastResolvedTick() { return lastResolvedTick; }
    void add(double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("dose must be finite and nonnegative");
        double scaled = amount * DOSE_MICRO_UNITS_PER_UNIT;
        long quanta = !Double.isFinite(scaled) || scaled >= MAX_DOSE_MICRO_UNITS
            ? MAX_DOSE_MICRO_UNITS : Math.max(0L, Math.round(scaled));
        doseMicrounits = quanta > MAX_DOSE_MICRO_UNITS - doseMicrounits
            ? MAX_DOSE_MICRO_UNITS : doseMicrounits + quanta;
    }
    void resolve(long tick) {
        if (tick < lastResolvedTick) throw new IllegalArgumentException("time cannot move backward");
        double seconds = (tick - lastResolvedTick) / BiologicalState.TICKS_PER_SECOND;
        // The body clears what it took in: half the dose every CLEARANCE_HALF_LIFE_SECONDS (two Minecraft days),
        // plus a small steady clearance so a trace reaches zero rather than lingering forever.
        double remaining = doseMicrounits * Math.pow(0.5, seconds / CLEARANCE_HALF_LIFE_SECONDS);
        double clearance = (doseMicrounits - remaining) + seconds * 0.001 * DOSE_MICRO_UNITS_PER_UNIT;
        long removed = !Double.isFinite(clearance) || clearance >= doseMicrounits
            ? doseMicrounits : Math.max(0L, Math.round(clearance));
        doseMicrounits -= removed;
        lastResolvedTick = tick;
    }
}
