package naturalexceptions.core.gravity;

import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

/**
 * Immutable identifier for a gravitational field or source.
 */
public final class GravitySourceId implements Comparable<GravitySourceId> {

    public static final GravitySourceId DEFAULT_MINECRAFT = of("minecraft", "standard_downward");
    public static final GravitySourceId ZERO_G = of("naturalexceptions", "zero_g");

    private final ResourceId id;

    private GravitySourceId(ResourceId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static GravitySourceId of(ResourceId id) {
        return new GravitySourceId(id);
    }

    public static GravitySourceId of(String namespace, String path) {
        return new GravitySourceId(ResourceId.of(namespace, path));
    }

    public ResourceId id() {
        return id;
    }

    @Override
    public int compareTo(GravitySourceId o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GravitySourceId)) return false;
        GravitySourceId that = (GravitySourceId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
