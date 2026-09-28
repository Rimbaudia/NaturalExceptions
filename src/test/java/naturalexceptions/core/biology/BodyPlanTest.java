package naturalexceptions.core.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.ResourceId;

final class BodyPlanTest {
    @Test
    void standardRegistryLoadsStableHumanAndCrystallinePlansWithDifferentPhysiology() {
        BodyPlanRegistry registry = BodyPlanRegistry.standard();
        BodyPlan human = registry.require(ResourceId.of("naturalexceptions", "human"));
        BodyPlan crystal = registry.require(ResourceId.of("naturalexceptions", "crystalline"));

        assertEquals(6, human.regions().size());
        assertEquals(6, crystal.regions().size());
        assertTrue(human.physiology().canBleed());
        assertFalse(crystal.physiology().canBleed());
        assertNotEquals(human.structuralMaterial().hardness(), crystal.structuralMaterial().hardness());
        assertEquals(human, registry.require(ResourceId.of("naturalexceptions", "human")));
    }

    @Test
    void plansRejectDuplicateOrUnboundedRegionDefinitions() {
        BodyRegionDefinition head = new BodyRegionDefinition(ResourceId.of("naturalexceptions", "head"),
            10.0, 20.0, 1.0, 1.0, 1.0);
        PhysiologyProfile physiology = PhysiologyProfile.humanLike();
        naturalexceptions.core.property.MaterialProperties material = naturalexceptions.core.property.MaterialProperties
            .builder().density(1).hardness(1).build();

        assertThrows(IllegalArgumentException.class, () -> new BodyPlan(ResourceId.of("test", "duplicate"),
            Arrays.asList(head, head), physiology, material));
        assertThrows(IllegalArgumentException.class, () -> new BodyPlan(ResourceId.of("test", "too_many"),
            Arrays.asList(head, head.withId(ResourceId.of("test", "a")), head.withId(ResourceId.of("test", "b")),
                head.withId(ResourceId.of("test", "c")), head.withId(ResourceId.of("test", "d")),
                head.withId(ResourceId.of("test", "e")), head.withId(ResourceId.of("test", "f")),
                head.withId(ResourceId.of("test", "g")), head.withId(ResourceId.of("test", "h"))),
            physiology, material));
        BodyPlanRegistry registry = BodyPlanRegistry.standard();
        assertThrows(IllegalArgumentException.class, () -> registry.register(new BodyPlan(BuiltinBodyPlans.HUMAN_ID,
            Arrays.asList(head), physiology, material)));
    }
}
