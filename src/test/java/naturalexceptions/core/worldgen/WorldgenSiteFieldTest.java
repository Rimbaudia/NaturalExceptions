package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.persistence.WorldgenEpoch;

final class WorldgenSiteFieldTest {

    @Test
    void structuresRequireRuggedSuitableGeologyAndEnvironment() {
        WorldgenSites sites = new WorldgenSites(new OverworldGeneration(615889L, WorldgenEpoch.INITIAL));
        int suitable = 0;
        int unsuitable = 0;
        for (int x = -180; x <= 180; x++) {
            for (int z = -180; z <= 180; z++) {
                WorldgenSite site = sites.structureAtCell(x, z);
                if (site != null) {
                    suitable++;
                    assertTrue(
                        site.sample()
                            .elevationTendency() >= 0.55);
                    assertTrue(
                        site.sample()
                            .slope() >= 0.025);
                    assertTrue(
                        site.sample()
                            .geology() != GeologicalProvince.SEDIMENTARY);
                } else {
                    unsuitable++;
                }
            }
        }
        assertTrue(suitable > 0);
        assertTrue(unsuitable > suitable);
    }

    @Test
    void initialAnomaliesAreRestrictedToDeclaredProvinceAndEnvironment() {
        WorldgenSites sites = new WorldgenSites(new OverworldGeneration(1459002L, WorldgenEpoch.INITIAL));
        WorldgenSite anomaly = null;
        for (int x = -400; x <= 400 && anomaly == null; x++) {
            for (int z = -400; z <= 400 && anomaly == null; z++) anomaly = sites.anomalyAtCell(x, z);
        }
        assertNotNull(anomaly, "the authored suitability is rare but reachable");
        assertEquals(
            GeologicalProvince.GRANITIC,
            anomaly.sample()
                .geology());
        assertTrue(
            anomaly.sample()
                .elevationTendency() >= 0.56);
        assertTrue(
            anomaly.sample()
                .moisture() <= 0.66);
        assertNull(sites.anomalyAtCell(100000, -100000), "distant deterministic cells need not host an anomaly");
    }
}
