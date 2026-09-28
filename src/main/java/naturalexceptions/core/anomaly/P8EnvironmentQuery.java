package naturalexceptions.core.anomaly;

import java.util.ArrayList;
import java.util.List;

import naturalexceptions.core.event.EventInfluence;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.worldgen.HydrologicalContext;
import naturalexceptions.core.worldgen.OverworldSample;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.material.MaterialPhase;

/** Shared environmental query composing P6 local truth with spatial and temporal P8 influences. */
public final class P8EnvironmentQuery {
    private static final ResourceId NIGHT_REACTIVITY = ResourceId.of("naturalexceptions", "night_reactivity");
    private P8EnvironmentQuery() { }

    public static ResolvedEnvironmentalContext at(OverworldSample local, int x, int z,
        SpatialAnomalyIndex anomalies, Iterable<EventInfluence> events) {
        double water;
        switch (local.hydrology()) {
            case WET_BASIN: water = 0.70; break;
            case MOIST_PLAIN: water = 0.42; break;
            case DRAINAGE_SLOPE: water = 0.24; break;
            default: water = 0.10;
        }
        double night = 0.08;
        List<EnvironmentalInfluence> influences = new ArrayList<>();
        if (anomalies != null) {
            for (SpatialAnomaly anomaly : anomalies.queryAt(x, z).anomalies()) influences.add(anomaly.influence());
        }
        if (events != null) {
            for (EventInfluence event : events) {
                if (NIGHT_REACTIVITY.equals(event.influence())) {
                    int strength = event.strength();
                    influences.add(EnvironmentalInfluence.of(event.eventId(), 0.0,
                        Math.max(-1.0, Math.min(1.0, strength / 100.0)), 0.0));
                }
            }
        }
        return EnvironmentalInfluenceResolver.resolve(water, night, local.moisture(), influences);
    }

    /** P8 water interaction applies only to an actual canonical liquid-water parcel. */
    public static double waterReactionIntensityFor(FluidParcel fluid, ResolvedEnvironmentalContext context) {
        if (!qualifiesAsLiquidWater(fluid) || context == null) return 0.0;
        return context.waterReactionIntensity();
    }

    public static boolean qualifiesAsLiquidWater(FluidParcel fluid) {
        return fluid != null && fluid.phase() == MaterialPhase.LIQUID
            && fluid.composition().contains(ResourceId.of("minecraft", "water"));
    }
}
