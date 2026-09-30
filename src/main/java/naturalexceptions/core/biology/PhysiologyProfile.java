package naturalexceptions.core.biology;

/** Compact data-defined physiological capabilities and coarse recovery/metabolism values. */
public final class PhysiologyProfile {

    private final boolean canBleed;
    private final double bloodCapacityMillilitres;
    private final double bleedMillilitresPerSecondAtSeverityOne;
    private final double woundHealingPerSecond;
    private final double structuralHealingPerSecond;
    private final double energyCapacityJoules;
    private final double hydrationCapacityMillilitres;
    private final double energyUseJoulesPerSecond;
    private final double hydrationUseMillilitresPerSecond;
    private final double exposureSusceptibility;

    public PhysiologyProfile(boolean canBleed, double bloodCapacityMillilitres,
        double bleedMillilitresPerSecondAtSeverityOne, double woundHealingPerSecond, double structuralHealingPerSecond,
        double energyCapacityJoules, double hydrationCapacityMillilitres, double energyUseJoulesPerSecond,
        double hydrationUseMillilitresPerSecond) {
        this(
            canBleed,
            bloodCapacityMillilitres,
            bleedMillilitresPerSecondAtSeverityOne,
            woundHealingPerSecond,
            structuralHealingPerSecond,
            energyCapacityJoules,
            hydrationCapacityMillilitres,
            energyUseJoulesPerSecond,
            hydrationUseMillilitresPerSecond,
            1.0);
    }

    public PhysiologyProfile(boolean canBleed, double bloodCapacityMillilitres,
        double bleedMillilitresPerSecondAtSeverityOne, double woundHealingPerSecond, double structuralHealingPerSecond,
        double energyCapacityJoules, double hydrationCapacityMillilitres, double energyUseJoulesPerSecond,
        double hydrationUseMillilitresPerSecond, double exposureSusceptibility) {
        requireFiniteNonnegative(bloodCapacityMillilitres, "blood capacity");
        requireFiniteNonnegative(bleedMillilitresPerSecondAtSeverityOne, "bleeding rate");
        requireFiniteNonnegative(woundHealingPerSecond, "wound healing rate");
        requireFiniteNonnegative(structuralHealingPerSecond, "structural healing rate");
        requireFiniteNonnegative(energyCapacityJoules, "energy capacity");
        requireFiniteNonnegative(hydrationCapacityMillilitres, "hydration capacity");
        requireFiniteNonnegative(energyUseJoulesPerSecond, "energy use");
        requireFiniteNonnegative(hydrationUseMillilitresPerSecond, "hydration use");
        requireFiniteNonnegative(exposureSusceptibility, "exposure susceptibility");
        if (canBleed && bloodCapacityMillilitres <= 0.0)
            throw new IllegalArgumentException("Bleeding physiology requires a positive blood reserve");
        if (!canBleed && (bloodCapacityMillilitres != 0.0 || bleedMillilitresPerSecondAtSeverityOne != 0.0))
            throw new IllegalArgumentException("Non-bleeding physiology cannot carry a blood reserve or bleeding rate");
        this.canBleed = canBleed;
        this.bloodCapacityMillilitres = bloodCapacityMillilitres;
        this.bleedMillilitresPerSecondAtSeverityOne = bleedMillilitresPerSecondAtSeverityOne;
        this.woundHealingPerSecond = woundHealingPerSecond;
        this.structuralHealingPerSecond = structuralHealingPerSecond;
        this.energyCapacityJoules = energyCapacityJoules;
        this.hydrationCapacityMillilitres = hydrationCapacityMillilitres;
        this.energyUseJoulesPerSecond = energyUseJoulesPerSecond;
        this.hydrationUseMillilitresPerSecond = hydrationUseMillilitresPerSecond;
        this.exposureSusceptibility = exposureSusceptibility;
    }

    private static void requireFiniteNonnegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0)
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
    }

    public static PhysiologyProfile humanLike() {
        // Healing in game time (a Minecraft day is 1200 s): a dressed wound of full severity closes in about three
        // days and a splinted break mends in about six; untreated, each takes about four times as long.
        // A full stomach lasts about five days at rest (a hunger point every five minutes) and water about three;
        // work, running and fighting use more. A deep cut bleeds a few millilitres a second until it clots.
        return new PhysiologyProfile(true, 5000.0, 4.0, 0.00035, 0.00017, 12_000_000.0, 2500.0, 2000.0, 0.7);
    }

    /** Vanilla's hunger bar has twenty points; each is a twentieth of the energy capacity. */
    public static final double HUNGER_POINTS = 20.0;

    public double energyPerHungerPoint() {
        return energyCapacityJoules / HUNGER_POINTS;
    }

    public static PhysiologyProfile crystalline() {
        return new PhysiologyProfile(false, 0.0, 0.0, 0.0, 0.0000064, 2_000_000.0, 300.0, 2.0, 0.000002, 0.2);
    }

    public boolean canBleed() {
        return canBleed;
    }

    public double bloodCapacityMillilitres() {
        return bloodCapacityMillilitres;
    }

    public double bleedMillilitresPerSecondAtSeverityOne() {
        return bleedMillilitresPerSecondAtSeverityOne;
    }

    public double woundHealingPerSecond() {
        return woundHealingPerSecond;
    }

    public double structuralHealingPerSecond() {
        return structuralHealingPerSecond;
    }

    public double energyCapacityJoules() {
        return energyCapacityJoules;
    }

    public double hydrationCapacityMillilitres() {
        return hydrationCapacityMillilitres;
    }

    public double energyUseJoulesPerSecond() {
        return energyUseJoulesPerSecond;
    }

    public double hydrationUseMillilitresPerSecond() {
        return hydrationUseMillilitresPerSecond;
    }

    public double exposureSusceptibility() {
        return exposureSusceptibility;
    }
}
