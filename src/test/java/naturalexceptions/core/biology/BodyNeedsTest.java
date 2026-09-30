package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

/** Hunger, thirst and blood that go somewhere; blows that bruise before they break. */
final class BodyNeedsTest {

    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId LEG = ResourceId.of("naturalexceptions", "left_leg");

    private static BiologicalState body() {
        return new BiologicalState(BuiltinBodyPlans.humanLike(), 0L);
    }

    @Test
    void restingSpendsAHungerPointEveryFiveMinutesAndWorkMore() {
        BiologicalState body = body();
        double point = body.physiology()
            .energyPerHungerPoint();
        double full = body.energyReserveJoules();
        body.advanceTo(20L * 300L);
        assertEquals(1.0, (full - body.energyReserveJoules()) / point, 0.05, "about a point in five minutes");
        double before = body.energyReserveJoules(), water = body.hydrationReserveMillilitres();
        body.spend(point * 2.0, 80.0); // eight exhaustion's worth of running
        assertEquals(before - 2.0 * point, body.energyReserveJoules(), 1.0e-6);
        assertTrue(body.hydrationReserveMillilitres() < water);
    }

    @Test
    void anArrowWoundBleedsThenClots() {
        BiologicalState body = body();
        BiologicalInteractionResolver.apply(
            body,
            new PhysicalInteraction(PhysicalInteraction.Type.PENETRATION, TORSO, 5 * 35.0, 0.3, 0.8, 20.0),
            0L);
        assertTrue(body.bleeding(), "an arrow opens a bleeding wound");
        double full = body.bloodReserveMillilitres();
        body.advanceTo(20L * 60L);
        double lostFirstMinute = full - body.bloodReserveMillilitres();
        assertTrue(lostFirstMinute > 20.0, "blood is lost in the first minute: " + lostFirstMinute);
        body.advanceTo(20L * 60L * 20L);
        double atTwenty = body.bloodReserveMillilitres();
        body.advanceTo(20L * 60L * 21L);
        double lostLater = atTwenty - body.bloodReserveMillilitres();
        assertTrue(lostLater < lostFirstMinute, "and it slows as it clots");
    }

    @Test
    void aSalveStanchesAndATonicRestoresBlood() {
        BiologicalState body = body();
        assertEquals(0, body.stanchBleeding(), "nothing to stanch");
        BiologicalInteractionResolver
            .apply(body, new PhysicalInteraction(PhysicalInteraction.Type.CUT, TORSO, 300.0, 0.1, 0.8, 20.0), 0L);
        double bleeding = body.injuries()
            .get(0)
            .bleedingMillilitresPerSecond();
        assertEquals(1, body.stanchBleeding());
        assertEquals(
            bleeding * 0.2,
            body.injuries()
                .get(0)
                .bleedingMillilitresPerSecond(),
            1.0e-9);
        body.advanceTo(20L * 60L * 30L);
        double low = body.bloodReserveMillilitres();
        body.addBloodRegeneration(600.0);
        body.advanceTo(20L * 60L * 31L);
        double capacity = body.physiology()
            .bloodCapacityMillilitres();
        assertTrue(
            body.bloodReserveMillilitres() >= Math.min(capacity, low + 550.0),
            "the tonic's blood is back within a minute");
    }

    @Test
    void anOrdinaryBlowBruisesAndAHardFallBreaks() {
        BiologicalState body = body();
        for (int i = 0; i < 8; i++) BiologicalInteractionResolver.apply(
            body,
            new PhysicalInteraction(PhysicalInteraction.Type.IMPACT, TORSO, 3 * 25.0, 0.0, 0.0, 20.0),
            i * 20L);
        for (BiologicalInjury injury : body.injuries())
            assertTrue(injury.severity() < 0.3, "a zombie's blows bruise: " + injury.severity());
        assertFalse(
            body.region(TORSO)
                .fractured(),
            "bruises alone never break a bone");
        BiologicalState faller = body();
        BiologicalInteractionResolver.apply(
            faller,
            new PhysicalInteraction(PhysicalInteraction.Type.IMPACT, LEG, 17 * 25.0 / 2.0, 0.0, 0.0, 20.0),
            0L);
        assertTrue(
            faller.injuries()
                .get(0)
                .severity() >= 0.75,
            "a twenty-block fall breaks a leg");
    }
}
