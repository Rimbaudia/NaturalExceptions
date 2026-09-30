package naturalexceptions.core.biology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.property.MaterialProperties;

/** Shared coarse organism state; elapsed physiology is resolved analytically, never replayed tick by tick. */
public final class BiologicalState {

    public static final double TICKS_PER_SECOND = 20.0;
    public static final int MAX_EXPOSURE_BURDENS = 64;
    public static final int MAX_INJURIES = 64;
    public static final int MAX_PROTECTION_ITEMS = 8;
    private final BodyPlan bodyPlan;
    private final DeterministicId organismId;
    private final Map<ResourceId, BodyRegionState> regions = new LinkedHashMap<>();
    private final List<BiologicalInjury> injuries = new ArrayList<>();
    private final List<ExposureBurden> exposures = new ArrayList<>();
    private final List<ExposureProtection> protection = new ArrayList<>();
    private final List<ArmorLayer> armor = new ArrayList<>();
    private double energyReserveJoules;
    private double hydrationReserveMillilitres;
    private double bloodReserveMillilitres;
    /** Blood still to come back from an Iron Tonic, restored at {@link #TONIC_MILLILITRES_PER_SECOND}. */
    private double bloodRegenerationMillilitres;
    private long lastResolvedTick;

    public static final double TONIC_MILLILITRES_PER_SECOND = 10.0;

    /**
     * Multiplier on healing and blood recovery from worn aids (e.g. a Band of Mending); set each second by the host
     * like exposure protection, never saved.
     */
    private double recoveryMultiplier = 1.0;

    public void setRecoveryMultiplier(double multiplier) {
        recoveryMultiplier = Double.isFinite(multiplier) ? Math.max(0.0, Math.min(4.0, multiplier)) : 1.0;
    }

    public double recoveryMultiplier() {
        return recoveryMultiplier;
    }

    /** Time to regain a full blood reserve from empty, well fed and watered. */
    public static final double BLOOD_RECOVERY_SECONDS = 4800.0;

    /** Seconds until {@code injury} closes at current reserves, if nothing else happens to it. */
    public double secondsToHeal(BiologicalInjury injury) {
        PhysiologyProfile physiology = physiology();
        double rate = injury.kind() == BiologicalInjury.Kind.STRUCTURAL ? physiology.structuralHealingPerSecond()
            : physiology.woundHealingPerSecond();
        double reserves = Math.min(
            energyReserveJoules / Math.max(1.0, physiology.energyCapacityJoules()),
            hydrationReserveMillilitres / Math.max(1.0, physiology.hydrationCapacityMillilitres()));
        double effective = rate * (0.25 + 0.75 * injury.treatment()) * (0.15 + 0.85 * reserves) * recoveryMultiplier;
        return effective <= 0.0 ? Double.POSITIVE_INFINITY : Math.max(0.0, injury.severity() - 0.001) / effective;
    }

    public BiologicalState(BodyPlan bodyPlan, long createdAtTick) {
        this(
            bodyPlan,
            DeterministicId.derive(
                "organism",
                bodyPlan.id()
                    .toString(),
                Long.toString(createdAtTick)),
            createdAtTick);
    }

    public BiologicalState(BodyPlan bodyPlan, DeterministicId organismId, long createdAtTick) {
        this.bodyPlan = Objects.requireNonNull(bodyPlan, "bodyPlan");
        this.organismId = Objects.requireNonNull(organismId, "organismId");
        if (createdAtTick < 0L) throw new IllegalArgumentException("Creation tick must be nonnegative");
        for (BodyRegionDefinition definition : bodyPlan.orderedRegions())
            regions.put(definition.id(), new BodyRegionState(definition));
        PhysiologyProfile physiology = bodyPlan.physiology();
        energyReserveJoules = physiology.energyCapacityJoules();
        hydrationReserveMillilitres = physiology.hydrationCapacityMillilitres();
        bloodReserveMillilitres = physiology.bloodCapacityMillilitres();
        lastResolvedTick = createdAtTick;
    }

    public BodyPlan bodyPlan() {
        return bodyPlan;
    }

    public DeterministicId organismId() {
        return organismId;
    }

    public PhysiologyProfile physiology() {
        return bodyPlan.physiology();
    }

    public Map<ResourceId, BodyRegionState> regions() {
        return Collections.unmodifiableMap(regions);
    }

    public BodyRegionState region(ResourceId id) {
        BodyRegionState state = regions.get(id);
        if (state == null)
            throw new IllegalArgumentException("Unknown body region " + id + " in plan " + bodyPlan.id());
        return state;
    }

    public double energyReserveJoules() {
        return energyReserveJoules;
    }

    public double hydrationReserveMillilitres() {
        return hydrationReserveMillilitres;
    }

    public double bloodReserveMillilitres() {
        return bloodReserveMillilitres;
    }

    public double bloodRegenerationMillilitres() {
        return bloodRegenerationMillilitres;
    }

    /** Blood to restore over the coming seconds (an Iron Tonic). */
    public void addBloodRegeneration(double millilitres) {
        requireFiniteNonnegative(millilitres, "blood regeneration");
        bloodRegenerationMillilitres = Math
            .min(physiology().bloodCapacityMillilitres(), bloodRegenerationMillilitres + millilitres);
    }

    /** A styptic salve on every bleeding cut; returns how many it stanched. */
    public int stanchBleeding() {
        int stanched = 0;
        for (BiologicalInjury injury : injuries) if (injury.stanch()) stanched++;
        return stanched;
    }

    /** Whether anything is bleeding. */
    public boolean bleeding() {
        for (BiologicalInjury injury : injuries) if (injury.bleedingMillilitresPerSecond() > 0.0) return true;
        return false;
    }

    /** Energy and water spent on work (walking, running, fighting, healing), on top of the resting use. */
    public void spend(double energyJoules, double hydrationMillilitres) {
        requireFiniteNonnegative(energyJoules, "energy spent");
        requireFiniteNonnegative(hydrationMillilitres, "water spent");
        energyReserveJoules = Math.max(0.0, energyReserveJoules - energyJoules);
        hydrationReserveMillilitres = Math.max(0.0, hydrationReserveMillilitres - hydrationMillilitres);
    }

    public long lastResolvedTick() {
        return lastResolvedTick;
    }

    public List<BiologicalInjury> injuries() {
        return Collections.unmodifiableList(injuries);
    }

    public List<ExposureBurden> exposures() {
        return Collections.unmodifiableList(exposures);
    }

    public List<ArmorLayer> armor() {
        return Collections.unmodifiableList(armor);
    }

    public List<ExposureProtection> protection() {
        return Collections.unmodifiableList(protection);
    }

    public void clearArmor() {
        armor.clear();
    }

    public void addInjury(BiologicalInjury injury) {
        Objects.requireNonNull(injury, "injury");
        if (injuries.size() < MAX_INJURIES) {
            injuries.add(injury);
            return;
        }
        for (BiologicalInjury existing : injuries) {
            if (existing.kind() == injury.kind() && existing.regionId()
                .equals(injury.regionId())) {
                existing.merge(injury.severity(), injury.bleedingMillilitresPerSecond());
                return;
            }
        }
        // At saturation retain the finite process set; its region's structural integrity still records further load.
    }

    public void addExposureBurden(ExposureBurden burden) {
        exposures.add(Objects.requireNonNull(burden, "burden"));
    }

    public void restoreBloodRegeneration(double millilitres) {
        bloodRegenerationMillilitres = Double.isFinite(millilitres) ? Math.max(0.0, millilitres) : 0.0;
    }

    public void restoreReserves(double energy, double hydration, double blood, long resolvedTick) {
        if (!Double.isFinite(energy) || !Double.isFinite(hydration)
            || !Double.isFinite(blood)
            || energy < 0
            || hydration < 0
            || blood < 0
            || resolvedTick < 0) throw new IllegalArgumentException("invalid restored biological state");
        energyReserveJoules = Math.min(physiology().energyCapacityJoules(), energy);
        hydrationReserveMillilitres = Math.min(physiology().hydrationCapacityMillilitres(), hydration);
        bloodReserveMillilitres = physiology().canBleed() ? Math.min(physiology().bloodCapacityMillilitres(), blood)
            : 0.0;
        lastResolvedTick = resolvedTick;
    }

    public void equipArmor(ArmorLayer layer) {
        Objects.requireNonNull(layer, "layer");
        for (ResourceId region : layer.coveredRegions()) region(region);
        armor.add(layer);
    }

    public boolean hasActiveProcesses() {
        if (!injuries.isEmpty()) return true;
        for (ExposureBurden burden : exposures) if (burden.dose() > 0.001) return true;
        return false;
    }

    public void treatInjury(DeterministicId injuryId, BiologicalTreatment treatment) {
        Objects.requireNonNull(injuryId, "injuryId");
        Objects.requireNonNull(treatment, "treatment");
        for (BiologicalInjury injury : injuries) {
            if (injury.id()
                .equals(injuryId)) {
                if (treatment == BiologicalTreatment.DRESS) injury.dress(0.75);
                else injury.splint(0.75);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown injury " + injuryId);
    }

    public void addExposureProtection(ExposureProtection item) {
        Objects.requireNonNull(item, "item");
        protection.removeIf(existing -> existing.route() == item.route());
        if (protection.size() < MAX_PROTECTION_ITEMS) protection.add(item);
    }

    public void clearExposureProtection() {
        protection.clear();
    }

    public double expose(ResourceId material, MaterialProperties properties, ExposureRoute route, double quantityGrams,
        long worldTick) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(route, "route");
        if (!Double.isFinite(quantityGrams) || quantityGrams < 0)
            throw new IllegalArgumentException("quantity must be finite and nonnegative");
        advanceTo(worldTick);
        double attenuation = 1.0;
        for (ExposureProtection item : protection) if (item.route() == route) attenuation *= (1.0 - item.attenuation());
        double dose = Math.min(
            1.0e12,
            quantityGrams * properties.biologicalHazard()
                * route.effectiveness()
                * attenuation
                * physiology().exposureSusceptibility());
        if (dose == 0.0) return 0.0;
        BiologicalDiagnostics.exposure();
        ExposureBurden target = null;
        for (ExposureBurden burden : exposures) if (burden.material()
            .equals(material) && burden.route() == route) {
                target = burden;
                break;
            }
        if (target == null) {
            if (exposures.size() >= MAX_EXPOSURE_BURDENS - ExposureRoute.values().length)
                material = ResourceId.of("naturalexceptions", "other_exposure");
            for (ExposureBurden burden : exposures) if (burden.material()
                .equals(material) && burden.route() == route) {
                    target = burden;
                    break;
                }
            if (target == null) {
                target = new ExposureBurden(material, route, 0.0, worldTick);
                exposures.add(target);
            }
        }
        target.add(dose);
        return dose;
    }

    public double exposureEffect() {
        double total = 0.0;
        for (ExposureBurden burden : exposures) total = Math.min(1.0e12, total + burden.dose());
        return total;
    }

    /**
     * How sound the body is, 0 to 1, which the host's health follows: injuries, blood lost and exposure take from it.
     * Hunger and thirst do not: an empty stomach is felt through the food bar (no healing, no sprinting, starving
     * when it is empty), not as wounds.
     */
    public double condition() {
        double injuryBurden = 0.0;
        for (BiologicalInjury injury : injuries) injuryBurden = Math.min(0.7, injuryBurden + injury.severity() * 0.15);
        return Math.max(
            0.0,
            Math.min(
                1.0,
                1.0 - injuryBurden
                    - Math.min(0.7, exposureEffect() * 0.0001)
                    - Math.min(
                        0.4,
                        (physiology().bloodCapacityMillilitres() == 0.0 ? 0.0
                            : 1.0 - bloodReserveMillilitres / physiology().bloodCapacityMillilitres()) * 0.4)));
    }

    public double movementCapability() {
        return functionCapability(true);
    }

    public double handlingCapability() {
        return functionCapability(false);
    }

    private double functionCapability(boolean mobility) {
        double weightedDamage = 0.0, weights = 0.0;
        for (BodyRegionState region : regions.values()) {
            double weight = mobility ? region.definition()
                .mobilityWeight()
                : region.definition()
                    .handlingWeight();
            if (weight > 0.0) {
                weights += weight;
                weightedDamage += weight * region.structuralDamage();
            }
        }
        double structural = weights == 0.0 ? 1.0 : 1.0 - weightedDamage / weights;
        double injuryPenalty = 0.0;
        for (BiologicalInjury injury : injuries) {
            BodyRegionDefinition d = region(injury.regionId()).definition();
            double weight = mobility ? d.mobilityWeight() : d.handlingWeight();
            injuryPenalty = Math.max(injuryPenalty, weight * injury.severity() * 0.65);
        }
        return Math.max(0.15, Math.min(1.0, structural * (1.0 - injuryPenalty)));
    }

    public void advanceTo(long worldTick) {
        if (worldTick < lastResolvedTick) throw new IllegalArgumentException("Biological time cannot move backward");
        long elapsedTicks = worldTick - lastResolvedTick;
        if (elapsedTicks == 0L) return;
        BiologicalDiagnostics.resolved(hasActiveProcesses());
        double elapsedSeconds = elapsedTicks / TICKS_PER_SECOND;
        PhysiologyProfile physiology = bodyPlan.physiology();
        energyReserveJoules = Math
            .max(0.0, energyReserveJoules - finiteConsumption(physiology.energyUseJoulesPerSecond(), elapsedSeconds));
        hydrationReserveMillilitres = Math.max(
            0.0,
            hydrationReserveMillilitres
                - finiteConsumption(physiology.hydrationUseMillilitresPerSecond(), elapsedSeconds));
        for (int i = injuries.size() - 1; i >= 0; i--) {
            BiologicalInjury injury = injuries.get(i);
            double oldSeverity = injury.severity();
            double reserves = Math.min(
                energyReserveJoules / Math.max(1.0, physiology.energyCapacityJoules()),
                hydrationReserveMillilitres / Math.max(1.0, physiology.hydrationCapacityMillilitres()));
            double bloodLost = injury.resolveTo(
                worldTick,
                physiology,
                bloodReserveMillilitres,
                (0.15 + 0.85 * reserves) * recoveryMultiplier);
            bloodReserveMillilitres = Math.max(0.0, bloodReserveMillilitres - bloodLost);
            if (injury.kind() == BiologicalInjury.Kind.STRUCTURAL)
                region(injury.regionId()).restoreStructure(Math.min(1.0, oldSeverity - injury.severity()));
            if (injury.resolved()) injuries.remove(i);
        }
        for (ExposureBurden burden : exposures) burden.resolve(worldTick);
        if (bloodRegenerationMillilitres > 0.0 && physiology.canBleed()) {
            double restored = Math.min(bloodRegenerationMillilitres, TONIC_MILLILITRES_PER_SECOND * elapsedSeconds);
            bloodRegenerationMillilitres -= restored;
            bloodReserveMillilitres = Math
                .min(physiology.bloodCapacityMillilitres(), bloodReserveMillilitres + restored);
        }
        boolean bleeding = false;
        for (BiologicalInjury injury : injuries) bleeding |= injury.bleedingMillilitresPerSecond() > 0.0;
        if (physiology.canBleed() && !bleeding) {
            // Once nothing bleeds, lost blood comes back over about four Minecraft days, slower when starved or dry.
            double reserves = Math.min(
                energyReserveJoules / Math.max(1.0, physiology.energyCapacityJoules()),
                hydrationReserveMillilitres / Math.max(1.0, physiology.hydrationCapacityMillilitres()));
            double regained = physiology.bloodCapacityMillilitres() / BLOOD_RECOVERY_SECONDS
                * elapsedSeconds
                * (0.15 + 0.85 * reserves)
                * recoveryMultiplier;
            bloodReserveMillilitres = Math
                .min(physiology.bloodCapacityMillilitres(), bloodReserveMillilitres + regained);
        }
        lastResolvedTick = worldTick;
    }

    public void replenishEnergyJoules(double joules) {
        requireFiniteNonnegative(joules, "energy replenishment");
        energyReserveJoules = Math.min(physiology().energyCapacityJoules(), energyReserveJoules + joules);
    }

    public void replenishHydrationMillilitres(double millilitres) {
        requireFiniteNonnegative(millilitres, "hydration replenishment");
        hydrationReserveMillilitres = Math
            .min(physiology().hydrationCapacityMillilitres(), hydrationReserveMillilitres + millilitres);
    }

    private static double finiteConsumption(double rate, double seconds) {
        double amount = rate * seconds;
        return Double.isFinite(amount) ? amount : Double.MAX_VALUE;
    }

    private static void requireFiniteNonnegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0)
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
    }
}
