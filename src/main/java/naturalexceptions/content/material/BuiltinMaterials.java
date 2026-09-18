package naturalexceptions.content.material;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialId;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Built-in authored materials for Natural Exceptions.
 */
public final class BuiltinMaterials {

    public static final ResourceId COPPER_RESOURCE_ID = ResourceId.of("naturalexceptions", "copper");
    public static final MaterialId COPPER_MATERIAL_ID = MaterialId.of("naturalexceptions", "copper");

    public static final CanonicalComposition COPPER_COMPOSITION = CanonicalComposition.of(COPPER_RESOURCE_ID);

    public static final MaterialProperties COPPER_PROPERTIES = MaterialProperties.builder()
        .density(8.96) // g/cm³ (8960 kg/m³)
        .hardness(3.0) // Mohs scale baseline
        .conductivity(100.0) // Legacy relative conductivity
        .thermalConductivity(401.0) // W / (m · K)
        .specificHeatSolid(385.0) // J / (kg · K)
        .specificHeatLiquid(410.0) // J / (kg · K)
        .meltingPointKelvin(1357.77) // K (1084.62 °C)
        .latentHeatOfFusion(205000.0) // J / kg
        .electricalResistivity(1.68e-8) // Ω · m
        .addTrait("antimicrobial")
        .addTrait("conductive")
        .build();

    public static final MaterialDefinition COPPER = new MaterialDefinition(
        COPPER_MATERIAL_ID,
        "material.naturalexceptions.copper",
        COPPER_COMPOSITION,
        COPPER_PROPERTIES);

    private BuiltinMaterials() {}
}
