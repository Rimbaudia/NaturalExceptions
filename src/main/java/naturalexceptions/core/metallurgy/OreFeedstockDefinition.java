package naturalexceptions.core.metallurgy;

import java.util.Objects;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Physical definition mapping raw mined mineral feedstock (e.g. copper ore chunk) into constituent matter fractions.
 *
 * <p>
 * Standard copper ore chunk breakdown (100 g total):
 * <ul>
 * <li>60% useful copper metal (60 g)</li>
 * <li>35% silicate gangue / molten slag (35 g)</li>
 * <li>5% volatile gases vented to declared exhaust sink (5 g)</li>
 * </ul>
 */
public final class OreFeedstockDefinition {

    public static final ResourceId SLAG_RESOURCE_ID = ResourceId.of("naturalexceptions", "molten_slag");
    public static final MaterialRef SLAG_REF = MaterialRef.authored(SLAG_RESOURCE_ID);

    public static final MaterialProperties SLAG_PROPERTIES = MaterialProperties.builder()
        .density(2.8) // g/cm³ (floats on copper ~8.9 g/cm³)
        .hardness(5.5) // glassy silicate hardness when solid
        .conductivity(1.5)
        .thermalConductivity(1.2)
        .specificHeatSolid(800.0) // J/(kg·K)
        .specificHeatLiquid(1100.0)
        .meltingPointKelvin(1250.0) // melts below copper
        .latentHeatOfFusion(350000.0)
        .electricalResistivity(1.0e4)
        .addTrait("silicate_slag")
        .build();

    private final ResourceId feedstockId;
    private final MaterialQuantity standardChunkMass;
    private final double usefulMetalFraction;
    private final double gangueSlagFraction;
    private final double volatileGasFraction;
    private final MaterialRef metallicProductRef;
    private final MaterialProperties metallicProperties;

    public OreFeedstockDefinition(ResourceId feedstockId, MaterialQuantity standardChunkMass,
        double usefulMetalFraction, double gangueSlagFraction, double volatileGasFraction,
        MaterialRef metallicProductRef, MaterialProperties metallicProperties) {
        this.feedstockId = Objects.requireNonNull(feedstockId, "feedstockId");
        this.standardChunkMass = Objects.requireNonNull(standardChunkMass, "standardChunkMass");
        this.usefulMetalFraction = usefulMetalFraction;
        this.gangueSlagFraction = gangueSlagFraction;
        this.volatileGasFraction = volatileGasFraction;
        this.metallicProductRef = Objects.requireNonNull(metallicProductRef, "metallicProductRef");
        this.metallicProperties = Objects.requireNonNull(metallicProperties, "metallicProperties");
    }

    public static OreFeedstockDefinition copperOreChunk() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "copper_ore_chunk"),
            MaterialQuantity.ofGrams(100.0), // 100g per ore chunk
            0.60, // 60g Cu
            0.35, // 35g slag
            0.05, // 5g gas
            MaterialRef.authored(BuiltinMaterials.COPPER_RESOURCE_ID),
            BuiltinMaterials.COPPER_PROPERTIES);
    }

    public ResourceId feedstockId() {
        return feedstockId;
    }

    public MaterialQuantity standardChunkMass() {
        return standardChunkMass;
    }

    public double usefulMetalFraction() {
        return usefulMetalFraction;
    }

    public double gangueSlagFraction() {
        return gangueSlagFraction;
    }

    public double volatileGasFraction() {
        return volatileGasFraction;
    }

    public MaterialRef metallicProductRef() {
        return metallicProductRef;
    }

    public MaterialProperties metallicProperties() {
        return metallicProperties;
    }

    /**
     * Splits an input quantity of raw ore feedstock into exact mass portions: [metal, slag, gas].
     * The sum of portions strictly equals the input mass.
     */
    public MaterialQuantity[] partition(MaterialQuantity inputMass) {
        Objects.requireNonNull(inputMass, "inputMass");
        MaterialQuantity metal = inputMass.portion(usefulMetalFraction);
        MaterialQuantity slag = inputMass.portion(gangueSlagFraction);
        // Residue is placed into gas portion to ensure 100% exact mass conservation
        MaterialQuantity gas = inputMass.subtract(metal)
            .subtract(slag);
        return new MaterialQuantity[] { metal, slag, gas };
    }
}
