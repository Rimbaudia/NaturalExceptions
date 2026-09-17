package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;

/**
 * Codec for translating MaterialRef, CanonicalMaterialKey, and CanonicalComposition to/from ItemStack NBT.
 *
 * <p>
 * Implements strict format versioning and consistency validation:
 * <ul>
 * <li>Format version must be positive and not exceed CURRENT_FORMAT.</li>
 * <li>Generated materials must include reconstruction information (StateKey or Composition).</li>
 * <li>Reconstruction payload is checked against the declared MaterialRef; mismatched hashes are rejected.</li>
 * <li>Corrupt records throw {@link PersistenceValidationException} to fail safely and visibly.</li>
 * </ul>
 */
public final class MaterialCarrierNbtCodec {

    public static final String KEY_FORMAT = "Format";
    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_STATE_KEY = "StateKey";
    public static final String KEY_COMPOSITION = "Composition";

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
