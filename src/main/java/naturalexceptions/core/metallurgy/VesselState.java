package naturalexceptions.core.metallurgy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

/**
 * Pure domain model of a smelting vessel and its internal aggregate contents.
 *
 * <p>
 * Core physical capabilities:
 * <ul>
 * <li>Exact integer mass conservation across feed partitioning, molten mixing, and extraction.</li>
 * <li>Joule-authoritative thermal simulation driven by fuel combustion and ambient dissipation.</li>
 * <li>Unscripted molten mixing: arbitrary compatible liquid metals combine into mass-weighted canonical alloys.</li>
 * <li>Physical phase separation: dense molten metal sinks, lighter slag floats atop, solid charge sits above.</li>
 * <li>Physical lining degradation under over-temperature conditions.</li>
 * <li>Dormancy: completely unheated, equilibrated vessels sleep without per-tick workload.</li>
 * </ul>
 */
public final class VesselState {

    public static final MaterialQuantity DEFAULT_CAPACITY = MaterialQuantity.ofKilograms(50.0);

    private final MaterialQuantity capacity;
    private final VesselLining lining;
    private final FuelBurnState fuelState;
    private final ExhaustSink exhaustSink;
    private final List<AggregateLayer> layers = new ArrayList<>();
    private final MaterialPropertyResolver propertyResolver;

    public VesselState() {
        this(
            DEFAULT_CAPACITY,
            new VesselLining(),
            new FuelBurnState(),
            new ExhaustSink(),
            new MaterialPropertyResolver());
    }

    public VesselState(MaterialQuantity capacity, VesselLining lining, FuelBurnState fuelState, ExhaustSink exhaustSink,
        MaterialPropertyResolver propertyResolver) {
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.lining = Objects.requireNonNull(lining, "lining");
        this.fuelState = Objects.requireNonNull(fuelState, "fuelState");
        this.exhaustSink = Objects.requireNonNull(exhaustSink, "exhaustSink");
        this.propertyResolver = Objects.requireNonNull(propertyResolver, "propertyResolver");
    }

    public MaterialQuantity capacity() {
        return capacity;
    }

    public VesselLining lining() {
        return lining;
    }

    public FuelBurnState fuelState() {
        return fuelState;
    }

    public ExhaustSink exhaustSink() {
        return exhaustSink;
    }

    public List<AggregateLayer> layers() {
        return Collections.unmodifiableList(layers);
    }

    public List<AggregateLayer> slagLayers() {
        List<AggregateLayer> slag = new ArrayList<>();
        for (AggregateLayer l : layers) {
            if (l.phaseType() == AggregatePhaseType.SLAG) {
                slag.add(l);
            }
        }
        return Collections.unmodifiableList(slag);
    }

    public MaterialPropertyResolver propertyResolver() {
        return propertyResolver;
    }

    public MaterialQuantity totalMass() {
        MaterialQuantity sum = MaterialQuantity.ZERO;
        for (AggregateLayer layer : layers) {
            sum = sum.add(layer.mass());
        }
        return sum;
    }

    public void addFuel(int burnTicks) {
        fuelState.addFuel(burnTicks);
    }

    public void addLayer(AggregateLayer layer) {
        Objects.requireNonNull(layer, "layer");
        MaterialQuantity newTotal = totalMass().add(layer.mass());
        if (newTotal.compareTo(capacity) > 0) {
            throw new IllegalStateException("Vessel capacity exceeded: " + newTotal + " > " + capacity);
        }
        layers.add(layer);
        sortLayers();
    }

    public void addOreChunk(OreFeedstockDefinition oreDef) {
        Objects.requireNonNull(oreDef, "oreDef");
        MaterialQuantity mass = oreDef.standardChunkMass();
        ActiveThermalState ambientThermal = ActiveThermalState.ambient(mass.toKilograms(), oreDef.metallicProperties());
        CanonicalComposition comp = CanonicalComposition.of(oreDef.feedstockId());
        CanonicalMaterialKey key = CanonicalMaterialKey.of(comp);

        AggregateLayer solidLayer = new AggregateLayer(
            oreDef.metallicProductRef(),
            key,
            comp,
            mass,
            ambientThermal,
            AggregatePhaseType.SOLID_CHARGE);
        addLayer(solidLayer);
    }

    public void addComponentCharge(ComponentInstance component, MaterialProperties properties) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(properties, "properties");
        ActiveThermalState ambientThermal = ActiveThermalState.ambient(
            component.mass()
                .toKilograms(),
            properties);
        CanonicalComposition comp = component.stateKey()
            .composition();
        CanonicalMaterialKey key = component.stateKey();

        AggregateLayer solidLayer = new AggregateLayer(
            component.materialRef(),
            key,
            comp,
            component.mass(),
            ambientThermal,
            AggregatePhaseType.SOLID_CHARGE);
        addLayer(solidLayer);
    }

    /**
     * Steps the vessel simulation by deltaTicks.
     */
    public void step(int deltaTicks, double ambientTempK) {
        if (deltaTicks <= 0) return;

        double deltaSeconds = (double) deltaTicks / FuelBurnState.TICKS_PER_SECOND;

        // 1. Combustion energy release
        double combustionJoules = fuelState.step(deltaTicks);
        if (combustionJoules > 0.0 && !layers.isEmpty()) {
            // Distribute energy across existing layers proportionally to mass
            long totalMg = totalMass().milligrams();
            if (totalMg > 0L) {
                for (AggregateLayer layer : layers) {
                    double fraction = (double) layer.mass()
                        .milligrams() / (double) totalMg;
                    layer.thermalState()
                        .addEnergyJoules(combustionJoules * fraction);
                }
            }
        }

        // 2. Heat exchange with ambient
        for (AggregateLayer layer : layers) {
            ActiveThermalState thermal = layer.thermalState();
            double temp = thermal.temperatureKelvin();
            if (temp > ambientTempK) {
                // Cooling towards ambient
                double heatTransferCoeff = 15.0; // W / (m² · K) natural convection proxy
                double surfaceArea = 0.10; // ~0.1 m² crucible surface area
                double qJoules = heatTransferCoeff * surfaceArea * (temp - ambientTempK) * deltaSeconds;
                thermal.removeEnergyJoules(qJoules);
            }
        }

        // 3. Metallurgical process rules & reduction
        processSolidCharges();

        // 4. Molten mixing of compatible metallic layers
        mixMoltenMetalLayers();

        // 5. Layer ordering & sorting
        sortLayers();

        // 6. Refractory lining thermal stress
        for (AggregateLayer layer : layers) {
            lining.applyThermalStress(layer.temperatureKelvin(), deltaSeconds);
        }
    }

    private void processSolidCharges() {
        List<AggregateLayer> newLayers = new ArrayList<>();
        List<AggregateLayer> toRemove = new ArrayList<>();

        for (AggregateLayer layer : layers) {
            if (layer.phaseType() == AggregatePhaseType.SOLID_CHARGE) {
                double temp = layer.temperatureKelvin();
                double meltPoint = layer.thermalState()
                    .properties()
                    .meltingPointKelvin();

                if (temp >= meltPoint || layer.thermalState()
                    .phase() == MaterialPhase.LIQUID) {
                    OreFeedstockDefinition oreDef = OreFeedstockDefinition.copperOreChunk();
                    boolean isRawOre = layer.composition()
                        .contains(oreDef.feedstockId());

                    if (isRawOre) {
                        MaterialQuantity[] portions = oreDef.partition(layer.mass());
                        MaterialQuantity metalMass = portions[0];
                        MaterialQuantity slagMass = portions[1];
                        MaterialQuantity gasMass = portions[2];

                        exhaustSink.vent(ExhaustSink.EXHAUST_GAS, gasMass);

                        ActiveThermalState metalThermal = ActiveThermalState
                            .atTemperature(temp, 1.0, metalMass.toKilograms(), oreDef.metallicProperties());
                        AggregateLayer metalLayer = new AggregateLayer(
                            oreDef.metallicProductRef(),
                            CanonicalMaterialKey.of(
                                CanonicalComposition.of(
                                    oreDef.metallicProductRef()
                                        .authoredId()))
                                .asMolten(),
                            CanonicalComposition.of(
                                oreDef.metallicProductRef()
                                    .authoredId()),
                            metalMass,
                            metalThermal,
                            AggregatePhaseType.MOLTEN_METAL);
                        newLayers.add(metalLayer);

                        ActiveThermalState slagThermal = ActiveThermalState
                            .atTemperature(temp, 1.0, slagMass.toKilograms(), OreFeedstockDefinition.SLAG_PROPERTIES);
                        AggregateLayer slagLayer = new AggregateLayer(
                            OreFeedstockDefinition.SLAG_REF,
                            CanonicalMaterialKey.of(CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID))
                                .asMolten(),
                            CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID),
                            slagMass,
                            slagThermal,
                            AggregatePhaseType.SLAG);
                        newLayers.add(slagLayer);
                    } else {
                        // Already refined metal or alloy: melts directly into MOLTEN_METAL
                        ActiveThermalState moltenThermal = ActiveThermalState.atTemperature(
                            temp,
                            1.0,
                            layer.mass()
                                .toKilograms(),
                            layer.thermalState()
                                .properties());
                        AggregateLayer moltenLayer = new AggregateLayer(
                            layer.materialRef(),
                            layer.stateKey()
                                .asMolten(),
                            layer.composition(),
                            layer.mass(),
                            moltenThermal,
                            AggregatePhaseType.MOLTEN_METAL);
                        newLayers.add(moltenLayer);
                    }
                    toRemove.add(layer);
                }
            }
        }

        layers.removeAll(toRemove);
        layers.addAll(newLayers);
    }

    private void mixMoltenMetalLayers() {
        List<AggregateLayer> moltenMetals = new ArrayList<>();
        for (AggregateLayer l : layers) {
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL && l.physicalPhase() == MaterialPhase.LIQUID) {
                moltenMetals.add(l);
            }
        }

        if (moltenMetals.size() < 2) {
            return;
        }

        // Combine all molten metals into one unified alloy layer
        layers.removeAll(moltenMetals);

        MaterialQuantity totalAlloyMass = MaterialQuantity.ZERO;
        double totalJoules = 0.0;
        for (AggregateLayer m : moltenMetals) {
            totalAlloyMass = totalAlloyMass.add(m.mass());
            totalJoules += m.thermalState()
                .energyJoules();
        }

        // Mass-weighted canonical composition
        CanonicalComposition.Builder compBuilder = CanonicalComposition.builder();
        long totalMg = totalAlloyMass.milligrams();

        // Accumulate constituent masses
        java.util.Map<ResourceId, Long> constituentMasses = new java.util.HashMap<>();
        for (AggregateLayer m : moltenMetals) {
            long layerMg = m.mass()
                .milligrams();
            CanonicalComposition c = m.composition();
            for (ResourceId res : c.constituents()) {
                double frac = c.fraction(res);
                long partMg = Math.round((double) layerMg * frac);
                constituentMasses.put(res, constituentMasses.getOrDefault(res, 0L) + partMg);
            }
        }

        for (java.util.Map.Entry<ResourceId, Long> entry : constituentMasses.entrySet()) {
            double frac = (double) entry.getValue() / (double) totalMg;
            int basisPoints = (int) Math.round(frac * 10000.0);
            if (basisPoints > 0) {
                compBuilder.add(entry.getKey(), basisPoints);
            }
        }

        CanonicalComposition mixedComp = compBuilder.build();
        CanonicalMaterialKey mixedKey = CanonicalMaterialKey.of(mixedComp)
            .asMolten();
        MaterialRef mixedRef = MaterialRef.generatedFrom(mixedKey);

        ResolvedProperties resolved = propertyResolver.resolve(mixedRef, mixedComp, MaterialPhase.LIQUID);
        ActiveThermalState mixedThermal = new ActiveThermalState(
            totalJoules,
            totalAlloyMass.toKilograms(),
            resolved.properties());

        AggregateLayer mergedLayer = new AggregateLayer(
            mixedRef,
            mixedKey,
            mixedComp,
            totalAlloyMass,
            mixedThermal,
            AggregatePhaseType.MOLTEN_METAL);
        layers.add(mergedLayer);
    }

    public void sortLayers() {
        // Deterministic layer ordering:
        // 1. MOLTEN_METAL (dense at the bottom, higher density first)
        // 2. SLAG (medium density silicate waste floating atop metal)
        // 3. SOLID_CHARGE (solid unmolten pieces resting atop or suspended)
        layers.sort(Comparator.comparingInt((AggregateLayer l) -> {
            switch (l.phaseType()) {
                case MOLTEN_METAL:
                    return 0;
                case SLAG:
                    return 1;
                case SOLID_CHARGE:
                    return 2;
                default:
                    return 3;
            }
        })
            .thenComparing(
                Comparator.comparingDouble(AggregateLayer::density)
                    .reversed()));
    }

    /**
     * Taps and extracts liquid metal from the bottom of the vessel.
     */
    public AggregateLayer pourLiquidMetal(MaterialQuantity desiredMass) {
        Objects.requireNonNull(desiredMass, "desiredMass");
        if (desiredMass.isZero()) {
            return null;
        }

        for (int i = 0; i < layers.size(); i++) {
            AggregateLayer l = layers.get(i);
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL && l.physicalPhase() == MaterialPhase.LIQUID) {
                MaterialQuantity extractAmount = l.mass()
                    .compareTo(desiredMass) <= 0 ? l.mass() : desiredMass;
                ActiveThermalState extractedThermal = l.extractMass(extractAmount);
                if (l.mass()
                    .isZero()) {
                    layers.remove(i);
                }
                return new AggregateLayer(
                    l.materialRef(),
                    l.stateKey(),
                    l.composition(),
                    extractAmount,
                    extractedThermal,
                    AggregatePhaseType.MOLTEN_METAL);
            }
        }
        return null;
    }

    /**
     * Skims and extracts slag from the top of the vessel.
     */
    public AggregateLayer skimSlag(MaterialQuantity desiredMass) {
        Objects.requireNonNull(desiredMass, "desiredMass");
        if (desiredMass.isZero()) {
            return null;
        }

        for (int i = layers.size() - 1; i >= 0; i--) {
            AggregateLayer l = layers.get(i);
            if (l.phaseType() == AggregatePhaseType.SLAG) {
                MaterialQuantity extractAmount = l.mass()
                    .compareTo(desiredMass) <= 0 ? l.mass() : desiredMass;
                ActiveThermalState extractedThermal = l.extractMass(extractAmount);
                if (l.mass()
                    .isZero()) {
                    layers.remove(i);
                }
                return new AggregateLayer(
                    l.materialRef(),
                    l.stateKey(),
                    l.composition(),
                    extractAmount,
                    extractedThermal,
                    AggregatePhaseType.SLAG);
            }
        }
        return null;
    }

    /**
     * Re-inserts extracted material into the vessel for remelting.
     */
    public void remelt(AggregateLayer sampleLayer) {
        Objects.requireNonNull(sampleLayer, "sampleLayer");
        // Convert to solid charge if not currently molten
        if (sampleLayer.physicalPhase() != MaterialPhase.LIQUID) {
            sampleLayer.setPhaseType(AggregatePhaseType.SOLID_CHARGE);
        }
        addLayer(sampleLayer);
    }

    public boolean isDormant(double ambientTempK) {
        if (fuelState.isBurning()) {
            return false;
        }
        for (AggregateLayer layer : layers) {
            if (!layer.thermalState()
                .isEquilibratedWith(ambientTempK, 2.0)) {
                return false;
            }
            if (layer.phaseType() == AggregatePhaseType.SOLID_CHARGE && layer.temperatureKelvin() >= 1350.0) {
                return false;
            }
        }
        return true;
    }
}
