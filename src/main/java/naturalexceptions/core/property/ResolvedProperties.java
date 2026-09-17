package naturalexceptions.core.property;

import java.util.Objects;

/**
 * Result of resolving properties for a material, pairing the final properties with the diagnostic trace.
 */
public final class ResolvedProperties {

    private final MaterialProperties properties;
    private final ResolverTrace trace;

    public ResolvedProperties(MaterialProperties properties, ResolverTrace trace) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.trace = Objects.requireNonNull(trace, "trace");
    }

    public MaterialProperties properties() {
        return properties;
    }

    public ResolverTrace trace() {
        return trace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedProperties)) return false;
        ResolvedProperties that = (ResolvedProperties) o;
        return properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public String toString() {
        return "ResolvedProperties{properties=" + properties + ", trace=" + trace + '}';
    }
}
