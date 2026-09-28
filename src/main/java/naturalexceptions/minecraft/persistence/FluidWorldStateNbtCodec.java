package naturalexceptions.minecraft.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.fluid.FluidSourceBoundary;
import naturalexceptions.core.fluid.FluidWorldState;
import naturalexceptions.core.material.MaterialRef;

/** Bounded persistence for explicit non-ambient spatial liquids and declared source boundaries. */
public final class FluidWorldStateNbtCodec {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_CELLS = FluidWorldState.MAX_CELLS;
    private FluidWorldStateNbtCodec() { }

    public static NBTTagCompound write(FluidWorldState state) {
        if (state == null) throw new IllegalArgumentException("Fluid world state required");
        if (state.snapshotCells().size() > MAX_CELLS) throw new IllegalStateException("Too many persisted P9 fluid cells");
        NBTTagCompound root = new NBTTagCompound(); root.setInteger("Version", CURRENT_VERSION);
        List<Map.Entry<FluidCellKey, FluidParcel>> cells = new ArrayList<>(state.snapshotCells().entrySet());
        cells.sort(Map.Entry.comparingByKey());
        NBTTagList encodedCells = new NBTTagList();
        for (Map.Entry<FluidCellKey, FluidParcel> entry : cells) {
            NBTTagCompound cell = new NBTTagCompound();
            writeKey(entry.getKey(), cell); cell.setTag("Parcel", FluidParcelNbtCodec.write(entry.getValue()));
            encodedCells.appendTag(cell);
        }
        root.setTag("Cells", encodedCells);
        NBTTagList sources = new NBTTagList();
        for (Map.Entry<FluidCellKey, FluidSourceBoundary> entry : state.sourceBoundariesSnapshot().entrySet()) {
            NBTTagCompound source = new NBTTagCompound(); writeKey(entry.getKey(), source);
            source.setString("Id", entry.getValue().stableId());
            source.setString("MaterialRef", entry.getValue().materialRef().asString());
            source.setBoolean("Infinite", entry.getValue().infinite()); sources.appendTag(source);
        }
        root.setTag("Sources", sources);
        return root;
    }

    public static FluidWorldState read(NBTTagCompound root) {
        FluidWorldState state = new FluidWorldState(MAX_CELLS * 2);
        if (root == null || root.getInteger("Version") != CURRENT_VERSION) return state;
        NBTTagList cells = root.getTagList("Cells", 10);
        for (int i = 0; i < cells.tagCount() && i < MAX_CELLS; i++) {
            NBTTagCompound row = cells.getCompoundTagAt(i);
            try {
                FluidCellKey key = readKey(row);
                FluidParcel parcel = FluidParcelNbtCodec.read(row.getCompoundTag("Parcel"));
                state.put(key, parcel);
            } catch (RuntimeException ignored) { }
        }
        NBTTagList sources = root.getTagList("Sources", 10);
        for (int i = 0; i < sources.tagCount() && i < MAX_CELLS; i++) {
            NBTTagCompound row = sources.getCompoundTagAt(i);
            try {
                state.registerSourceBoundary(readKey(row), new FluidSourceBoundary(row.getString("Id"),
                    MaterialRef.parse(row.getString("MaterialRef")), row.getBoolean("Infinite")));
            } catch (RuntimeException ignored) { }
        }
        return state;
    }

    private static void writeKey(FluidCellKey key, NBTTagCompound tag) {
        tag.setInteger("Dimension", key.dimension()); tag.setInteger("X", key.x());
        tag.setInteger("Y", key.y()); tag.setInteger("Z", key.z());
    }
    private static FluidCellKey readKey(NBTTagCompound tag) {
        if (!tag.hasKey("Dimension", 99) || !tag.hasKey("X", 99) || !tag.hasKey("Y", 99) || !tag.hasKey("Z", 99))
            throw new IllegalArgumentException("Incomplete fluid cell address");
        return new FluidCellKey(tag.getInteger("Dimension"), tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
    }
}
