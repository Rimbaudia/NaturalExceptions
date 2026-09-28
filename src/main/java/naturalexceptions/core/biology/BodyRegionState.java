package naturalexceptions.core.biology;

import java.util.Objects;

/** Compact mutable state for one of a plan's bounded anatomical/structural regions. */
public final class BodyRegionState {
    private final BodyRegionDefinition definition;
    private double structuralIntegrity = 1.0;

    BodyRegionState(BodyRegionDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public BodyRegionDefinition definition() { return definition; }
    public double structuralIntegrity() { return structuralIntegrity; }
    public double structuralDamage() { return 1.0 - structuralIntegrity; }
    public boolean fractured() { return structuralIntegrity <= 0.20; }

    public void damageStructure(double amount) {
        requireUnit(amount, "structural damage");
        structuralIntegrity = Math.max(0.0, structuralIntegrity - amount);
    }

    public void restoreStructure(double amount) {
        requireUnit(amount, "structural recovery");
        structuralIntegrity = Math.min(1.0, structuralIntegrity + amount);
    }

    public void restoreIntegrity(double value) {
        requireUnit(value, "structural integrity");
        structuralIntegrity = value;
    }

    private static void requireUnit(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0)
            throw new IllegalArgumentException(name + " must be finite and in [0,1]");
    }
}
