package naturalexceptions.core.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import naturalexceptions.core.id.ResourceId;

/**
 * Thermodynamic and physical profile of a pure constituent element or baseline compound.
 *
 * <p>
 * Internal units:
 * <ul>
 * <li>Density: g/cm³ (kg/m³ = g/cm³ * 1000)</li>
 * <li>Specific heat solid/liquid: J / (kg · K)</li>
 * <li>Thermal conductivity: W / (m · K)</li>
 * <li>Melting point: Kelvin (K)</li>
 * <li>Latent heat of fusion: J / kg</li>
 * <li>Mechanical hardness: dimensionless (Mohs scale baseline)</li>
 * <li>Electrical resistivity: Ω · m</li>
 * <li>Biological traits: discrete categorical trait tags</li>
 * </ul>
 */
public final class ConstituentData {

    private final ResourceId id;
    private final double density;
    private final double hardness;
    private final double conductivity; // Legacy P2 compatibility scale
    private final double thermalConductivity; // W / (m · K)
    private final double specificHeatSolid; // J / (kg · K)
    private final double specificHeatLiquid; // J / (kg · K)
    private final double meltingPointKelvin; // K
    private final double latentHeatOfFusion; // J / kg
    private final double electricalResistivity; // Ω · m
    private final Set<String> biologicalTraits;

    public ConstituentData(ResourceId id, double density, double hardness, double conductivity,
        double thermalConductivity, double specificHeatSolid, double specificHeatLiquid, double meltingPointKelvin,
        double latentHeatOfFusion, double electricalResistivity, Set<String> biologicalTraits) {
        this.id = Objects.requireNonNull(id, "id");
        this.density = density;
        this.hardness = hardness;
        this.conductivity = conductivity;
        this.thermalConductivity = thermalConductivity;
        this.specificHeatSolid = specificHeatSolid;
        this.specificHeatLiquid = specificHeatLiquid;
        this.meltingPointKelvin = meltingPointKelvin;
        this.latentHeatOfFusion = latentHeatOfFusion;
        this.electricalResistivity = electricalResistivity;
        this.biologicalTraits = Collections.unmodifiableSet(new LinkedHashSet<>(biologicalTraits));
    }

    public ResourceId id() {
        return id;
    }

    public double density() {
        return density;
    }

    public double hardness() {
        return hardness;
    }

    public double conductivity() {
        return conductivity;
    }

    public double thermalConductivity() {
        return thermalConductivity;
    }

    public double specificHeatSolid() {
        return specificHeatSolid;
    }

    public double specificHeatLiquid() {
        return specificHeatLiquid;
    }

    public double meltingPointKelvin() {
        return meltingPointKelvin;
    }

    public double latentHeatOfFusion() {
        return latentHeatOfFusion;
    }

    public double electricalResistivity() {
        return electricalResistivity;
    }

    public Set<String> biologicalTraits() {
        return biologicalTraits;
    }

    // Static registry of known constituent profiles
    private static final Map<ResourceId, ConstituentData> KNOWN = new HashMap<>();

    public static final ResourceId ID_CU = ResourceId.of("naturalexceptions", "copper");
    public static final ResourceId ID_SN = ResourceId.of("naturalexceptions", "tin");
    public static final ResourceId ID_ZN = ResourceId.of("naturalexceptions", "zinc");
    public static final ResourceId ID_PB = ResourceId.of("naturalexceptions", "lead");
    public static final ResourceId ID_STELLAR = ResourceId.of("naturalexceptions", "stellar_matter");

    static {
        register(
            new ConstituentData(
                ID_CU,
                8.96, // g/cm³
                3.0, // Mohs hardness
                100.0, // baseline conductivity scale
                401.0, // W/(m·K)
                385.0, // J/(kg·K) solid
                410.0, // J/(kg·K) liquid
                1357.77, // K (1084.62 °C)
                205000.0, // J/kg
                1.68e-8, // Ω·m
                Collections.singleton("antimicrobial")));

        register(
            new ConstituentData(
                ID_SN,
                7.31,
                1.5,
                15.0,
                66.8,
                228.0,
                250.0,
                505.08, // K (231.93 °C)
                59600.0,
                1.15e-7,
                Collections.singleton("low_toxicity")));

        register(
            new ConstituentData(
                ID_ZN,
                7.14,
                2.5,
                28.0,
                116.0,
                388.0,
                415.0,
                692.68, // K (419.53 °C)
                112000.0,
                5.9e-8,
                Collections.singleton("galvanic_protection")));

        register(
            new ConstituentData(
                ID_PB,
                11.34,
                1.5,
                8.0,
                35.3,
                129.0,
                140.0,
                600.61, // K (327.46 °C)
                23000.0,
                2.08e-7,
                Collections.singleton("radiation_shield")));

        register(
            new ConstituentData(
                ID_STELLAR,
                15.0,
                10.0,
                200.0,
                2000.0,
                1500.0,
                1600.0,
                3500.0,
                800000.0,
                1.0e-10,
                Collections.singleton("stellar_radiant")));
    }

    private static void register(ConstituentData data) {
        KNOWN.put(data.id(), data);
    }

    /**
     * Looks up constituent data or returns a physically sensible universal default for unknown elements.
     */
    public static ConstituentData lookup(ResourceId id) {
        ConstituentData known = KNOWN.get(id);
        if (known != null) {
            return known;
        }
        // Universal fallback: guaranteed positive, physically consistent baseline
        return new ConstituentData(
            id,
            5.0, // g/cm³ default metal density
            2.0, // default hardness
            10.0, // default conductivity scale
            20.0, // W/(m·K)
            450.0, // J/(kg·K) solid
            500.0, // J/(kg·K) liquid
            1000.0, // K default melting point
            100000.0, // J/kg default latent heat
            1.0e-6, // Ω·m default resistivity
            Collections.emptySet());
    }
}
