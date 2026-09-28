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

    public static final ResourceId WOOD_RESOURCE_ID = ResourceId.of("naturalexceptions", "wood");
    public static final MaterialId WOOD_MATERIAL_ID = MaterialId.of("naturalexceptions", "wood");

    public static final CanonicalComposition WOOD_COMPOSITION = CanonicalComposition.of(WOOD_RESOURCE_ID);

    public static final MaterialProperties WOOD_PROPERTIES = MaterialProperties.builder()
        .density(0.65) // g/cm³ (650 kg/m³)
        .hardness(1.2) // Mohs scale baseline
        .conductivity(0.1)
        .thermalConductivity(0.15) // W / (m · K)
        .specificHeatSolid(1700.0) // J / (kg · K)
        .specificHeatLiquid(1700.0)
        .meltingPointKelvin(600.0) // Pyrolysis / combustion threshold proxy
        .latentHeatOfFusion(0.0)
        .electricalResistivity(1.0e14) // Insulator
        .addTrait("organic")
        .addTrait("fibrous")
        .addTrait("shock_absorbing")
        .build();

    public static final MaterialDefinition WOOD = new MaterialDefinition(
        WOOD_MATERIAL_ID,
        "material.naturalexceptions.wood",
        WOOD_COMPOSITION,
        WOOD_PROPERTIES);

    public static final ResourceId TIN_RESOURCE_ID = ResourceId.of("naturalexceptions", "tin");
    public static final MaterialId TIN_MATERIAL_ID = MaterialId.of("naturalexceptions", "tin");
    public static final CanonicalComposition TIN_COMPOSITION = CanonicalComposition.of(TIN_RESOURCE_ID);
    public static final MaterialProperties TIN_PROPERTIES = MaterialProperties.builder()
        .density(7.31)
        .hardness(1.5)
        .conductivity(15.0)
        .thermalConductivity(66.8)
        .specificHeatSolid(228.0)
        .specificHeatLiquid(250.0)
        .meltingPointKelvin(505.08)
        .latentHeatOfFusion(59600.0)
        .electricalResistivity(1.15e-7)
        .addTrait("low_toxicity")
        .addTrait("soft_metal")
        .build();
    public static final MaterialDefinition TIN = new MaterialDefinition(
        TIN_MATERIAL_ID,
        "material.naturalexceptions.tin",
        TIN_COMPOSITION,
        TIN_PROPERTIES);

    public static final ResourceId LEAD_RESOURCE_ID = ResourceId.of("naturalexceptions", "lead");
    public static final MaterialId LEAD_MATERIAL_ID = MaterialId.of("naturalexceptions", "lead");
    public static final CanonicalComposition LEAD_COMPOSITION = CanonicalComposition.of(LEAD_RESOURCE_ID);
    public static final MaterialProperties LEAD_PROPERTIES = MaterialProperties.builder()
        .density(11.34)
        .hardness(1.5)
        .conductivity(8.0)
        .thermalConductivity(35.3)
        .specificHeatSolid(129.0)
        .specificHeatLiquid(140.0)
        .meltingPointKelvin(600.61)
        .latentHeatOfFusion(23000.0)
        .electricalResistivity(2.08e-7)
        .addTrait("radiation_shield")
        .addTrait("dense")
        .addTrait("toxic")
        .build();
    public static final MaterialDefinition LEAD = new MaterialDefinition(
        LEAD_MATERIAL_ID,
        "material.naturalexceptions.lead",
        LEAD_COMPOSITION,
        LEAD_PROPERTIES);

    public static final ResourceId BRONZE_RESOURCE_ID = ResourceId.of("naturalexceptions", "bronze");
    public static final MaterialId BRONZE_MATERIAL_ID = MaterialId.of("naturalexceptions", "bronze");
    public static final CanonicalComposition BRONZE_COMPOSITION = CanonicalComposition.builder()
        .add(COPPER_RESOURCE_ID, 0.88)
        .add(TIN_RESOURCE_ID, 0.12)
        .build();
    public static final MaterialProperties BRONZE_PROPERTIES = MaterialProperties.builder()
        .density(8.75)
        .hardness(4.2)
        .conductivity(40.0)
        .thermalConductivity(60.0)
        .specificHeatSolid(375.0)
        .specificHeatLiquid(420.0)
        .meltingPointKelvin(1223.15)
        .latentHeatOfFusion(190000.0)
        .electricalResistivity(5.0e-8)
        .addTrait("durable")
        .addTrait("corrosion_resistant")
        .build();
    public static final MaterialDefinition BRONZE = new MaterialDefinition(
        BRONZE_MATERIAL_ID,
        "material.naturalexceptions.bronze",
        BRONZE_COMPOSITION,
        BRONZE_PROPERTIES);

    public static final ResourceId SLAG_RESOURCE_ID = ResourceId.of("naturalexceptions", "slag");
    public static final MaterialId SLAG_MATERIAL_ID = MaterialId.of("naturalexceptions", "slag");
    public static final CanonicalComposition SLAG_COMPOSITION = CanonicalComposition.of(SLAG_RESOURCE_ID);
    public static final MaterialProperties SLAG_PROPERTIES = MaterialProperties.builder()
        .density(2.8)
        .hardness(5.5)
        .conductivity(1.5)
        .thermalConductivity(1.2)
        .specificHeatSolid(800.0)
        .specificHeatLiquid(1100.0)
        .meltingPointKelvin(1250.0)
        .latentHeatOfFusion(350000.0)
        .electricalResistivity(1.0e4)
        .addTrait("silicate_slag")
        .build();
    public static final MaterialDefinition SLAG = new MaterialDefinition(
        SLAG_MATERIAL_ID,
        "material.naturalexceptions.slag",
        SLAG_COMPOSITION,
        SLAG_PROPERTIES);

    public static final ResourceId IRON_RESOURCE_ID = ResourceId.of("naturalexceptions", "iron");
    public static final MaterialId IRON_MATERIAL_ID = MaterialId.of("naturalexceptions", "iron");
    public static final CanonicalComposition IRON_COMPOSITION = CanonicalComposition.of(IRON_RESOURCE_ID);
    public static final MaterialProperties IRON_PROPERTIES = MaterialProperties.builder()
        .density(7.87)
        .hardness(4.5)
        .conductivity(18.0)
        .thermalConductivity(80.4)
        .specificHeatSolid(449.0)
        .specificHeatLiquid(820.0)
        .meltingPointKelvin(1811.0)
        .latentHeatOfFusion(247000.0)
        .electricalResistivity(9.71e-8)
        .addTrait("ferromagnetic")
        .addTrait("structural")
        .build();
    public static final MaterialDefinition IRON = new MaterialDefinition(
        IRON_MATERIAL_ID,
        "material.naturalexceptions.iron",
        IRON_COMPOSITION,
        IRON_PROPERTIES);

    public static final ResourceId GOLD_RESOURCE_ID = ResourceId.of("naturalexceptions", "gold");
    public static final MaterialId GOLD_MATERIAL_ID = MaterialId.of("naturalexceptions", "gold");
    public static final CanonicalComposition GOLD_COMPOSITION = CanonicalComposition.of(GOLD_RESOURCE_ID);
    public static final MaterialProperties GOLD_PROPERTIES = MaterialProperties.builder()
        .density(19.30)
        .hardness(2.5)
        .conductivity(70.0)
        .thermalConductivity(318.0)
        .specificHeatSolid(129.0)
        .specificHeatLiquid(150.0)
        .meltingPointKelvin(1337.33)
        .latentHeatOfFusion(64000.0)
        .electricalResistivity(2.21e-8)
        .addTrait("malleable")
        .addTrait("noble_metal")
        .build();
    public static final MaterialDefinition GOLD = new MaterialDefinition(
        GOLD_MATERIAL_ID,
        "material.naturalexceptions.gold",
        GOLD_COMPOSITION,
        GOLD_PROPERTIES);

    public static final ResourceId STELLAR_MATTER_RESOURCE_ID = ResourceId.of("naturalexceptions", "stellar_matter");
    public static final MaterialId STELLAR_MATTER_MATERIAL_ID = MaterialId.of("naturalexceptions", "stellar_matter");
    public static final CanonicalComposition STELLAR_MATTER_COMPOSITION = CanonicalComposition
        .of(STELLAR_MATTER_RESOURCE_ID);
    public static final MaterialProperties STELLAR_MATTER_PROPERTIES = MaterialProperties.builder()
        .density(8.2)
        .hardness(5.5)
        .conductivity(40.0)
        .thermalConductivity(920.0)
        .specificHeatSolid(1500.0)
        .specificHeatLiquid(1600.0)
        .meltingPointKelvin(1300.0)
        .latentHeatOfFusion(800000.0)
        .electricalResistivity(2.0e-8)
        .addTrait("stellar_radiant")
        .addTrait("low_toxicity")
        .build();
    public static final MaterialDefinition STELLAR_MATTER = new MaterialDefinition(
        STELLAR_MATTER_MATERIAL_ID, "material.naturalexceptions.stellar_matter", STELLAR_MATTER_COMPOSITION,
        STELLAR_MATTER_PROPERTIES);

    /** Ground rock fused into glassy crust around a fallen star; ordinary terrestrial matter, basalt-like. */
    public static final ResourceId IMPACT_CRUST_RESOURCE_ID = ResourceId.of("naturalexceptions", "impact_crust");
    public static final MaterialId IMPACT_CRUST_MATERIAL_ID = MaterialId.of("naturalexceptions", "impact_crust");
    public static final CanonicalComposition IMPACT_CRUST_COMPOSITION = CanonicalComposition
        .of(IMPACT_CRUST_RESOURCE_ID);
    public static final MaterialProperties IMPACT_CRUST_PROPERTIES = MaterialProperties.builder()
        .density(2.6)
        .hardness(6.0)
        .thermalConductivity(1.8)
        .specificHeatSolid(840.0)
        .specificHeatLiquid(1100.0)
        .meltingPointKelvin(1450.0)
        .latentHeatOfFusion(420000.0)
        .electricalResistivity(1.0e6)
        .build();
    public static final MaterialDefinition IMPACT_CRUST = new MaterialDefinition(
        IMPACT_CRUST_MATERIAL_ID, "material.naturalexceptions.impact_crust", IMPACT_CRUST_COMPOSITION,
        IMPACT_CRUST_PROPERTIES);

    private BuiltinMaterials() {}
}
