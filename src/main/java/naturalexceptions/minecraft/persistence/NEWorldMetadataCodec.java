package naturalexceptions.minecraft.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.scheduler.ScheduledEvent;

public final class NEWorldMetadataCodec {

    public static final String KEY_SCHEMA_VERSION = "SchemaVersion";
    public static final String KEY_WORLDGEN_EPOCH = "WorldgenEpoch";
    public static final String KEY_WORLD_ID = "WorldId";
    public static final String KEY_CONTENT_VERSION = "ContentVersion";
    public static final String KEY_CREATED_AT_TICK = "CreatedAtTick";
    public static final String KEY_SCHEDULED_EVENTS = "ScheduledEvents";

    public static final String EVENT_KEY_ID = "Id";
    public static final String EVENT_KEY_TYPE = "EventType";
    public static final String EVENT_KEY_DUE_TICK = "DueTick";
    public static final String EVENT_KEY_PAYLOAD = "Payload";

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

    public static void writeScheduledEventsToNbt(Collection<ScheduledEvent> events, NBTTagCompound nbt) {
        if (events == null || nbt == null) {
            return;
        }
        NBTTagList list = new NBTTagList();
        for (ScheduledEvent event : events) {
            NBTTagCompound eventTag = new NBTTagCompound();
            eventTag.setString(
                EVENT_KEY_ID,
                event.id()
                    .hex());
            eventTag.setString(
                EVENT_KEY_TYPE,
                event.eventType()
                    .toString());
            eventTag.setLong(EVENT_KEY_DUE_TICK, event.dueTick());
            if (event.payload() != null && !event.payload()
                .isEmpty()) {
                eventTag.setString(EVENT_KEY_PAYLOAD, event.payload());
            }
            list.appendTag(eventTag);
        }
        nbt.setTag(KEY_SCHEDULED_EVENTS, list);
    }

    public static List<ScheduledEvent> readScheduledEventsFromNbt(NBTTagCompound nbt) {
        List<ScheduledEvent> events = new ArrayList<>();
        if (nbt == null || !nbt.hasKey(KEY_SCHEDULED_EVENTS, 9)) {
            return events;
        }
        NBTTagList list = nbt.getTagList(KEY_SCHEDULED_EVENTS, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.hasKey(EVENT_KEY_ID, 8)) {
                throw new PersistenceValidationException("Scheduled event missing 'Id'");
            }
            DeterministicId id;
            try {
                id = DeterministicId.parse(tag.getString(EVENT_KEY_ID));
            } catch (IllegalArgumentException e) {
                throw new PersistenceValidationException(
                    "Malformed scheduled event 'Id': " + tag.getString(EVENT_KEY_ID),
                    e);
            }

            if (!tag.hasKey(EVENT_KEY_TYPE, 8)) {
                throw new PersistenceValidationException("Scheduled event missing 'EventType'");
            }
            ResourceId eventType;
            try {
                eventType = ResourceId.parse(tag.getString(EVENT_KEY_TYPE));
            } catch (IllegalArgumentException e) {
                throw new PersistenceValidationException(
                    "Malformed scheduled event 'EventType': " + tag.getString(EVENT_KEY_TYPE),
                    e);
            }

            if (!tag.hasKey(EVENT_KEY_DUE_TICK, 99)) {
                throw new PersistenceValidationException("Scheduled event missing 'DueTick'");
            }
            long dueTick = tag.getLong(EVENT_KEY_DUE_TICK);
            if (dueTick < 0) {
                throw new PersistenceValidationException("Negative 'DueTick' in scheduled event: " + dueTick);
            }

            String payload = tag.getString(EVENT_KEY_PAYLOAD);
            events.add(new ScheduledEvent(id, eventType, dueTick, payload));
        }
        return events;
    }
}
