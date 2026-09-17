package naturalexceptions.content.material;

import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialId;

public final class BuiltinMaterials {

    public static final MaterialDefinition COPPER = new MaterialDefinition(
        MaterialId.of("naturalexceptions", "copper"),
        "material.naturalexceptions.copper");

    private BuiltinMaterials() {}
}
