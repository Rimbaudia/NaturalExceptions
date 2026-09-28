package naturalexceptions.minecraft.persistence;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.anomaly.DestabilizationCause;
import naturalexceptions.core.anomaly.DestabilizationField;
import naturalexceptions.core.anomaly.EnvironmentalInfluence;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.anomaly.SpatialAnomaly;
import naturalexceptions.core.anomaly.SpatialAnomalyIndex;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventCooldownHistory;
import naturalexceptions.core.event.EventPhase;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.EventScheduleSnapshot;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.event.WorldEventSchedule;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/** Defensive bounded codec for persistent world-scoped P8 history. */
public final class P8WorldStateNbtCodec {
    private static final int VERSION = 1;
    private static final int MAX_CAUSES_TOTAL = 65536;
    private P8WorldStateNbtCodec() { }

    public static NBTTagCompound write(P8WorldState state) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", VERSION);
        root.setLong("Seed", state.seed());
        root.setInteger("Epoch", state.epoch());
        root.setLong("CreationTick", state.creationTick());
        root.setLong("BloodYear", state.scheduledBloodNightYear());
        root.setLong("CalendarYear", state.lastCalendarYear());
        writeEvents(state.events().snapshot(), root);
        writeAnomalies(state.anomalies().snapshot(), root);
        writeDestabilization(state.destabilization().snapshot(), root);
        return root;
    }

    public static P8WorldState read(NBTTagCompound root, long seed, int epoch, long creationTick) {
        if (root == null || root.getInteger("Version") > VERSION) return new P8WorldState(seed, epoch, creationTick);
        if (root.hasKey("Seed", 99) && root.getLong("Seed") != seed
            || root.hasKey("Epoch", 99) && root.getInteger("Epoch") != epoch) {
            return new P8WorldState(seed, epoch, creationTick);
        }
        WorldEventSchedule schedule = readEvents(root);
        SpatialAnomalyIndex index = readAnomalies(root);
        DestabilizationField field = readDestabilization(root);
        P8WorldState result = new P8WorldState(seed, epoch, creationTick, schedule, index, field);
        if (root.hasKey("Seed", 99) && root.getLong("Seed") == seed
            && root.hasKey("Epoch", 99) && root.getInteger("Epoch") == epoch) {
            result.restoreMarkers(root.hasKey("BloodYear", 99) ? root.getLong("BloodYear") : -1L,
                root.hasKey("CalendarYear", 99) ? root.getLong("CalendarYear") : -1L);
        }
        return result;
    }

    private static void writeEvents(EventScheduleSnapshot snapshot, NBTTagCompound root) {
        NBTTagList list = new NBTTagList();
        for (EventRecord event : snapshot.events()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", event.id().hex()); tag.setString("Type", event.type().toString());
            tag.setBoolean("Global", event.targetRegion().isGlobal());
            tag.setInteger("RegionX", event.targetRegion().x()); tag.setInteger("RegionZ", event.targetRegion().z());
            tag.setBoolean("Protected", event.protectedOpportunity());
            tag.setLong("Start", event.scheduledAtTick()); tag.setLong("End", event.endsAtTick());
            tag.setString("Phase", event.phase().name()); tag.setLong("PhaseTick", event.phaseChangedAtTick());
            tag.setLong("ParameterSeed", event.parameterSeed()); tag.setLong("Cooldown", event.cooldownUntilTick());
            if (event.temporalInfluence() != null) tag.setString("Influence", event.temporalInfluence().toString());
            tag.setInteger("InfluenceStrength", event.temporalInfluenceStrength());
            list.appendTag(tag);
        }
        root.setTag("Events", list); root.setLong("EventTick", snapshot.lastAdvancedTick());
        NBTTagList history = new NBTTagList();
        for (EventCooldownHistory item : snapshot.cooldownHistory()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Type", item.type().toString()); tag.setBoolean("Global", item.region().isGlobal());
            tag.setInteger("RegionX", item.region().x()); tag.setInteger("RegionZ", item.region().z());
            tag.setLong("Last", item.lastOccurrenceTick()); tag.setLong("Until", item.cooldownUntilTick());
            history.appendTag(tag);
        }
        root.setTag("Cooldowns", history);
    }

    private static WorldEventSchedule readEvents(NBTTagCompound root) {
        List<EventRecord> records = new ArrayList<>();
        List<EventCooldownHistory> cooldowns = new ArrayList<>();
        if (root.hasKey("Events", 9)) {
            NBTTagList list = root.getTagList("Events", 10);
            for (int i = 0; i < Math.min(list.tagCount(), WorldEventSchedule.DEFAULT_MAX_RETAINED_EVENTS); i++) {
                try {
                    NBTTagCompound t = list.getCompoundTagAt(i);
                    ResourceId influence = t.hasKey("Influence", 8) ? ResourceId.parse(t.getString("Influence")) : null;
                    records.add(new EventRecord(DeterministicId.parse(t.getString("Id")),
                        ResourceId.parse(t.getString("Type")), region(t), t.getBoolean("Protected"),
                        t.getLong("Start"), t.getLong("End"), EventPhase.valueOf(t.getString("Phase")),
                        t.getLong("PhaseTick"), t.getLong("ParameterSeed"), t.getLong("Cooldown"), influence,
                        t.getInteger("InfluenceStrength")));
                } catch (RuntimeException ignored) { }
            }
        }
        if (root.hasKey("Cooldowns", 9)) {
            NBTTagList list = root.getTagList("Cooldowns", 10);
            for (int i = 0; i < Math.min(list.tagCount(), WorldEventSchedule.DEFAULT_MAX_COOLDOWN_HISTORY); i++) {
                try {
                    NBTTagCompound t = list.getCompoundTagAt(i);
                    cooldowns.add(new EventCooldownHistory(ResourceId.parse(t.getString("Type")), region(t),
                        t.getLong("Last"), t.getLong("Until")));
                } catch (RuntimeException ignored) { }
            }
        }
        try {
            return WorldEventSchedule.restore(new EventScheduleSnapshot(records, cooldowns,
                Math.max(0L, root.getLong("EventTick"))));
        } catch (RuntimeException ignored) { return new WorldEventSchedule(); }
    }

    private static EventRegion region(NBTTagCompound tag) {
        return tag.getBoolean("Global") ? EventRegion.global()
            : EventRegion.of(tag.getInteger("RegionX"), tag.getInteger("RegionZ"));
    }

    private static void writeAnomalies(List<SpatialAnomaly> anomalies, NBTTagCompound root) {
        NBTTagList list = new NBTTagList(); int count = 0;
        for (SpatialAnomaly anomaly : anomalies) {
            if (count++ >= P8WorldState.MAX_RETAINED_ANOMALIES) break;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", anomaly.id().hex()); tag.setString("Type", anomaly.type().toString());
            tag.setInteger("X", anomaly.centerX()); tag.setInteger("Z", anomaly.centerZ());
            tag.setInteger("Radius", anomaly.radiusBlocks()); tag.setString("Source", anomaly.influence().sourceId().hex());
            tag.setDouble("Water", anomaly.influence().waterAvailabilityDelta());
            tag.setDouble("Night", anomaly.influence().nocturnalReactivityDelta());
            tag.setDouble("Moisture", anomaly.influence().moistureDelta()); list.appendTag(tag);
        }
        root.setTag("Anomalies", list);
    }

    private static SpatialAnomalyIndex readAnomalies(NBTTagCompound root) {
        SpatialAnomalyIndex result = new SpatialAnomalyIndex();
        if (!root.hasKey("Anomalies", 9)) return result;
        NBTTagList list = root.getTagList("Anomalies", 10);
        for (int i = 0; i < Math.min(list.tagCount(), P8WorldState.MAX_RETAINED_ANOMALIES); i++) {
            try {
                NBTTagCompound t = list.getCompoundTagAt(i);
                DeterministicId id = DeterministicId.parse(t.getString("Id"));
                result.add(new SpatialAnomaly(id, ResourceId.parse(t.getString("Type")), t.getInteger("X"),
                    t.getInteger("Z"), t.getInteger("Radius"), EnvironmentalInfluence.of(
                        DeterministicId.parse(t.getString("Source")), t.getDouble("Water"), t.getDouble("Night"),
                        t.getDouble("Moisture"))));
            } catch (RuntimeException ignored) { }
        }
        return result;
    }

    private static void writeDestabilization(DestabilizationField.Snapshot snapshot, NBTTagCompound root) {
        root.setLong("DestabTick", snapshot.lastAdvanceTick());
        NBTTagList regions = new NBTTagList(); int totalCauses = 0;
        for (DestabilizationField.RegionState state : snapshot.regions()) {
            NBTTagCompound tag = new NBTTagCompound(); tag.setLong("X", state.key().x());
            tag.setLong("Z", state.key().z()); tag.setDouble("Value", state.value());
            tag.setLong("Resolved", state.lastResolvedTick());
            NBTTagList causes = new NBTTagList();
            for (DestabilizationCause cause : state.causes()) {
                if (totalCauses++ >= MAX_CAUSES_TOTAL) break;
                NBTTagCompound c = new NBTTagCompound(); c.setString("Type", cause.type().toString());
                c.setString("Source", cause.sourceId().hex()); c.setDouble("Amount", cause.amount());
                c.setLong("Tick", cause.tick()); causes.appendTag(c);
            }
            tag.setTag("Causes", causes); regions.appendTag(tag);
            if (totalCauses >= MAX_CAUSES_TOTAL) break;
        }
        root.setTag("Regions", regions);
    }

    private static DestabilizationField readDestabilization(NBTTagCompound root) {
        List<DestabilizationField.RegionState> regions = new ArrayList<>(); int causeCount = 0;
        if (root.hasKey("Regions", 9)) {
            NBTTagList list = root.getTagList("Regions", 10);
            for (int i = 0; i < Math.min(list.tagCount(), 65536); i++) {
                try {
                    NBTTagCompound t = list.getCompoundTagAt(i); List<DestabilizationCause> causes = new ArrayList<>();
                    if (t.hasKey("Causes", 9)) {
                        NBTTagList cs = t.getTagList("Causes", 10);
                        for (int j = 0; j < cs.tagCount() && causeCount < MAX_CAUSES_TOTAL; j++, causeCount++) {
                            try { NBTTagCompound c = cs.getCompoundTagAt(j); causes.add(new DestabilizationCause(
                                ResourceId.parse(c.getString("Type")), DeterministicId.parse(c.getString("Source")),
                                c.getDouble("Amount"), c.getLong("Tick"))); } catch (RuntimeException ignored) { }
                        }
                    }
                    regions.add(new DestabilizationField.RegionState(new RegionKey(t.getLong("X"), t.getLong("Z")),
                        t.getDouble("Value"), t.getLong("Resolved"), causes));
                } catch (RuntimeException ignored) { }
            }
        }
        try { return DestabilizationField.restore(new DestabilizationField.Snapshot(
            Math.max(0L, root.getLong("DestabTick")), regions)); }
        catch (RuntimeException ignored) { return new DestabilizationField(); }
    }
}
