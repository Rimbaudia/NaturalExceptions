package naturalexceptions.minecraft.persistence;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.scheduler.DueTimeScheduler;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.core.electrical.ElectricalWorldState;
import naturalexceptions.core.event.StarRainHistory;

public final class NEWorldSavedData extends WorldSavedData {

    public static final String DATA_IDENTIFIER = "naturalexceptions_world_data";

    private WorldMetadata metadata;
    private final DueTimeScheduler scheduler = new DueTimeScheduler();
    private NBTTagCompound p8PendingNbt;
    private P8WorldState p8;
    private NBTTagCompound cachedP8Tag;
    private NBTTagCompound fluidPendingNbt;
    private FluidWorldState fluids;
    private NBTTagCompound cachedFluidsTag;
    private NBTTagCompound electricalPendingNbt;
    private ElectricalWorldState electrical;
    private NBTTagCompound cachedElectricalTag;
    private NBTTagCompound p11PendingNbt;
    private StarRainHistory p11;
    private NBTTagCompound cachedP11Tag;

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
                    naturalexceptions.core.persistence.WorldgenEpoch.CURRENT,
                    BuiltinContent.CONTENT_VERSION,
                    initialTick));
            data.markDirty();
            world.mapStorage.setData(DATA_IDENTIFIER, data);
        }
        data.ensureP8(world);
        data.ensureP11();
        data.ensureFluids();
        return data;
    }

    public WorldMetadata metadata() {
        return metadata;
    }

    public void setMetadata(WorldMetadata metadata) {
        this.metadata = metadata;
        this.markDirty();
    }

    public DueTimeScheduler scheduler() {
        return scheduler;
    }

    public synchronized P8WorldState p8() { return p8; }

    private synchronized void ensureP8(World world) {
        if (p8 != null || metadata == null || world == null) return;
        long createdAt = Math.max(0L, metadata.createdAtTick());
        p8 = p8PendingNbt == null
            ? new P8WorldState(world.getSeed(), metadata.worldgenEpoch().value(), createdAt)
            : P8WorldStateNbtCodec.read(p8PendingNbt, world.getSeed(), metadata.worldgenEpoch().value(), createdAt);
        p8PendingNbt = null;
        markP8Dirty();
    }

    private synchronized void ensureFluids() {
        if (fluids != null) return;
        fluids = fluidPendingNbt == null ? new FluidWorldState()
            : FluidWorldStateNbtCodec.read(fluidPendingNbt);
        fluidPendingNbt = null;
        cachedFluidsTag = null;
    }

    public synchronized FluidWorldState fluids() {
        ensureFluids();
        return fluids;
    }

    private synchronized void ensureElectrical() {
        if (electrical != null) return;
        electrical = electricalPendingNbt == null ? new ElectricalWorldState()
            : ElectricalWorldStateNbtCodec.read(electricalPendingNbt);
        electricalPendingNbt = null;
        cachedElectricalTag = null;
    }

    public synchronized ElectricalWorldState electrical() {
        ensureElectrical();
        return electrical;
    }

    private synchronized void ensureP11() {
        if (p11 != null) return;
        p11 = p11PendingNbt == null ? new StarRainHistory() : StarRainHistoryNbtCodec.read(p11PendingNbt);
        p11PendingNbt = null;
        cachedP11Tag = null;
    }

    public synchronized StarRainHistory p11() {
        ensureP11();
        return p11;
    }

    public synchronized void markP8Dirty() {
        cachedP8Tag = null;
        super.markDirty();
    }

    public synchronized void markFluidsDirty() {
        cachedFluidsTag = null;
        super.markDirty();
    }

    public synchronized void markElectricalDirty() {
        cachedElectricalTag = null;
        super.markDirty();
    }

    public synchronized void markP11Dirty() {
        cachedP11Tag = null;
        super.markDirty();
    }

    public synchronized void markSchedulerDirty() {
        super.markDirty();
    }

    @Override
    public synchronized void markDirty() {
        cachedP8Tag = null;
        cachedFluidsTag = null;
        cachedElectricalTag = null;
        cachedP11Tag = null;
        super.markDirty();
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound nbt) {
        this.metadata = NEWorldMetadataCodec.readFromNbt(nbt);
        this.scheduler.restore(NEWorldMetadataCodec.readScheduledEventsFromNbt(nbt));
        this.p8 = null;
        this.cachedP8Tag = null;
        this.fluids = null;
        this.cachedFluidsTag = null;
        this.electrical = null;
        this.cachedElectricalTag = null;
        this.p11 = null;
        this.cachedP11Tag = null;
        this.p8PendingNbt = nbt.hasKey("P8", 10) ? (NBTTagCompound) nbt.getCompoundTag("P8").copy() : null;
        this.fluidPendingNbt = nbt.hasKey("P9Fluids", 10)
            ? (NBTTagCompound) nbt.getCompoundTag("P9Fluids").copy() : null;
        this.electricalPendingNbt = nbt.hasKey("P9Electricity", 10)
            ? (NBTTagCompound) nbt.getCompoundTag("P9Electricity").copy() : null;
        this.p11PendingNbt = nbt.hasKey("P11StarRain", 10)
            ? (NBTTagCompound) nbt.getCompoundTag("P11StarRain").copy() : null;
        // P7 personal aliases/evidence are owned by each player, never shared world state.
    }

    @Override
    public synchronized void writeToNBT(NBTTagCompound nbt) {
        if (metadata != null) {
            NEWorldMetadataCodec.writeToNbt(metadata, nbt);
        }
        NEWorldMetadataCodec.writeScheduledEventsToNbt(scheduler.snapshot(), nbt);
        if (p8 != null) {
            if (cachedP8Tag == null) cachedP8Tag = P8WorldStateNbtCodec.write(p8);
            nbt.setTag("P8", (NBTTagCompound) cachedP8Tag.copy());
        } else if (p8PendingNbt != null) {
            nbt.setTag("P8", (NBTTagCompound) p8PendingNbt.copy());
        }
        if (fluids != null) {
            if (cachedFluidsTag == null) cachedFluidsTag = FluidWorldStateNbtCodec.write(fluids);
            nbt.setTag("P9Fluids", (NBTTagCompound) cachedFluidsTag.copy());
        } else if (fluidPendingNbt != null) {
            nbt.setTag("P9Fluids", (NBTTagCompound) fluidPendingNbt.copy());
        }
        if (electrical != null) {
            if (cachedElectricalTag == null) cachedElectricalTag = ElectricalWorldStateNbtCodec.write(electrical);
            nbt.setTag("P9Electricity", (NBTTagCompound) cachedElectricalTag.copy());
        } else if (electricalPendingNbt != null) {
            nbt.setTag("P9Electricity", (NBTTagCompound) electricalPendingNbt.copy());
        }
        if (p11 != null) {
            if (cachedP11Tag == null) cachedP11Tag = StarRainHistoryNbtCodec.write(p11);
            nbt.setTag("P11StarRain", (NBTTagCompound) cachedP11Tag.copy());
        } else if (p11PendingNbt != null) {
            nbt.setTag("P11StarRain", (NBTTagCompound) p11PendingNbt.copy());
        }
        // Legacy world-global material names are intentionally not written: their author is unknown.
    }
}
