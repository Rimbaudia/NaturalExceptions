package naturalexceptions.core.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

import naturalexceptions.core.id.ResourceId;

public final class ContentManifest implements Iterable<ContentManifestEntry> {

    public static final int CURRENT_FORMAT_VERSION = 1;

    private final int formatVersion;
    private final String buildVersion;
    private final Map<ResourceId, ContentManifestEntry> entriesById;
    private final List<ContentManifestEntry> sortedEntries;

    private ContentManifest(int formatVersion, String buildVersion, Map<ResourceId, ContentManifestEntry> entries) {
        this.formatVersion = formatVersion;
        this.buildVersion = Objects.requireNonNull(buildVersion, "buildVersion");
        Map<ResourceId, ContentManifestEntry> sortedMap = new TreeMap<>(entries);
        this.entriesById = Collections.unmodifiableMap(new LinkedHashMap<>(sortedMap));
        this.sortedEntries = Collections.unmodifiableList(new ArrayList<>(sortedMap.values()));
    }

    public int formatVersion() {
        return formatVersion;
    }

    public String buildVersion() {
        return buildVersion;
    }

    public int size() {
        return entriesById.size();
    }

    public boolean isEmpty() {
        return entriesById.isEmpty();
    }

    public boolean contains(ResourceId id) {
        return entriesById.containsKey(id);
    }

    public ContentManifestEntry get(ResourceId id) {
        ContentManifestEntry entry = entriesById.get(id);
        if (entry == null) {
            throw new NoSuchElementException("No entry for id: " + id);
        }
        return entry;
    }

    public Collection<ContentManifestEntry> entries() {
        return sortedEntries;
    }

    @Override
    public Iterator<ContentManifestEntry> iterator() {
        return sortedEntries.iterator();
    }

    public static Builder builder(int formatVersion, String buildVersion) {
        return new Builder(formatVersion, buildVersion);
    }

    public static final class Builder {

        private final int formatVersion;
        private final String buildVersion;
        private final Map<ResourceId, ContentManifestEntry> entries = new LinkedHashMap<>();

        public Builder(int formatVersion, String buildVersion) {
            if (formatVersion <= 0) {
                throw new IllegalArgumentException("Format version must be positive: " + formatVersion);
            }
            if (buildVersion == null || buildVersion.trim()
                .isEmpty()) {
                throw new IllegalArgumentException("Build version cannot be null or empty");
            }
            this.formatVersion = formatVersion;
            this.buildVersion = buildVersion;
        }

        public Builder add(ContentManifestEntry entry) {
            if (entry == null) {
                throw new IllegalArgumentException("Entry cannot be null");
            }
            if (entries.containsKey(entry.id())) {
                throw new IllegalArgumentException("Duplicate content manifest id: " + entry.id());
            }
            entries.put(entry.id(), entry);
            return this;
        }

        public Builder add(ResourceId id, String type, long version) {
            return add(new ContentManifestEntry(id, type, version));
        }

        public ContentManifest build() {
            return new ContentManifest(formatVersion, buildVersion, entries);
        }
    }
}
