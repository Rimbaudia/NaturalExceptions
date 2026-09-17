package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.WorldgenEpoch;

public final class NEWorldMetadataCodec {

    public static final String KEY_SCHEMA_VERSION = "SchemaVersion";
    public static final String KEY_WORLDGEN_EPOCH = "WorldgenEpoch";
    public static final String KEY_WORLD_ID = "WorldId";
    public static final String KEY_CONTENT_VERSION = "ContentVersion";
    public static final String KEY_CREATED_AT_TICK = "CreatedAtTick";

    private NEWorldMetadataCodec() {}

    public static void writeToNbt(WorldMetadata metadata, NBTTagCompound nbt) {
        if (metadata == null) {
            throw new IllegalArgumentException("WorldMetadata cannot be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("NBTTagCompound cannot be null");
        }
        nbt.setInteger(
            KEY_SCHEMA_VERSION,
            metadata.schemaVersion()
                .value());
        nbt.setInteger(
            KEY_WORLDGEN_EPOCH,
            metadata.worldgenEpoch()
                .value());
        nbt.setString(KEY_WORLD_ID, metadata.worldId());
        nbt.setString(KEY_CONTENT_VERSION, metadata.contentVersion());
        nbt.setLong(KEY_CREATED_AT_TICK, metadata.createdAtTick());
    }

    public static WorldMetadata readFromNbt(NBTTagCompound nbt) {
        if (nbt == null) {
            throw new PersistenceValidationException("NBTTagCompound cannot be null");
        }
        if (!nbt.hasKey(KEY_SCHEMA_VERSION, 99)) {
            throw new PersistenceValidationException("Missing required 'SchemaVersion' field in world data");
        }
        int schemaVal = nbt.getInteger(KEY_SCHEMA_VERSION);
        if (schemaVal <= 0) {
            throw new PersistenceValidationException("Invalid 'SchemaVersion' value: " + schemaVal);
        }
        SaveSchemaVersion schemaVersion = SaveSchemaVersion.of(schemaVal);
        if (!SaveSchemaVersion.CURRENT.isCompatibleWith(schemaVersion)) {
            throw new PersistenceValidationException(
                "Unsupported save schema version: " + schemaVal
                    + " (supported: "
                    + SaveSchemaVersion.CURRENT.value()
                    + ")");
        }

        if (!nbt.hasKey(KEY_WORLDGEN_EPOCH, 99)) {
            throw new PersistenceValidationException("Missing required 'WorldgenEpoch' field in world data");
        }
        int epochVal = nbt.getInteger(KEY_WORLDGEN_EPOCH);
        if (epochVal <= 0) {
            throw new PersistenceValidationException("Invalid 'WorldgenEpoch' value: " + epochVal);
        }
        WorldgenEpoch worldgenEpoch = WorldgenEpoch.of(epochVal);

        if (!nbt.hasKey(KEY_WORLD_ID, 8)) {
            throw new PersistenceValidationException("Missing required 'WorldId' field in world data");
        }
        String worldId = nbt.getString(KEY_WORLD_ID);
        if (worldId.trim()
            .isEmpty()) {
            throw new PersistenceValidationException("Empty 'WorldId' field in world data");
        }

        if (!nbt.hasKey(KEY_CONTENT_VERSION, 8)) {
            throw new PersistenceValidationException("Missing required 'ContentVersion' field in world data");
        }
        String contentVersion = nbt.getString(KEY_CONTENT_VERSION);
        if (contentVersion.trim()
            .isEmpty()) {
            throw new PersistenceValidationException("Empty 'ContentVersion' field in world data");
        }

        long createdAtTick = nbt.getLong(KEY_CREATED_AT_TICK);
        if (createdAtTick < 0) {
            createdAtTick = 0;
        }

        return WorldMetadata.create(worldId, schemaVersion, worldgenEpoch, contentVersion, createdAtTick);
    }
}
