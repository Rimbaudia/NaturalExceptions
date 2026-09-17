package naturalexceptions.minecraft.registry;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.material.MaterialId;

public final class MaterialBindingRegistry {

    private static final MaterialBindingRegistry GLOBAL = new MaterialBindingRegistry();

    private final Map<Object, MaterialId> bindings = new IdentityHashMap<>();

    public static MaterialBindingRegistry global() {
        return GLOBAL;
    }

    public static MaterialId materialFor(Object minecraftObject) {
        return GLOBAL.get(minecraftObject);
    }

    public void bind(Object representation, MaterialId id) {
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(id, "id");
        if (bindings.putIfAbsent(representation, id) != null) {
            throw new IllegalStateException("Representation already bound: " + representation);
        }
    }

    public MaterialId require(Object representation) {
        MaterialId id = bindings.get(representation);
        if (id == null) {
            throw new IllegalArgumentException("No material bound for: " + representation);
        }
        return id;
    }

    public MaterialId get(Object representation) {
        return bindings.get(representation);
    }
}
