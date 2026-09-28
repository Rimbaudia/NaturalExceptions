package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;

final class BiologicalInteractionTest {
    private static final ResourceId TORSO = ResourceId.of("naturalexceptions", "torso");
    private static final ResourceId LEFT_ARM = ResourceId.of("naturalexceptions", "left_arm");

    private static BiologicalState human(String id) {
        return new BiologicalState(BuiltinBodyPlans.humanLike(), DeterministicId.derive("organism", id), 0L);
    }

    private static PhysicalInteraction cut(double energy) {
        return new PhysicalInteraction(PhysicalInteraction.Type.CUT, TORSO, energy, 1.0, 0.9, 0.0);
    }

    private static ArmorLayer ironPlate() {
        CanonicalComposition iron = CanonicalComposition.of(ResourceId.of("minecraft", "iron"));
        CanonicalMaterialKey key = CanonicalMaterialKey.of(iron, MaterialPhase.SOLID, null, null);
        ComponentInstance plate = new ComponentInstance(DeterministicId.derive("armor", "iron-plate"),
            GeometryId.PLATE, MaterialRef.authored("minecraft", "iron"), key, MaterialQuantity.ofGrams(156),
            ManufacturingState.FINISHED, new ComponentDamage());
        MaterialProperties material = MaterialProperties.builder().density(7.8).hardness(4.0).build();
        return new ArmorLayer(plate, material, 0.01, Collections.singleton(TORSO));
    }

    @Test
    void armorStopsAWeakCutWhileReceivingComponentStress() {
        BiologicalState body = human("weak-cut");
        ArmorLayer armor = ironPlate();
        body.equipArmor(armor);

        BiologicalInteractionResolver.apply(body, cut(10.0), 0L);

        assertTrue(armor.component().damage().cracking() > 0.0);
        assertTrue(body.injuries().isEmpty());
        assertEquals(1.0, body.region(TORSO).structuralIntegrity(), 0.0);
    }

    @Test void strongerCutCanDamageArmorWithoutReachingTissue() {
        BiologicalState body = human("armor-stress-only");
        ArmorLayer armor = ironPlate();
        body.equipArmor(armor);
        PhysicalInteraction broadEdge = new PhysicalInteraction(PhysicalInteraction.Type.CUT,
            TORSO, 100.0, 0.0, 0.9, 0.0);
        BiologicalInteractionResolver.apply(body, broadEdge, 0L);
        assertTrue(armor.component().damage().cracking() > 0.0);
        assertFalse(armor.component().damage().isFractured());
        assertTrue(body.injuries().isEmpty());
    }

    @Test
    void penetrativeCutFracturesArmorAndCreatesOnlyLocalizedBleedingWound() {
        BiologicalState body = human("penetrating-cut");
        ArmorLayer armor = ironPlate();
        body.equipArmor(armor);

        BiologicalInteractionResolver.apply(body, cut(160.0), 20L);

        assertTrue(armor.component().damage().isFractured());
        assertEquals(1, body.injuries().size());
        BiologicalInjury wound = body.injuries().get(0);
        assertEquals(BiologicalInjury.Kind.CUT, wound.kind());
        assertEquals(TORSO, wound.regionId());
        assertTrue(wound.bleedingMillilitresPerSecond() > 0.0);
        assertEquals(1.0, body.region(LEFT_ARM).structuralIntegrity(), 0.0);
    }

    @Test
    void sameCutProducesFleshWoundInHumanAndStructuralCrackInCrystallineOrganism() {
        BiologicalState human = human("same-cut-human");
        BiologicalState crystal = new BiologicalState(BuiltinBodyPlans.crystalline(),
            DeterministicId.derive("organism", "same-cut-crystal"), 0L);

        BiologicalInteractionResolver.apply(human, cut(100.0), 0L);
        BiologicalInteractionResolver.apply(crystal, cut(100.0), 0L);

        assertTrue(human.injuries().stream().anyMatch(i -> i.kind() == BiologicalInjury.Kind.CUT));
        assertTrue(human.injuries().get(0).bleedingMillilitresPerSecond() > 0.0);
        assertTrue(crystal.injuries().stream().anyMatch(i -> i.kind() == BiologicalInjury.Kind.STRUCTURAL));
        assertFalse(crystal.injuries().stream().anyMatch(i -> i.kind() == BiologicalInjury.Kind.CUT));
        assertEquals(0.0, crystal.bloodReserveMillilitres(), 0.0);
    }

    @Test void thermalContactUsesPhysicalInteractionAndCreatesABurnProcess() {
        BiologicalState body = human("thermal-contact");
        BiologicalInteractionResolver.apply(body, new PhysicalInteraction(PhysicalInteraction.Type.THERMAL,
            LEFT_ARM, 50.0, 0.0, 0.0, 160.0), 0L);
        assertTrue(body.injuries().stream().anyMatch(i -> i.kind() == BiologicalInjury.Kind.BURN));
        assertFalse(body.injuries().stream().anyMatch(i -> i.regionId().equals(TORSO)));
    }
}
