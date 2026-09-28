package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;

final class OverworldGenerationTest {

    @Test
    void sameSeedAndEpochReproduceSampledTruthAcrossIndependentContexts() {
        OverworldGeneration first = new OverworldGeneration(0x123456789ABCDEFL, WorldgenEpoch.INITIAL);
        OverworldGeneration second = new OverworldGeneration(0x123456789ABCDEFL, WorldgenEpoch.INITIAL);

        for (int x = -513; x <= 513; x += 37) {
            for (int z = -513; z <= 513; z += 41) {
                assertEquals(first.sample(x, z), second.sample(x, z), "sample at " + x + "," + z);
            }
        }
    }

    @Test
    void epochChangesGeneratedTruthWithoutUsingSaveSchema() {
        OverworldGeneration epochOne = new OverworldGeneration(9918273L, WorldgenEpoch.of(1));
        OverworldGeneration epochTwo = new OverworldGeneration(9918273L, WorldgenEpoch.of(2));

        assertNotEquals(epochOne.sample(120, -88), epochTwo.sample(120, -88));
    }

    @Test
    void spatialChoicesAreStableAndDoNotDependOnCallOrder() {
        OverworldGeneration first = new OverworldGeneration(99L, WorldgenEpoch.CURRENT);
        OverworldGeneration second = new OverworldGeneration(99L, WorldgenEpoch.CURRENT);

        boolean expected = first.passesChanceAt(-12, 41, 77L, 0.37);
        first.sample(900, -220);
        assertEquals(expected, first.passesChanceAt(-12, 41, 77L, 0.37));
        assertEquals(expected, second.passesChanceAt(-12, 41, 77L, 0.37));
        assertTrue(!first.passesChanceAt(-12, 41, 77L, 0.0));
        assertTrue(first.passesChanceAt(-12, 41, 77L, 1.0));
    }

    @Test
    void coarseFieldsRemainCenteredInsteadOfClampingIntoHighlandProvinces() {
        OverworldGeneration generation = new OverworldGeneration(88371002L, WorldgenEpoch.CURRENT);
        double elevation = 0.0;
        double moisture = 0.0;
        double temperature = 0.0;
        int basaltic = 0;
        int sedimentary = 0;
        int granitic = 0;
        int count = 0;
        for (int x = -1800; x <= 1800; x += 73) {
            for (int z = -1800; z <= 1800; z += 79) {
                OverworldSample sample = generation.sample(x, z);
                elevation += sample.elevationTendency();
                moisture += sample.moisture();
                temperature += sample.temperature();
                if (sample.geology() == GeologicalProvince.BASALTIC) basaltic++;
                if (sample.geology() == GeologicalProvince.SEDIMENTARY) sedimentary++;
                if (sample.geology() == GeologicalProvince.GRANITIC) granitic++;
                count++;
            }
        }
        assertTrue(Math.abs(elevation / count - 0.5) < 0.08, "elevation field should remain centered");
        assertTrue(Math.abs(moisture / count - 0.5) < 0.08, "moisture field should remain centered");
        assertTrue(Math.abs(temperature / count - 0.5) < 0.08, "temperature field should remain centered");
        assertTrue(basaltic > count / 10 && basaltic < count * 7 / 10);
        assertTrue(sedimentary > count / 10 && sedimentary < count * 7 / 10);
        assertTrue(granitic > count / 10 && granitic < count * 7 / 10);
    }

    @Test
    void samplesExposeBoundedEnvironmentalValuesAndDerivedStages() {
        OverworldSample sample = new OverworldGeneration(77L, WorldgenEpoch.INITIAL).sample(-321, 917);

        assertTrue(sample.elevationTendency() >= 0.0 && sample.elevationTendency() <= 1.0);
        assertTrue(sample.moisture() >= 0.0 && sample.moisture() <= 1.0);
        assertTrue(sample.temperature() >= 0.0 && sample.temperature() <= 1.0);
        assertNotEquals(null, sample.geology());
        assertNotEquals(null, sample.hydrology());
        assertNotEquals(null, sample.soil());
        assertTrue(sample.ecologySuitability() >= 0.0 && sample.ecologySuitability() <= 1.0);
    }
}
