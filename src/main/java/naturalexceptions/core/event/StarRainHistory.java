package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/** P11 consequence history keyed to the P8 parent events; indexes are rebuilt from persisted semantic impacts. */
public final class StarRainHistory {

    public static final int MAX_IMPACTS = 8192;

    private final Map<DeterministicId, StarRainImpact> impacts = new LinkedHashMap<>();
    private final Map<DeterministicId, Boolean> parents = new HashMap<>();
    private final NavigableSet<StarRainImpact> pendingDue = new TreeSet<>(new Comparator<StarRainImpact>() {

        @Override
        public int compare(StarRainImpact left, StarRainImpact right) {
            int byTime = Long.compare(left.effectiveDueTick(), right.effectiveDueTick());
            return byTime != 0 ? byTime
                : left.id()
                    .compareTo(right.id());
        }
    });
    /** Resolved, not-yet-materialized sites indexed so chunk load never scans world history. */
    private final Map<Long, NavigableSet<DeterministicId>> awaitingByChunk = new HashMap<>();
    private int pendingCount;
    private int awaitingMaterializationCount;

    public StarRainHistory() {}

    public StarRainHistory(Collection<StarRainImpact> restored) {
        if (restored == null || restored.size() > MAX_IMPACTS) {
            throw new IllegalArgumentException("Invalid or oversized Star Rain history");
        }
        for (StarRainImpact impact : restored) {
            Objects.requireNonNull(impact, "impact");
            if (impacts.put(impact.id(), impact) != null) {
                throw new IllegalArgumentException("Duplicate Star Rain impact identity");
            }
            parents.put(impact.parentEventId(), Boolean.TRUE);
            if (!impact.isResolved()) {
                pendingDue.add(impact);
                pendingCount++;
            } else if (!impact.isMaterialized()) {
                awaitingMaterializationCount++;
                indexAwaiting(impact);
            }
        }
    }

    /** Captures each scheduled P8 Star Rain once and persists its deterministic targeting immediately. */
    public synchronized boolean synchronize(EventScheduleSnapshot schedule, int centerX, int centerZ) {
        Objects.requireNonNull(schedule, "schedule");
        boolean changed = false;
        for (EventRecord event : schedule.events()) {
            if (!EventRecord.PROTECTED_STAR_RAIN.equals(event.type()) || parents.containsKey(event.id())) continue;
            List<StarRainImpact> generated = StarRainImpactFactory.create(event, centerX, centerZ);
            if (impacts.size() + generated.size() > MAX_IMPACTS) {
                throw new IllegalStateException("P11 impact history reached its explicit capacity");
            }
            for (StarRainImpact impact : generated) {
                impacts.put(impact.id(), impact);
                pendingDue.add(impact);
                pendingCount++;
            }
            parents.put(event.id(), Boolean.TRUE);
            changed = true;
        }
        return changed;
    }

    /** Returns due, unresolved impacts in deterministic time/id order without exceeding the caller's budget. */
    public synchronized List<StarRainImpact> dueImpacts(long nowTick, int budget) {
        if (nowTick < 0 || budget < 0) throw new IllegalArgumentException("Invalid impact work limit");
        if (budget == 0) return Collections.emptyList();
        discardResolvedHead();
        List<StarRainImpact> due = new ArrayList<>(budget);
        for (StarRainImpact impact : pendingDue) {
            if (due.size() >= budget || impact.effectiveDueTick() > nowTick) break;
            due.add(impact);
        }
        return Collections.unmodifiableList(due);
    }

    public synchronized boolean resolve(DeterministicId id) {
        StarRainImpact impact = id == null ? null : impacts.get(id);
        if (impact == null || !impact.resolve()) return false;
        pendingDue.remove(impact);
        pendingCount--;
        awaitingMaterializationCount++;
        indexAwaiting(impact);
        return true;
    }

    /** Advances only one existing unresolved impact; normal bounded tick processing still owns resolution. */
    public synchronized boolean expedite(DeterministicId id, long nowTick) {
        StarRainImpact impact = id == null ? null : impacts.get(id);
        if (impact == null || impact.isResolved() || nowTick < 0L || impact.effectiveDueTick() <= nowTick) return false;
        pendingDue.remove(impact);
        if (!impact.expedite(nowTick)) {
            pendingDue.add(impact);
            return false;
        }
        pendingDue.add(impact);
        return true;
    }

    public synchronized MaterialQuantity transferToSpatialSite(DeterministicId id) {
        StarRainImpact impact = id == null ? null : impacts.get(id);
        if (impact == null || impact.isMaterialized()) return MaterialQuantity.ZERO;
        MaterialQuantity moved = impact.transferToSpatialSite();
        if (moved.isPositive()) {
            awaitingMaterializationCount--;
            removeAwaiting(impact);
        }
        return moved;
    }

    /** Absolute next physical work time, or -1 while all remembered events are settled. */
    public synchronized long nextDueTick() {
        discardResolvedHead();
        return pendingDue.isEmpty() ? -1L
            : pendingDue.first()
                .effectiveDueTick();
    }

    private void discardResolvedHead() {
        Iterator<StarRainImpact> iterator = pendingDue.iterator();
        while (iterator.hasNext()) {
            StarRainImpact impact = iterator.next();
            if (!impact.isResolved()) break;
            iterator.remove();
            pendingCount--;
        }
    }

    public synchronized StarRainImpact get(DeterministicId id) {
        return id == null ? null : impacts.get(id);
    }

    /** Impacts that happened historically but whose chunks have not yet materialized their site. */
    public synchronized List<StarRainImpact> awaitingMaterializationInChunk(int chunkX, int chunkZ) {
        return awaitingMaterializationInChunk(chunkX, chunkZ, Integer.MAX_VALUE);
    }

    /** Deterministic bounded chunk wake query. */
    public synchronized List<StarRainImpact> awaitingMaterializationInChunk(int chunkX, int chunkZ, int budget) {
        if (budget < 0) throw new IllegalArgumentException("Materialization budget cannot be negative");
        if (budget == 0) return Collections.emptyList();
        List<StarRainImpact> result = new ArrayList<>();
        NavigableSet<DeterministicId> ids = awaitingByChunk.get(chunkKey(chunkX, chunkZ));
        if (ids != null) {
            for (DeterministicId id : ids) {
                StarRainImpact impact = impacts.get(id);
                if (impact != null && impact.isResolved() && !impact.isMaterialized()) result.add(impact);
                if (result.size() >= budget) break;
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int impactCount() {
        return impacts.size();
    }

    public synchronized int parentCount() {
        return parents.size();
    }

    public synchronized int pendingCount() {
        return pendingCount;
    }

    public synchronized int awaitingMaterializationCount() {
        return awaitingMaterializationCount;
    }

    public synchronized List<StarRainImpact> snapshot() {
        List<StarRainImpact> result = new ArrayList<>(impacts.values());
        Collections.sort(result, new Comparator<StarRainImpact>() {

            @Override
            public int compare(StarRainImpact left, StarRainImpact right) {
                return left.id()
                    .compareTo(right.id());
            }
        });
        return Collections.unmodifiableList(result);
    }

    private void indexAwaiting(StarRainImpact impact) {
        long key = chunkKey(impact.x() >> 4, impact.z() >> 4);
        NavigableSet<DeterministicId> ids = awaitingByChunk.get(key);
        if (ids == null) {
            ids = new TreeSet<>();
            awaitingByChunk.put(key, ids);
        }
        ids.add(impact.id());
    }

    private void removeAwaiting(StarRainImpact impact) {
        long key = chunkKey(impact.x() >> 4, impact.z() >> 4);
        NavigableSet<DeterministicId> ids = awaitingByChunk.get(key);
        if (ids == null) return;
        ids.remove(impact.id());
        if (ids.isEmpty()) awaitingByChunk.remove(key);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
