package naturalexceptions.core.anomaly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import naturalexceptions.core.id.DeterministicId;

/** Bucketed point-query index. Queries inspect one bucket, then exact-check only its candidates. */
public final class SpatialAnomalyIndex {
    public static final int BUCKET_SIZE_BLOCKS = 512;
    private final Map<Long, Set<DeterministicId>> buckets = new HashMap<>();
    private final Map<DeterministicId, SpatialAnomaly> byId = new HashMap<>();

    public void add(SpatialAnomaly anomaly) {
        if (anomaly == null) throw new IllegalArgumentException("anomaly cannot be null");
        SpatialAnomaly previous = byId.get(anomaly.id());
        if (previous != null) {
            if (previous.equals(anomaly)) return;
            throw new IllegalArgumentException("Duplicate anomaly identity: " + anomaly.id());
        }
        byId.put(anomaly.id(), anomaly);
        forEachBucket(anomaly, new BucketConsumer() {
            @Override public void accept(long key) {
                Set<DeterministicId> ids = buckets.get(key);
                if (ids == null) { ids = new HashSet<>(); buckets.put(key, ids); }
                ids.add(anomaly.id());
            }
        });
    }

    public boolean remove(DeterministicId id) {
        SpatialAnomaly anomaly = byId.remove(id);
        if (anomaly == null) return false;
        forEachBucket(anomaly, new BucketConsumer() {
            @Override public void accept(long key) {
                Set<DeterministicId> ids = buckets.get(key);
                if (ids != null) {
                    ids.remove(anomaly.id());
                    if (ids.isEmpty()) buckets.remove(key);
                }
            }
        });
        return true;
    }

    public SpatialAnomaly get(DeterministicId id) { return byId.get(id); }
    public int size() { return byId.size(); }

    public List<SpatialAnomaly> snapshot() {
        List<SpatialAnomaly> result = new ArrayList<>(byId.values());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public AnomalyQueryResult queryAt(int x, int z) {
        Set<DeterministicId> ids = buckets.get(bucketKey(Math.floorDiv(x, BUCKET_SIZE_BLOCKS),
            Math.floorDiv(z, BUCKET_SIZE_BLOCKS)));
        if (ids == null || ids.isEmpty()) return new AnomalyQueryResult(Collections.<SpatialAnomaly>emptyList(), 1, 0);
        List<SpatialAnomaly> result = new ArrayList<>();
        int candidates = 0;
        for (DeterministicId id : ids) {
            SpatialAnomaly anomaly = byId.get(id);
            if (anomaly != null) {
                candidates++;
                if (anomaly.influences(x, z)) result.add(anomaly);
            }
        }
        Collections.sort(result);
        return new AnomalyQueryResult(result, 1, candidates);
    }

    private static void forEachBucket(SpatialAnomaly anomaly, BucketConsumer consumer) {
        long minX = Math.floorDiv((long) anomaly.centerX() - anomaly.radiusBlocks(), BUCKET_SIZE_BLOCKS);
        long maxX = Math.floorDiv((long) anomaly.centerX() + anomaly.radiusBlocks(), BUCKET_SIZE_BLOCKS);
        long minZ = Math.floorDiv((long) anomaly.centerZ() - anomaly.radiusBlocks(), BUCKET_SIZE_BLOCKS);
        long maxZ = Math.floorDiv((long) anomaly.centerZ() + anomaly.radiusBlocks(), BUCKET_SIZE_BLOCKS);
        for (long bx = minX; bx <= maxX; bx++) {
            for (long bz = minZ; bz <= maxZ; bz++) consumer.accept(bucketKey(bx, bz));
        }
    }

    private static long bucketKey(long x, long z) { return (x << 32) ^ (z & 0xffffffffL); }
    private interface BucketConsumer { void accept(long key); }
}
