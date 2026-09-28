package naturalexceptions.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

final class StarRainNavigationTest {

    private static final DeterministicId PARENT = DeterministicId.derive("p11-navigation-parent");

    @Test
    void nearestUnrecoveredImpactIsSelectedBeforeCloserInFlightOrPendingImpacts() {
        StarRainImpact impacted = impact(1, 0, 40, 2, true, true);
        StarRainImpact inFlight = impact(2, 1, 2, 1, false, false);
        StarRainImpact pending = impact(3, 50, 0, 1, false, false);

        StarRainNavigation.Target result = StarRainNavigation
            .select(Arrays.asList(pending, inFlight, impacted), 10, 0.0, 0.0, siteMasses(impacted, 700));

        assertSame(impacted, result.impact());
        assertEquals(StarRainNavigation.State.IMPACTED, result.state());
        assertEquals(
            700,
            result.remainingMass()
                .milligrams());
    }

    @Test
    void recoveredImpactIsSkippedWhenUnrecoveredImpactExists() {
        StarRainImpact recovered = impact(4, 0, 1, 1, true, true);
        StarRainImpact unrecovered = impact(5, 8, 0, 2, true, true);

        StarRainNavigation.Target result = StarRainNavigation
            .select(Arrays.asList(recovered, unrecovered), 100, 0, 0, siteMasses(recovered, 0, unrecovered, 900));

        assertSame(unrecovered, result.impact());
        assertEquals(StarRainNavigation.State.IMPACTED, result.state());
    }

    @Test
    void pendingImpactsAreSelectedByDistanceWhenNoImpactedSitesRemain() {
        StarRainImpact fartherDueSoon = impact(6, 1, 10, 1, false, false);
        StarRainImpact nearerDueLater = impact(7, 1, 0, 1, false, false);

        StarRainNavigation.Target result = StarRainNavigation.select(
            Arrays.asList(fartherDueSoon, nearerDueLater),
            0,
            0,
            0,
            Collections.<DeterministicId, Long>emptyMap());

        assertSame(nearerDueLater, result.impact());
        assertEquals(StarRainNavigation.State.PENDING, result.state());
    }

    @Test
    void inFlightImpactPrecedesPendingEvenWhenItIsFartherAway() {
        StarRainImpact inFlight = impact(14, 100, 100, 1, false, false, 5L);
        StarRainImpact pending = impact(15, 0, 1, 1, false, false, 20L);

        StarRainNavigation.Target result = StarRainNavigation
            .select(Arrays.asList(pending, inFlight), 5, 0, 0, Collections.<DeterministicId, Long>emptyMap());

        assertSame(inFlight, result.impact());
        assertEquals(StarRainNavigation.State.IN_FLIGHT, result.state());
    }

    @Test
    void impactNowSelectionSkipsResolvedSitesAndPrefersNearestDueUnresolvedImpact() {
        StarRainImpact impacted = impact(17, 0, 1, 1, true, true);
        StarRainImpact recovered = impact(21, 0, 0, 1, true, true);
        StarRainImpact fartherInFlight = impact(18, 0, 10, 1, false, false, 5L);
        StarRainImpact nearerInFlight = impact(19, 0, 4, 1, false, false, 5L);
        StarRainImpact pending = impact(20, 0, 2, 1, false, false, 20L);

        StarRainNavigation.Target selected = StarRainNavigation
            .selectUnresolved(Arrays.asList(impacted, recovered, pending, fartherInFlight, nearerInFlight), 5L, 0, 0);

        assertSame(nearerInFlight, selected.impact());
        assertEquals(StarRainNavigation.State.IN_FLIGHT, selected.state());
        assertSame(
            pending,
            StarRainNavigation.selectUnresolved(Arrays.asList(impacted, pending), 5L, 0, 0)
                .impact());
        assertNull(StarRainNavigation.selectUnresolved(Arrays.asList(impacted, recovered), 5L, 0, 0));
    }

    @Test
    void distancesAndTieBreaksAreDeterministicAndListingUsesDueTimeForPending() {
        StarRainImpact farther = impact(8, 0, 6, 1, false, false);
        StarRainImpact nearer = impact(9, 3, 4, 1, false, false);
        StarRainImpact dueLater = impact(10, 1, 0, 1, false, false, 200L);
        StarRainImpact dueSooner = impact(11, 0, 1, 1, false, false, 100L);

        assertEquals(
            nearer.id(),
            StarRainNavigation
                .select(Arrays.asList(farther, nearer), 0, 0, 0, Collections.<DeterministicId, Long>emptyMap())
                .impact()
                .id());
        List<StarRainNavigation.Target> listing = StarRainNavigation
            .list(Arrays.asList(dueLater, dueSooner), 0, 0, 0, Collections.<DeterministicId, Long>emptyMap());
        assertEquals(
            dueSooner.id(),
            listing.get(0)
                .impact()
                .id());
        assertEquals(
            dueLater.id(),
            listing.get(1)
                .impact()
                .id());
    }

    @Test
    void recoveredRecordsSortLastAndLocatorDoesNotMutateImpactOrMassOwnership() {
        StarRainImpact recovered = impact(12, 1, 1, 1, true, true);
        StarRainImpact pending = impact(13, 2, 2, 1, false, false);
        MaterialQuantity before = pending.stellarMass();

        StarRainNavigation.Target result = StarRainNavigation
            .select(Arrays.asList(recovered, pending), 0, 0, 0, Collections.<DeterministicId, Long>emptyMap());

        assertSame(pending, result.impact());
        assertEquals(before, pending.stellarMass());
        assertTrue(!pending.isResolved());
        assertTrue(!pending.isMaterialized());
        assertEquals(
            StarRainNavigation.State.RECOVERED,
            StarRainNavigation.describe(recovered, 0, 0, 0, 0L)
                .state());

        StarRainImpact fallenStar = impact(16, 4, 4, 2, true, true);
        Map<DeterministicId, Long> siteMass = siteMasses(fallenStar, 875_000L);
        StarRainNavigation.Target located = StarRainNavigation
            .select(Collections.singletonList(fallenStar), 0, 0, 0, siteMass);
        assertSame(fallenStar, located.impact());
        assertEquals(
            875_000L,
            located.remainingMass()
                .milligrams());
        assertTrue(fallenStar.isResolved());
        assertTrue(fallenStar.isMaterialized());
        assertEquals(MaterialQuantity.ZERO, fallenStar.stellarMass());
        assertEquals(Long.valueOf(875_000L), siteMass.get(fallenStar.id()));
        assertEquals(pending.dueTick(), pending.effectiveDueTick());
        assertEquals(fallenStar.dueTick(), fallenStar.effectiveDueTick());
    }

    @Test
    void missingOrMalformedHistoryReturnsNoTarget() {
        assertNull(StarRainNavigation.select(null, 0, 0, 0, Collections.<DeterministicId, Long>emptyMap()));
        assertNull(
            StarRainNavigation.select(
                Collections.<StarRainImpact>emptyList(),
                0,
                0,
                0,
                Collections.<DeterministicId, Long>emptyMap()));
        assertNull(
            StarRainNavigation
                .select(Arrays.asList((StarRainImpact) null), 0, 0, 0, Collections.<DeterministicId, Long>emptyMap()));
    }

    private static StarRainImpact impact(int identity, long x, long z, int severity, boolean resolved,
        boolean materialized) {
        return impact(identity, x, z, severity, resolved, materialized, 100 + identity);
    }

    private static StarRainImpact impact(int identity, long x, long z, int severity, boolean resolved,
        boolean materialized, long dueTick) {
        DeterministicId id = DeterministicId.derive("p11-navigation-impact", Integer.toString(identity));
        return new StarRainImpact(
            PARENT,
            id,
            0,
            dueTick,
            (int) x,
            (int) z,
            severity,
            materialized ? MaterialQuantity.ZERO : MaterialQuantity.ofMilligrams(1000),
            resolved,
            materialized);
    }

    private static Map<DeterministicId, Long> siteMasses(StarRainImpact first, long firstMg) {
        Map<DeterministicId, Long> masses = new HashMap<>();
        masses.put(first.id(), firstMg);
        return masses;
    }

    private static Map<DeterministicId, Long> siteMasses(StarRainImpact first, long firstMg, StarRainImpact second,
        long secondMg) {
        Map<DeterministicId, Long> masses = siteMasses(first, firstMg);
        masses.put(second.id(), secondMg);
        return masses;
    }
}
