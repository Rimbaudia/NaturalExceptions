package naturalexceptions.core.id;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ResourceId implements Comparable<ResourceId> {

    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");

    private final String namespace;
    private final String path;

    private ResourceId(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceId of(String namespace, String path) {
        requirePart("namespace", namespace);
        requirePart("path", path);
        return new ResourceId(namespace, path);
    }

    public static ResourceId parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ResourceId string cannot be null");
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Expected namespaced id namespace:path: " + value);
        }
        return of(value.substring(0, separator), value.substring(separator + 1));
    }

    private static void requirePart(String name, String value) {
        if (value == null || !PART.matcher(value)
            .matches()) {
            throw new IllegalArgumentException("Invalid " + name + ": " + value);
        }
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    @Override
    public int compareTo(ResourceId other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare ResourceId to null");
        }
        int cmp = this.namespace.compareTo(other.namespace);
        if (cmp != 0) {
            return cmp;
        }
        return this.path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ResourceId)) return false;
        ResourceId that = (ResourceId) other;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }
}
