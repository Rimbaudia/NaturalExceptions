package naturalexceptions.core.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;

final class EnvironmentalInfluenceResolverTest {

    @Test
    void independentSpatialAndTemporalInfluencesComposeThroughWaterContext() {
        EnvironmentalInfluence cowLake = EnvironmentalInfluence.of(DeterministicId.derive("cow-lake"), 0.75, 0, 0.2);
        EnvironmentalInfluence bloodNight = EnvironmentalInfluence.of(DeterministicId.derive("blood-night"), 0, 0.8, 0);
        ResolvedEnvironmentalContext lakeOnly = EnvironmentalInfluenceResolver.resolve(Arrays.asList(cowLake));
        ResolvedEnvironmentalContext nightOnly = EnvironmentalInfluenceResolver.resolve(Arrays.asList(bloodNight));
        ResolvedEnvironmentalContext overlap = EnvironmentalInfluenceResolver.resolve(Arrays.asList(bloodNight, cowLake));

        assertEquals(0.75, lakeOnly.waterAvailability(), 1e-12);
        assertEquals(0.8, nightOnly.nocturnalReactivity(), 1e-12);
        assertEquals(0.6, overlap.waterReactionIntensity(), 1e-12);
        assertTrue(overlap.waterReactionIntensity() > lakeOnly.waterReactionIntensity());
        assertTrue(overlap.waterReactionIntensity() > nightOnly.waterReactionIntensity());
    }

    @Test
    void summationIsIndependentOfInputOrderAndBounded() {
        EnvironmentalInfluence a = EnvironmentalInfluence.of(DeterministicId.derive("a"), 0.8, 0.4, 0.3);
        EnvironmentalInfluence b = EnvironmentalInfluence.of(DeterministicId.derive("b"), 0.7, 0.9, 0.8);
        ResolvedEnvironmentalContext first = EnvironmentalInfluenceResolver.resolve(Arrays.asList(a, b));
        ResolvedEnvironmentalContext reversed = EnvironmentalInfluenceResolver.resolve(Arrays.asList(b, a));
        assertEquals(first, reversed);
        assertEquals(1, first.waterAvailability(), 0);
        assertEquals(1, first.nocturnalReactivity(), 0);
        assertEquals(1, first.moisture(), 0);
    }

    @Test
    void composesWithBaseEnvironmentalTruthRatherThanReplacingIt() {
        EnvironmentalInfluence night = EnvironmentalInfluence.of(DeterministicId.derive("night"), 0, 0.5, 0);
        ResolvedEnvironmentalContext resolved = EnvironmentalInfluenceResolver.resolve(0.6, 0.1, 0.35,
            Collections.singletonList(night));
        assertEquals(0.6, resolved.waterAvailability(), 1e-12);
        assertEquals(0.6, resolved.nocturnalReactivity(), 1e-12);
        assertEquals(0.35, resolved.moisture(), 1e-12);
    }
}
