package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

/**
 * Host-independent event lifecycle registry. It owns semantic history; the P1 due-time queue can be
 * rebuilt as an execution aid, but is not authoritative because it discards polled events.
 */
public final class WorldEventSchedule {

    public static final int DEFAULT_MAX_RETAINED_EVENTS = 512;
    public static final int DEFAULT_MAX_COOLDOWN_HISTORY = 4096;

    private final int maxRetainedEvents;
    private final int maxCooldownHistory;
    private final Map<DeterministicId, EventRecord> events = new LinkedHashMap<>();
    private final Map<HistoryKey, EventCooldownHistory> cooldownHistory = new HashMap<>();
    private final PriorityQueue<Transition> transitions = new PriorityQueue<>();
    private final PriorityQueue<CooldownExpiry> cooldownExpiries = new PriorityQueue<>();
    private long lastAdvancedTick;

    public WorldEventSchedule() {
        this(DEFAULT_MAX_RETAINED_EVENTS, DEFAULT_MAX_COOLDOWN_HISTORY);
    }

    public WorldEventSchedule(int maxRetainedEvents, int maxCooldownHistory) {
        if (maxRetainedEvents < 1 || maxCooldownHistory < 1) {
            throw new IllegalArgumentException("Event schedule bounds must be positive");
        }
        this.maxRetainedEvents = maxRetainedEvents;
        this.maxCooldownHistory = maxCooldownHistory;
    }

    private WorldEventSchedule(EventScheduleSnapshot snapshot, int maxRetainedEvents, int maxCooldownHistory) {
        this(maxRetainedEvents, maxCooldownHistory);
        if (snapshot == null) throw new IllegalArgumentException("Snapshot cannot be null");
        if (snapshot.events().size() > maxRetainedEvents || snapshot.cooldownHistory().size() > maxCooldownHistory) {
            throw new IllegalArgumentException("Snapshot exceeds configured event schedule bounds");
        }
        this.lastAdvancedTick = snapshot.lastAdvancedTick();
        for (EventRecord event : snapshot.events()) {
            if (events.put(event.id(), event) != null) throw new IllegalArgumentException("Duplicate event identity");
            if (event.phase() == EventPhase.SCHEDULED) {
                transitions.add(new Transition(event.scheduledAtTick(), event.id(), TransitionKind.START));
            } else if (event.phase() == EventPhase.ACTIVE) {
                transitions.add(new Transition(event.endsAtTick(), event.id(), TransitionKind.END));
            }
        }
        for (EventCooldownHistory history : snapshot.cooldownHistory()) {
            HistoryKey key = new HistoryKey(history.type(), history.region());
            if (cooldownHistory.put(key, history) != null) throw new IllegalArgumentException("Duplicate cooldown key");
            cooldownExpiries.add(new CooldownExpiry(history.cooldownUntilTick(), key));
        }
        if (protectedStarRainCount() > 1) throw new IllegalArgumentException("Multiple protected Star Rain opportunities");
    }

    public static WorldEventSchedule restore(EventScheduleSnapshot snapshot) {
        return new WorldEventSchedule(snapshot, DEFAULT_MAX_RETAINED_EVENTS, DEFAULT_MAX_COOLDOWN_HISTORY);
    }

    public synchronized boolean schedule(EventRecord event) {
        if (event == null) throw new IllegalArgumentException("Cannot schedule null event");
        EventRecord sameId = events.get(event.id());
        if (sameId != null) {
            if (!sameId.equals(event) && event.phase() == EventPhase.SCHEDULED) {
                throw new IllegalArgumentException("Conflicting contents for event identity " + event.id());
            }
            return false;
        }
        if (event.phase() != EventPhase.SCHEDULED) {
            throw new IllegalArgumentException("New event instances must be scheduled before entering lifecycle phases");
        }
        if (event.protectedOpportunity() && protectedStarRainCount() > 0) return false;

        if (!event.protectedOpportunity()) {
            pruneExpiredCooldownHistory(event.scheduledAtTick());
            for (EventRecord existing : events.values()) {
                if (existing.type().equals(event.type()) && existing.targetRegion().equals(event.targetRegion())
                    && existing.phase() != EventPhase.COMPLETED) {
                    return false;
                }
            }
            HistoryKey key = new HistoryKey(event.type(), event.targetRegion());
            EventCooldownHistory history = cooldownHistory.get(key);
            if (history != null && event.scheduledAtTick() < history.cooldownUntilTick()) return false;
            if (history == null && cooldownHistory.size() >= maxCooldownHistory) return false;
        }

        pruneCompletedEventsForCapacity();
        if (events.size() >= maxRetainedEvents) return false;
        events.put(event.id(), event);
        transitions.add(new Transition(event.scheduledAtTick(), event.id(), TransitionKind.START));
        return true;
    }

    /** Installs the unique protected first Star Rain opportunity, if not already present. */
    public synchronized boolean ensureProtectedFirstStarRain(EventRecord event) {
        if (event == null || !event.protectedOpportunity()
            || !EventRecord.PROTECTED_STAR_RAIN.equals(event.type())) {
            throw new IllegalArgumentException("Expected a protected first Star Rain event record");
        }
        return schedule(event);
    }

    /**
     * Resolves due lifecycle transitions in one coarse call. A late SCHEDULED event that has already ended is
     * marked completed directly; no missed ticks or fictitious active ticks are replayed.
     */
    public synchronized List<EventRecord> advance(long nowTick) {
        if (nowTick < 0) throw new IllegalArgumentException("World tick cannot be negative");
        if (nowTick < lastAdvancedTick) return Collections.emptyList();
        lastAdvancedTick = nowTick;
        List<EventRecord> changed = new ArrayList<>();
        while (!transitions.isEmpty() && transitions.peek().atTick <= nowTick) {
            Transition transition = transitions.poll();
            EventRecord old = events.get(transition.eventId);
            if (old == null) continue;
            EventRecord next;
            if (transition.kind == TransitionKind.START && old.phase() == EventPhase.SCHEDULED) {
                if (nowTick >= old.endsAtTick()) {
                    // The event interval elapsed while dormant. Record its historical completion boundary
                    // without replaying an active interval or its missed ticks.
                    next = old.transitionTo(EventPhase.COMPLETED, old.endsAtTick());
                } else {
                    next = old.transitionTo(EventPhase.ACTIVE, old.scheduledAtTick());
                    transitions.add(new Transition(old.endsAtTick(), old.id(), TransitionKind.END));
                }
            } else if (transition.kind == TransitionKind.END && old.phase() == EventPhase.ACTIVE) {
                next = old.transitionTo(EventPhase.COMPLETED, old.endsAtTick());
            } else {
                continue;
            }
            events.put(next.id(), next);
            changed.add(next);
            if (next.phase() == EventPhase.COMPLETED && !next.protectedOpportunity()) rememberCooldown(next);
        }
        pruneExpiredCooldownHistory(nowTick);
        return Collections.unmodifiableList(changed);
    }

    public synchronized EventRecord get(DeterministicId id) {
        return id == null ? null : events.get(id);
    }

    /** Returns active event influences at a target region without consulting any unrelated world state. */
    public synchronized List<EventInfluence> activeInfluences(EventRegion region, long nowTick) {
        if (region == null || nowTick < 0) throw new IllegalArgumentException("Region and nonnegative time are required");
        List<EventInfluence> result = new ArrayList<>();
        for (EventRecord event : events.values()) {
            if (!event.hasTemporalInfluence() || nowTick < event.scheduledAtTick() || nowTick >= event.endsAtTick()) {
                continue;
            }
            if (event.phase() == EventPhase.COMPLETED) continue;
            if (event.targetRegion().isGlobal() || event.targetRegion().equals(region)) {
                result.add(new EventInfluence(event));
            }
        }
        Collections.sort(result, new Comparator<EventInfluence>() {
            @Override public int compare(EventInfluence left, EventInfluence right) {
                return left.eventId().compareTo(right.eventId());
            }
        });
        return Collections.unmodifiableList(result);
    }

    public synchronized long cooldownUntil(ResourceId type, EventRegion region) {
        if (type == null || region == null) return 0L;
        EventCooldownHistory history = cooldownHistory.get(new HistoryKey(type, region));
        return history == null ? 0L : history.cooldownUntilTick();
    }

    public synchronized int protectedStarRainCount() {
        int count = 0;
        for (EventRecord event : events.values()) {
            if (event.protectedOpportunity() && EventRecord.PROTECTED_STAR_RAIN.equals(event.type())) count++;
        }
        return count;
    }

    public synchronized int eventCount() { return events.size(); }
    public synchronized int cooldownHistoryCount() { return cooldownHistory.size(); }
    public synchronized long lastAdvancedTick() { return lastAdvancedTick; }

    /** Absolute due time of the next lifecycle transition, or {@code -1} when the schedule can sleep. */
    public synchronized long nextTransitionTick() {
        return transitions.isEmpty() ? -1L : transitions.peek().atTick;
    }

    public synchronized EventScheduleSnapshot snapshot() {
        List<EventRecord> eventSnapshot = new ArrayList<>(events.values());
        Collections.sort(eventSnapshot, new Comparator<EventRecord>() {
            @Override public int compare(EventRecord left, EventRecord right) { return left.id().compareTo(right.id()); }
        });
        List<EventCooldownHistory> historySnapshot = new ArrayList<>(cooldownHistory.values());
        Collections.sort(historySnapshot);
        return new EventScheduleSnapshot(eventSnapshot, historySnapshot, lastAdvancedTick);
    }

    private void rememberCooldown(EventRecord event) {
        HistoryKey key = new HistoryKey(event.type(), event.targetRegion());
        EventCooldownHistory next = new EventCooldownHistory(event.type(), event.targetRegion(),
            event.scheduledAtTick(), event.cooldownUntilTick());
        EventCooldownHistory old = cooldownHistory.get(key);
        if (old == null || next.lastOccurrenceTick() >= old.lastOccurrenceTick()) {
            cooldownHistory.put(key, next);
            cooldownExpiries.add(new CooldownExpiry(next.cooldownUntilTick(), key));
        }
    }

    private void pruneExpiredCooldownHistory(long nowTick) {
        while (!cooldownExpiries.isEmpty() && cooldownExpiries.peek().atTick <= nowTick) {
            CooldownExpiry expiry = cooldownExpiries.poll();
            EventCooldownHistory current = cooldownHistory.get(expiry.key);
            if (current != null && current.cooldownUntilTick() <= nowTick
                && current.cooldownUntilTick() == expiry.atTick) {
                cooldownHistory.remove(expiry.key);
            }
        }
    }

    private void pruneCompletedEventsForCapacity() {
        if (events.size() < maxRetainedEvents) return;
        List<EventRecord> removable = new ArrayList<>();
        for (EventRecord event : events.values()) {
            if (event.phase() == EventPhase.COMPLETED && !event.protectedOpportunity()) removable.add(event);
        }
        Collections.sort(removable, new Comparator<EventRecord>() {
            @Override public int compare(EventRecord left, EventRecord right) {
                int byCompletion = Long.compare(left.phaseChangedAtTick(), right.phaseChangedAtTick());
                return byCompletion != 0 ? byCompletion : left.id().compareTo(right.id());
            }
        });
        for (EventRecord event : removable) {
            if (events.size() < maxRetainedEvents) break;
            events.remove(event.id());
        }
    }

    private static final class HistoryKey {
        private final ResourceId type;
        private final EventRegion region;
        private HistoryKey(ResourceId type, EventRegion region) { this.type = type; this.region = region; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof HistoryKey)) return false;
            HistoryKey that = (HistoryKey) other;
            return type.equals(that.type) && region.equals(that.region);
        }
        @Override public int hashCode() { return 31 * type.hashCode() + region.hashCode(); }
    }

    private enum TransitionKind { START, END }

    private static final class Transition implements Comparable<Transition> {
        private final long atTick;
        private final DeterministicId eventId;
        private final TransitionKind kind;
        private Transition(long atTick, DeterministicId eventId, TransitionKind kind) {
            this.atTick = atTick;
            this.eventId = eventId;
            this.kind = kind;
        }
        @Override public int compareTo(Transition other) {
            int c = Long.compare(atTick, other.atTick);
            if (c != 0) return c;
            c = eventId.compareTo(other.eventId);
            return c != 0 ? c : kind.compareTo(other.kind);
        }
    }

    private static final class CooldownExpiry implements Comparable<CooldownExpiry> {
        private final long atTick;
        private final HistoryKey key;
        private CooldownExpiry(long atTick, HistoryKey key) { this.atTick = atTick; this.key = key; }
        @Override public int compareTo(CooldownExpiry other) {
            int c = Long.compare(atTick, other.atTick);
            if (c != 0) return c;
            c = key.type.compareTo(other.key.type);
            return c != 0 ? c : key.region.compareTo(other.key.region);
        }
    }
}
