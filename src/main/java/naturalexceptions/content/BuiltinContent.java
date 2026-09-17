package naturalexceptions.content;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.material.MaterialCatalogue;

public final class BuiltinContent {

    private static final MaterialCatalogue MATERIALS = new MaterialCatalogue();
    private static boolean registered;

    private BuiltinContent() {}

    public static void register() {
        if (registered) return;
        MATERIALS.register(BuiltinMaterials.COPPER);
        registered = true;
    }

    public static MaterialCatalogue materials() {
        return MATERIALS;
    }
}
