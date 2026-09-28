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
}
