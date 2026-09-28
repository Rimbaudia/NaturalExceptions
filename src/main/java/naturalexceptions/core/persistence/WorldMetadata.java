package naturalexceptions.core.persistence;

import java.util.Objects;

public final class WorldMetadata {

    private final String worldId;
    private final SaveSchemaVersion schemaVersion;
    private final WorldgenEpoch worldgenEpoch;
    private final String contentVersion;
    private final long createdAtTick;

    private WorldMetadata(String worldId, SaveSchemaVersion schemaVersion, WorldgenEpoch worldgenEpoch,
        String contentVersion, long createdAtTick) {
        this.worldId = worldId;
        this.schemaVersion = schemaVersion;
        this.worldgenEpoch = worldgenEpoch;
        this.contentVersion = contentVersion;
        this.createdAtTick = createdAtTick;
    }

    public static WorldMetadata create(String worldId, SaveSchemaVersion schemaVersion, WorldgenEpoch worldgenEpoch,
        String contentVersion, long createdAtTick) {
        if (worldId == null || worldId.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("World ID cannot be null or empty");
        }
        if (schemaVersion == null) {
            throw new IllegalArgumentException("Save schema version cannot be null");
        }
        if (worldgenEpoch == null) {
            throw new IllegalArgumentException("Worldgen epoch cannot be null");
        }
        if (contentVersion == null || contentVersion.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Content version cannot be null or empty");
        }
        if (createdAtTick < 0) {
            throw new IllegalArgumentException("Creation tick cannot be negative: " + createdAtTick);
        }
        return new WorldMetadata(worldId, schemaVersion, worldgenEpoch, contentVersion, createdAtTick);
    }

    public static WorldMetadata createNew(String worldId, String contentVersion) {
        return create(worldId, SaveSchemaVersion.CURRENT, WorldgenEpoch.CURRENT, contentVersion, 0L);
    }

    public String worldId() {
        return worldId;
    }

    public SaveSchemaVersion schemaVersion() {
        return schemaVersion;
    }

    public WorldgenEpoch worldgenEpoch() {
        return worldgenEpoch;
    }

    public String contentVersion() {
        return contentVersion;
    }

    public long createdAtTick() {
        return createdAtTick;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldMetadata)) return false;
        WorldMetadata that = (WorldMetadata) o;
        return createdAtTick == that.createdAtTick && worldId.equals(that.worldId)
            && schemaVersion.equals(that.schemaVersion)
            && worldgenEpoch.equals(that.worldgenEpoch)
            && contentVersion.equals(that.contentVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, schemaVersion, worldgenEpoch, contentVersion, createdAtTick);
    }

    @Override
    public String toString() {
        return "WorldMetadata{" + "worldId='"
            + worldId
            + '\''
            + ", schemaVersion="
            + schemaVersion
            + ", worldgenEpoch="
            + worldgenEpoch
            + ", contentVersion='"
            + contentVersion
            + '\''
            + ", createdAtTick="
            + createdAtTick
            + '}';
    }
}
