package naturalexceptions.core.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Declared product parcels and thermal energy exchange for a simple material transformation. */
public final class FluidTransformation {
    private final List<FluidParcel> products;
    private final double thermalEnergyDeltaJoules;
    private FluidTransformation(List<FluidParcel> products, double thermalEnergyDeltaJoules) {
        if (products == null || products.isEmpty() || !Double.isFinite(thermalEnergyDeltaJoules))
            throw new IllegalArgumentException("Transformation requires products and finite energy delta");
        for (FluidParcel product : products) if (product == null) throw new IllegalArgumentException("Null product parcel");
        this.products = Collections.unmodifiableList(new ArrayList<>(products));
        this.thermalEnergyDeltaJoules = thermalEnergyDeltaJoules;
    }
    public static FluidTransformation of(List<FluidParcel> products, double thermalEnergyDeltaJoules) {
        return new FluidTransformation(products, thermalEnergyDeltaJoules);
    }
    public List<FluidParcel> products() { return products; }
    /** Positive energy is released into products; negative energy is absorbed from them. */
    public double thermalEnergyDeltaJoules() { return thermalEnergyDeltaJoules; }
}
