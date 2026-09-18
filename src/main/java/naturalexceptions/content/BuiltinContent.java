package naturalexceptions.content;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.content.ContentManifest;
import naturalexceptions.core.material.MaterialCatalogue;

public final class BuiltinContent {

    public static final String CONTENT_VERSION = "N0.01.0-dev";

    private static final MaterialCatalogue MATERIALS = new MaterialCatalogue();
    private static final ContentManifest MANIFEST = ContentManifest
        .builder(ContentManifest.CURRENT_FORMAT_VERSION, CONTENT_VERSION)
        .add(
            BuiltinMaterials.COPPER.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.WOOD.id()
                .resourceId(),
            "material",
            1L)
        .build();

    private static boolean registered;

    private BuiltinContent() {}

    public static void register() {
        if (registered) return;
        MATERIALS.register(BuiltinMaterials.COPPER);
        MATERIALS.register(BuiltinMaterials.WOOD);
        registered = true;
    }

    public static MaterialCatalogue materials() {
        return MATERIALS;
    }

    public static ContentManifest manifest() {
        return MANIFEST;
    }
}
