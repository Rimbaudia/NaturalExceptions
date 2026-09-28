package naturalexceptions.minecraft.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.StarRainImpactFactory;
import naturalexceptions.core.id.DeterministicId;

final class StarRainPresentationTest {
    @Test void sensoryCuesDoNotChangePersistedEventOrImpactIdentity() {
        EventRecord event = EventInstanceFactory.protectedFirstStarRain(521L, 2, 10L, 0L, 0L, 100L);
        DeterministicId eventId = event.id();
        assertTrue(StarRainPresentation.eventStartMessage(event).contains("Star Rain"));
        assertEquals(eventId, event.id());

        StarRainImpact impact = StarRainImpactFactory.create(event, 0, 0).get(0);
        DeterministicId impactId = impact.id();
        DeterministicId parentId = impact.parentEventId();
        assertTrue(StarRainPresentation.impactMessage(impact).contains("crater"));
        assertEquals(impactId, impact.id());
        assertEquals(parentId, impact.parentEventId());
    }
}
