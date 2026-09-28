package naturalexceptions.core.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/** Read-only, deterministic view of persisted Star Rain impacts for developer navigation. */
public final class StarRainNavigation {

    public enum State {
        IMPACTED,
        IN_FLIGHT,
        PENDING,
        RECOVERED
    }

    public static final class Target {

        private final StarRainImpact impact;
        private final State state;
        private final MaterialQuantity remainingMass;
        private final double distanceSquared;

        private Target(StarRainImpact impact, State state, MaterialQuantity remainingMass, double distanceSquared) {
            this.impact = impact;
            this.state = state;
            this.remainingMass = remainingMass;
            this.distanceSquared = distanceSquared;
        }

        public StarRainImpact impact() {
            return impact;
        }

        public State state() {
            return state;
        }

        public MaterialQuantity remainingMass() {
            return remainingMass;
        }

        public double distance() {
            return Math.sqrt(distanceSquared);
        }
    }

    private StarRainNavigation() {}

    /** Operator selection ignores every resolved site, even one that still contains recoverable matter. */
    public static Target selectUnresolved(List<StarRainImpact> impacts, long nowTick, double playerX, double playerZ) {
        if (impacts == null || impacts.isEmpty() || !Double.isFinite(playerX) || !Double.isFinite(playerZ)) return null;
        Target best = null;
        for (StarRainImpact impact : impacts) {
            if (impact == null || impact.isResolved()) continue;
            Target candidate = describe(
                impact,
                nowTick,
                playerX,
                playerZ,
                Collections.<DeterministicId, Long>emptyMap());
            if (best == null || rank(candidate.state()) < rank(best.state())
                || (candidate.state() == best.state() && (candidate.distanceSquared < best.distanceSquared
                    || (candidate.distanceSquared == best.distanceSquared && candidate.impact()
                        .id()
                        .compareTo(
                            best.impact()
                                .id())
                        < 0))))
                best = candidate;
        }
        return best;
    }

    /** Chooses the nearest unrecovered impact, then in-flight impact, then pending impact. */
    public static Target select(List<StarRainImpact> impacts, long nowTick, double playerX, double playerZ,
        Map<DeterministicId, Long> remainingMassMgByImpact) {
        if (impacts == null || impacts.isEmpty() || !Double.isFinite(playerX) || !Double.isFinite(playerZ)) return null;
        Target best = null;
        for (StarRainImpact impact : impacts) {
            if (impact == null) continue;
            Target candidate = describe(impact, nowTick, playerX, playerZ, remainingMassMgByImpact);
            if (candidate.state() == State.RECOVERED) continue;
            if (best == null || rank(candidate.state()) < rank(best.state())
                || (candidate.state() == best.state() && (candidate.distanceSquared < best.distanceSquared
                    || (candidate.distanceSquared == best.distanceSquared && candidate.impact()
                        .id()
                        .compareTo(
                            best.impact()
                                .id())
                        < 0))))
                best = candidate;
        }
        return best;
    }

    /** Category-ordered listing; pending impacts are ordered by due time, others by player distance. */
    public static List<Target> list(List<StarRainImpact> impacts, long nowTick, double playerX, double playerZ,
        Map<DeterministicId, Long> remainingMassMgByImpact) {
        if (impacts == null || impacts.isEmpty() || !Double.isFinite(playerX) || !Double.isFinite(playerZ))
            return Collections.emptyList();
        List<Target> result = new ArrayList<>();
        for (StarRainImpact impact : impacts) {
            if (impact != null) result.add(describe(impact, nowTick, playerX, playerZ, remainingMassMgByImpact));
        }
        Collections.sort(result, new Comparator<Target>() {

            @Override
            public int compare(Target left, Target right) {
                int byState = Integer.compare(rank(left.state()), rank(right.state()));
                if (byState != 0) return byState;
                int byPendingDue = left.state() == State.PENDING ? Long.compare(
                    left.impact()
                        .effectiveDueTick(),
                    right.impact()
                        .effectiveDueTick())
                    : 0;
                if (byPendingDue != 0) return byPendingDue;
                int byDistance = Double.compare(left.distanceSquared, right.distanceSquared);
                return byDistance != 0 ? byDistance
                    : left.impact()
                        .id()
                        .compareTo(
                            right.impact()
                                .id());
            }
        });
        return Collections.unmodifiableList(result);
    }

    public static Target describe(StarRainImpact impact, long nowTick, double playerX, double playerZ,
        long remainingMassMg) {
        return describe(
            impact,
            nowTick,
            playerX,
            playerZ,
            Collections.singletonMap(impact.id(), Math.max(0L, remainingMassMg)));
    }

    private static Target describe(StarRainImpact impact, long nowTick, double playerX, double playerZ,
        Map<DeterministicId, Long> remainingMassMgByImpact) {
        long siteMg = remainingMassMgByImpact == null ? 0L : Math.max(0L, value(remainingMassMgByImpact, impact.id()));
        State state;
        MaterialQuantity remaining;
        if (impact.isResolved()) {
            if (!impact.isMaterialized()) siteMg = impact.stellarMass()
                .milligrams();
            remaining = MaterialQuantity.ofMilligrams(siteMg);
            state = remaining.isPositive() ? State.IMPACTED : State.RECOVERED;
        } else {
            remaining = MaterialQuantity.ZERO;
            state = impact.effectiveDueTick() <= nowTick ? State.IN_FLIGHT : State.PENDING;
        }
        double dx = impact.x() + 0.5D - playerX;
        double dz = impact.z() + 0.5D - playerZ;
        return new Target(impact, state, remaining, dx * dx + dz * dz);
    }

    private static long value(Map<DeterministicId, Long> values, DeterministicId id) {
        Long value = values.get(id);
        return value == null ? 0L : value;
    }

    private static int rank(State state) {
        switch (state) {
            case IMPACTED:
                return 0;
            case IN_FLIGHT:
                return 1;
            case PENDING:
                return 2;
            default:
                return 3;
        }
    }
}
