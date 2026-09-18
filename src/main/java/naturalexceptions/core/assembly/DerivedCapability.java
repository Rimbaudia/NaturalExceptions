package naturalexceptions.core.assembly;

import java.util.Objects;

import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Continuous derived capability calculator for physical tool assemblies.
 *
 * <p>
 * Does not use static artificial tiers (e.g. WOOD_TIER, IRON_TIER). Capabilities emerge naturally
 * from head geometry, material hardness, density, component masses, and accumulated damage.
 */
public final class DerivedCapability {

    private final AssemblyGraph assembly;
    private final MaterialProperties headProps;
    private final MaterialProperties handleProps;

    public DerivedCapability(AssemblyGraph assembly, MaterialProperties headProps, MaterialProperties handleProps) {
        this.assembly = Objects.requireNonNull(assembly, "assembly");
        this.headProps = Objects.requireNonNull(headProps, "headProps");
        this.handleProps = Objects.requireNonNull(handleProps, "handleProps");
    }

    public AssemblyGraph assembly() {
        return assembly;
    }

    /**
     * Penetration capability (concentrated hardness and point sharpness).
     */
    public double penetration() {
        if (assembly.isBroken()) {
            return 0.1;
        }
        double baseHardness = headProps.hardness();
        double geomFactor = assembly.head()
            .geometryId() == GeometryId.PICK_HEAD ? 1.5 : 1.0;
        double damageMultiplier = assembly.head()
            .damage()
            .effectiveMultiplier();
        return baseHardness * geomFactor * damageMultiplier;
    }

    /**
     * Impact capability (momentum delivered to target).
     */
    public double impact() {
        if (assembly.isBroken()) {
            return 0.1;
        }
        double headMassG = assembly.head()
            .mass()
            .toGrams();
        double density = headProps.density();
        double damageMultiplier = assembly.head()
            .damage()
            .effectiveMultiplier();
        return (headMassG / 50.0) * (density / 5.0) * damageMultiplier;
    }

    /**
     * Continuous dynamic mining efficiency against a physical block target.
     */
    public double miningSpeed(TargetMaterialProfile target) {
        Objects.requireNonNull(target, "target");
        if (assembly.isBroken()) {
            return 1.0; // Ineffective broken tool speed
        }

        double p = penetration();
        if (p < target.hardness() * 0.6) {
            return 1.0; // Too dull/soft to effectively fracture target
        }

        double speed = 2.5 + (p - target.hardness()) * 2.0 + impact() * 0.5;
        return Math.max(1.0, speed);
    }

    /**
     * Applies physical stress and wear to assembly components from interacting with a target.
     */
    public void interactWith(TargetMaterialProfile target) {
        Objects.requireNonNull(target, "target");
        if (assembly.isBroken()) {
            return;
        }

        double headHardness = headProps.hardness();
        double targetHardness = target.hardness();

        // Wear accumulation: abrasive contact dulls cutting/point edges
        double wearIncrement = 0.005 * (targetHardness / Math.max(1.0, headHardness));
        assembly.head()
            .damage()
            .addWear(wearIncrement);

        // Stress accumulation: attempting to break targets harder than tool causes deformation and cracking
        if (targetHardness > headHardness) {
            double overload = targetHardness - headHardness;
            assembly.head()
                .damage()
                .addDeformation(0.015 * overload);
            assembly.head()
                .damage()
                .addCracking(0.020 * overload);
        } else {
            // Normal use micro-fatigue
            assembly.head()
                .damage()
                .addCracking(0.001);
        }

        // Handle stress
        assembly.handle()
            .damage()
            .addWear(0.001);
    }
}
