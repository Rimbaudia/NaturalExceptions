package naturalexceptions.core.anomaly;

/** Combined local environmental modifiers used by independent phenomena and world queries. */
public final class ResolvedEnvironmentalContext {
    private final double waterAvailability;
    private final double nocturnalReactivity;
    private final double moisture;

    ResolvedEnvironmentalContext(double waterAvailability, double nocturnalReactivity, double moisture) {
        this.waterAvailability = waterAvailability;
        this.nocturnalReactivity = nocturnalReactivity;
        this.moisture = moisture;
    }

    public double waterAvailability() { return waterAvailability; }
    public double nocturnalReactivity() { return nocturnalReactivity; }
    public double moisture() { return moisture; }

    /** Shared water response: water by itself or night pressure by itself is insufficient. */
    public double waterReactionIntensity() { return waterAvailability * nocturnalReactivity; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ResolvedEnvironmentalContext)) return false;
        ResolvedEnvironmentalContext that = (ResolvedEnvironmentalContext) other;
        return Double.compare(waterAvailability, that.waterAvailability) == 0
            && Double.compare(nocturnalReactivity, that.nocturnalReactivity) == 0
            && Double.compare(moisture, that.moisture) == 0;
    }

    @Override
    public int hashCode() {
        long a = Double.doubleToLongBits(waterAvailability);
        long b = Double.doubleToLongBits(nocturnalReactivity);
        long c = Double.doubleToLongBits(moisture);
        return (int) (a ^ (a >>> 32) ^ b ^ (b >>> 32) ^ c ^ (c >>> 32));
    }
}
