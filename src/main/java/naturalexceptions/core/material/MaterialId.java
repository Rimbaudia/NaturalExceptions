package naturalexceptions.core.material;

import naturalexceptions.core.id.ResourceId;

public final class MaterialId implements Comparable<MaterialId> {

    private final ResourceId value;

    private MaterialId(ResourceId value) {
        this.value = value;
    }

    public static MaterialId of(String namespace, String path) {
        return new MaterialId(ResourceId.of(namespace, path));
    }

    public static MaterialId parse(String value) {
        return new MaterialId(ResourceId.parse(value));
    }

    public ResourceId resourceId() {
        return value;
    }

    @Override
    public int compareTo(MaterialId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MaterialId && value.equals(((MaterialId) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
