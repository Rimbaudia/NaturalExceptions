package naturalexceptions.core.fluid;

import java.util.Objects;
import java.math.BigDecimal;
import java.util.Map;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;

/** A finite quantity of canonical matter in liquid/spatial handling, with shared authoritative thermal energy. */
public final class FluidParcel {

    public static final long MICROLITRES_PER_LITRE = 1_000_000L;
    public static final long MICROLITRES_PER_CUBIC_METRE = 1_000_000_000L;

    private MaterialRef materialRef;
    private CanonicalMaterialKey stateKey;
    private final CanonicalComposition composition;
    private MaterialQuantity mass;
    private final ActiveThermalState thermalState;
    /** Optional durable history source, retained for impact parcels across cell/vessel serialization. */
    private DeterministicId provenanceId;
    private boolean consumed;

    public FluidParcel(MaterialRef materialRef, CanonicalMaterialKey stateKey, CanonicalComposition composition,
        MaterialQuantity mass, ActiveThermalState thermalState) {
        this(materialRef, stateKey, composition, mass, thermalState, null);
    }

    public FluidParcel(MaterialRef materialRef, CanonicalMaterialKey stateKey, CanonicalComposition composition,
        MaterialQuantity mass, ActiveThermalState thermalState, DeterministicId provenanceId) {
        this.materialRef = Objects.requireNonNull(materialRef, "materialRef");
        this.stateKey = Objects.requireNonNull(stateKey, "stateKey");
        this.composition = Objects.requireNonNull(composition, "composition");
        this.mass = Objects.requireNonNull(mass, "mass");
        this.thermalState = Objects.requireNonNull(thermalState, "thermalState");
        this.provenanceId = provenanceId;
        if (mass.isZero()) throw new IllegalArgumentException("Fluid parcel mass must be positive");
        if (!stateKey.composition().equals(composition)) {
            throw new IllegalArgumentException("Fluid state key and composition disagree");
        }
        if (MaterialQuantity.ofKilograms(thermalState.massKg()).milligrams() != mass.milligrams()) {
            throw new IllegalArgumentException("Fluid mass and thermal-state mass disagree");
        }
    }

    public MaterialRef materialRef() { return materialRef; }
    public CanonicalMaterialKey stateKey() { return stateKey; }
    public CanonicalComposition composition() { return composition; }
    public MaterialQuantity mass() { return consumed ? MaterialQuantity.ZERO : mass; }
    public ActiveThermalState thermalState() { return thermalState; }
    public synchronized DeterministicId provenanceId() { return provenanceId; }
    public MaterialPhase phase() { ensureLive(); return thermalState.phase(); }

    /** Lossless bridge from the existing smelter's molten matter representation. */
    public static FluidParcel fromAggregateLayer(AggregateLayer layer) {
        Objects.requireNonNull(layer, "layer");
        if (layer.physicalPhase() != MaterialPhase.LIQUID) {
            throw new IllegalArgumentException("Only a fully liquid aggregate layer can enter a fluid parcel");
        }
        CanonicalMaterialKey key = layer.stateKey().withPhase(MaterialPhase.LIQUID);
        return new FluidParcel(layer.materialRef(), key, layer.composition(), layer.mass(), layer.thermalState());
    }

    /** Returns a molten aggregate adapter for existing smelter/mold extraction paths. */
    public AggregateLayer toAggregateLayer() {
        reconcilePhase();
        return new AggregateLayer(materialRef, stateKey, composition, mass, thermalState,
            phase() == MaterialPhase.LIQUID ? AggregatePhaseType.MOLTEN_METAL : AggregatePhaseType.SOLID_CHARGE);
    }

    /** Rounded to the nearest microlitre; mass and joules remain authoritative and are never rounded away. */
    public long volumeMicrolitres() {
        if (consumed) return 0L;
        double density = thermalState.properties().density(phase()); // g / cm³ == g / mL
        if (!Double.isFinite(density) || density <= 0.0) {
            throw new IllegalStateException("Fluid density must be positive and finite: " + density);
        }
        double microlitres = mass.toGrams() * 1000.0 / density;
        if (!Double.isFinite(microlitres) || microlitres > Long.MAX_VALUE) {
            throw new ArithmeticException("Fluid volume is outside the quantized microlitre range");
        }
        return Math.max(1L, Math.round(microlitres));
    }

    /** Extracts a strict subset and transfers the proportional thermal energy; all integer mass stays accounted for. */
    public FluidParcel extract(MaterialQuantity extractedMass) {
        ensureLive();
        Objects.requireNonNull(extractedMass, "extractedMass");
        if (extractedMass.isZero() || extractedMass.compareTo(mass) >= 0) {
            throw new IllegalArgumentException("Partial extraction must be positive and smaller than parcel mass");
        }
        double fraction = (double) extractedMass.milligrams() / (double) mass.milligrams();
        double extractedEnergy = thermalState.energyJoules() * fraction;
        mass = mass.subtract(extractedMass);
        thermalState.setMassKg(mass.toKilograms());
        thermalState.removeEnergyJoules(extractedEnergy);
        return new FluidParcel(materialRef, stateKey, composition, extractedMass,
            new ActiveThermalState(extractedEnergy, extractedMass.toKilograms(), thermalState.properties()),
            provenanceId);
    }

    FluidParcel previewPortion(MaterialQuantity portionMass) {
        ensureLive();
        if (portionMass == null || portionMass.isZero() || portionMass.compareTo(mass) > 0)
            throw new IllegalArgumentException("Preview portion must fit within parcel mass");
        double energy = thermalState.energyJoules() * ((double) portionMass.milligrams() / mass.milligrams());
        return new FluidParcel(materialRef, stateKey, composition, portionMass,
            new ActiveThermalState(energy, portionMass.toKilograms(), thermalState.properties()), provenanceId);
    }

    /** Commits a portion after the receiving operation has succeeded. */
    void consume(MaterialQuantity portionMass) {
        ensureLive();
        if (portionMass == null || portionMass.isZero() || portionMass.compareTo(mass) > 0)
            throw new IllegalArgumentException("Consumed portion must fit within parcel mass");
        double energy = thermalState.energyJoules() * ((double) portionMass.milligrams() / mass.milligrams());
        if (portionMass.equals(mass)) {
            consumed = true; mass = MaterialQuantity.ZERO; thermalState.setEnergyJoules(0.0);
        } else {
            mass = mass.subtract(portionMass);
            thermalState.setMassKg(mass.toKilograms());
            thermalState.removeEnergyJoules(energy);
        }
    }

    /** Combines equal canonical matter without losing mass or energy. */
    void mergeSameMaterial(FluidParcel incoming) {
        ensureLive(); incoming.ensureLive();
        if (!materialRef.equals(incoming.materialRef) || !composition.equals(incoming.composition)) {
            throw new IllegalArgumentException("Only identical canonical matter can use direct merge");
        }
        mass = mass.add(incoming.mass);
        thermalState.setMassKg(mass.toKilograms());
        thermalState.addEnergyJoules(incoming.thermalState.energyJoules());
        if (!Objects.equals(provenanceId, incoming.provenanceId)) provenanceId = null;
    }

    /** Updates phase identity after shared thermal state has crossed a complete melt/freeze boundary. */
    public boolean reconcilePhase() {
        if (consumed) return false;
        MaterialPhase derived = thermalState.phase();
        if (stateKey.phase() == derived) return false;
        stateKey = stateKey.withPhase(derived);
        if (materialRef.isGenerated()) materialRef = MaterialRef.generatedFrom(stateKey);
        return true;
    }

    boolean isConsumed() { return consumed; }

    /** Transfers this whole parcel to a new owner and retires the caller's handle. */
    FluidParcel copyAndConsume() {
        ensureLive();
        FluidParcel copy = new FluidParcel(materialRef, stateKey, composition, mass,
            new ActiveThermalState(thermalState.energyJoules(), mass.toKilograms(), thermalState.properties()),
            provenanceId);
        consumed = true;
        mass = MaterialQuantity.ZERO;
        thermalState.setEnergyJoules(0.0);
        return copy;
    }

    private void ensureLive() {
        if (consumed) throw new IllegalStateException("Fluid parcel has already been transferred to another owner");
    }

    /** Canonical composition merge for a miscible pair; joules and milligrams are additive. */
    static FluidParcel mix(FluidParcel a, FluidParcel b) {
        MaterialQuantity total = a.mass.add(b.mass);
        CanonicalComposition.Builder compositionBuilder = CanonicalComposition.builder();
        addCompositionWeights(compositionBuilder, a);
        addCompositionWeights(compositionBuilder, b);
        CanonicalComposition mixedComposition = compositionBuilder.build();
        CanonicalMaterialKey mixedKey = CanonicalMaterialKey.of(mixedComposition, MaterialPhase.LIQUID,
            a.stateKey.microstructure(), a.stateKey.conditioning());
        MaterialRef mixedRef = MaterialRef.generatedFrom(mixedKey);
        MaterialProperties mixedProperties = new MaterialPropertyResolver().resolve(mixedKey).properties();
        ActiveThermalState mixedThermal = new ActiveThermalState(
            a.thermalState.energyJoules() + b.thermalState.energyJoules(), total.toKilograms(), mixedProperties);
        return new FluidParcel(mixedRef, mixedKey, mixedComposition, total, mixedThermal);
    }

    private static void addCompositionWeights(CanonicalComposition.Builder builder, FluidParcel parcel) {
        for (Map.Entry<naturalexceptions.core.id.ResourceId, Integer> constituent : parcel.composition.asMap().entrySet()) {
            BigDecimal weight = BigDecimal.valueOf(parcel.mass.milligrams())
                .multiply(BigDecimal.valueOf(constituent.getValue()));
            builder.add(constituent.getKey(), weight);
        }
    }
}
