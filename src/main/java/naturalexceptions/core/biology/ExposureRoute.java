package naturalexceptions.core.biology;

/** Directly modeled uptake paths; values are dose multipliers, not immunity flags. */
public enum ExposureRoute {
    INGESTION(1.0), INHALATION(0.7), CONTACT(0.15), WOUND(1.5);
    private final double effectiveness;
    ExposureRoute(double effectiveness) { this.effectiveness = effectiveness; }
    public double effectiveness() { return effectiveness; }
}
