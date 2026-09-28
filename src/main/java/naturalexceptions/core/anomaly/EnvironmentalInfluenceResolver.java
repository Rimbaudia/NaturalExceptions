package naturalexceptions.core.anomaly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Deterministically composes independent phenomenon contributions without pairwise rules. */
public final class EnvironmentalInfluenceResolver {
    private EnvironmentalInfluenceResolver() { }

    public static ResolvedEnvironmentalContext resolve(Iterable<EnvironmentalInfluence> influences) {
        return resolve(0.0, 0.0, 0.0, influences);
    }

    /** Combines modifiers with authoritative base context (such as P6 water/moisture tendency). */
    public static ResolvedEnvironmentalContext resolve(double baseWaterAvailability,
                                                        double baseNocturnalReactivity,
                                                        double baseMoisture,
                                                        Iterable<EnvironmentalInfluence> influences) {
        requireBaseline(baseWaterAvailability, "baseWaterAvailability");
        requireBaseline(baseNocturnalReactivity, "baseNocturnalReactivity");
        requireBaseline(baseMoisture, "baseMoisture");
        List<EnvironmentalInfluence> sorted = new ArrayList<>();
        if (influences != null) {
            for (EnvironmentalInfluence influence : influences) {
                if (influence != null) sorted.add(influence);
            }
        }
        Collections.sort(sorted, new Comparator<EnvironmentalInfluence>() {
            @Override public int compare(EnvironmentalInfluence a, EnvironmentalInfluence b) {
                int c = a.sourceId().compareTo(b.sourceId());
                if (c != 0) return c;
                c = Double.compare(a.waterAvailabilityDelta(), b.waterAvailabilityDelta());
                if (c != 0) return c;
                c = Double.compare(a.nocturnalReactivityDelta(), b.nocturnalReactivityDelta());
                return c != 0 ? c : Double.compare(a.moistureDelta(), b.moistureDelta());
            }
        });
        double water = baseWaterAvailability;
        double night = baseNocturnalReactivity;
        double moisture = baseMoisture;
        for (EnvironmentalInfluence influence : sorted) {
            water += influence.waterAvailabilityDelta();
            night += influence.nocturnalReactivityDelta();
            moisture += influence.moistureDelta();
        }
        return new ResolvedEnvironmentalContext(clamp01(water), clamp01(night), clamp01(moisture));
    }

    private static void requireBaseline(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
