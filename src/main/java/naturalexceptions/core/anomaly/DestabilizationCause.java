package naturalexceptions.core.anomaly;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Compact provenance for an explicit destabilization increase. */
public final class DestabilizationCause {
    private final ResourceId type;
    private final DeterministicId sourceId;
    private final double amount;
    private final long tick;

    public DestabilizationCause(ResourceId type, DeterministicId sourceId, double amount, long tick) {
        this.type = Objects.requireNonNull(type, "type");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        if (!finite(amount) || amount <= 0 || amount > 1) throw new IllegalArgumentException("amount must be in (0,1]");
        if (tick < 0) throw new IllegalArgumentException("tick must be nonnegative");
        this.amount = amount;
        this.tick = tick;
    }

    public ResourceId type() { return type; }
    public DeterministicId sourceId() { return sourceId; }
    public double amount() { return amount; }
    public long tick() { return tick; }

    public boolean sameIdentity(DestabilizationCause other) {
        return other != null && type.equals(other.type) && sourceId.equals(other.sourceId);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DestabilizationCause)) return false;
        DestabilizationCause that = (DestabilizationCause) o;
        return Double.compare(amount, that.amount) == 0 && tick == that.tick
            && type.equals(that.type) && sourceId.equals(that.sourceId);
    }
    @Override public int hashCode() {
        long bits = Double.doubleToLongBits(amount);
        return Objects.hash(type, sourceId, bits, tick);
    }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
}
