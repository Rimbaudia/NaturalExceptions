package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.List;

import naturalexceptions.core.anomaly.DestabilizationField;
import naturalexceptions.core.anomaly.EnvironmentalInfluence;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.anomaly.SpatialAnomaly;
import naturalexceptions.core.anomaly.SpatialAnomalyIndex;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.calendar.StrangeSeason;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.worldgen.HydrologicalContext;
import naturalexceptions.core.worldgen.OverworldGeneration;
import naturalexceptions.core.worldgen.WorldgenSite;
import naturalexceptions.core.worldgen.WorldgenSites;

/** World-owned P8 semantic state. Time is supplied by the persisted Minecraft world clock. */
public final class P8WorldState {

    public static final ResourceId BLOOD_NIGHT = ResourceId.of("naturalexceptions", "blood_night");
    public static final ResourceId ASHEN_SEASON = ResourceId.of("naturalexceptions", "ashen_autumn");
    public static final ResourceId NIGHT_REACTIVITY = ResourceId.of("naturalexceptions", "night_reactivity");
    public static final int MAX_RETAINED_ANOMALIES = 16384;

    private final long seed;
    private final int epoch;
    private final long creationTick;
    private final WorldEventSchedule events;
    private final SpatialAnomalyIndex anomalies;
    private final DestabilizationField destabilization;
    private long scheduledBloodNightYear = -1;
    private long lastCalendarYear = -1;
    /** Transient: re-derived after load, because regular Star Rain scheduling is idempotent by identity. */
    private long lastStarRainCheckDay = -1;

    public P8WorldState(long seed, int epoch, long creationTick) {
        this(
            seed,
            epoch,
            creationTick,
            new WorldEventSchedule(),
            new SpatialAnomalyIndex(),
            new DestabilizationField());
    }

    public P8WorldState(long seed, int epoch, long creationTick, WorldEventSchedule events,
        SpatialAnomalyIndex anomalies, DestabilizationField destabilization) {
        if (epoch <= 0 || creationTick < 0 || events == null || anomalies == null || destabilization == null) {
            throw new IllegalArgumentException("Invalid P8 world state");
        }
        this.seed = seed;
        this.epoch = epoch;
        this.creationTick = creationTick;
        this.events = events;
        this.anomalies = anomalies;
        this.destabilization = destabilization;
        if (this.events.protectedStarRainCount() == 0) {
            this.events.ensureProtectedFirstStarRain(
                EventInstanceFactory.protectedFirstStarRain(
                    seed,
                    epoch,
                    creationTick,
                    7L * NaturalCalendar.TICKS_PER_DAY,
                    48L * NaturalCalendar.TICKS_PER_DAY,
                    NaturalCalendar.TICKS_PER_DAY));
        }
    }

    /** Coarse server wake-up. Event identities/history are persisted; missed active intervals are not replayed. */
    public synchronized boolean advance(long nowTick) {
        return advance(nowTick, java.util.Collections.<RegionKey>emptyList());
    }

    /**
     * As {@link #advance(long)}, where {@code witnessedRegions} are the regions the event was actually experienced
     * in (for a world-wide event: where players are). A completed Blood Night destabilizes its own target region,
     * or for a world-wide one the witnessed regions; never an unrelated region.
     */
    public synchronized boolean advance(long nowTick, java.util.Collection<RegionKey> witnessedRegions) {
        if (nowTick < 0) throw new IllegalArgumentException("time must be nonnegative");
        boolean changed = false;
        NaturalCalendar.Position calendar = NaturalCalendar.at(nowTick);
        if (calendar.year() != lastCalendarYear) {
            lastCalendarYear = calendar.year();
            changed |= scheduleNextBloodNight(calendar.year(), nowTick);
        }
        if (calendar.absoluteDay() != lastStarRainCheckDay) {
            lastStarRainCheckDay = calendar.absoluteDay();
            changed |= scheduleNextRegularStarRain(nowTick);
        }
        for (EventRecord event : events.advance(nowTick)) {
            changed = true;
            if (event.phase() == EventPhase.COMPLETED && BLOOD_NIGHT.equals(event.type())) {
                for (RegionKey target : destabilizationTargets(event, witnessedRegions)) {
                    destabilization.increase(target, BLOOD_NIGHT, event.id(), 0.38, event.endsAtTick());
                }
            }
        }
        boolean coarseUpdate = destabilization.isCoarseUpdateDue(nowTick);
        if (coarseUpdate) {
            destabilization.advanceCoarse(nowTick);
            changed = true;
        }
        if (coarseUpdate && destabilization.derivedGlobal() >= 0.22 && !hasCurrentStrangeSeason(nowTick)) {
            long ordinal = Math.max(0, nowTick / (NaturalCalendar.TICKS_PER_DAY * 8L));
            EventRecord strange = EventInstanceFactory.regular(
                seed,
                epoch,
                ASHEN_SEASON,
                EventRegion.global(),
                ordinal,
                nowTick,
                24L * NaturalCalendar.TICKS_PER_DAY,
                NaturalCalendar.DAYS_PER_YEAR * NaturalCalendar.TICKS_PER_DAY,
                null,
                0);
            changed |= events.schedule(strange);
        }
        return changed;
    }

    private boolean scheduleNextBloodNight(long year, long nowTick) {
        long targetYear = year;
        long due = targetYear * NaturalCalendar.DAYS_PER_YEAR * NaturalCalendar.TICKS_PER_DAY
            + 60L * NaturalCalendar.TICKS_PER_DAY;
        if (due <= nowTick) {
            targetYear++;
            due = targetYear * NaturalCalendar.DAYS_PER_YEAR * NaturalCalendar.TICKS_PER_DAY
                + 60L * NaturalCalendar.TICKS_PER_DAY;
        }
        if (scheduledBloodNightYear >= targetYear) return false;
        boolean added = scheduleBloodNight(
            EventRegion.global(),
            targetYear,
            due,
            3L * NaturalCalendar.TICKS_PER_DAY,
            36L * NaturalCalendar.TICKS_PER_DAY);
        if (added) scheduledBloodNightYear = targetYear;
        return added;
    }

    /** Schedules a Blood Night for a chosen region; used by authored scheduling and operator acceptance tools. */
    public synchronized boolean scheduleBloodNight(EventRegion target, long ordinal, long startTick, long durationTicks,
        long cooldownTicks) {
        EventRecord event = EventInstanceFactory.regular(
            seed,
            epoch,
            BLOOD_NIGHT,
            target,
            ordinal,
            startTick,
            durationTicks,
            cooldownTicks,
            NIGHT_REACTIVITY,
            100);
        return events.schedule(event);
    }

    private boolean hasCurrentStrangeSeason(long nowTick) {
        for (EventRecord event : events.snapshot()
            .events()) {
            if (ASHEN_SEASON.equals(event.type()) && event.phase() != EventPhase.COMPLETED
                && nowTick < event.endsAtTick()) return true;
        }
        return false;
    }

    /** At most this many witnessed regions are destabilized by one world-wide event. */
    public static final int MAX_WITNESSED_REGIONS = 8;

    static List<RegionKey> destabilizationTargets(EventRecord event, java.util.Collection<RegionKey> witnessed) {
        List<RegionKey> targets = new ArrayList<>();
        if (!event.targetRegion()
            .isGlobal()) {
            targets.add(
                new RegionKey(
                    event.targetRegion()
                        .x(),
                    event.targetRegion()
                        .z()));
            return targets;
        }
        if (witnessed == null) return targets;
        java.util.TreeSet<RegionKey> ordered = new java.util.TreeSet<>(witnessed);
        for (RegionKey region : ordered) {
            if (targets.size() >= MAX_WITNESSED_REGIONS) break;
            targets.add(region);
        }
        return targets;
    }

    /** Ordinary Star Rain: once per season, on a deterministic night, after the protected first one settles. */
    public static final long REGULAR_STAR_RAIN_PERIOD_TICKS = NaturalCalendar.DAYS_PER_SEASON
        * NaturalCalendar.TICKS_PER_DAY;
    /** Nightfall within the chosen day; showers last through the night. */
    static final long STAR_RAIN_NIGHTFALL_TICK = 13_000L;
    static final long STAR_RAIN_DURATION_TICKS = 10_000L;
    static final long STAR_RAIN_COOLDOWN_TICKS = 6L * NaturalCalendar.TICKS_PER_DAY;

    /** Deterministic start tick of the {@code ordinal}th ordinary Star Rain window. */
    public long regularStarRainStart(long ordinal) {
        long hash = mix(seed ^ (ordinal * 0x9e3779b97f4a7c15L) ^ ((long) epoch << 41) ^ 0x5354415252414e53L);
        long day = 4L + Long.remainderUnsigned(hash, NaturalCalendar.DAYS_PER_SEASON - 8L);
        return ordinal * REGULAR_STAR_RAIN_PERIOD_TICKS + day * NaturalCalendar.TICKS_PER_DAY
            + STAR_RAIN_NIGHTFALL_TICK;
    }

    private boolean protectedStarRainSettled() {
        for (EventRecord event : events.snapshot()
            .events()) {
            if (event.protectedOpportunity() && EventRecord.PROTECTED_STAR_RAIN.equals(event.type()))
                return event.phase() == EventPhase.COMPLETED;
        }
        return false;
    }

    private boolean scheduleNextRegularStarRain(long nowTick) {
        if (!protectedStarRainSettled()) return false;
        long ordinal = nowTick / REGULAR_STAR_RAIN_PERIOD_TICKS;
        for (int attempt = 0; attempt < 2; attempt++, ordinal++) {
            long start = regularStarRainStart(ordinal);
            if (start <= nowTick) continue;
            EventRecord rain = EventInstanceFactory.regular(
                seed,
                epoch,
                EventRecord.PROTECTED_STAR_RAIN,
                EventRegion.global(),
                ordinal,
                start,
                STAR_RAIN_DURATION_TICKS,
                STAR_RAIN_COOLDOWN_TICKS,
                null,
                0);
            return events.schedule(rain);
        }
        return false;
    }

    /** True while a Blood Night is active over this block position. */
    public synchronized boolean bloodNightActiveAt(int x, int z, long nowTick) {
        for (EventInfluence influence : activeInfluencesAt(x, z, nowTick)) {
            if (BLOOD_NIGHT.equals(influence.eventType())) return true;
        }
        return false;
    }

    public synchronized StrangeSeason strangeSeasonAt(long nowTick) {
        for (EventRecord event : events.snapshot()
            .events()) {
            if (ASHEN_SEASON.equals(event.type()) && event.phase() != EventPhase.COMPLETED
                && nowTick >= event.scheduledAtTick()
                && nowTick < event.endsAtTick()) {
                return StrangeSeason.ASHEN_AUTUMN;
            }
        }
        return StrangeSeason.NONE;
    }

    public synchronized List<EventInfluence> activeInfluencesAt(int x, int z, long nowTick) {
        RegionKey region = RegionKey.fromBlock(x, z);
        return events.activeInfluences(EventRegion.of((int) region.x(), (int) region.z()), nowTick);
    }

    /** Deterministically refines only the 3x3 nearby site cells; registered results then persist as history. */
    public synchronized boolean ensureAnomaliesNear(int x, int z) {
        OverworldGeneration generation = new OverworldGeneration(
            seed,
            naturalexceptions.core.persistence.WorldgenEpoch.of(epoch));
        WorldgenSites sites = new WorldgenSites(generation);
        long cellX = Math.floorDiv(x, 1024);
        long cellZ = Math.floorDiv(z, 1024);
        boolean changed = false;
        for (long cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (long cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                long hash = mix(seed ^ (cx * 0x9e3779b97f4a7c15L) ^ (cz * 0xc2b2ae3d27d4eb4fL) ^ ((long) epoch << 43));
                double roll = (hash >>> 11) * 0x1.0p-53;
                if (generation.sample((int) (cx * 1024 + 512), (int) (cz * 1024 + 512))
                    .hydrology() == HydrologicalContext.WET_BASIN && roll < 0.18
                    && anomalies.size() < MAX_RETAINED_ANOMALIES) {
                    int centerX = (int) (cx * 1024 + ((hash >>> 32) & 1023));
                    int centerZ = (int) (cz * 1024 + ((hash >>> 42) & 1023));
                    DeterministicId id = DeterministicId.derive(
                        "naturalexceptions:p8:cow_lake:v1",
                        Long.toString(seed),
                        Integer.toString(epoch),
                        Long.toString(cx),
                        Long.toString(cz));
                    if (anomalies.get(id) == null) {
                        anomalies.add(
                            new SpatialAnomaly(
                                id,
                                ResourceId.of("naturalexceptions", "cow_lake"),
                                centerX,
                                centerZ,
                                160,
                                EnvironmentalInfluence.of(id, 0.62, 0.0, 0.35)));
                        changed = true;
                    }
                }
                WorldgenSite mark = sites.anomalyAtCell(cx, cz);
                if (mark != null && anomalies.size() < MAX_RETAINED_ANOMALIES) {
                    DeterministicId id = mark.id();
                    if (anomalies.get(id) == null) {
                        anomalies.add(
                            new SpatialAnomaly(
                                id,
                                ResourceId.of("naturalexceptions", "quiet_scar"),
                                mark.x(),
                                mark.z(),
                                88,
                                EnvironmentalInfluence.of(id, -0.06, 0.08, -0.10)));
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    public synchronized WorldEventSchedule events() {
        return events;
    }

    public synchronized SpatialAnomalyIndex anomalies() {
        return anomalies;
    }

    public synchronized DestabilizationField destabilization() {
        return destabilization;
    }

    public long seed() {
        return seed;
    }

    public int epoch() {
        return epoch;
    }

    public long creationTick() {
        return creationTick;
    }

    public synchronized long scheduledBloodNightYear() {
        return scheduledBloodNightYear;
    }

    public synchronized long lastCalendarYear() {
        return lastCalendarYear;
    }

    public synchronized void restoreMarkers(long bloodYear, long calendarYear) {
        scheduledBloodNightYear = bloodYear;
        lastCalendarYear = calendarYear;
    }
}
