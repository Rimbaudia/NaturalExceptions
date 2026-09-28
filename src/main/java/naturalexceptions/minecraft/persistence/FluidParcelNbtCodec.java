package naturalexceptions.minecraft.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/** Versioned semantic fluid parcel codec; caches and region membership are not serialized. */
public final class FluidParcelNbtCodec {
    public static final int CURRENT_VERSION = 1;
    private FluidParcelNbtCodec() { }

    public static NBTTagCompound write(FluidParcel parcel) {
        if (parcel == null) throw new IllegalArgumentException("Fluid parcel required");
        parcel.reconcilePhase();
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", CURRENT_VERSION);
        root.setString("MaterialRef", parcel.materialRef().asString());
        root.setString("StateKey", parcel.stateKey().canonicalString());
        root.setString("Composition", parcel.composition().canonicalString());
        root.setLong("MassMg", parcel.mass().milligrams());
        root.setDouble("EnergyJ", parcel.thermalState().energyJoules());
        if (parcel.provenanceId() != null) root.setString("ProvenanceId", parcel.provenanceId().hex());
        NBTTagList properties = new NBTTagList();
        for (Map.Entry<String, Double> entry : parcel.thermalState().properties().asMap().entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Key", entry.getKey()); tag.setDouble("Value", entry.getValue()); properties.appendTag(tag);
        }
        root.setTag("Properties", properties);
        return root;
    }

    public static FluidParcel read(NBTTagCompound root) {
        if (root == null || root.getInteger("Version") != CURRENT_VERSION) {
            throw new PersistenceValidationException("Unsupported fluid parcel version");
        }
        try {
            MaterialRef ref = MaterialRef.parse(root.getString("MaterialRef"));
            CanonicalMaterialKey key = CanonicalMaterialKey.parse(root.getString("StateKey"));
            CanonicalComposition composition = CanonicalComposition.parse(root.getString("Composition"));
            if (!key.composition().equals(composition)) throw new IllegalArgumentException("State/composition mismatch");
            if (ref.isGenerated() && !ref.equals(MaterialRef.generatedFrom(key)))
                throw new IllegalArgumentException("Generated material reference mismatch");
            long massMg = root.getLong("MassMg");
            double energy = root.getDouble("EnergyJ");
            DeterministicId provenance = root.hasKey("ProvenanceId")
                ? DeterministicId.parse(root.getString("ProvenanceId")) : null;
            if (massMg <= 0L || !Double.isFinite(energy) || energy < 0.0)
                throw new IllegalArgumentException("Invalid fluid mass or energy");
            MaterialProperties props = readProperties(root.getTagList("Properties", 10));
            MaterialQuantity mass = MaterialQuantity.ofMilligrams(massMg);
            return new FluidParcel(ref, key, composition, mass,
                new ActiveThermalState(energy, mass.toKilograms(), props), provenance);
        } catch (RuntimeException error) {
            if (error instanceof PersistenceValidationException) throw error;
            throw new PersistenceValidationException("Malformed fluid parcel NBT", error);
        }
    }

    private static MaterialProperties readProperties(NBTTagList list) {
        if (list == null || list.tagCount() == 0 || list.tagCount() > 64)
            throw new IllegalArgumentException("Invalid fluid property count");
        Map<String, Double> values = new LinkedHashMap<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound item = list.getCompoundTagAt(i);
            String key = item.getString("Key");
            double value = item.getDouble("Value");
            if (key.isEmpty() || !Double.isFinite(value) || values.put(key, value) != null)
                throw new IllegalArgumentException("Invalid or duplicate fluid property");
        }
        double density = values.getOrDefault(MaterialProperties.KEY_DENSITY, 0.0);
        if (density <= 0.0) throw new IllegalArgumentException("Fluid density must be positive");
        return new MaterialProperties(values);
    }
}
