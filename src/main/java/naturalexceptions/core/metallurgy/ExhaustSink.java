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

    public void reset() {
        totalVentedMass = MaterialQuantity.ZERO;
        ventedByConstituent.clear();
    }
}
