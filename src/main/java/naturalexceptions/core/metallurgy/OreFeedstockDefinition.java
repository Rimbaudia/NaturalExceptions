package naturalexceptions.core.metallurgy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Physical definition mapping raw mined mineral feedstock (e.g. copper ore block) into constituent matter fractions.
 *
 * <p>
 * Standard copper ore block breakdown (1000 g total):
 * <ul>
 * <li>60% useful copper metal (600 g)</li>
 * <li>35% silicate gangue / molten slag (350 g)</li>
 * <li>5% volatile gases vented to declared exhaust sink (50 g)</li>
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
    private final naturalexceptions.core.composition.CanonicalComposition metallicProductComposition;
    private final MaterialProperties metallicProperties;

    private static final Map<ResourceId, OreFeedstockDefinition> REGISTERED = createRegistry();

    public OreFeedstockDefinition(ResourceId feedstockId, MaterialQuantity standardChunkMass,
        double usefulMetalFraction, double gangueSlagFraction, double volatileGasFraction,
        MaterialRef metallicProductRef, MaterialProperties metallicProperties) {
        this(
            feedstockId,
            standardChunkMass,
            usefulMetalFraction,
            gangueSlagFraction,
            volatileGasFraction,
            metallicProductRef,
            metallicProductRef.isAuthored()
                ? naturalexceptions.core.composition.CanonicalComposition.of(metallicProductRef.authoredId())
                : null,
            metallicProperties);
    }

    public OreFeedstockDefinition(ResourceId feedstockId, MaterialQuantity standardChunkMass,
        double usefulMetalFraction, double gangueSlagFraction, double volatileGasFraction,
        MaterialRef metallicProductRef,
        naturalexceptions.core.composition.CanonicalComposition metallicProductComposition,
        MaterialProperties metallicProperties) {
        this.feedstockId = Objects.requireNonNull(feedstockId, "feedstockId");
        this.standardChunkMass = Objects.requireNonNull(standardChunkMass, "standardChunkMass");
        this.usefulMetalFraction = usefulMetalFraction;
        this.gangueSlagFraction = gangueSlagFraction;
        this.volatileGasFraction = volatileGasFraction;
        this.metallicProductRef = Objects.requireNonNull(metallicProductRef, "metallicProductRef");
        this.metallicProductComposition = Objects
            .requireNonNull(metallicProductComposition, "metallicProductComposition");
        this.metallicProperties = Objects.requireNonNull(metallicProperties, "metallicProperties");
    }

    private static Map<ResourceId, OreFeedstockDefinition> createRegistry() {
        Map<ResourceId, OreFeedstockDefinition> definitions = new LinkedHashMap<>();
        registerInto(definitions, createCopperOre());
        registerInto(definitions, createTinOreChunk());
        registerInto(definitions, createLeadOreChunk());
        registerInto(definitions, createIronOreChunk());
        registerInto(definitions, createGoldOreChunk());
        return definitions;
    }

    private static void registerInto(Map<ResourceId, OreFeedstockDefinition> definitions,
        OreFeedstockDefinition definition) {
        definitions.put(definition.feedstockId(), definition);
    }

    /** Registers a feedstock so vessel reduction can discover it by canonical feedstock identity. */
    public static void register(OreFeedstockDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        REGISTERED.put(definition.feedstockId(), definition);
    }

    /** Returns the registered raw feedstock for a single-component layer identity, or {@code null}. */
    public static OreFeedstockDefinition forFeedstockId(ResourceId feedstockId) {
        if (feedstockId == null) {
            return null;
        }
        OreFeedstockDefinition def = REGISTERED.get(feedstockId);
        if (def == null && "copper_ore_chunk".equals(feedstockId.path())
            && "naturalexceptions".equals(feedstockId.namespace())) {
            return copperOre();
        }
        return def;
    }

    public static OreFeedstockDefinition copperOre() {
        return forFeedstockId(ResourceId.of("naturalexceptions", "copper_ore"));
    }

    @Deprecated
    public static OreFeedstockDefinition copperOreChunk() {
        return copperOre();
    }

    private static OreFeedstockDefinition createCopperOre() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "copper_ore"),
            MaterialQuantity.ofGrams(1000.0), // 1 kg standard ore block
            0.60, // 600g Cu
            0.35, // 350g slag
            0.05, // 50g gas
            MaterialRef.authored(BuiltinMaterials.COPPER_RESOURCE_ID),
            BuiltinMaterials.COPPER_PROPERTIES);
    }

    public static OreFeedstockDefinition tinOreChunk() {
        return forFeedstockId(ResourceId.of("naturalexceptions", "tin_ore_chunk"));
    }

    private static OreFeedstockDefinition createTinOreChunk() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "tin_ore_chunk"),
            MaterialQuantity.ofGrams(100.0),
            0.60,
            0.35,
            0.05,
            MaterialRef.authored(BuiltinMaterials.TIN_RESOURCE_ID),
            BuiltinMaterials.TIN_PROPERTIES);
    }

    public static OreFeedstockDefinition leadOreChunk() {
        return forFeedstockId(ResourceId.of("naturalexceptions", "lead_ore_chunk"));
    }

    private static OreFeedstockDefinition createLeadOreChunk() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "lead_ore_chunk"),
            MaterialQuantity.ofGrams(100.0),
            0.60,
            0.35,
            0.05,
            MaterialRef.authored(BuiltinMaterials.LEAD_RESOURCE_ID),
            BuiltinMaterials.LEAD_PROPERTIES);
    }

    public static OreFeedstockDefinition ironOreChunk() {
        return forFeedstockId(ResourceId.of("naturalexceptions", "iron_ore_chunk"));
    }

    private static OreFeedstockDefinition createIronOreChunk() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "iron_ore_chunk"),
            MaterialQuantity.ofGrams(1000.0), // 1 kg standard ore block
            0.60, // 600g Fe
            0.35, // 350g slag
            0.05, // 50g gas
            MaterialRef.authored(BuiltinMaterials.IRON_RESOURCE_ID),
            BuiltinMaterials.IRON_PROPERTIES);
    }

    public static OreFeedstockDefinition goldOreChunk() {
        return forFeedstockId(ResourceId.of("naturalexceptions", "gold_ore_chunk"));
    }

    private static OreFeedstockDefinition createGoldOreChunk() {
        return new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "gold_ore_chunk"),
            MaterialQuantity.ofGrams(1000.0), // 1 kg standard ore block
            0.70, // 700g Au
            0.28, // 280g slag
            0.02, // 20g gas
            MaterialRef.authored(BuiltinMaterials.GOLD_RESOURCE_ID),
            BuiltinMaterials.GOLD_PROPERTIES);
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

    public naturalexceptions.core.composition.CanonicalComposition metallicProductComposition() {
        return metallicProductComposition;
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
