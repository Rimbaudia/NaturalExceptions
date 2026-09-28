package naturalexceptions.core.biology;

import java.util.Objects;
import naturalexceptions.core.id.ResourceId;

/** Bounded physical input shared by equipment interception and organism injury resolution. */
public final class PhysicalInteraction {
    public enum Type { CUT, IMPACT, PENETRATION, THERMAL }
    private final Type type;
    private final ResourceId targetRegion;
    private final double energyJoules;
    private final double penetration;
    private final double sharpness;
    private final double temperatureCelsius;

    public PhysicalInteraction(Type type, ResourceId targetRegion, double energyJoules,
        double penetration, double sharpness, double temperatureCelsius) {
        this.type = Objects.requireNonNull(type, "type");
        this.targetRegion = Objects.requireNonNull(targetRegion, "targetRegion");
        finiteNonnegative(energyJoules, "energy");
        unit(penetration, "penetration");
        unit(sharpness, "sharpness");
        if (!Double.isFinite(temperatureCelsius)) throw new IllegalArgumentException("temperature must be finite");
        this.energyJoules = energyJoules;
        this.penetration = penetration;
        this.sharpness = sharpness;
        this.temperatureCelsius = temperatureCelsius;
    }

    public Type type() { return type; }
    public ResourceId targetRegion() { return targetRegion; }
    public double energyJoules() { return energyJoules; }
    public double penetration() { return penetration; }
    public double sharpness() { return sharpness; }
    public double temperatureCelsius() { return temperatureCelsius; }
    public PhysicalInteraction withEnergy(double joules) {
        return new PhysicalInteraction(type, targetRegion, joules, penetration, sharpness, temperatureCelsius);
    }
    private static void finiteNonnegative(double v, String n) {
        if (!Double.isFinite(v) || v < 0.0) throw new IllegalArgumentException(n + " must be finite and nonnegative");
    }
    private static void unit(double v, String n) {
        if (!Double.isFinite(v) || v < 0.0 || v > 1.0) throw new IllegalArgumentException(n + " must be in [0,1]");
    }
}
