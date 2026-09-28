package naturalexceptions.core.biology;

import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.id.DeterministicId;

/** Applies one localized interaction through physical armor and into shared biological state. */
public final class BiologicalInteractionResolver {
    private BiologicalInteractionResolver() { }

    public static void apply(BiologicalState body, PhysicalInteraction interaction, long worldTick) {
        apply(body, interaction, worldTick, java.util.Collections.<ArmorLayer>emptyList());
    }

    public static void apply(BiologicalState body, PhysicalInteraction interaction, long worldTick,
        Iterable<ArmorLayer> additionalArmor) {
        if (body == null || interaction == null) throw new NullPointerException();
        if (additionalArmor == null) throw new NullPointerException("additionalArmor");
        if (worldTick < body.lastResolvedTick()) throw new IllegalArgumentException("interaction predates organism state");
        body.advanceTo(worldTick);
        BiologicalDiagnostics.interaction();
        BodyRegionState region = body.region(interaction.targetRegion());
        double energy = interaction.energyJoules();
        for (ArmorLayer layer : body.armor()) {
            energy = intercept(layer, interaction, energy);
        }
        for (ArmorLayer layer : additionalArmor) {
            energy = intercept(layer, interaction, energy);
        }
        if (energy <= 0.0) return;
        double rawHardness = body.bodyPlan().structuralMaterial().hardness();
        double materialHardness = Double.isFinite(rawHardness) && rawHardness > 0.0 ? Math.max(0.05, rawHardness) : 1.0;
        BodyRegionDefinition definition = region.definition();
        if (interaction.type() == PhysicalInteraction.Type.THERMAL && interaction.temperatureCelsius() >= 55.0
            && energy >= 5.0) {
            double severity = unit((interaction.temperatureCelsius() - 45.0) / 300.0 * Math.min(1.0, energy / 25.0));
            if (severity > 0.02) body.addInjury(new BiologicalInjury(DeterministicId.derive("injury",
                body.organismId().hex(), Long.toString(worldTick), interaction.targetRegion().toString(),
                Integer.toString(body.injuries().size())), BiologicalInjury.Kind.BURN, interaction.targetRegion(),
                severity, 0.0, worldTick));
        }
        if ((interaction.type() == PhysicalInteraction.Type.CUT || interaction.type() == PhysicalInteraction.Type.PENETRATION)
            && body.physiology().canBleed() && energy >= definition.cutThresholdJoules()) {
            double sharpFactor = Math.max(0.15, interaction.sharpness());
            double severity = unit((energy / definition.cutThresholdJoules() - 0.55) * 0.40 * sharpFactor);
            if (severity > 0.03) {
                double bleed = body.physiology().bleedMillilitresPerSecondAtSeverityOne()
                    * definition.bleedingFactor() * severity;
                body.addInjury(new BiologicalInjury(DeterministicId.derive("injury", body.organismId().hex(),
                    Long.toString(worldTick), interaction.targetRegion().toString(), Integer.toString(body.injuries().size())),
                    BiologicalInjury.Kind.CUT, interaction.targetRegion(), severity, bleed, worldTick));
            }
        }
        double structuralThreshold = definition.fractureThresholdJoules();
        // Hard crystal is harder to cut but its coarse body regions crack under concentrated work.
        boolean bluntStructuralLoad = interaction.type() == PhysicalInteraction.Type.IMPACT
            && energy >= structuralThreshold * 0.20;
        if ((energy >= structuralThreshold || bluntStructuralLoad) && (interaction.type() == PhysicalInteraction.Type.IMPACT
            || !body.physiology().canBleed() || (interaction.type() == PhysicalInteraction.Type.PENETRATION
                && energy >= structuralThreshold * materialHardness / 7.0))) {
            double severity = bluntStructuralLoad && energy < structuralThreshold
                ? unit(Math.max(0.04, energy / structuralThreshold * 0.12))
                : unit((energy / structuralThreshold - 0.55) * 0.45);
            if (severity > 0.03) {
                region.damageStructure(Math.min(0.8, severity * (bluntStructuralLoad ? 0.30 : 0.65)));
                body.addInjury(new BiologicalInjury(DeterministicId.derive("injury", body.organismId().hex(),
                    Long.toString(worldTick), interaction.targetRegion().toString(), Integer.toString(body.injuries().size())),
                    interaction.type() == PhysicalInteraction.Type.THERMAL ? BiologicalInjury.Kind.BURN
                        : BiologicalInjury.Kind.STRUCTURAL,
                    interaction.targetRegion(), severity, 0.0, worldTick));
            }
        }
    }

    private static double unit(double value) { return Math.max(0.0, Math.min(1.0, value)); }

    private static double intercept(ArmorLayer layer, PhysicalInteraction interaction, double energy) {
        if (layer == null || !layer.covers(interaction.targetRegion()) || layer.component().damage().isFractured()
            || energy <= 0.0) return energy;
        ComponentDamage damage = layer.component().damage();
        double capacity = layer.material().hardness() * layer.thicknessMetres() * 3000.0
            * Math.max(0.05, damage.effectiveMultiplier());
        capacity *= 1.0 - 0.55 * interaction.penetration();
        capacity = Math.max(0.01, capacity);
        double load = energy / capacity;
        damage.addCracking(Math.min(1.0, load * 0.8));
        if (interaction.type() == PhysicalInteraction.Type.IMPACT) damage.addDeformation(Math.min(1.0, load * 0.25));
        if (energy <= capacity || !damage.isFractured()) {
            // A stopped cut may still transmit a small blunt impulse.
            return interaction.type() == PhysicalInteraction.Type.IMPACT ? energy * 0.12 : 0.0;
        }
        // Fracture absorbs work up to its capacity; the remainder reaches tissue.
        return Math.max(0.0, energy - capacity * 0.75);
    }
}
