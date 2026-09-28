package naturalexceptions.core.worldgen;

import java.util.Objects;

/** Immutable coarse truth at a horizontal location. Values are normalized to [0, 1]. */
public final class OverworldSample {

    private final double elevationTendency;
    private final double moisture;
    private final double temperature;
    private final double slope;
    private final GeologicalProvince geology;
    private final HydrologicalContext hydrology;
    private final SoilContext soil;
    private final double ecologySuitability;

    OverworldSample(double elevationTendency, double moisture, double temperature, double slope,
        GeologicalProvince geology, HydrologicalContext hydrology, SoilContext soil, double ecologySuitability) {
        this.elevationTendency = elevationTendency;
        this.moisture = moisture;
        this.temperature = temperature;
        this.slope = slope;
        this.geology = Objects.requireNonNull(geology, "geology");
        this.hydrology = Objects.requireNonNull(hydrology, "hydrology");
        this.soil = Objects.requireNonNull(soil, "soil");
        this.ecologySuitability = ecologySuitability;
    }

    public double elevationTendency() {
        return elevationTendency;
    }

    public double moisture() {
        return moisture;
    }

    public double temperature() {
        return temperature;
    }

    public double slope() {
        return slope;
    }

    public GeologicalProvince geology() {
        return geology;
    }

    public HydrologicalContext hydrology() {
        return hydrology;
    }

    public SoilContext soil() {
        return soil;
    }

    public double ecologySuitability() {
        return ecologySuitability;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OverworldSample)) return false;
        OverworldSample that = (OverworldSample) other;
        return Double.compare(elevationTendency, that.elevationTendency) == 0
            && Double.compare(moisture, that.moisture) == 0
            && Double.compare(temperature, that.temperature) == 0
            && Double.compare(slope, that.slope) == 0
            && geology == that.geology
            && hydrology == that.hydrology
            && soil == that.soil
            && Double.compare(ecologySuitability, that.ecologySuitability) == 0;
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(elevationTendency, moisture, temperature, slope, geology, hydrology, soil, ecologySuitability);
    }
}
