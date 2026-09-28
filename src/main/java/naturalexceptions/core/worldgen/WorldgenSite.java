package naturalexceptions.core.worldgen;

import java.util.Objects;

import naturalexceptions.core.id.DeterministicId;

/** Deterministic proposed placement that is accepted only after checking its sampled setting. */
public final class WorldgenSite {

    public enum Kind {
        ROCK_OUTCROP,
        INITIAL_ANOMALY
    }

    private final DeterministicId id;
    private final Kind kind;
    private final int x;
    private final int z;
    private final OverworldSample sample;

    WorldgenSite(DeterministicId id, Kind kind, int x, int z, OverworldSample sample) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.x = x;
        this.z = z;
        this.sample = Objects.requireNonNull(sample, "sample");
    }

    public DeterministicId id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public OverworldSample sample() {
        return sample;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WorldgenSite)) return false;
        WorldgenSite that = (WorldgenSite) other;
        return x == that.x && z == that.z && id.equals(that.id) && kind == that.kind && sample.equals(that.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, kind, x, z, sample);
    }
}
