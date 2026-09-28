package naturalexceptions.core.calendar;

import java.util.Objects;

import naturalexceptions.core.worldgen.OverworldSample;

/** Local environmental interpretation of calendar and strange-season conditions. */
public final class SeasonalContext {

    private final NaturalCalendar.Season ordinarySeason;
    private final double seasonProgress;
    private final StrangeSeason strangeSeason;
    private final double effectiveTemperature;
    private final double effectiveMoisture;
    private final double moldCoolingMultiplier;
    private final double waterAvailability;
    private final double nocturnalReactivity;

    private SeasonalContext(NaturalCalendar.Position position, StrangeSeason strangeSeason,
        double effectiveTemperature, double effectiveMoisture, double moldCoolingMultiplier,
        double waterAvailability, double nocturnalReactivity) {
        this.ordinarySeason = position.season();
        this.seasonProgress = position.seasonProgress();
        this.strangeSeason = strangeSeason;
        this.effectiveTemperature = effectiveTemperature;
        this.effectiveMoisture = effectiveMoisture;
        this.moldCoolingMultiplier = moldCoolingMultiplier;
        this.waterAvailability = waterAvailability;
        this.nocturnalReactivity = nocturnalReactivity;
    }

    /**
     * Adds bounded seasonal tendencies to local P6 environmental truth. A strange season overlays those
     * tendencies; neither can erase the local temperature/moisture ordering between locations.
     */
    public static SeasonalContext query(OverworldSample local, NaturalCalendar.Position position,
        StrangeSeason strangeSeason) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(position, "position");
        double baseWater;
        switch (local.hydrology()) {
            case WET_BASIN: baseWater = 0.70; break;
            case MOIST_PLAIN: baseWater = 0.42; break;
            case DRAINAGE_SLOPE: baseWater = 0.24; break;
            default: baseWater = 0.10;
        }
        return query(local, position, strangeSeason, local.moisture(), baseWater, 0.08);
    }

    /** Composes already-resolved independent spatial/temporal influences with the local seasonal interpretation. */
    public static SeasonalContext query(OverworldSample local, NaturalCalendar.Position position,
        StrangeSeason strangeSeason, double resolvedMoisture, double waterAvailability,
        double nocturnalReactivity) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(position, "position");
        requireUnit(resolvedMoisture, "resolvedMoisture");
        requireUnit(waterAvailability, "waterAvailability");
        requireUnit(nocturnalReactivity, "nocturnalReactivity");
        StrangeSeason strange = strangeSeason == null ? StrangeSeason.NONE : strangeSeason;

        double temperatureDelta;
        double moistureDelta;
        double moldCooling;
        switch (position.season()) {
            case SPRING:
                temperatureDelta = 0.04;
                moistureDelta = 0.06;
                moldCooling = 1.05;
                break;
            case SUMMER:
                temperatureDelta = 0.12;
                moistureDelta = -0.08;
                moldCooling = 0.90;
                break;
            case AUTUMN:
                temperatureDelta = -0.03;
                moistureDelta = 0.04;
                moldCooling = 0.98;
                break;
            case WINTER:
            default:
                temperatureDelta = -0.14;
                moistureDelta = -0.05;
                moldCooling = 1.15;
                break;
        }

        if (strange == StrangeSeason.ASHEN_AUTUMN) {
            temperatureDelta -= 0.05;
            moistureDelta -= 0.12;
            moldCooling += 0.12;
        }

        // Blend toward a bounded seasonal tendency instead of adding then clamping: this keeps
        // local ordering even at the hot/cold or wet/dry limits of P6's normalized fields.
        double seasonalTemperature = clamp(0.5 + temperatureDelta * 2.0);
        double seasonalMoisture = clamp(0.5 + moistureDelta * 2.0);
        return new SeasonalContext(position, strange,
            local.temperature() * 0.80 + seasonalTemperature * 0.20,
            resolvedMoisture * 0.80 + seasonalMoisture * 0.20,
            moldCooling, waterAvailability, nocturnalReactivity);
    }

    private static void requireUnit(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 1.0)
            throw new IllegalArgumentException(name + " must be between 0 and 1");
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public NaturalCalendar.Season ordinarySeason() { return ordinarySeason; }
    public double seasonProgress() { return seasonProgress; }
    public StrangeSeason strangeSeason() { return strangeSeason; }
    public double effectiveTemperature() { return effectiveTemperature; }
    public double effectiveMoisture() { return effectiveMoisture; }
    /** Environmental rule input for current mold cooling; does not modify the vessel's physical truth. */
    public double moldCoolingMultiplier() { return moldCoolingMultiplier; }
    public double waterAvailability() { return waterAvailability; }
    public double nocturnalReactivity() { return nocturnalReactivity; }
    public double waterReactionIntensity() { return waterAvailability * nocturnalReactivity; }
}
