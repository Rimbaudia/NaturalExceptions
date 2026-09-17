package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.MaterialRef;

/**
 * Codec for translating MaterialRef and CanonicalComposition to/from ItemStack NBT.
 */
public final class MaterialCarrierNbtCodec {

    public static final String KEY_FORMAT = "Format";
    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_COMPOSITION = "Composition";

    public static final int CURRENT_FORMAT = 1;

    private MaterialCarrierNbtCodec() {}

    public static void writeToNbt(MaterialRef ref, CanonicalComposition composition, NBTTagCompound nbt) {
        if (ref == null) {
            throw new IllegalArgumentException("MaterialRef cannot be null");
        }
        if (nbt == null) {
            throw new IllegalArgumentException("NBTTagCompound cannot be null");
        }
        nbt.setInteger(KEY_FORMAT, CURRENT_FORMAT);
        nbt.setString(KEY_MATERIAL_REF, ref.asString());
        if (composition != null) {
            nbt.setString(KEY_COMPOSITION, composition.canonicalString());
        }
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
        try {
            return MaterialRef.parse(refStr);
        } catch (IllegalArgumentException e) {
            throw new PersistenceValidationException("Malformed MaterialRef in carrier NBT: " + refStr, e);
        }
    }

    public static CanonicalComposition readCompositionFromNbt(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey(KEY_COMPOSITION, 8)) {
            return null;
        }
        String compStr = nbt.getString(KEY_COMPOSITION);
        try {
            return CanonicalComposition.parse(compStr);
        } catch (IllegalArgumentException e) {
            throw new PersistenceValidationException("Malformed Composition in carrier NBT: " + compStr, e);
        }
    }
}
