package naturalexceptions.minecraft.biology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BodyPlanRegistry;
import naturalexceptions.core.biology.BuiltinBodyPlans;
import naturalexceptions.core.biology.ExposureProtection;
import naturalexceptions.core.biology.ExposureRoute;

final class CombustionInhalationTest {
    @Test void nearbyCombustionAccumulatesDoseAndFilterMaskReducesRatherThanBlocksIt() {
        BiologicalState bare = new BiologicalState(BodyPlanRegistry.standard().require(BuiltinBodyPlans.HUMAN_ID), 0L);
        BiologicalState masked = new BiologicalState(BodyPlanRegistry.standard().require(BuiltinBodyPlans.HUMAN_ID), 0L);
        masked.addExposureProtection(new ExposureProtection(ExposureRoute.INHALATION, 0.75));

        double firstBare = BiologicalEvents.applyCombustionSmoke(bare, 20L);
        double firstMasked = BiologicalEvents.applyCombustionSmoke(masked, 20L);
        double secondBare = BiologicalEvents.applyCombustionSmoke(bare, 40L);

        assertTrue(firstBare > firstMasked);
        assertTrue(firstMasked > 0.0, "a physical filter reduces dose but is not immunity");
        assertEquals(firstBare * 0.25, firstMasked, 1.0e-9);
        assertEquals(firstBare + secondBare, bare.exposureEffect(), 2.0e-5);
    }
}
