package naturalexceptions.minecraft.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.electrical.CircuitNetwork;
import naturalexceptions.core.electrical.ResistiveConductor;
import naturalexceptions.core.electrical.ResistiveLoad;
import naturalexceptions.core.electrical.SwitchElement;
import naturalexceptions.core.electrical.VoltageSource;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/** Physical circuit persistence. Solved V/I/R/P values and caches are deliberately reconstructed after load. */
public final class CircuitNetworkNbtCodec {
    public static final int CURRENT_VERSION = 1;
    private CircuitNetworkNbtCodec() { }

    public static NBTTagCompound write(CircuitNetwork network) {
        if (network == null) throw new IllegalArgumentException("Circuit network required");
        NBTTagCompound root = new NBTTagCompound(); root.setInteger("Version", CURRENT_VERSION);
        root.setString("Id", network.stableId());
        root.setString("SourceId", network.source().stableId()); root.setDouble("Voltage", network.source().voltageVolts());
        root.setString("SwitchId", network.switchElement().stableId()); root.setBoolean("SwitchClosed", network.switchElement().closed());
        NBTTagCompound load = new NBTTagCompound(); load.setString("Id", network.load().stableId());
        load.setDouble("Resistance", network.load().resistanceOhms());
        load.setDouble("MaxTemp", network.load().maximumServiceTemperatureKelvin());
        writeProperties(network.load().thermalState().properties(), load);
        writeDamage(network.load().damage(), load); writeThermal(network.load().thermalState(), load);
        root.setTag("Load", load);
        NBTTagList conductors = new NBTTagList();
        for (ResistiveConductor conductor : network.conductors()) {
            NBTTagCompound item = new NBTTagCompound(); item.setString("Id", conductor.stableId());
            item.setString("MaterialRef", conductor.materialRef().asString());
            item.setDouble("LengthM", conductor.lengthMeters()); item.setDouble("AreaM2", conductor.crossSectionSquareMeters());
            item.setDouble("MaxTemp", conductor.maximumServiceTemperatureKelvin());
            writeProperties(conductor.materialProperties(), item); writeDamage(conductor.damage(), item);
            writeThermal(conductor.thermalState(), item); conductors.appendTag(item);
        }
        root.setTag("Conductors", conductors);
        return root;
    }

    public static CircuitNetwork read(NBTTagCompound root) {
        if (root == null || root.getInteger("Version") != CURRENT_VERSION)
            throw new PersistenceValidationException("Unsupported circuit network version");
        try {
            NBTTagCompound loadTag = root.getCompoundTag("Load");
            MaterialProperties loadProperties = readProperties(loadTag);
            ComponentDamage loadDamage = readDamage(loadTag);
            ActiveThermalState loadThermal = readThermal(loadTag, loadProperties);
            ResistiveLoad load = new ResistiveLoad(loadTag.getString("Id"), loadTag.getDouble("Resistance"),
                loadThermal, loadDamage, loadTag.getDouble("MaxTemp"));
            CircuitNetwork network = new CircuitNetwork(root.getString("Id"),
                new VoltageSource(root.getString("SourceId"), root.getDouble("Voltage")),
                new SwitchElement(root.getString("SwitchId"), root.getBoolean("SwitchClosed")), load);
            NBTTagList conductors = root.getTagList("Conductors", 10);
            if (conductors.tagCount() > 1024) throw new IllegalArgumentException("Too many circuit conductors");
            for (int i = 0; i < conductors.tagCount(); i++) {
                NBTTagCompound item = conductors.getCompoundTagAt(i);
                MaterialProperties properties = readProperties(item);
                ResistiveConductor conductor = new ResistiveConductor(item.getString("Id"),
                    MaterialRef.parse(item.getString("MaterialRef")), properties, item.getDouble("LengthM"),
                    item.getDouble("AreaM2"), readThermal(item, properties), readDamage(item), item.getDouble("MaxTemp"));
                network.addConductor(conductor);
            }
            return network;
        } catch (RuntimeException error) {
            if (error instanceof PersistenceValidationException) throw error;
            throw new PersistenceValidationException("Malformed circuit network NBT", error);
        }
    }

    private static void writeThermal(ActiveThermalState state, NBTTagCompound root) {
        root.setDouble("EnergyJ", state.energyJoules()); root.setDouble("MassKg", state.massKg());
    }
    private static ActiveThermalState readThermal(NBTTagCompound root, MaterialProperties properties) {
        double energy = root.getDouble("EnergyJ"), mass = root.getDouble("MassKg");
        if (!Double.isFinite(energy) || energy < 0 || !Double.isFinite(mass) || mass <= 0)
            throw new IllegalArgumentException("Invalid circuit thermal state");
        return new ActiveThermalState(energy, mass, properties);
    }
    private static void writeDamage(ComponentDamage damage, NBTTagCompound root) {
        root.setDouble("Wear", damage.wear()); root.setDouble("Deformation", damage.deformation());
        root.setDouble("Cracking", damage.cracking()); root.setBoolean("Fractured", damage.isFractured());
    }
    private static ComponentDamage readDamage(NBTTagCompound root) {
        double wear = root.getDouble("Wear"), deformation = root.getDouble("Deformation"), cracking = root.getDouble("Cracking");
        if (!Double.isFinite(wear) || !Double.isFinite(deformation) || !Double.isFinite(cracking))
            throw new IllegalArgumentException("Invalid circuit component damage");
        return new ComponentDamage(wear, deformation, cracking, root.getBoolean("Fractured"));
    }
    private static void writeProperties(MaterialProperties properties, NBTTagCompound root) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Double> entry : properties.asMap().entrySet()) {
            NBTTagCompound item = new NBTTagCompound(); item.setString("Key", entry.getKey());
            item.setDouble("Value", entry.getValue()); list.appendTag(item);
        }
        root.setTag("Properties", list);
    }
    private static MaterialProperties readProperties(NBTTagCompound root) {
        NBTTagList list = root.getTagList("Properties", 10);
        if (list.tagCount() == 0 || list.tagCount() > 64) throw new IllegalArgumentException("Invalid material property count");
        Map<String, Double> values = new LinkedHashMap<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound item = list.getCompoundTagAt(i); String key = item.getString("Key");
            double value = item.getDouble("Value");
            if (key.isEmpty() || !Double.isFinite(value) || values.put(key, value) != null)
                throw new IllegalArgumentException("Invalid material property");
        }
        return new MaterialProperties(values);
    }
}
