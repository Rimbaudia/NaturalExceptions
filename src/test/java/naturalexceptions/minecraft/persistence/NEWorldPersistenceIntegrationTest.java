package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinEvents;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.scheduler.ScheduledEvent;
import naturalexceptions.minecraft.tick.NEWorldTickHandler;

final class NEWorldPersistenceIntegrationTest {

    @Test
    void delayedEventSaveReloadFiresExactlyOnceWithPreservedDeterministicIdentity() {
        // Step 1: Initialize world data and scheduler
        NEWorldSavedData worldData1 = new NEWorldSavedData();
        WorldMetadata originalMeta = WorldMetadata.createNew("integration-world-1", "N0.01.0-dev");
        worldData1.setMetadata(originalMeta);

        // Step 2: Schedule deterministic delayed test event at tick 1000
        DeterministicId expectedEventId = DeterministicId.derive("integration-world-1", "test/delayed_event", "1000");
        ScheduledEvent testEvent = new ScheduledEvent(
            expectedEventId,
            BuiltinEvents.DELAYED_TEST_EVENT,
            1000L,
            "test-payload-1");
        worldData1.scheduler()
            .schedule(testEvent);

        List<ScheduledEvent> firedEvents = new ArrayList<>();
        SubsystemProfiler profiler = new SubsystemProfiler();
        NEWorldTickHandler tickHandler = new NEWorldTickHandler(profiler, 64, firedEvents::add);

        // Step 3: Advance time to tick 500 (event is NOT due)
        tickHandler.tickWorld(worldData1, 500L);
        assertEquals(0, firedEvents.size(), "Event must not fire before due tick");
        assertEquals(
            1,
            worldData1.scheduler()
                .pendingCount());
        assertEquals(
            1000L,
            worldData1.scheduler()
                .peekNextDueTick());

        // Step 4: Save state before firing
        NBTTagCompound saveNbt = new NBTTagCompound();
        worldData1.writeToNBT(saveNbt);

        // Step 5: Reload into fresh world data instance
        NEWorldSavedData worldData2 = new NEWorldSavedData();
        worldData2.readFromNBT(saveNbt);

        // Verify preserved metadata and event identity
        assertEquals(originalMeta, worldData2.metadata());
        assertEquals(
            1,
            worldData2.scheduler()
                .pendingCount());
        assertEquals(
            1000L,
            worldData2.scheduler()
                .peekNextDueTick());

        // Step 6: Advance time to tick 1000 on the reloaded instance
        tickHandler.tickWorld(worldData2, 1000L);
        assertEquals(1, firedEvents.size(), "Event must fire when due tick is reached");
        ScheduledEvent fired = firedEvents.get(0);
        assertEquals(expectedEventId, fired.id(), "Event must retain exact deterministic instance identity");
        assertEquals(BuiltinEvents.DELAYED_TEST_EVENT, fired.eventType());
        assertEquals("test-payload-1", fired.payload());
        assertEquals(
            0,
            worldData2.scheduler()
                .pendingCount(),
            "Event must be consumed from scheduler");
        assertTrue(worldData2.isDirty(), "WorldSavedData must be marked dirty after state modification");

        // Step 7: Save state after firing
        NBTTagCompound postFireNbt = new NBTTagCompound();
        worldData2.writeToNBT(postFireNbt);

        // Step 8: Reload a second time after firing
        NEWorldSavedData worldData3 = new NEWorldSavedData();
        worldData3.readFromNBT(postFireNbt);
        assertEquals(
            0,
            worldData3.scheduler()
                .pendingCount(),
            "Consumed event must not reappear on reload");

        // Step 9: Advance time further to tick 1500
        tickHandler.tickWorld(worldData3, 1500L);
        assertEquals(1, firedEvents.size(), "Event must not fire a second time");
    }
}
