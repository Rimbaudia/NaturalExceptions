package naturalexceptions.core.anomaly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable spatial query result with work counters for performance proofs. */
public final class AnomalyQueryResult {
    private final List<SpatialAnomaly> anomalies;
    private final int bucketsExamined;
    private final int candidatesExamined;

    AnomalyQueryResult(List<SpatialAnomaly> anomalies, int bucketsExamined, int candidatesExamined) {
        this.anomalies = Collections.unmodifiableList(new ArrayList<>(anomalies));
        this.bucketsExamined = bucketsExamined;
        this.candidatesExamined = candidatesExamined;
    }

    public List<SpatialAnomaly> anomalies() { return anomalies; }
    public int bucketsExamined() { return bucketsExamined; }
    public int candidatesExamined() { return candidatesExamined; }
}
