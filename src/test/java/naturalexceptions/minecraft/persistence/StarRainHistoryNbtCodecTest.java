package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.StarRainHistory;

final class StarRainHistoryNbtCodecTest {

    @Test
    void impactIdentityAndMatterOwnershipSurviveNbtReload() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(5L, 2, 10L, 0L, 0L, 100L);
        StarRainHistory original = new StarRainHistory();
        naturalexceptions.core.event.WorldEventSchedule schedule = new naturalexceptions.core.event.WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        original.synchronize(schedule.snapshot(), 12, -9);
        naturalexceptions.core.event.StarRainImpact first = original.dueImpacts(Long.MAX_VALUE, 1)
            .get(0);
        original.resolve(first.id());
        original.transferToSpatialSite(first.id());

        StarRainHistory restored = StarRainHistoryNbtCodec.read(StarRainHistoryNbtCodec.write(original));
        naturalexceptions.core.event.StarRainImpact restoredFirst = restored.get(first.id());
        assertTrue(restoredFirst.isResolved());
        assertTrue(restoredFirst.isMaterialized());
        assertEquals(first.stellarMass(), restoredFirst.stellarMass());
        assertEquals(original.snapshot(), restored.snapshot());
    }

    @Test
    void acceleratedDueTimeSurvivesReloadWithoutChangingOriginalImpactParameters() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(15L, 2, 10L, 0L, 0L, 100L);
        naturalexceptions.core.event.WorldEventSchedule schedule = new naturalexceptions.core.event.WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        StarRainHistory original = new StarRainHistory();
        original.synchronize(schedule.snapshot(), 25, -7);
        naturalexceptions.core.event.StarRainImpact first = original.dueImpacts(Long.MAX_VALUE, 1)
            .get(0);
        long due = first.dueTick();
        long now = due - 1L;

        assertTrue(original.expedite(first.id(), now));
        StarRainHistory restored = StarRainHistoryNbtCodec.read(StarRainHistoryNbtCodec.write(original));
        naturalexceptions.core.event.StarRainImpact reloaded = restored.get(first.id());
        assertEquals(first, reloaded);
        assertEquals(due, reloaded.dueTick());
        assertEquals(now, reloaded.effectiveDueTick());
        assertEquals(first.stellarMass(), reloaded.stellarMass());
        assertEquals(
            first.id(),
            restored.dueImpacts(now, 2)
                .get(0)
                .id());
    }

    @Test
    void versionOneHistoryWithoutDueOverrideStillLoadsAtOriginalDueTime() {
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(16L, 2, 10L, 0L, 0L, 100L);
        naturalexceptions.core.event.WorldEventSchedule schedule = new naturalexceptions.core.event.WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        StarRainHistory original = new StarRainHistory();
        original.synchronize(schedule.snapshot(), 0, 0);
        NBTTagCompound legacy = StarRainHistoryNbtCodec.write(original);
        legacy.setInteger("Version", 1);

        StarRainHistory restored = StarRainHistoryNbtCodec.read(legacy);

        assertEquals(original.snapshot(), restored.snapshot());
        assertEquals(original.nextDueTick(), restored.nextDueTick());
    }

}
