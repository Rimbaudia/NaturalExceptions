package naturalexceptions.minecraft.persistence;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.persistence.WorldMetadata;

public final class NEWorldSavedData extends WorldSavedData {

    public static final String DATA_IDENTIFIER = "naturalexceptions_world_data";

    private WorldMetadata metadata;

    public NEWorldSavedData(String name) {
        super(name);
    }

    public NEWorldSavedData() {
        this(DATA_IDENTIFIER);
    }

    public static NEWorldSavedData get(World world) {
        if (world == null || world.mapStorage == null) {
            return null;
        }
        NEWorldSavedData data = (NEWorldSavedData) world.mapStorage.loadData(NEWorldSavedData.class, DATA_IDENTIFIER);
        if (data == null) {
            data = new NEWorldSavedData(DATA_IDENTIFIER);
            String initialWorldId = UUID.randomUUID()
                .toString();
            long initialTick = world.getWorldInfo() != null ? world.getWorldInfo()
                .getWorldTotalTime() : 0L;
            data.setMetadata(
                WorldMetadata.create(
                    initialWorldId,
                    naturalexceptions.core.persistence.SaveSchemaVersion.CURRENT,
                    naturalexceptions.core.persistence.WorldgenEpoch.INITIAL,
                    BuiltinContent.CONTENT_VERSION,
                    initialTick));
            data.markDirty();
            world.mapStorage.setData(DATA_IDENTIFIER, data);
        }
        return data;
    }

    public WorldMetadata metadata() {
        return metadata;
    }

    public void setMetadata(WorldMetadata metadata) {
        this.metadata = metadata;
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.metadata = NEWorldMetadataCodec.readFromNbt(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        if (metadata != null) {
            NEWorldMetadataCodec.writeToNbt(metadata, nbt);
        }
    }
}
