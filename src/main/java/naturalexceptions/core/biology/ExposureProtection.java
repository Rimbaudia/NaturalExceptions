package naturalexceptions.core.biology;

import java.util.Objects;

/** Route-specific physical barrier efficiency in [0,1]. */
public final class ExposureProtection {
    private final ExposureRoute route;
    private final double attenuation;
    public ExposureProtection(ExposureRoute route, double attenuation) {
        this.route = Objects.requireNonNull(route, "route");
        if (!Double.isFinite(attenuation) || attenuation < 0 || attenuation > 1)
            throw new IllegalArgumentException("attenuation must be in [0,1]");
        this.attenuation = attenuation;
    }
    public ExposureRoute route() { return route; }
    public double attenuation() { return attenuation; }
}
