package naturalexceptions.core.anomaly;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;

/** A single independent contribution to shared local environmental context. */
public final class EnvironmentalInfluence {
    private final DeterministicId sourceId;
    private final double waterAvailabilityDelta;
    private final double nocturnalReactivityDelta;
    private final double moistureDelta;

    private EnvironmentalInfluence(DeterministicId sourceId, double waterAvailabilityDelta,
                                   double nocturnalReactivityDelta, double moistureDelta) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.waterAvailabilityDelta = finiteDelta(waterAvailabilityDelta, "waterAvailabilityDelta");
        this.nocturnalReactivityDelta = finiteDelta(nocturnalReactivityDelta, "nocturnalReactivityDelta");
        this.moistureDelta = finiteDelta(moistureDelta, "moistureDelta");
    }

    public static EnvironmentalInfluence of(DeterministicId sourceId, double waterAvailabilityDelta,
                                            double nocturnalReactivityDelta, double moistureDelta) {
        return new EnvironmentalInfluence(sourceId, waterAvailabilityDelta, nocturnalReactivityDelta, moistureDelta);
    }

    private static double finiteDelta(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < -1.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be finite and between -1 and 1");
        }
        return value;
    }

    public DeterministicId sourceId() { return sourceId; }
    public double waterAvailabilityDelta() { return waterAvailabilityDelta; }
    public double nocturnalReactivityDelta() { return nocturnalReactivityDelta; }
    public double moistureDelta() { return moistureDelta; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EnvironmentalInfluence)) return false;
        EnvironmentalInfluence that = (EnvironmentalInfluence) other;
        return sourceId.equals(that.sourceId)
            && Double.compare(waterAvailabilityDelta, that.waterAvailabilityDelta) == 0
            && Double.compare(nocturnalReactivityDelta, that.nocturnalReactivityDelta) == 0
            && Double.compare(moistureDelta, that.moistureDelta) == 0;
    }

    @Override public int hashCode() {
        return Objects.hash(sourceId, waterAvailabilityDelta, nocturnalReactivityDelta, moistureDelta);
    }
}
