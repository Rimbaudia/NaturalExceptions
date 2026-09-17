package naturalexceptions.core.content;

import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

public final class ContentManifestEntry implements Comparable<ContentManifestEntry> {

    private final ResourceId id;
    private final String type;
    private final long version;

    public ContentManifestEntry(ResourceId id, String type, long version) {
        if (id == null) {
            throw new IllegalArgumentException("ResourceId cannot be null");
        }
        if (type == null || type.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative: " + version);
        }
        this.id = id;
        this.type = type;
        this.version = version;
    }

    public ResourceId id() {
        return id;
    }

    public String type() {
        return type;
    }

    public long version() {
        return version;
    }

    @Override
    public int compareTo(ContentManifestEntry other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare ContentManifestEntry to null");
        }
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentManifestEntry)) return false;
        ContentManifestEntry that = (ContentManifestEntry) o;
        return version == that.version && id.equals(that.id) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, version);
    }

    @Override
    public String toString() {
        return "ContentManifestEntry{id=" + id + ", type='" + type + "', version=" + version + "}";
    }
}
