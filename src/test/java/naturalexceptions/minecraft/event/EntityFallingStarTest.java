package naturalexceptions.minecraft.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;

final class EntityFallingStarTest {

    @Test
    void flightStartsSlowlyAndEndsExactlyOnTheImpactPoint() {
        assertEquals(0.0, EntityFallingStar.progress(0), 0.0);
        assertEquals(1.0, EntityFallingStar.progress(EntityFallingStar.FLIGHT_TICKS), 0.0);
        assertEquals(1.0, EntityFallingStar.progress(EntityFallingStar.FLIGHT_TICKS + 10), 0.0);
        double previous = 0.0;
        for (int age = 1; age <= EntityFallingStar.FLIGHT_TICKS; age++) {
            double p = EntityFallingStar.progress(age);
            assertTrue(p > previous, "always moving toward the target");
            previous = p;
        }
        assertTrue(EntityFallingStar.progress(EntityFallingStar.FLIGHT_TICKS / 2) < 0.5, "accelerates as it falls");
    }

    @Test
    void unknownImpactsAreNotInFlight() {
        assertFalse(EntityFallingStar.isInFlight(null));
        assertFalse(EntityFallingStar.isInFlight(DeterministicId.derive("test", "nothing")));
    }
}
