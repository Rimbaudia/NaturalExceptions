package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.anomaly.EnvironmentalInfluence;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.anomaly.SpatialAnomaly;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventPhase;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class P8WorldStateNbtCodecTest {
    @Test
    void eventHistoryAnomaliesAndRegionalCausesSurviveSaveReload() {
        P8WorldState original = new P8WorldState(81L, 2, 0L);
        original.advance(0L);
        original.advance(60L * NaturalCalendar.TICKS_PER_DAY + 1L);
        DeterministicId anomalyId = DeterministicId.derive("p8-persistence-anomaly");
        original.anomalies().add(new SpatialAnomaly(anomalyId, ResourceId.of("naturalexceptions", "cow_lake"),
            -40, 70, 120, EnvironmentalInfluence.of(anomalyId, 0.7, 0, 0.2)));
        original.destabilization().increase(RegionKey.fromBlock(-1, 2),
            ResourceId.of("naturalexceptions", "test_cause"), DeterministicId.derive("p8-persistence-cause"),
            0.7, original.destabilization().lastAdvanceTick());

        NBTTagCompound encoded = P8WorldStateNbtCodec.write(original);
        P8WorldState restored = P8WorldStateNbtCodec.read(encoded, 81L, 2, 0L);
        assertEquals(original.events().snapshot().events(), restored.events().snapshot().events());
        assertTrue(restored.events().snapshot().events().stream().anyMatch(e ->
            P8WorldState.BLOOD_NIGHT.equals(e.type()) && e.phase() == EventPhase.ACTIVE));
        assertEquals(original.events().protectedStarRainCount(), restored.events().protectedStarRainCount());
        assertEquals(original.anomalies().snapshot(), restored.anomalies().snapshot());
        assertEquals(original.destabilization().snapshot(), restored.destabilization().snapshot());
        assertFalse(restored.events().schedule(original.events().snapshot().events().stream()
            .filter(e -> P8WorldState.BLOOD_NIGHT.equals(e.type())).findFirst().get()));
    }

    @Test
    void malformedP8RowsDegradeSafelyWithoutRebindingOrRerollingValidProtectedSlot() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", 1); root.setLong("Seed", 82L); root.setInteger("Epoch", 2);
        root.setLong("CreationTick", 0L); root.setLong("BloodYear", -1L); root.setLong("CalendarYear", -1L);
        NBTTagList regions = new NBTTagList(); NBTTagCompound invalid = new NBTTagCompound();
        invalid.setLong("X", Long.MAX_VALUE); invalid.setLong("Z", 0L); invalid.setDouble("Value", 0.9);
        invalid.setLong("Resolved", 0L); regions.appendTag(invalid); root.setTag("Regions", regions);
        NBTTagList anomalies = new NBTTagList(); NBTTagCompound unknown = new NBTTagCompound();
        unknown.setString("Id", "not-an-id"); anomalies.appendTag(unknown); root.setTag("Anomalies", anomalies);
        NBTTagList events = new NBTTagList(); NBTTagCompound malformed = new NBTTagCompound();
        malformed.setString("Id", "broken"); events.appendTag(malformed); root.setTag("Events", events);

        P8WorldState restored = P8WorldStateNbtCodec.read(root, 82L, 2, 0L);
        assertEquals(1, restored.events().protectedStarRainCount());
        assertEquals(0, restored.anomalies().size());
        assertEquals(0.0, restored.destabilization().derivedGlobal(), 0.0);
    }
}
