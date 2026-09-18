package naturalexceptions.core.assembly;

import java.util.Objects;

/**
 * Physical profile of a target material or block for tool interactions.
 */
public final class TargetMaterialProfile {

    private final String name;
    private final double hardness;
    private final double toughness;

    public TargetMaterialProfile(String name, double hardness, double toughness) {
        this.name = Objects.requireNonNull(name, "name");
        this.hardness = hardness;
        this.toughness = toughness;
    }

    public static TargetMaterialProfile stone() {
        return new TargetMaterialProfile("stone", 3.0, 2.0);
    }

    public static TargetMaterialProfile copperOre() {
        return new TargetMaterialProfile("copper_ore", 3.2, 3.0);
    }

    public static TargetMaterialProfile wood() {
        return new TargetMaterialProfile("wood", 1.0, 1.5);
    }

    public String name() {
        return name;
    }

    public double hardness() {
        return hardness;
    }

    public double toughness() {
        return toughness;
    }
}
