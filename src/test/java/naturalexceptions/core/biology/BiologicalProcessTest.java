package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class BiologicalProcessTest {
    @Test void dressingReducesBleedingAndElapsedRecoveryProgresses() {
        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        BiologicalInteractionResolver.apply(body, new PhysicalInteraction(PhysicalInteraction.Type.CUT,
            ResourceId.of("naturalexceptions", "torso"), 100.0, 1.0, 0.9, 20.0), 0L);
        BiologicalInjury wound = body.injuries().get(0);
        double before = wound.bleedingMillilitresPerSecond();
        body.treatInjury(wound.id(), BiologicalTreatment.DRESS);
        assertTrue(wound.bleedingMillilitresPerSecond() < before);
        double severity = wound.severity();
        body.advanceTo(20L * 60L);
        assertTrue(wound.severity() < severity);
        assertTrue(body.bloodReserveMillilitres() < body.physiology().bloodCapacityMillilitres());
    }

    @Test void structuralInjuryImpairmentRecoversWithoutPerTickReplay() {
        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(), DeterministicId.derive("test", "leg"), 0L);
        double before = body.movementCapability();
        BiologicalInteractionResolver.apply(body, new PhysicalInteraction(PhysicalInteraction.Type.IMPACT,
            ResourceId.of("naturalexceptions", "left_leg"), 120.0, 0.0, 0.0, 20.0), 0L);
        assertTrue(body.injuries().stream().anyMatch(injury -> injury.kind() == BiologicalInjury.Kind.STRUCTURAL));
        assertTrue(body.movementCapability() < before);
        double impaired = body.movementCapability();
        body.treatInjury(body.injuries().get(0).id(), BiologicalTreatment.SPLINT);
        body.advanceTo(20L * 60L * 60L);
        assertTrue(body.movementCapability() > impaired);
    }

    @Test void woundsCloseAndBloodComesBackInGameTime() {
        BiologicalState body = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        BiologicalInteractionResolver.apply(body, new PhysicalInteraction(PhysicalInteraction.Type.CUT,
            ResourceId.of("naturalexceptions", "torso"), 100.0, 1.0, 0.9, 20.0), 0L);
        BiologicalInjury wound = body.injuries().get(0);
        body.treatInjury(wound.id(), BiologicalTreatment.DRESS);
        double estimate = body.secondsToHeal(wound);
        assertTrue(estimate > 1200.0 && estimate < 6.0 * 1200.0, "a dressed deep cut takes days, not weeks: " + estimate);
        long tick = 0L;
        for (int minute = 0; minute < 6 * 20 && !body.injuries().isEmpty(); minute++) {
            tick += 20L * 60L;
            body.replenishEnergyJoules(body.physiology().energyCapacityJoules());
            body.replenishHydrationMillilitres(body.physiology().hydrationCapacityMillilitres());
            body.advanceTo(tick);
        }
        assertTrue(body.injuries().isEmpty(), "healed within six Minecraft days");
        for (int minute = 0; minute < 5 * 20; minute++) {
            tick += 20L * 60L;
            body.replenishEnergyJoules(body.physiology().energyCapacityJoules());
            body.replenishHydrationMillilitres(body.physiology().hydrationCapacityMillilitres());
            body.advanceTo(tick);
        }
        assertEquals(body.physiology().bloodCapacityMillilitres(), body.bloodReserveMillilitres(), 1e-6,
            "blood comes back once nothing bleeds");
    }

    @Test void severeEnergyAndHydrationDeficitSlowRecovery() {
        BiologicalState nourished = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        BiologicalState depleted = new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
        ResourceId torso = ResourceId.of("naturalexceptions", "torso");
        PhysicalInteraction hit = new PhysicalInteraction(PhysicalInteraction.Type.CUT, torso, 100.0, 1.0, 0.9, 20.0);
        BiologicalInteractionResolver.apply(nourished, hit, 0L);
        BiologicalInteractionResolver.apply(depleted, hit, 0L);
        double healthySeverity = nourished.injuries().get(0).severity();
        double depletedSeverity = depleted.injuries().get(0).severity();
        depleted.restoreReserves(0.0, 0.0, depleted.bloodReserveMillilitres(), 0L);
        long elapsed = 20L * 60L;
        nourished.advanceTo(elapsed);
        depleted.advanceTo(elapsed);
        assertTrue(nourished.injuries().get(0).severity() < healthySeverity);
        assertTrue(depleted.injuries().get(0).severity() > nourished.injuries().get(0).severity());
        assertTrue(depleted.injuries().get(0).severity() < depletedSeverity);
    }
}
