package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Regression: hits stopped hurting once injuries piled up, leaving players and mobs unkillable. */
final class HitDamageTest {

    @Test
    void aHitCostsAtLeastItsVanillaDamage() {
        assertEquals(6.0F, BiologicalEvents.hitDamage(6.0F, 1.0F, 20.0F, 0.0), 1.0e-6F, "no modelled loss");
        assertEquals(6.0F, BiologicalEvents.hitDamage(6.0F, 1.0F, 20.0F, 0.01), 1.0e-6F, "tiny modelled loss");
        assertEquals(10.0F, BiologicalEvents.hitDamage(6.0F, 1.0F, 20.0F, 0.5), 1.0e-6F, "worse injury counts");
        assertEquals(3.0F, BiologicalEvents.hitDamage(6.0F, 0.5F, 20.0F, 0.0), 1.0e-6F, "armour still helps");
    }

    @Test
    void vanillaArmourShare() {
        assertEquals(1.0F, BiologicalEvents.vanillaArmourFactor(0), 1.0e-6F);
        assertEquals(0.2F, BiologicalEvents.vanillaArmourFactor(20), 1.0e-6F, "full diamond keeps 20%");
        assertTrue(BiologicalEvents.vanillaArmourFactor(99) > 0.0F, "never total immunity");
    }

    @Test
    void aBadlyHurtBodyStillLosesHealthToEveryHit() {
        BiologicalState state = new BiologicalState(
            BodyPlanRegistry.standard()
                .require(BuiltinBodyPlans.HUMAN_ID),
            0L);
        ResourceId torso = ResourceId.of("naturalexceptions", "torso");
        for (int i = 0; i < 12; i++) state.addInjury(
            new BiologicalInjury(
                DeterministicId.derive("pile", Integer.toString(i)),
                BiologicalInjury.Kind.BURN,
                torso,
                0.9,
                0.0,
                0L));
        double capped = state.condition();
        state.addInjury(
            new BiologicalInjury(
                DeterministicId.derive("pile", "more"),
                BiologicalInjury.Kind.BURN,
                torso,
                0.9,
                0.0,
                0L));
        double lost = capped - state.condition();
        assertEquals(0.0, lost, 1.0e-9, "the injury burden is capped, so condition stops falling");
        float health = 6.0F;
        for (int hit = 0; hit < 3; hit++) health -= BiologicalEvents.hitDamage(3.0F, 1.0F, 20.0F, lost);
        assertTrue(health <= 0.0F, "three zombie-strength hits still kill someone on three hearts");
    }
}
