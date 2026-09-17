package naturalexceptions.core.property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable numerical properties of a material.
 */
public final class MaterialProperties {

    public static final String KEY_DENSITY = "density";
    public static final String KEY_HARDNESS = "hardness";
    public static final String KEY_CONDUCTIVITY = "conductivity";

    private final Map<String, Double> values;

    public MaterialProperties(Map<String, Double> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    public static MaterialProperties of(double density, double hardness, double conductivity) {
        Map<String, Double> map = new HashMap<>();
        map.put(KEY_DENSITY, density);
        map.put(KEY_HARDNESS, hardness);
        map.put(KEY_CONDUCTIVITY, conductivity);
        return new MaterialProperties(map);
    }

    public double get(String key, double defaultValue) {
        Double val = values.get(key);
        return val != null ? val : defaultValue;
    }

    public double density() {
        return get(KEY_DENSITY, 0.0);
    }

    public double hardness() {
        return get(KEY_HARDNESS, 0.0);
    }

    public double conductivity() {
        return get(KEY_CONDUCTIVITY, 0.0);
    }

    public Map<String, Double> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialProperties)) return false;
        MaterialProperties that = (MaterialProperties) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "MaterialProperties" + values;
    }
}
