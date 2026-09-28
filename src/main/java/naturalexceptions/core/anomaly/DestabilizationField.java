package naturalexceptions.core.anomaly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/**
 * Sparse, cause-driven regional scalar. A cause spreads one 512-block region per coarse day and
 * decays analytically after arrival, so loaded and dormant areas resolve the same state without ticks.
 */
public final class DestabilizationField {
    public static final long COARSE_STEP_TICKS = 24_000L;
    public static final int MAX_COARSE_STEPS_PER_ADVANCE = 64;
    private static final int MAX_CAUSES_TOTAL = 8192;
    private static final int MAX_CAUSES_PER_REGION = 8192;
    private static final int MAX_SPREAD_REGIONS = 16;
    private static final long CAUSE_RETENTION_TICKS = 180L * COARSE_STEP_TICKS;
    private static final double DECAY_RATE = Math.log(2.0) / (7.0 * COARSE_STEP_TICKS);
    private static final double SPREAD_RETENTION = 0.20;
    private static final double BASELINE = 0.0;
    private static final double ACTIVE_EPSILON = 1.0e-12;

    private final TreeMap<RegionKey, List<DestabilizationCause>> causesByRegion = new TreeMap<>();
    private final Set<String> causeIdentities = new HashSet<>();
    private long lastAdvanceTick;
    private int lastAdvanceCoarseSteps;

    /** A read/visit does not add baseline regions or alter the global aggregation. */
    public void ensureRegion(RegionKey key, long tick) {
        requireKey(key);
        advanceCoarse(tick);
    }

    /** Raises a regional cause once. Ordinary activity must not call this implicitly. */
    public void increase(RegionKey key, ResourceId causeType, DeterministicId sourceId, double amount, long tick) {
        requireKey(key);
        DestabilizationCause cause = new DestabilizationCause(causeType, sourceId, amount, tick);
        advanceCoarse(tick);
        String identity = identity(cause);
        if (causeIdentities.contains(identity) || causeIdentities.size() >= MAX_CAUSES_TOTAL) return;
        List<DestabilizationCause> causes = causesByRegion.get(key);
        if (causes == null) {
            causes = new ArrayList<>();
            causesByRegion.put(key, causes);
        }
        causes.add(cause);
        causeIdentities.add(identity);
    }

    /** Updates only the absolute query time and prunes fully expired causes; no tick replay or region scan. */
    public void advanceCoarse(long tick) {
        requireTick(tick);
        if (tick < lastAdvanceTick) throw new IllegalArgumentException("time cannot move backwards");
        lastAdvanceCoarseSteps = tick >= nextCoarseUpdateTick() ? 1 : 0;
        if (lastAdvanceCoarseSteps == 0) return;
        lastAdvanceTick = tick;
        pruneExpired(tick);
    }

    private void pruneExpired(long tick) {
        for (Map.Entry<RegionKey, List<DestabilizationCause>> entry : causesByRegion.entrySet()) {
            List<DestabilizationCause> causes = entry.getValue();
            for (int i = causes.size() - 1; i >= 0; i--) {
                DestabilizationCause cause = causes.get(i);
                if (tick - cause.tick() > CAUSE_RETENTION_TICKS) {
                    causes.remove(i);
                    causeIdentities.remove(identity(cause));
                }
            }
        }
        java.util.Iterator<Map.Entry<RegionKey, List<DestabilizationCause>>> iterator = causesByRegion.entrySet().iterator();
        while (iterator.hasNext()) if (iterator.next().getValue().isEmpty()) iterator.remove();
    }

    private static String identity(DestabilizationCause cause) {
        return cause.type().toString() + '/' + cause.sourceId().hex();
    }

    /** O(1) scheduler wake-up for integrations; dormant state has no continuously active work. */
    public long nextCoarseUpdateTick() {
        return lastAdvanceTick > Long.MAX_VALUE - COARSE_STEP_TICKS
            ? Long.MAX_VALUE : lastAdvanceTick + COARSE_STEP_TICKS;
    }

    public boolean isCoarseUpdateDue(long currentTick) {
        requireTick(currentTick);
        return currentTick >= nextCoarseUpdateTick();
    }

    /** Returns current analytic value for a region; spread fronts advance only at coarse-day boundaries. */
    public double value(RegionKey key) {
        requireKey(key);
        double result = BASELINE;
        for (Map.Entry<RegionKey, List<DestabilizationCause>> entry : causesByRegion.entrySet()) {
            long distance = manhattan(key, entry.getKey());
            if (distance > MAX_SPREAD_REGIONS) continue;
            for (DestabilizationCause cause : entry.getValue()) {
                long age = Math.max(0L, lastAdvanceTick - cause.tick());
                long arrivedSteps = age / COARSE_STEP_TICKS;
                if (distance > arrivedSteps) continue;
                long elapsedAfterArrival = age - distance * COARSE_STEP_TICKS;
                double decayed = cause.amount() * Math.pow(SPREAD_RETENTION, distance)
                    * Math.exp(-DECAY_RATE * elapsedAfterArrival);
                result += decayed;
            }
        }
        return clamp01(result);
    }

    /** Mean of affected source regions (severity and source distribution); unvisited baseline regions are omitted. */
    public double derivedGlobal() {
        if (causesByRegion.isEmpty()) return BASELINE;
        double sum = 0.0;
        int active = 0;
        for (List<DestabilizationCause> causes : causesByRegion.values()) {
            double value = BASELINE;
            for (DestabilizationCause cause : causes) {
                long age = Math.max(0L, lastAdvanceTick - cause.tick());
                value += cause.amount() * Math.exp(-DECAY_RATE * age);
            }
            value = clamp01(value);
            if (value > ACTIVE_EPSILON) { sum += value; active++; }
        }
        return active == 0 ? BASELINE : sum / active;
    }

    public int regionCount() { return causesByRegion.size(); }
    public long lastAdvanceTick() { return lastAdvanceTick; }
    public int lastAdvanceCoarseSteps() { return lastAdvanceCoarseSteps; }
    /** Always zero: work is closed-form over retained causes, never Minecraft-tick replay. */
    public int lastAdvanceTickReplayCount() { return 0; }

    public Snapshot snapshot() {
        List<RegionState> states = new ArrayList<>();
        for (Map.Entry<RegionKey, List<DestabilizationCause>> entry : causesByRegion.entrySet()) {
            states.add(new RegionState(entry.getKey(), value(entry.getKey()), lastAdvanceTick, entry.getValue()));
        }
        return new Snapshot(lastAdvanceTick, states);
    }

    public static DestabilizationField restore(Snapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot cannot be null");
        DestabilizationField field = new DestabilizationField();
        field.lastAdvanceTick = snapshot.lastAdvanceTick;
        for (RegionState state : snapshot.regions) {
            if (state == null || state.lastResolvedTick > snapshot.lastAdvanceTick || state.lastResolvedTick < 0
                || !finite(state.value) || state.value < 0 || state.value > 1) continue;
            List<DestabilizationCause> accepted = new ArrayList<>();
            for (DestabilizationCause cause : state.causes) {
                if (field.causeIdentities.size() >= MAX_CAUSES_TOTAL) break;
                if (field.causeIdentities.add(identity(cause))) accepted.add(cause);
            }
            if (!accepted.isEmpty()) field.causesByRegion.put(state.key, accepted);
        }
        return field;
    }

    private static long manhattan(RegionKey a, RegionKey b) {
        long dx = a.x() >= b.x() ? a.x() - b.x() : b.x() - a.x();
        long dz = a.z() >= b.z() ? a.z() - b.z() : b.z() - a.z();
        if (dx > MAX_SPREAD_REGIONS || dz > MAX_SPREAD_REGIONS) return MAX_SPREAD_REGIONS + 1L;
        return dx + dz;
    }

    private static void requireKey(RegionKey key) { if (key == null) throw new IllegalArgumentException("key cannot be null"); }
    private static void requireTick(long tick) { if (tick < 0) throw new IllegalArgumentException("tick must be nonnegative"); }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }

    public static final class Snapshot {
        private final long lastAdvanceTick;
        private final List<RegionState> regions;
        public Snapshot(long lastAdvanceTick, List<RegionState> regions) {
            if (lastAdvanceTick < 0) throw new IllegalArgumentException("lastAdvanceTick must be nonnegative");
            this.lastAdvanceTick = lastAdvanceTick;
            this.regions = Collections.unmodifiableList(new ArrayList<>(regions == null
                ? Collections.<RegionState>emptyList() : regions));
        }
        public long lastAdvanceTick() { return lastAdvanceTick; }
        public List<RegionState> regions() { return regions; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Snapshot)) return false;
            Snapshot that = (Snapshot) o;
            return lastAdvanceTick == that.lastAdvanceTick && regions.equals(that.regions);
        }
        @Override public int hashCode() { return 31 * (int) (lastAdvanceTick ^ (lastAdvanceTick >>> 32)) + regions.hashCode(); }
    }

    public static final class RegionState {
        private final RegionKey key;
        private final double value;
        private final long lastResolvedTick;
        private final List<DestabilizationCause> causes;
        public RegionState(RegionKey key, double value, long lastResolvedTick, List<DestabilizationCause> causes) {
            if (key == null || !finite(value) || value < 0 || value > 1 || lastResolvedTick < 0) {
                throw new IllegalArgumentException("invalid region state");
            }
            List<DestabilizationCause> copied = new ArrayList<>(causes == null
                ? Collections.<DestabilizationCause>emptyList() : causes);
            if (copied.size() > MAX_CAUSES_PER_REGION) throw new IllegalArgumentException("too many causes");
            this.key = key; this.value = value; this.lastResolvedTick = lastResolvedTick;
            this.causes = Collections.unmodifiableList(copied);
        }
        public RegionKey key() { return key; }
        public double value() { return value; }
        public long lastResolvedTick() { return lastResolvedTick; }
        public List<DestabilizationCause> causes() { return causes; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionState)) return false;
            RegionState that = (RegionState) o;
            return Double.compare(value, that.value) == 0 && lastResolvedTick == that.lastResolvedTick
                && key.equals(that.key) && causes.equals(that.causes);
        }
        @Override public int hashCode() {
            long bits = Double.doubleToLongBits(value);
            int result = key.hashCode();
            result = 31 * result + (int) (bits ^ (bits >>> 32));
            result = 31 * result + (int) (lastResolvedTick ^ (lastResolvedTick >>> 32));
            return 31 * result + causes.hashCode();
        }
    }
}
