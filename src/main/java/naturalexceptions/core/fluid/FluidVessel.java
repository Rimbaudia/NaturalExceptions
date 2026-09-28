package naturalexceptions.core.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import naturalexceptions.core.metallurgy.MaterialQuantity;

/** Small finite container that stores parcels and stratifies them only when contents change. */
public final class FluidVessel {
    private final long capacityMicrolitres;
    private final FluidInteractionRules rules;
    private final List<FluidParcel> contents = new ArrayList<>();
    private FluidParcel lastTransformation;
    private double lastTransformationEnergyDeltaJoules;

    public FluidVessel(long capacityMicrolitres, FluidInteractionRules rules) {
        if (capacityMicrolitres <= 0L) throw new IllegalArgumentException("Vessel capacity must be positive");
        this.capacityMicrolitres = capacityMicrolitres;
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public long capacityMicrolitres() { return capacityMicrolitres; }
    public List<FluidParcel> contents() { return Collections.unmodifiableList(new ArrayList<>(contents)); }
    public FluidParcel lastTransformation() { return lastTransformation; }
    public double lastTransformationEnergyDeltaJoules() { return lastTransformationEnergyDeltaJoules; }

    public long occupiedVolumeMicrolitres() {
        long volume = 0L;
        for (FluidParcel parcel : contents) volume = Math.addExact(volume, parcel.volumeMicrolitres());
        return volume;
    }

    public MaterialQuantity totalMass() {
        MaterialQuantity total = MaterialQuantity.ZERO;
        for (FluidParcel parcel : contents) total = total.add(parcel.mass());
        return total;
    }

    public double totalThermalEnergyJoules() {
        double total = 0.0;
        for (FluidParcel parcel : contents) total += parcel.thermalState().energyJoules();
        return total;
    }

    /** Accepts as much as capacity permits, mutating the incoming parcel only for the accepted amount. */
    public MaterialQuantity fill(FluidParcel incoming) {
        Objects.requireNonNull(incoming, "incoming");
        long free = capacityMicrolitres - occupiedVolumeMicrolitres();
        if (free <= 0L) return MaterialQuantity.ZERO;
        long incomingVolume = incoming.volumeMicrolitres();
        MaterialQuantity accepted = incoming.mass();
        if (incomingVolume > free) {
            long mg = Math.min(incoming.mass().milligrams(), Math.max(0L,
                (long) Math.floor((double) incoming.mass().milligrams() * free / incomingVolume)));
            if (mg <= 0L) return MaterialQuantity.ZERO;
            accepted = MaterialQuantity.ofMilligrams(mg);
        }
        FluidParcel transferred = incoming.previewPortion(accepted);
        FluidParcel target = compatibleTarget(transferred);
        if (target != null) {
            FluidInteractionRules.Result interaction = rules.classify(target, transferred);
            if (interaction == FluidInteractionRules.Result.MISCIBLE) {
                if (target.materialRef().equals(transferred.materialRef())
                    && target.composition().equals(transferred.composition())) {
                    target.mergeSameMaterial(transferred);
                } else {
                    contents.remove(target);
                    contents.add(FluidParcel.mix(target, transferred));
                }
            } else if (interaction == FluidInteractionRules.Result.TRANSFORM) {
                FluidTransformation transformed = rules.transform(target, transferred);
                if (transformed != null) {
                    long otherVolume = occupiedVolumeMicrolitres() - target.volumeMicrolitres();
                    long productVolume = 0L;
                    long productMass = 0L;
                    double productEnergyWeight = 0.0;
                    for (FluidParcel product : transformed.products()) {
                        productVolume = Math.addExact(productVolume, product.volumeMicrolitres());
                        productMass = Math.addExact(productMass, product.mass().milligrams());
                        productEnergyWeight += product.thermalState().energyJoules();
                    }
                    if (otherVolume + productVolume > capacityMicrolitres)
                        throw new IllegalStateException("Transformation products exceed vessel capacity");
                    long consumedMass = Math.addExact(target.mass().milligrams(), transferred.mass().milligrams());
                    if (productMass != consumedMass)
                        throw new IllegalArgumentException("Transformation must account for all consumed matter in products");
                    double inputEnergy = target.thermalState().energyJoules() + transferred.thermalState().energyJoules();
                    double outputEnergy = inputEnergy + transformed.thermalEnergyDeltaJoules();
                    if (!Double.isFinite(outputEnergy) || outputEnergy < 0.0)
                        throw new IllegalArgumentException("Transformation energy exchange is outside the physical range");
                    List<FluidParcel> ownedProducts = new ArrayList<>();
                    for (FluidParcel product : transformed.products()) ownedProducts.add(product.copyAndConsume());
                    FluidParcel first = ownedProducts.get(0);
                    double allocatedEnergy = 0.0;
                    for (int productIndex = 0; productIndex < ownedProducts.size(); productIndex++) {
                        FluidParcel product = ownedProducts.get(productIndex);
                        double share;
                        if (productIndex == ownedProducts.size() - 1) share = outputEnergy - allocatedEnergy;
                        else {
                            double weight = productEnergyWeight > 0.0
                                ? product.thermalState().energyJoules() / productEnergyWeight
                                : product.mass().milligrams() / (double) productMass;
                            share = outputEnergy * weight;
                            allocatedEnergy += share;
                        }
                        product.thermalState().setEnergyJoules(Math.max(0.0, share));
                    }
                    contents.remove(target);
                    contents.remove(transferred);
                    contents.addAll(ownedProducts);
                    lastTransformation = first;
                    lastTransformationEnergyDeltaJoules = transformed.thermalEnergyDeltaJoules();
                } else {
                    contents.add(transferred);
                }
            } else {
                contents.add(transferred);
            }
        } else {
            contents.add(transferred);
        }
        sortLayers();
        incoming.consume(accepted);
        return accepted;
    }

    /** Removes one layer from the bottom, preserving its identity and thermal energy. */
    public FluidParcel drain(MaterialQuantity requested) {
        Objects.requireNonNull(requested, "requested");
        if (requested.isZero() || contents.isEmpty()) return null;
        FluidParcel bottom = contents.get(0);
        if (requested.compareTo(bottom.mass()) >= 0) {
            contents.remove(0);
            sortLayers();
            return bottom;
        }
        FluidParcel result = bottom.extract(requested);
        sortLayers();
        return result;
    }

    private FluidParcel compatibleTarget(FluidParcel incoming) {
        for (FluidParcel existing : contents) {
            if (rules.classify(existing, incoming) != FluidInteractionRules.Result.IMMISCIBLE) return existing;
        }
        return null;
    }

    private void sortLayers() {
        contents.sort(Comparator.comparingDouble((FluidParcel p) -> p.thermalState().properties().density(p.phase()))
            .reversed().thenComparing(p -> p.materialRef().asString()));
    }
}
