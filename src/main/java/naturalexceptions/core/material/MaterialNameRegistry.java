package naturalexceptions.core.material;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import naturalexceptions.core.composition.CanonicalComposition;

/**
 * Server-authoritative and persistent registry of player-assigned custom alloy names.
 *
 * <p>
 * Renaming is strictly a presentation label; it does not alter composition,
 * density, melting point, hardness, conductivity, or any physical property.
 * Names are bound to the material identity (canonical composition or generated ref)
 * and survive world save/load and server restart.
 */
public final class MaterialNameRegistry {

    private static final MaterialNameRegistry GLOBAL = new MaterialNameRegistry();

    // Map from canonical composition string to player custom name
    private final Map<String, String> customNamesByComposition = new ConcurrentHashMap<>();
    // Map from material ref string to player custom name
    private final Map<String, String> customNamesByRef = new ConcurrentHashMap<>();

    public MaterialNameRegistry() {}

    public static MaterialNameRegistry global() {
        return GLOBAL;
    }

    public static void register(CanonicalComposition composition, String name) {
        GLOBAL.registerCustomName(composition, name);
    }

    public static void register(MaterialRef ref, String name) {
        GLOBAL.registerCustomName(ref, null, name);
    }

    public static String get(CanonicalComposition composition) {
        return GLOBAL.getCustomName(composition);
    }

    public static String get(MaterialRef ref) {
        return GLOBAL.getCustomName(ref, null);
    }

    public static void reset() {
        GLOBAL.clear();
    }

    /**
     * Registers a custom display name for a generated material identity.
     *
     * @param composition The canonical composition of the material.
     * @param name        The player-assigned display name (e.g. "Redsteel").
     */
    public void registerCustomName(CanonicalComposition composition, String name) {
        if (composition == null || name == null
            || name.trim()
                .isEmpty()) {
            return;
        }
        String trimmed = name.trim();
        customNamesByComposition.put(composition.canonicalString(), trimmed);
        MaterialRef genRef = MaterialRef.generatedFrom(composition);
        customNamesByRef.put(genRef.asString(), trimmed);
    }

    /**
     * Registers a custom display name for a material ref and optional state key.
     */
    public void registerCustomName(MaterialRef ref, CanonicalMaterialKey key, String name) {
        if (name == null || name.trim()
            .isEmpty()) {
            return;
        }
        String trimmed = name.trim();
        if (ref != null) {
            customNamesByRef.put(ref.asString(), trimmed);
        }
        if (key != null && key.composition() != null) {
            customNamesByComposition.put(
                key.composition()
                    .canonicalString(),
                trimmed);
            MaterialRef genRef = MaterialRef.generatedFrom(key.composition());
            customNamesByRef.put(genRef.asString(), trimmed);
        }
    }

    /**
     * Looks up a player-assigned custom name for a given material ref and state key.
     *
     * @return The custom name, or {@code null} if unnamed.
     */
    public String getCustomName(MaterialRef ref, CanonicalMaterialKey key) {
        if (key != null && key.composition() != null) {
            String name = customNamesByComposition.get(
                key.composition()
                    .canonicalString());
            if (name != null) {
                return name;
            }
        }
        if (ref != null) {
            String name = customNamesByRef.get(ref.asString());
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    /**
     * Looks up a custom name strictly by composition.
     */
    public String getCustomName(CanonicalComposition composition) {
        if (composition == null) return null;
        return customNamesByComposition.get(composition.canonicalString());
    }

    public Map<String, String> allCustomNames() {
        return Collections.unmodifiableMap(customNamesByComposition);
    }

    public void clear() {
        customNamesByComposition.clear();
        customNamesByRef.clear();
    }

    public void writeToNbt(NBTTagCompound nbt) {
        if (nbt == null) return;
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, String> entry : customNamesByComposition.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Composition", entry.getKey());
            tag.setString("Name", entry.getValue());
            list.appendTag(tag);
        }
        nbt.setTag("CustomMaterialNames", list);
    }

    public void readFromNbt(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("CustomMaterialNames", 9)) {
            return;
        }
        NBTTagList list = nbt.getTagList("CustomMaterialNames", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String compStr = tag.getString("Composition");
            String name = tag.getString("Name");
            if (!compStr.isEmpty() && !name.isEmpty()) {
                try {
                    CanonicalComposition comp = CanonicalComposition.parse(compStr);
                    registerCustomName(comp, name);
                } catch (Exception e) {
                    customNamesByComposition.put(compStr, name);
                }
            }
        }
    }
}
