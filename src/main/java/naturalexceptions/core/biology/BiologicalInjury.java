package naturalexceptions.core.biology;

import java.util.Objects;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Persistent, coarse injury process. Healing and bleeding resolve over elapsed time. */
public final class BiologicalInjury {
    public enum Kind { CUT, STRUCTURAL, BURN }
    private final DeterministicId id;
    private final Kind kind;
    private final ResourceId regionId;
    private double severity;
    private double bleedingMillilitresPerSecond;
    private double treatment;
    private boolean closed;
    private long lastResolvedTick;

    public BiologicalInjury(DeterministicId id, Kind kind, ResourceId regionId, double severity,
        double bleedingMillilitresPerSecond, long createdAtTick) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.regionId = Objects.requireNonNull(regionId, "regionId");
        unit(severity, "severity"); nonnegative(bleedingMillilitresPerSecond, "bleeding");
        if (createdAtTick < 0) throw new IllegalArgumentException("created tick must be nonnegative");
        this.severity = severity;
        this.bleedingMillilitresPerSecond = bleedingMillilitresPerSecond;
        this.lastResolvedTick = createdAtTick;
    }
    public DeterministicId id() { return id; }
    public Kind kind() { return kind; }
    public ResourceId regionId() { return regionId; }
    public double severity() { return severity; }
    public double bleedingMillilitresPerSecond() { return bleedingMillilitresPerSecond; }
    public double treatment() { return treatment; }
    public boolean closed() { return closed; }
    public long lastResolvedTick() { return lastResolvedTick; }
    public void restoreProgress(double treatment, boolean closed, long lastTick) {
        unit(treatment, "treatment");
        if (lastTick < 0) throw new IllegalArgumentException("last tick must be nonnegative");
        this.treatment = treatment;
        this.closed = closed;
        this.lastResolvedTick = lastTick;
    }

    /** A dressing reduces loss and supports closure gradually; it does not erase the wound. */
    public void dress(double effectiveness) {
        unit(effectiveness, "effectiveness");
        treatment = Math.max(treatment, effectiveness);
        bleedingMillilitresPerSecond *= (1.0 - 0.9 * effectiveness);
        if (effectiveness >= 0.5) closed = true;
    }
    public void splint(double effectiveness) {
        unit(effectiveness, "effectiveness");
        treatment = Math.max(treatment, effectiveness);
    }
    double resolveTo(long tick, PhysiologyProfile physiology, double bloodReserve, double recoveryFactor) {
        if (tick < lastResolvedTick) throw new IllegalArgumentException("injury time cannot move backward");
        double seconds = (tick - lastResolvedTick) / BiologicalState.TICKS_PER_SECOND;
        lastResolvedTick = tick;
        if (seconds <= 0.0) return 0.0;
        double bloodLost = Math.min(bloodReserve, bleedingMillilitresPerSecond * seconds);
        double healingRate = kind == Kind.STRUCTURAL ? physiology.structuralHealingPerSecond()
            : physiology.woundHealingPerSecond();
        severity = Math.max(0.0, severity - healingRate * seconds * (0.25 + 0.75 * treatment)
            * Math.max(0.0, Math.min(1.0, recoveryFactor)));
        return bloodLost;
    }
    public boolean resolved() { return severity <= 0.001; }
    void merge(double addedSeverity, double addedBleeding) {
        if (!Double.isFinite(addedSeverity) || !Double.isFinite(addedBleeding) || addedSeverity < 0.0 || addedBleeding < 0.0)
            throw new IllegalArgumentException("merged injury values must be finite and nonnegative");
        severity = Math.min(1.0, severity + addedSeverity);
        bleedingMillilitresPerSecond = Math.min(1000.0, bleedingMillilitresPerSecond + addedBleeding);
    }
    private static void unit(double v, String n) { if (!Double.isFinite(v) || v < 0 || v > 1) throw new IllegalArgumentException(n + " must be in [0,1]"); }
    private static void nonnegative(double v, String n) { if (!Double.isFinite(v) || v < 0) throw new IllegalArgumentException(n + " must be finite and nonnegative"); }
}
