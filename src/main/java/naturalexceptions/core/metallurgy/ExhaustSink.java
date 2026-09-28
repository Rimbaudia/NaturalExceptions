package naturalexceptions.core.metallurgy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import naturalexceptions.core.id.ResourceId;

/**
 * Declared exhaust sink accounting for gaseous reaction products that are not spatially simulated in P4.
 *
 * <p>
 * Guarantees that matter never silently vanishes: vented volatiles, SO₂, and CO₂ are explicitly tracked
 * by mass and constituent identity.
 */
public final class ExhaustSink {

    public static final ResourceId EXHAUST_GAS = ResourceId.of("naturalexceptions", "exhaust_gas");

    private MaterialQuantity totalVentedMass = MaterialQuantity.ZERO;
    private final Map<ResourceId, MaterialQuantity> ventedByConstituent = new HashMap<>();
    private double ventedEnergyJoules;

    public ExhaustSink() {}

    public MaterialQuantity totalVentedMass() {
        return totalVentedMass;
    }

    public Map<ResourceId, MaterialQuantity> ventedByConstituent() {
        return Collections.unmodifiableMap(ventedByConstituent);
    }

    public MaterialQuantity getVented(ResourceId constituent) {
        MaterialQuantity q = ventedByConstituent.get(constituent);
        return q != null ? q : MaterialQuantity.ZERO;
    }

    public void vent(ResourceId constituent, MaterialQuantity mass) {
        Objects.requireNonNull(constituent, "constituent");
        Objects.requireNonNull(mass, "mass");
        if (mass.isZero()) {
            return;
        }
        totalVentedMass = totalVentedMass.add(mass);
        MaterialQuantity current = ventedByConstituent.getOrDefault(constituent, MaterialQuantity.ZERO);
        ventedByConstituent.put(constituent, current.add(mass));
    }

    /** Thermal energy carried out of the vessel by vented matter (a declared sink, not a loss of accounting). */
    public double ventedEnergyJoules() {
        return ventedEnergyJoules;
    }

    public void ventEnergy(double joules) {
        if (!Double.isFinite(joules) || joules < 0.0) {
            throw new IllegalArgumentException("Vented energy must be finite and non-negative: " + joules);
        }
        ventedEnergyJoules += joules;
    }

    public void reset() {
        ventedEnergyJoules = 0.0;
        totalVentedMass = MaterialQuantity.ZERO;
        ventedByConstituent.clear();
    }
}
