package naturalexceptions.core.event;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/** Persistable physical consequence of one ordinal within a parent Star Rain event. */
public final class StarRainImpact {

    private final DeterministicId parentEventId;
    private final DeterministicId id;
    private final int ordinal;
    private final long dueTick;
    /** Operator acceleration changes the work deadline, never the generated impact parameters. */
    private long expeditedDueTick;
    private final int x;
    private final int z;
    private final int severity;
    private MaterialQuantity stellarMass;
    private boolean resolved;
    private boolean materialized;

    public StarRainImpact(DeterministicId parentEventId, DeterministicId id, int ordinal, long dueTick, int x, int z,
        int severity, MaterialQuantity stellarMass, boolean resolved, boolean materialized) {
        this(parentEventId, id, ordinal, dueTick, x, z, severity, stellarMass, resolved, materialized, -1L);
    }

    public StarRainImpact(DeterministicId parentEventId, DeterministicId id, int ordinal, long dueTick, int x, int z,
        int severity, MaterialQuantity stellarMass, boolean resolved, boolean materialized, long expeditedDueTick) {
        this.parentEventId = Objects.requireNonNull(parentEventId, "parentEventId");
        this.id = Objects.requireNonNull(id, "id");
        this.stellarMass = Objects.requireNonNull(stellarMass, "stellarMass");
        if (ordinal < 0 || dueTick < 0
            || severity < 1
            || severity > 3
            || (expeditedDueTick != -1L && (expeditedDueTick < 0L || expeditedDueTick >= dueTick))
            || stellarMass.isZero() != materialized
            || materialized && !resolved) {
            throw new IllegalArgumentException("Invalid Star Rain impact state");
        }
        this.ordinal = ordinal;
        this.dueTick = dueTick;
        this.expeditedDueTick = expeditedDueTick;
        this.x = x;
        this.z = z;
        this.severity = severity;
        this.resolved = resolved;
        this.materialized = materialized;
    }

    public DeterministicId parentEventId() {
        return parentEventId;
    }

    public DeterministicId id() {
        return id;
    }

    public int ordinal() {
        return ordinal;
    }

    public long dueTick() {
        return dueTick;
    }

    public synchronized long expeditedDueTick() {
        return expeditedDueTick;
    }

    public synchronized long effectiveDueTick() {
        return expeditedDueTick >= 0L ? expeditedDueTick : dueTick;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public int severity() {
        return severity;
    }

    public synchronized MaterialQuantity stellarMass() {
        return stellarMass;
    }

    public synchronized boolean isResolved() {
        return resolved;
    }

    public synchronized boolean isMaterialized() {
        return materialized;
    }

    /** Called only by StarRainHistory after removing this record from its ordered due index. */
    synchronized boolean expedite(long nowTick) {
        if (resolved || nowTick < 0L || effectiveDueTick() <= nowTick) return false;
        expeditedDueTick = nowTick;
        return true;
    }

    /** Idempotently records that the historical impact happened. */
    synchronized boolean resolve() {
        if (resolved) return false;
        resolved = true;
        return true;
    }

    /** Idempotently records that its persistent world site was written. */
    synchronized MaterialQuantity transferToSpatialSite() {
        if (!resolved || materialized) return MaterialQuantity.ZERO;
        MaterialQuantity moved = stellarMass;
        if (moved.isZero()) return MaterialQuantity.ZERO;
        stellarMass = MaterialQuantity.ZERO;
        materialized = true;
        return moved;
    }

    @Override
    public synchronized boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StarRainImpact)) return false;
        StarRainImpact that = (StarRainImpact) other;
        return ordinal == that.ordinal && dueTick == that.dueTick
            && expeditedDueTick == that.expeditedDueTick
            && x == that.x
            && z == that.z
            && severity == that.severity
            && resolved == that.resolved
            && materialized == that.materialized
            && parentEventId.equals(that.parentEventId)
            && id.equals(that.id)
            && stellarMass.equals(that.stellarMass);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(
            parentEventId,
            id,
            ordinal,
            dueTick,
            expeditedDueTick,
            x,
            z,
            severity,
            stellarMass,
            resolved,
            materialized);
    }
}
