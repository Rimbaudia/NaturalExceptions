package naturalexceptions.core.power;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.property.MaterialProperties;

/**
 * Copper conductors, a metre to a block: three gauges, each bare or wrapped in cloth. Their resistance and how fast
 * they heat come from copper itself; they cool to the air round them. Cloth chars past {@link #CLOTH_LIMIT_KELVIN}
 * (and leaves the bare conductor); bare copper melts at its melting point. Bare copper carrying more than
 * {@link #SHOCK_VOLTS} bites whoever touches it.
 */
public enum Cable {

    LIGHT_BARE("copperWire", Gauge.LIGHT, false),
    LIGHT_INSULATED("insulatedWire", Gauge.LIGHT, true),
    STANDARD_BARE("copperCable", Gauge.STANDARD, false),
    STANDARD_INSULATED("insulatedCable", Gauge.STANDARD, true),
    HEAVY_BARE("heavyCable", Gauge.HEAVY, false),
    HEAVY_INSULATED("insulatedHeavyCable", Gauge.HEAVY, true);

    /** Conductor cross-sections (square metres), and how fast each sheds heat to the air per metre (W / K). */
    public enum Gauge {

        LIGHT(1.0e-6, 0.035, 0.10),
        STANDARD(2.5e-6, 0.06, 0.14),
        HEAVY(16.0e-6, 0.15, 0.28);

        public final double crossSection;
        final double bareCooling, clothCooling;

        Gauge(double crossSection, double bareCooling, double clothCooling) {
            this.crossSection = crossSection;
            this.bareCooling = bareCooling;
            this.clothCooling = clothCooling;
        }
    }

    public static final double AMBIENT_KELVIN = 293.15;
    /** Cloth insulation chars past this. */
    public static final double CLOTH_LIMIT_KELVIN = 360.0;
    /** Cloth's own heat capacity per metre of cable (J / K). */
    static final double CLOTH_HEAT_PER_METRE = 30.0;
    /** Above this, bare copper bites. */
    public static final double SHOCK_VOLTS = 30.0;
    /** Bare copper glows from here (for the eye). */
    public static final double GLOW_KELVIN = 800.0;

    private static final MaterialProperties COPPER = BuiltinMaterials.COPPER_PROPERTIES;

    public final String unlocalizedName;
    public final Gauge gauge;
    public final boolean insulated;

    Cable(String unlocalizedName, Gauge gauge, boolean insulated) {
        this.unlocalizedName = unlocalizedName;
        this.gauge = gauge;
        this.insulated = insulated;
    }

    /** Resistance of a metre (one block) of it. */
    public double ohmsPerMetre() {
        return COPPER.electricalResistivity() / gauge.crossSection;
    }

    /** Heat to warm a metre of it by a kelvin (copper, and its cloth). */
    public double heatCapacityPerMetre() {
        double copperKg = COPPER.density() * 1000.0 * gauge.crossSection;
        return copperKg * COPPER.specificHeatSolid() + (insulated ? CLOTH_HEAT_PER_METRE : 0.0);
    }

    /** What a metre sheds to the air per kelvin above it. */
    public double coolingPerKelvin() {
        return insulated ? gauge.clothCooling : gauge.bareCooling;
    }

    /** The temperature it fails at: cloth chars, or copper melts. */
    public double limitKelvin() {
        return insulated ? CLOTH_LIMIT_KELVIN : COPPER.meltingPointKelvin();
    }

    /** What it becomes when it fails: cloth chars off to bare copper of the same gauge; bare copper is gone (null). */
    public Cable failed() {
        if (!insulated) return null;
        return gauge == Gauge.LIGHT ? LIGHT_BARE : gauge == Gauge.STANDARD ? STANDARD_BARE : HEAVY_BARE;
    }

    /** The steady current it can carry without failing, in amps (where heat in equals heat out at its limit). */
    public double ratedAmps() {
        double watts = coolingPerKelvin() * (limitKelvin() - AMBIENT_KELVIN);
        return Math.sqrt(watts / ohmsPerMetre());
    }

    /** Its temperature after {@code seconds} carrying {@code watts} of heat per metre. */
    public double step(double kelvin, double watts, double seconds) {
        double capacity = heatCapacityPerMetre();
        double k = coolingPerKelvin() / capacity;
        // Exact for constant power: it relaxes towards ambient + P / h with time constant C / h.
        double settled = AMBIENT_KELVIN + watts / coolingPerKelvin();
        return settled + (kelvin - settled) * Math.exp(-k * seconds);
    }

    public static Cable of(int meta) {
        Cable[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, meta))];
    }
}
