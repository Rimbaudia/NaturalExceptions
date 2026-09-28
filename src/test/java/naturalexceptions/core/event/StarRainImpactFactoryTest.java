package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

final class StarRainImpactFactoryTest {

    @Test
    void impactIdentityAndParametersAreStableForOnePersistedParentEvent() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(71L, 2, 100L, 0L, 100L, 500L);
        List<StarRainImpact> first = StarRainImpactFactory.create(rain, 900, -400);
        List<StarRainImpact> reloaded = StarRainImpactFactory.create(rain, 900, -400);
        EventRecord otherRain = EventInstanceFactory.protectedFirstStarRain(72L, 2, 100L, 0L, 100L, 500L);
        List<StarRainImpact> otherWorld = StarRainImpactFactory.create(otherRain, 900, -400);

        assertEquals(4, first.size());
        assertEquals(first, reloaded);
        assertNotEquals(first.get(0).id(), otherWorld.get(0).id());
        Set<DeterministicId> identities = new HashSet<>();
        for (StarRainImpact impact : first) {
            assertTrue(identities.add(impact.id()));
            assertEquals(rain.id(), impact.parentEventId());
            assertTrue(impact.dueTick() > rain.scheduledAtTick());
            assertTrue(impact.dueTick() < rain.endsAtTick());
            assertTrue(Math.abs(impact.x() - 900) <= 96);
            assertTrue(Math.abs(impact.z() + 400) <= 96);
            assertTrue(impact.stellarMass().isPositive());
        }
    }

    @Test
    void impactSiteTransferMovesFiniteMassExactlyOnceAndResolutionIsIdempotent() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(7L, 2, 0L, 0L, 100L, 500L);
        StarRainImpact impact = StarRainImpactFactory.create(rain, 0, 0).get(0);
        MaterialQuantity original = impact.stellarMass();

        assertTrue(impact.resolve());
        assertTrue(!impact.resolve());
        assertEquals(original, impact.transferToSpatialSite());
        assertEquals(MaterialQuantity.ZERO, impact.transferToSpatialSite());
        assertEquals(MaterialQuantity.ZERO, impact.stellarMass());
        assertTrue(impact.isMaterialized());
    }
}
