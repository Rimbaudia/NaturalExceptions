package naturalexceptions.core.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import naturalexceptions.core.material.MaterialPhase;

/**
 * Immutable numerical and qualitative properties of a material.
 *
 * <p>
 * Exposes production property surfaces for:
 * <ul>
 * <li>Density (g/cm³ and kg/m³)</li>
 * <li>Mechanical hardness (Mohs scale baseline)</li>
 * <li>Thermal conductivity (W / (m · K))</li>
 * <li>Specific heat capacity in solid and liquid phases (J / (kg · K))</li>
 * <li>Melting point (Kelvin)</li>
 * <li>Latent heat of fusion (J / kg)</li>
 * <li>Electrical resistivity hook (Ω · m)</li>
 * <li>Biological trait hook (discrete categorical tags)</li>
 * </ul>
 */
public final class MaterialProperties {

    public static final String KEY_DENSITY = "density";
    public static final String KEY_HARDNESS = "hardness";
    public static final String KEY_CONDUCTIVITY = "conductivity";
    public static final String KEY_THERMAL_CONDUCTIVITY = "thermal_conductivity";
    public static final String KEY_SPECIFIC_HEAT_SOLID = "specific_heat_solid";
    public static final String KEY_SPECIFIC_HEAT_LIQUID = "specific_heat_liquid";
    public static final String KEY_MELTING_POINT = "melting_point";
    public static final String KEY_LATENT_HEAT_FUSION = "latent_heat_fusion";
    public static final String KEY_ELECTRICAL_RESISTIVITY = "electrical_resistivity";

    private final Map<String, Double> values;
    private final Set<String> biologicalTraits;

    public MaterialProperties(Map<String, Double> values) {
        this(values, Collections.emptySet());
    }

    public MaterialProperties(Map<String, Double> values, Set<String> biologicalTraits) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
        this.biologicalTraits = Collections
            .unmodifiableSet(biologicalTraits != null ? new LinkedHashSet<>(biologicalTraits) : Collections.emptySet());
    }

    public static MaterialProperties of(double density, double hardness, double conductivity) {
        Map<String, Double> map = new HashMap<>();
        map.put(KEY_DENSITY, density);
        map.put(KEY_HARDNESS, hardness);
        map.put(KEY_CONDUCTIVITY, conductivity);
        map.put(KEY_THERMAL_CONDUCTIVITY, conductivity * 4.01);
        map.put(KEY_SPECIFIC_HEAT_SOLID, 400.0);
        map.put(KEY_SPECIFIC_HEAT_LIQUID, 450.0);
        map.put(KEY_MELTING_POINT, 1000.0);
        map.put(KEY_LATENT_HEAT_FUSION, 100000.0);
        map.put(KEY_ELECTRICAL_RESISTIVITY, 1.0e-6);
        return new MaterialProperties(map);
    }

    public double get(String key, double defaultValue) {
        Double val = values.get(key);
        return val != null ? val : defaultValue;
    }

    public double density() {
        return get(KEY_DENSITY, 5.0);
    }

    public double densityKgPerM3() {
        return density() * 1000.0;
    }

    public double hardness() {
        return get(KEY_HARDNESS, 2.0);
    }

    public double hardness(MaterialPhase phase) {
        if (phase == MaterialPhase.LIQUID) {
            return 0.0;
        }
        return hardness();
    }

    public double conductivity() {
        return get(KEY_CONDUCTIVITY, 10.0);
    }

    public double thermalConductivity() {
        return get(KEY_THERMAL_CONDUCTIVITY, conductivity() * 4.01);
    }

    public double specificHeatSolid() {
        return get(KEY_SPECIFIC_HEAT_SOLID, 400.0);
    }

    public double specificHeatLiquid() {
        return get(KEY_SPECIFIC_HEAT_LIQUID, 450.0);
    }

    public double meltingPointKelvin() {
        return get(KEY_MELTING_POINT, 1000.0);
    }

    public double latentHeatOfFusion() {
        return get(KEY_LATENT_HEAT_FUSION, 100000.0);
    }

    public double electricalResistivity() {
        return get(KEY_ELECTRICAL_RESISTIVITY, 1.0e-6);
    }

    public Set<String> biologicalTraits() {
        return biologicalTraits;
    }

    public Map<String, Double> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialProperties)) return false;
        MaterialProperties that = (MaterialProperties) o;
        return values.equals(that.values) && biologicalTraits.equals(that.biologicalTraits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, biologicalTraits);
    }

    @Override
    public String toString() {
        return "MaterialProperties" + values + (biologicalTraits.isEmpty() ? "" : "[traits=" + biologicalTraits + "]");
    }

    public MaterialProperties forPhase(MaterialPhase phase) {
        if (phase == MaterialPhase.LIQUID) {
            if (hardness() == 0.0) {
                return this;
            }
            return toBuilder().hardness(0.0)
                .build();
        }
        return this;
    }

    public Builder toBuilder() {
        Builder b = builder();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            b.set(entry.getKey(), entry.getValue());
        }
        for (String trait : biologicalTraits) {
            b.addTrait(trait);
        }
        return b;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, Double> values = new HashMap<>();
        private final Set<String> traits = new LinkedHashSet<>();

        public Builder set(String key, double value) {
            values.put(key, value);
            return this;
        }

        public Builder density(double val) {
            return set(KEY_DENSITY, val);
        }

        public Builder hardness(double val) {
            return set(KEY_HARDNESS, val);
        }

        public Builder conductivity(double val) {
            return set(KEY_CONDUCTIVITY, val);
        }

        public Builder thermalConductivity(double val) {
            return set(KEY_THERMAL_CONDUCTIVITY, val);
        }

        public Builder specificHeatSolid(double val) {
            return set(KEY_SPECIFIC_HEAT_SOLID, val);
        }

        public Builder specificHeatLiquid(double val) {
            return set(KEY_SPECIFIC_HEAT_LIQUID, val);
        }

        public Builder meltingPointKelvin(double val) {
            return set(KEY_MELTING_POINT, val);
        }

        public Builder latentHeatOfFusion(double val) {
            return set(KEY_LATENT_HEAT_FUSION, val);
        }

        public Builder electricalResistivity(double val) {
            return set(KEY_ELECTRICAL_RESISTIVITY, val);
        }

        public Builder addTrait(String trait) {
            if (trait != null && !trait.isEmpty()) {
                traits.add(trait);
            }
            return this;
        }

        public MaterialProperties build() {
            return new MaterialProperties(values, traits);
        }
    }
}
