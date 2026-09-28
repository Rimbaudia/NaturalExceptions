package naturalexceptions.minecraft.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.electrical.CircuitNetwork;
import naturalexceptions.core.electrical.ElectricalWorldState;

/** Persists physical network elements; each network solver cache is rebuilt dirty after loading. */
public final class ElectricalWorldStateNbtCodec {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_NETWORKS = 4096;
    private ElectricalWorldStateNbtCodec() { }
    public static NBTTagCompound write(ElectricalWorldState state) {
        NBTTagCompound root = new NBTTagCompound(); root.setInteger("Version", CURRENT_VERSION);
        List<CircuitNetwork> networks = new ArrayList<>(state.networks());
        networks.sort(Comparator.comparing(CircuitNetwork::stableId));
        NBTTagList list = new NBTTagList();
        for (CircuitNetwork network : networks) list.appendTag(CircuitNetworkNbtCodec.write(network));
        root.setTag("Networks", list); return root;
    }
    public static ElectricalWorldState read(NBTTagCompound root) {
        ElectricalWorldState state = new ElectricalWorldState();
        if (root == null || root.getInteger("Version") != CURRENT_VERSION) return state;
        NBTTagList list = root.getTagList("Networks", 10);
        for (int i = 0; i < list.tagCount() && i < MAX_NETWORKS; i++) {
            try { state.register(CircuitNetworkNbtCodec.read(list.getCompoundTagAt(i))); }
            catch (RuntimeException ignored) { }
        }
        return state;
    }
}
