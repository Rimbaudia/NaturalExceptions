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
        .add(
            BuiltinMaterials.TIN.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.LEAD.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.BRONZE.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.SLAG.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.IRON.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.GOLD.id()
                .resourceId(),
            "material",
            1L)
        .add(
            BuiltinMaterials.STELLAR_MATTER.id().resourceId(),
            "material",
            1L)
        .build();

    private static boolean registered;

    private BuiltinContent() {}

    public static void register() {
        if (registered) return;
        MATERIALS.register(BuiltinMaterials.COPPER);
        MATERIALS.register(BuiltinMaterials.WOOD);
        MATERIALS.register(BuiltinMaterials.TIN);
        MATERIALS.register(BuiltinMaterials.LEAD);
        MATERIALS.register(BuiltinMaterials.BRONZE);
        MATERIALS.register(BuiltinMaterials.SLAG);
        MATERIALS.register(BuiltinMaterials.IRON);
        MATERIALS.register(BuiltinMaterials.GOLD);
        MATERIALS.register(BuiltinMaterials.STELLAR_MATTER);
        registered = true;
    }

    public static MaterialCatalogue materials() {
        return MATERIALS;
    }

    public static ContentManifest manifest() {
        return MANIFEST;
    }
}
