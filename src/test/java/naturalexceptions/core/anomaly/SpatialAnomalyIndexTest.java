package naturalexceptions.core.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;

final class SpatialAnomalyIndexTest {

    @Test
    void indexesFiniteCircularInfluenceAcrossNegativeCoordinates() {
        SpatialAnomalyIndex index = new SpatialAnomalyIndex();
        SpatialAnomaly lake = anomaly("lake", -513, -1, 20, 0.8, 0.0);
        index.add(lake);

        assertEquals(Collections.singletonList(lake), index.queryAt(-513, -1).anomalies());
        assertEquals(Collections.singletonList(lake), index.queryAt(-493, -1).anomalies());
        assertTrue(index.queryAt(-492, -1).anomalies().isEmpty());
        assertTrue(index.queryAt(0, 0).anomalies().isEmpty());
    }

    @Test
    void distantAnomaliesAreNotScannedForLocalQuery() {
        SpatialAnomalyIndex index = new SpatialAnomalyIndex();
        for (int i = 1; i <= 2500; i++) {
            index.add(anomaly("a" + i, i * 8192, i * -8192, 12, 0.1, 0.0));
        }
        AnomalyQueryResult local = index.queryAt(11, -17);
        assertTrue(local.anomalies().isEmpty());
        assertEquals(0, local.candidatesExamined());
        assertEquals(1, local.bucketsExamined());
    }

    @Test
    void indexReportsCandidatesInsteadOfTotalIndexedCount() {
        SpatialAnomalyIndex index = new SpatialAnomalyIndex();
        SpatialAnomaly first = anomaly("first", 0, 0, 32, 0.2, 0.0);
        index.add(first);
        for (int i = 1; i < 20; i++) index.add(anomaly("far" + i, i * 16384, 0, 8, 0.1, 0.0));
        AnomalyQueryResult query = index.queryAt(1, 1);
        assertEquals(Collections.singletonList(first), query.anomalies());
        assertEquals(1, query.candidatesExamined());
    }

    @Test
    void equivalentDurableReconstructionIsIdempotentButConflictingIdentityIsRejected() {
        SpatialAnomalyIndex index = new SpatialAnomalyIndex();
        SpatialAnomaly first = anomaly("same", -20, 10, 15, 0.5, 0.2);
        index.add(first);
        index.add(anomaly("same", -20, 10, 15, 0.5, 0.2));
        assertEquals(1, index.size());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> index.add(anomaly("same", -19, 10, 15, 0.5, 0.2)));
    }

    private static SpatialAnomaly anomaly(String id, int x, int z, int radius, double water, double night) {
        return new SpatialAnomaly(DeterministicId.derive("test", id), ResourceId.of("test", "anomaly"),
            x, z, radius, EnvironmentalInfluence.of(DeterministicId.derive("test-source", id), water, night, 0));
    }
}
