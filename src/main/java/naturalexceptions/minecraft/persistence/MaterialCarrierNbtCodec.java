package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.core.thermal.ThermodynamicModel;

/**
 * Codec for translating MaterialRef, CanonicalMaterialKey, CanonicalComposition, and active thermal state to/from
 * ItemStack NBT.
 *
 * <p>
 * Implements strict format versioning and consistency validation:
 * <ul>
 * <li>Format version must be positive and not exceed CURRENT_FORMAT.</li>
 * <li>Generated materials must include reconstruction information (StateKey or Composition).</li>
 * <li>Reconstruction payload is checked against the declared MaterialRef; mismatched hashes are rejected.</li>
 * <li>Thermal state is sparse: samples at ambient equilibrium omit the "Thermal" tag to sleep implicitly.</li>
 * <li>Corrupt records throw {@link PersistenceValidationException} to fail safely and visibly.</li>
 * </ul>
 */
public final class MaterialCarrierNbtCodec {

    public static final String KEY_FORMAT = "Format";
    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_STATE_KEY = "StateKey";
    public static final String KEY_COMPOSITION = "Composition";

    public static final String KEY_THERMAL = "Thermal";
    public static final String KEY_THERMAL_ENERGY = "Energy";
    public static final String KEY_THERMAL_MASS = "Mass";

    public static final int FORMAT_V1 = 1;
    public static final int CURRENT_FORMAT = 2;

    private MaterialCarrierNbtCodec() {}

    public static void writeToNbt(MaterialRef ref, CanonicalMaterialKey stateKey, NBTTagCompound nbt) {
        if (ref == null) {
            throw new IllegalArgumentException("MaterialRef cannot be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("NBTTagCompound cannot be null");
        }
        nbt.setInteger(KEY_FORMAT, CURRENT_FORMAT);
        nbt.setString(KEY_MATERIAL_REF, ref.asString());
        if (stateKey != null) {
            nbt.setString(KEY_STATE_KEY, stateKey.canonicalString());
            nbt.setString(
                KEY_COMPOSITION,
                stateKey.composition()
                    .canonicalString());
        }
    }

    public static void writeToNbt(MaterialRef ref, CanonicalComposition composition, NBTTagCompound nbt) {
        writeToNbt(ref, composition != null ? CanonicalMaterialKey.of(composition) : null, nbt);
    }

    public static void writeToNbt(MaterialRef ref, CanonicalMaterialKey stateKey, ActiveThermalState thermalState,
        NBTTagCompound nbt) {
        if (ref == null) {
            throw new IllegalArgumentException("MaterialRef cannot be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("NBTTagCompound cannot be null");
        }

        CanonicalMaterialKey effectiveKey = stateKey;
        MaterialRef effectiveRef = ref;

        if (thermalState != null && effectiveKey != null) {
            naturalexceptions.core.material.MaterialPhase activePhase = thermalState.phase();
            if (effectiveKey.phase() != activePhase) {
                effectiveKey = effectiveKey.withPhase(activePhase);
                if (effectiveRef.isGenerated()) {
                    effectiveRef = MaterialRef.generatedFrom(effectiveKey);
                }
            }
        }

        writeToNbt(effectiveRef, effectiveKey, nbt);
        if (thermalState != null) {
            writeThermalStateToNbt(thermalState, nbt);
        }
    }

    public static void writeThermalStateToNbt(ActiveThermalState thermalState, NBTTagCompound nbt) {
        if (nbt == null) {
            throw new IllegalArgumentException("NBTTagCompound cannot be null");
        }
        if (thermalState == null
            || thermalState.isEquilibratedWith(ThermodynamicModel.DEFAULT_AMBIENT_TEMPERATURE_K, 0.5)) {
            nbt.removeTag(KEY_THERMAL);
            return;
        }
        NBTTagCompound thermal = new NBTTagCompound();
        thermal.setDouble(KEY_THERMAL_ENERGY, thermalState.energyJoules());
        thermal.setDouble(KEY_THERMAL_MASS, thermalState.massKg());
        nbt.setTag(KEY_THERMAL, thermal);
    }

    public static boolean hasActiveThermalState(NBTTagCompound nbt) {
        return nbt != null && nbt.hasKey(KEY_THERMAL, 10);
    }

    public static ActiveThermalState readThermalStateFromNbt(NBTTagCompound nbt, MaterialProperties props) {
        if (nbt == null || !nbt.hasKey(KEY_THERMAL, 10)) {
            return null;
        }
        NBTTagCompound thermal = nbt.getCompoundTag(KEY_THERMAL);
        if (!thermal.hasKey(KEY_THERMAL_ENERGY, 99)) {
            throw new PersistenceValidationException("Missing thermal energy in carrier NBT");
        }
        double energy = thermal.getDouble(KEY_THERMAL_ENERGY);
        if (Double.isNaN(energy) || Double.isInfinite(energy) || energy < 0.0) {
            throw new PersistenceValidationException("Corrupted thermal energy in carrier NBT: " + energy);
        }
        double mass = thermal.hasKey(KEY_THERMAL_MASS, 99) ? thermal.getDouble(KEY_THERMAL_MASS) : 1.0;
        if (Double.isNaN(mass) || Double.isInfinite(mass) || mass <= 0.0) {
            throw new PersistenceValidationException("Corrupted thermal mass in carrier NBT: " + mass);
        }
        return new ActiveThermalState(energy, mass, props);
    }

    public static MaterialRef readRefFromNbt(NBTTagCompound nbt) {
        if (nbt == null) {
            throw new PersistenceValidationException("Carrier NBT cannot be null");
        }
        if (!nbt.hasKey(KEY_FORMAT, 99)) {
            throw new PersistenceValidationException("Missing carrier format version in NBT");
        }
        int format = nbt.getInteger(KEY_FORMAT);
        if (format <= 0 || format > CURRENT_FORMAT) {
            throw new PersistenceValidationException("Unsupported carrier format version: " + format);
        }
        if (!nbt.hasKey(KEY_MATERIAL_REF, 8)) {
            throw new PersistenceValidationException("Missing MaterialRef tag in carrier NBT");
        }
        String refStr = nbt.getString(KEY_MATERIAL_REF);
        MaterialRef ref;
        try {
            ref = MaterialRef.parse(refStr);
        } catch (IllegalArgumentException e) {
            throw new PersistenceValidationException("Malformed MaterialRef in carrier NBT: " + refStr, e);
        }

        // Validate generated material self-consistency
        if (ref.isGenerated()) {
            boolean hasStateKey = nbt.hasKey(KEY_STATE_KEY, 8);
            boolean hasComp = nbt.hasKey(KEY_COMPOSITION, 8);
            if (!hasStateKey && !hasComp) {
                throw new PersistenceValidationException(
                    "Generated MaterialRef has missing reconstruction payload in NBT: " + ref);
            }
            CanonicalMaterialKey key = readStateKeyFromNbt(nbt);
            if (key == null) {
                throw new PersistenceValidationException(
                    "Failed to decode reconstruction payload for generated MaterialRef: " + ref);
            }
            MaterialRef expectedRef = MaterialRef.generatedFrom(key);
            if (!ref.equals(expectedRef)) {
                throw new PersistenceValidationException(
                    "Reconstruction payload inconsistent with declared MaterialRef: declared=" + ref
                        + ", computed="
                        + expectedRef);
            }
        }

        return ref;
    }

    public static CanonicalMaterialKey readStateKeyFromNbt(NBTTagCompound nbt) {
        if (nbt == null) {
            return null;
        }
        if (nbt.hasKey(KEY_STATE_KEY, 8)) {
            String keyStr = nbt.getString(KEY_STATE_KEY);
            try {
                return CanonicalMaterialKey.parse(keyStr);
            } catch (IllegalArgumentException e) {
                throw new PersistenceValidationException("Malformed StateKey in carrier NBT: " + keyStr, e);
            }
        }
        if (nbt.hasKey(KEY_COMPOSITION, 8)) {
            String compStr = nbt.getString(KEY_COMPOSITION);
            try {
                CanonicalComposition comp = CanonicalComposition.parse(compStr);
                return CanonicalMaterialKey.of(comp);
            } catch (IllegalArgumentException e) {
                throw new PersistenceValidationException("Malformed Composition in carrier NBT: " + compStr, e);
            }
        }
        return null;
    }

    public static CanonicalComposition readCompositionFromNbt(NBTTagCompound nbt) {
        CanonicalMaterialKey stateKey = readStateKeyFromNbt(nbt);
        return stateKey != null ? stateKey.composition() : null;
    }
}
