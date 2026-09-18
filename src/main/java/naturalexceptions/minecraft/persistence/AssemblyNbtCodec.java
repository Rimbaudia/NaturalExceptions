package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;

import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.AttachmentInterface;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/**
 * NBT serialization codec for physical tool assemblies.
 */
public final class AssemblyNbtCodec {

    public static final String KEY_HEAD = "Head";
    public static final String KEY_HANDLE = "Handle";
    public static final String KEY_ATTACHMENT = "Attachment";

    public static final String KEY_INSTANCE_ID = "Id";
    public static final String KEY_GEOMETRY = "Geometry";
    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_STATE_KEY = "StateKey";
    public static final String KEY_MASS_MG = "MassMg";
    public static final String KEY_MANUFACTURING = "Manufacturing";
    public static final String KEY_WEAR = "Wear";
    public static final String KEY_DEFORMATION = "Deformation";
    public static final String KEY_CRACKING = "Cracking";
    public static final String KEY_FRACTURED = "Fractured";

    private AssemblyNbtCodec() {}

    public static void writeToNbt(AssemblyGraph assembly, NBTTagCompound nbt) {
        if (assembly == null || nbt == null) return;

        NBTTagCompound headTag = new NBTTagCompound();
        writeComponentToNbt(assembly.head(), headTag);
        nbt.setTag(KEY_HEAD, headTag);

        NBTTagCompound handleTag = new NBTTagCompound();
        writeComponentToNbt(assembly.handle(), handleTag);
        nbt.setTag(KEY_HANDLE, handleTag);

        nbt.setString(
            KEY_ATTACHMENT,
            assembly.attachment()
                .name());
    }

    public static void writeComponentToNbt(ComponentInstance comp, NBTTagCompound nbt) {
        if (comp == null || nbt == null) return;

        nbt.setString(
            KEY_INSTANCE_ID,
            comp.instanceId()
                .hex());
        nbt.setString(
            KEY_GEOMETRY,
            comp.geometryId()
                .id());
        nbt.setString(
            KEY_MATERIAL_REF,
            comp.materialRef()
                .asString());
        nbt.setString(
            KEY_STATE_KEY,
            comp.stateKey()
                .canonicalString());
        nbt.setLong(
            KEY_MASS_MG,
            comp.mass()
                .milligrams());
        nbt.setString(
            KEY_MANUFACTURING,
            comp.manufacturingState()
                .id());

        ComponentDamage dmg = comp.damage();
        nbt.setDouble(KEY_WEAR, dmg.wear());
        nbt.setDouble(KEY_DEFORMATION, dmg.deformation());
        nbt.setDouble(KEY_CRACKING, dmg.cracking());
        nbt.setBoolean(KEY_FRACTURED, dmg.isFractured());
    }

    public static AssemblyGraph readFromNbt(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey(KEY_HEAD, 10) || !nbt.hasKey(KEY_HANDLE, 10)) {
            return null;
        }

        try {
            ComponentInstance head = readComponentFromNbt(nbt.getCompoundTag(KEY_HEAD));
            ComponentInstance handle = readComponentFromNbt(nbt.getCompoundTag(KEY_HANDLE));
            if (head == null || handle == null) {
                return null;
            }
            AttachmentInterface attachment = AttachmentInterface.valueOf(
                nbt.hasKey(KEY_ATTACHMENT, 8) ? nbt.getString(KEY_ATTACHMENT) : AttachmentInterface.SOCKET.name());

            return new AssemblyGraph(head, handle, attachment);
        } catch (Exception e) {
            return null;
        }
    }

    public static ComponentInstance readComponentFromNbt(NBTTagCompound nbt) {
        if (nbt == null) return null;
        if (!nbt.hasKey(KEY_INSTANCE_ID, 8) || !nbt.hasKey(KEY_GEOMETRY, 8)
            || !nbt.hasKey(KEY_MATERIAL_REF, 8)
            || !nbt.hasKey(KEY_STATE_KEY, 8)
            || !nbt.hasKey(KEY_MANUFACTURING, 8)) {
            return null;
        }

        try {
            DeterministicId id = DeterministicId.parse(nbt.getString(KEY_INSTANCE_ID));
            GeometryId geometry = GeometryId.parse(nbt.getString(KEY_GEOMETRY));
            MaterialRef ref = MaterialRef.parse(nbt.getString(KEY_MATERIAL_REF));
            CanonicalMaterialKey key = CanonicalMaterialKey.parse(nbt.getString(KEY_STATE_KEY));
            MaterialQuantity mass = MaterialQuantity.ofMilligrams(nbt.getLong(KEY_MASS_MG));
            ManufacturingState state = ManufacturingState.parse(nbt.getString(KEY_MANUFACTURING));

            double wear = nbt.getDouble(KEY_WEAR);
            double deformation = nbt.getDouble(KEY_DEFORMATION);
            double cracking = nbt.getDouble(KEY_CRACKING);
            boolean fractured = nbt.getBoolean(KEY_FRACTURED);
            ComponentDamage damage = new ComponentDamage(wear, deformation, cracking, fractured);

            return new ComponentInstance(id, geometry, ref, key, mass, state, damage);
        } catch (Exception e) {
            return null;
        }
    }
}
