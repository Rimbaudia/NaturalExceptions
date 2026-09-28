package naturalexceptions.minecraft.item;

/** How hot something looks and feels, without an instrument. */
public final class HeatWords {

    private HeatWords() {}

    public static String describe(double kelvin) {
        if (kelvin >= 1500.0) return "white-hot";
        if (kelvin >= 1200.0) return "glowing orange";
        if (kelvin >= 900.0) return "glowing red";
        if (kelvin >= 700.0) return "faintly glowing";
        if (kelvin >= 373.15) return "too hot to touch";
        if (kelvin >= 318.15) return "warm";
        return "cool";
    }

    /** Colour of hot or cold metal as seen: molten glow, incandescent when very hot, else its own colour. */
    static int seenMetalColour(naturalexceptions.core.material.MaterialRef ref,
        naturalexceptions.core.material.CanonicalMaterialKey key,
        naturalexceptions.core.thermal.ActiveThermalState thermal) {
        double kelvin = thermal != null ? thermal.temperatureKelvin() : 293.15;
        if (kelvin >= 900.0) {
            return naturalexceptions.core.material.MaterialAppearance.getMoltenColor(ref, key, kelvin) & 0xFFFFFF;
        }
        return naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, key) & 0xFFFFFF;
    }
}
