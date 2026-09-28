package naturalexceptions.core.biology;

import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

/** Static functional and injury thresholds for one manageable body region. */
public final class BodyRegionDefinition {
    private final ResourceId id;
    private final double cutThresholdJoules;
    private final double fractureThresholdJoules;
    private final double bleedingFactor;
    private final double mobilityWeight;
    private final double handlingWeight;

    public BodyRegionDefinition(ResourceId id, double cutThresholdJoules, double fractureThresholdJoules,
        double bleedingFactor, double mobilityWeight, double handlingWeight) {
        this.id = Objects.requireNonNull(id, "id");
        requirePositiveFinite(cutThresholdJoules, "cut threshold");
        requirePositiveFinite(fractureThresholdJoules, "fracture threshold");
        requireUnit(bleedingFactor, "bleeding factor");
        requireUnit(mobilityWeight, "mobility weight");
        requireUnit(handlingWeight, "handling weight");
        this.cutThresholdJoules = cutThresholdJoules;
        this.fractureThresholdJoules = fractureThresholdJoules;
        this.bleedingFactor = bleedingFactor;
        this.mobilityWeight = mobilityWeight;
        this.handlingWeight = handlingWeight;
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(name + " must be positive and finite");
    }
    private static void requireUnit(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) throw new IllegalArgumentException(name + " must be in [0,1]");
    }

    public ResourceId id() { return id; }
    public double cutThresholdJoules() { return cutThresholdJoules; }
    public double fractureThresholdJoules() { return fractureThresholdJoules; }
    public double bleedingFactor() { return bleedingFactor; }
    public double mobilityWeight() { return mobilityWeight; }
    public double handlingWeight() { return handlingWeight; }

    public BodyRegionDefinition withId(ResourceId newId) {
        return new BodyRegionDefinition(newId, cutThresholdJoules, fractureThresholdJoules,
            bleedingFactor, mobilityWeight, handlingWeight);
    }
}
