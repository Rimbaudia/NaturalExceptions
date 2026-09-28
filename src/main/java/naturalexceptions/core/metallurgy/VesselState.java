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

    public static final double FLAME_TEMPERATURE_K = 1950.0;
    public static final double COMBUSTION_CONDUCTANCE_W_PER_K = 90.0;
    public static final double STEFAN_BOLTZMANN_SIGMA = 5.670374419e-8;
    public static final double CRUCIBLE_EXPOSED_AREA_M2 = 0.020;
    public static final double EMISSIVITY = 0.80;
    public static final double CONVECTIVE_HEAT_COEFF = 12.0;
    public static final double INTERLAYER_CONDUCTANCE_W_PER_K = 100.0;

    private double surroundingsHeatJoules;

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

    /** Cumulative heat radiated/convected into the surroundings (transient accounting, not persisted). */
    public double surroundingsHeatJoules() {
        return surroundingsHeatJoules;
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

    /** Sum of resolved layer volumes in litres; independent of the legacy mass safety ceiling. */
    public double totalOccupiedVolumeLitres() {
        double litres = 0.0;
        for (AggregateLayer layer : layers) litres += layer.occupiedVolumeLitres();
        return Double.isFinite(litres) ? litres : 0.0;
    }

    public void addFuel(int burnTicks) {
        fuelState.addFuel(burnTicks);
    }

    public void addFuel(int burnTicks, double flameTempK, String label) {
        fuelState.addFuel(burnTicks, flameTempK, label);
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

    public void addRawOre(OreFeedstockDefinition oreDef) {
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

    @Deprecated
    public void addOreChunk(OreFeedstockDefinition oreDef) {
        addRawOre(oreDef);
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
     * Steps the vessel simulation by deltaTicks with natural draft airflow.
     */
    public void step(int deltaTicks, double ambientTempK) {
        step(deltaTicks, ambientTempK, 1.0);
    }

    /**
     * Steps the vessel simulation by deltaTicks with variable combustion air draft.
     *
     * @param airMultiplier draft airflow multiplier (1.0 = standard natural draft, >1.0 = tuyere / forced blast)
     */
    public void step(int deltaTicks, double ambientTempK, double airMultiplier) {
        if (deltaTicks <= 0) return;

        double deltaSeconds = (double) deltaTicks / FuelBurnState.TICKS_PER_SECOND;
        double effectiveDraft = Math.max(0.2, Math.min(3.0, airMultiplier));

        // 1. Combustion energy release with physical flame temperature boundary
        double combustionJoules = fuelState.step(deltaTicks, effectiveDraft);
        long totalMg = totalMass().milligrams();

        double absorbedCombustionJoules = 0.0;
        if (combustionJoules > 0.0 && !layers.isEmpty() && totalMg > 0L) {
            double baseFlame = fuelState.flameTemperatureK() > 0.0 ? fuelState.flameTemperatureK()
                : FLAME_TEMPERATURE_K;
            double effectiveFlameTemp = baseFlame + (effectiveDraft - 1.0) * 350.0;
            double effectiveConductance = COMBUSTION_CONDUCTANCE_W_PER_K * effectiveDraft;
            for (AggregateLayer layer : layers) {
                double fraction = (double) layer.mass()
                    .milligrams() / (double) totalMg;
                double currentTemp = layer.temperatureKelvin();
                double deltaTFlame = Math.max(0.0, effectiveFlameTemp - currentTemp);
                // Heat transfer rate bounded by (T_flame - T_charge) and fuel combustion energy
                double qTransferCap = (effectiveConductance * fraction * deltaTFlame) * deltaSeconds;
                double qIn = Math.min(combustionJoules * fraction, qTransferCap);
                if (qIn > 0.0) {
                    layer.thermalState()
                        .addEnergyJoules(qIn);
                    absorbedCombustionJoules += qIn;
                }
            }
        }
        // Combustion heat the charge could not absorb leaves with the flue gas.
        if (combustionJoules > absorbedCombustionJoules) {
            exhaustSink.ventEnergy(combustionJoules - absorbedCombustionJoules);
        }

        // 2. Heat exchange with ambient (Stefan-Boltzmann radiation + natural convection)
        if (!layers.isEmpty() && totalMg > 0L) {
            for (AggregateLayer layer : layers) {
                ActiveThermalState thermal = layer.thermalState();
                double temp = thermal.temperatureKelvin();
                if (temp > ambientTempK) {
                    double fraction = (double) layer.mass()
                        .milligrams() / (double) totalMg;
                    double layerArea = CRUCIBLE_EXPOSED_AREA_M2 * fraction;
                    double qRad = EMISSIVITY * STEFAN_BOLTZMANN_SIGMA
                        * layerArea
                        * (Math.pow(temp, 4.0) - Math.pow(ambientTempK, 4.0))
                        * deltaSeconds;
                    double qConv = CONVECTIVE_HEAT_COEFF * layerArea * (temp - ambientTempK) * deltaSeconds;
                    double qLoss = Math.min(thermal.energyJoules(), Math.max(0.0, qRad + qConv));
                    thermal.removeEnergyJoules(qLoss);
                    surroundingsHeatJoules += qLoss;
                }
            }
        }

        // 3. Interlayer heat exchange (Conduction between touching layers)
        if (layers.size() > 1) {
            for (int i = 0; i < layers.size() - 1; i++) {
                AggregateLayer l1 = layers.get(i);
                AggregateLayer l2 = layers.get(i + 1);
                double t1 = l1.temperatureKelvin();
                double t2 = l2.temperatureKelvin();
                double deltaT = t1 - t2;
                if (Math.abs(deltaT) > 0.1) {
                    double qExchange = INTERLAYER_CONDUCTANCE_W_PER_K * deltaT * deltaSeconds;
                    double c1 = l1.mass()
                        .toKilograms()
                        * l1.thermalState()
                            .properties()
                            .specificHeatSolid();
                    double c2 = l2.mass()
                        .toKilograms()
                        * l2.thermalState()
                            .properties()
                            .specificHeatSolid();
                    if (c1 > 0.0 && c2 > 0.0) {
                        double maxExchange = Math.abs(deltaT) * (c1 * c2) / (c1 + c2) * 0.5;
                        if (Math.abs(qExchange) > maxExchange) {
                            qExchange = deltaT > 0 ? maxExchange : -maxExchange;
                        }
                        if (qExchange > 0.0) {
                            l1.thermalState()
                                .removeEnergyJoules(qExchange);
                            l2.thermalState()
                                .addEnergyJoules(qExchange);
                        } else if (qExchange < 0.0) {
                            l1.thermalState()
                                .addEnergyJoules(-qExchange);
                            l2.thermalState()
                                .removeEnergyJoules(-qExchange);
                        }
                    }
                }
            }
        }

        // 4. Metallurgical process rules & reduction
        processSolidCharges();

        // 5. Molten mixing of compatible metallic layers
        mixMoltenMetalLayers();

        // 6. Layer ordering & sorting
        sortLayers();

        // 7. Refractory lining thermal stress
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
                    OreFeedstockDefinition oreDef = findFeedstock(layer);
                    boolean isRawOre = oreDef != null;

                    // Energy is authoritative: the charge's own thermal energy is partitioned by mass. The
                    // products keep whatever melt fraction that energy supports and absorb any remaining latent
                    // heat from the fire; the vented gas carries its share to the declared exhaust sink.
                    double chargeJoules = layer.thermalState()
                        .energyJoules();
                    long chargeMg = layer.mass()
                        .milligrams();
                    if (isRawOre) {
                        MaterialQuantity[] portions = oreDef.partition(layer.mass());
                        MaterialQuantity metalMass = portions[0];
                        MaterialQuantity slagMass = portions[1];
                        MaterialQuantity gasMass = portions[2];
                        double metalJoules = share(chargeJoules, metalMass, chargeMg);
                        double slagJoules = share(chargeJoules, slagMass, chargeMg);
                        double gasJoules = Math.max(0.0, chargeJoules - metalJoules - slagJoules);

                        exhaustSink.vent(ExhaustSink.EXHAUST_GAS, gasMass);
                        exhaustSink.ventEnergy(gasJoules);

                        if (!metalMass.isZero()) {
                            AggregateLayer metalLayer = new AggregateLayer(
                                oreDef.metallicProductRef(),
                                CanonicalMaterialKey.of(oreDef.metallicProductComposition())
                                    .asMolten(),
                                oreDef.metallicProductComposition(),
                                metalMass,
                                new ActiveThermalState(
                                    metalJoules,
                                    metalMass.toKilograms(),
                                    oreDef.metallicProperties()),
                                AggregatePhaseType.MOLTEN_METAL);
                            newLayers.add(metalLayer);
                        }

                        if (!slagMass.isZero()) {
                            AggregateLayer slagLayer = new AggregateLayer(
                                OreFeedstockDefinition.SLAG_REF,
                                CanonicalMaterialKey
                                    .of(CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID))
                                    .asMolten(),
                                CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID),
                                slagMass,
                                new ActiveThermalState(
                                    slagJoules,
                                    slagMass.toKilograms(),
                                    OreFeedstockDefinition.SLAG_PROPERTIES),
                                AggregatePhaseType.SLAG);
                            newLayers.add(slagLayer);
                        }
                    } else {
                        // Already refined metal or alloy keeps its exact thermal state; only its role changes.
                        AggregateLayer moltenLayer = new AggregateLayer(
                            layer.materialRef(),
                            layer.stateKey()
                                .asMolten(),
                            layer.composition(),
                            layer.mass(),
                            new ActiveThermalState(
                                chargeJoules,
                                layer.mass()
                                    .toKilograms(),
                                layer.thermalState()
                                    .properties()),
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

    private static double share(double joules, MaterialQuantity part, long wholeMg) {
        if (wholeMg <= 0L || part.isZero()) return 0.0;
        return joules * ((double) part.milligrams() / (double) wholeMg);
    }

    private static OreFeedstockDefinition findFeedstock(AggregateLayer layer) {
        if (layer.composition()
            .constituentCount() != 1) {
            return null;
        }
        ResourceId feedstockId = layer.composition()
            .constituents()
            .iterator()
            .next();
        return OreFeedstockDefinition.forFeedstockId(feedstockId);
    }

    private void mixMoltenMetalLayers() {
        List<AggregateLayer> moltenMetals = new ArrayList<>();
        for (AggregateLayer l : layers) {
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL
                && (l.physicalPhase() == MaterialPhase.LIQUID || l.thermalState()
                    .meltFraction() >= 0.50)) {
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
        MaterialRef mixedRef = resolveMixedRef(mixedComp, mixedKey);

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

    private static MaterialRef resolveMixedRef(CanonicalComposition comp, CanonicalMaterialKey key) {
        if (comp.constituentCount() == 1) {
            ResourceId res = comp.constituents()
                .iterator()
                .next();
            MaterialRef authored = MaterialRef.authored(res);
            if (naturalexceptions.content.BuiltinContent.materials()
                .getAuthored(authored) != null) {
                return authored;
            }
        }
        if (comp.equals(naturalexceptions.content.material.BuiltinMaterials.BRONZE_COMPOSITION)) {
            return naturalexceptions.content.material.BuiltinMaterials.BRONZE.ref();
        }
        return MaterialRef.generatedFrom(key);
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
        return pourLiquidMetal(null, desiredMass);
    }

    /**
     * Taps and extracts liquid metal of a specific material (or any liquid metal if null) from the vessel.
     * Cleanly partitions mass and thermal energy proportionally between the extracted layer and vessel layer.
     */
    public AggregateLayer pourLiquidMetal(MaterialRef requestedMaterial, MaterialQuantity desiredMass) {
        Objects.requireNonNull(desiredMass, "desiredMass");
        if (desiredMass.isZero()) {
            return null;
        }

        for (int i = 0; i < layers.size(); i++) {
            AggregateLayer l = layers.get(i);
            if (l.isPourable() && (requestedMaterial == null || requestedMaterial.equals(l.materialRef()))) {
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
            if (layer.phaseType() == AggregatePhaseType.SOLID_CHARGE
                && layer.temperatureKelvin() >= layer.thermalState()
                    .properties()
                    .meltingPointKelvin()) {
                return false;
            }
        }
        return true;
    }
}
