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
        double geomFactor = 1.0;
        GeometryId geom = assembly.head()
            .geometryId();
        if (geom == GeometryId.PICK_HEAD) {
            geomFactor = 1.5;
        } else if (geom == GeometryId.BLADE) {
            geomFactor = 1.4;
        } else if (geom == GeometryId.AXE_HEAD) {
            geomFactor = 1.3;
        } else if (geom == GeometryId.SHOVEL_HEAD) {
            geomFactor = 1.1;
        } else if (geom == GeometryId.HOE_HEAD) {
            geomFactor = 1.1;
        }
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
     * Calculates the wear rate per interaction against a target material based on physical material
     * properties (hardness, density, mass), component geometry, and handle traits.
     */
    public double calculateWearRate(TargetMaterialProfile target) {
        double headHardness = Math.max(1.0, headProps.hardness());
        double targetHardness = target != null ? target.hardness() : 3.0;

        // Density and mass factor: more massive heads distribute wear over greater working volume
        double massG = assembly.head()
            .mass()
            .toGrams();
        double massFactor = Math.max(0.6, Math.min(1.5, Math.sqrt(massG / 200.0)));

        // Material trait multipliers
        double traitMultiplier = 1.0;
        if (headProps.hasTrait("durable")) {
            traitMultiplier *= 1.25;
        }

        // Handle damping contribution
        double handleFactor = handleProps.hasTrait("shock_absorbing") ? 1.0
            : Math.max(0.75, Math.min(1.25, handleProps.hardness() / 1.5));

        // Base rate: 0.0045 scaled inversely by hardness, mass factor, traits, and handle
        double rate = (0.0045 * targetHardness) / (headHardness * massFactor * traitMultiplier * handleFactor);
        return Math.max(0.0005, Math.min(0.20, rate));
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
        double wearIncrement = calculateWearRate(target);
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
            // Normal use micro-fatigue: handle damping absorbs shock
            double fatigueFactor = handleProps.hasTrait("shock_absorbing") ? 0.0005 : 0.001;
            assembly.head()
                .damage()
                .addCracking(fatigueFactor);
        }

        // Handle stress: handle absorbs recoil
        assembly.handle()
            .damage()
            .addWear(0.0005);
    }
}
